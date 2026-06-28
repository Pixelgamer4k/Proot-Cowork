package com.proot.cowork.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.proot.cowork.MainActivity
import com.proot.cowork.ProotCoworkApp
import com.proot.cowork.R
import com.proot.cowork.data.llm.LlmEndpoint
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.domain.agent.AgentExecutionSession
import com.proot.cowork.domain.agent.AgentRunContext
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.CoworkAgentRunner
import com.proot.cowork.domain.agent.AgentRunController
import com.proot.cowork.domain.agent.DEFAULT_MAX_AGENT_POOL
import com.proot.cowork.domain.agent.DEFAULT_MAX_TOOL_CALLS
import com.proot.cowork.domain.agent.ExecutionMode
import com.proot.cowork.domain.agent.MessageRole
import com.proot.cowork.domain.agent.TaskPlan
import com.proot.cowork.domain.agent.ToolLimitReachedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray

class AgentExecutionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runJob: Job? = null
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var runner: CoworkAgentRunner

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        runner = CoworkAgentRunner(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AgentExecutionSession.onStopRequested()
                AgentExecutionSession.markRunningShellCommandsCancelled()
                runJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CANCEL_SUBTASK -> {
                val taskId = intent.getStringExtra(EXTRA_SUBTASK_ID) ?: return START_NOT_STICKY
                AgentExecutionSession.cancelSubtask(taskId)
                updateNotification("Cancelling step $taskId…")
                return START_STICKY
            }
            ACTION_EXECUTE_SWARM -> {
                val planJson = intent.getStringExtra(EXTRA_PLAN_JSON) ?: return START_NOT_STICKY
                val historyJson = intent.getStringExtra(EXTRA_HISTORY_JSON).orEmpty()
                val maxPool = intent.getIntExtra(EXTRA_MAX_POOL, DEFAULT_MAX_AGENT_POOL)
                val maxTools = intent.getIntExtra(EXTRA_MAX_TOOL_CALLS, DEFAULT_MAX_TOOL_CALLS)
                beginForeground("Swarm executing…")
                executeSwarm(parsePlan(planJson), parseHistory(historyJson), maxPool, maxTools)
            }
            ACTION_EXECUTE_FAST -> {
                val task = intent.getStringExtra(EXTRA_USER_TASK) ?: return START_NOT_STICKY
                val historyJson = intent.getStringExtra(EXTRA_HISTORY_JSON).orEmpty()
                val maxTools = intent.getIntExtra(EXTRA_MAX_TOOL_CALLS, DEFAULT_MAX_TOOL_CALLS)
                val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
                beginForeground("Cowork agent running…")
                executeFast(task, parseHistory(historyJson), maxTools, threadId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun executeSwarm(plan: TaskPlan, history: List<AgentMessage>, maxPool: Int, maxToolCalls: Int) {
        runJob?.cancel()
        runJob = scope.launch {
            AgentExecutionSession.resetForNewRun(ExecutionMode.SWARM, maxPool, maxToolCalls)
            AgentExecutionSession.clearApproval()
            val config = settingsRepository.getLlmConfigSnapshot()
            val assistantId = AgentExecutionSession.newMessageId()
            AgentExecutionSession.appendMessage(
                AgentMessage(assistantId, MessageRole.ASSISTANT, ""),
            )
            try {
                runner.executeSwarmPlan(
                    config = config,
                    plan = plan,
                    history = history,
                    maxPool = maxPool,
                    isActive = ::isRunActive,
                    onSubtaskUpdate = AgentExecutionSession::updateSwarmTasks,
                    onAgentStates = AgentExecutionSession::updateAgentStates,
                    onAssistantMessage = { text ->
                        AgentExecutionSession.updateMessage(assistantId) { it.copy(content = text) }
                    },
                    onAssistantDelta = { delta ->
                        AgentExecutionSession.updateMessage(assistantId) { it.copy(content = it.content + delta) }
                    },
                    onToolEvent = { msg ->
                        val existing = AgentExecutionSession.snapshot.value.messages.any { it.id == msg.id }
                        if (existing) {
                            AgentExecutionSession.updateMessage(msg.id) { msg }
                        } else {
                            AgentExecutionSession.appendMessage(msg)
                        }
                        updateNotification("${msg.agentName ?: "Agent"} → ${msg.toolName}")
                    },
                )
            } catch (e: ToolLimitReachedException) {
                val limit = AgentExecutionSession.snapshot.value.maxToolCalls
                AgentExecutionSession.onStopRequested("Tool call limit ($limit) reached")
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Swarm stopped: tool call limit ($limit) reached.",
                    ),
                )
            } catch (e: CancellationException) {
                AgentExecutionSession.onStopRequested("Swarm cancelled")
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Swarm run cancelled.",
                    ),
                )
            } catch (e: Exception) {
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Swarm failed: ${e.message}",
                    ),
                )
            } finally {
                AgentExecutionSession.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun executeFast(task: String, history: List<AgentMessage>, maxToolCalls: Int, threadId: String?) {
        runJob?.cancel()
        runJob = scope.launch {
            AgentExecutionSession.resetForNewRun(ExecutionMode.FAST, maxToolCalls = maxToolCalls)
            AgentRunContext.reset(threadId)
            val config = settingsRepository.getLlmConfigSnapshot()
            val assistantId = AgentExecutionSession.newMessageId()
            AgentExecutionSession.appendMessage(
                AgentMessage(assistantId, MessageRole.ASSISTANT, ""),
            )
            try {
                if (!LlmEndpoint.isConfigured(config)) {
                    throw IllegalStateException("API not configured")
                }
                runner.runFast(
                    config = config,
                    userTask = task,
                    history = history,
                    threadId = threadId,
                    isActive = ::isRunActive,
                    onAssistantDelta = { delta ->
                        AgentExecutionSession.updateMessage(assistantId) { it.copy(content = it.content + delta) }
                    },
                    onToolEvent = { msg ->
                        val existing = AgentExecutionSession.snapshot.value.messages.any { it.id == msg.id }
                        if (existing) AgentExecutionSession.updateMessage(msg.id) { msg }
                        else AgentExecutionSession.appendMessage(msg)
                        updateNotification("Agent → ${msg.toolName}")
                    },
                    onSystemNotice = { notice ->
                        AgentExecutionSession.appendMessage(
                            AgentMessage(
                                AgentExecutionSession.newMessageId(),
                                MessageRole.SYSTEM,
                                notice,
                            ),
                        )
                    },
                )
            } catch (e: ToolLimitReachedException) {
                val limit = AgentExecutionSession.snapshot.value.maxToolCalls
                AgentExecutionSession.onStopRequested("Tool call limit ($limit) reached")
                AgentExecutionSession.updateMessage(assistantId) { msg ->
                    val bad = msg.content.isBlank() || msg.content.replace("null", "").isBlank()
                    if (bad) {
                        msg.copy(
                            content = "Stopped after reaching the tool call limit ($limit). " +
                                "Check ${com.proot.cowork.data.files.GuestPaths.AGENT_OUTPUT_DIR} for partial files.",
                        )
                    } else {
                        msg.copy(content = msg.content + "\n\n*(Stopped: tool call limit $limit reached.)*")
                    }
                }
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Fast run stopped: tool call limit ($limit) reached.",
                    ),
                )
            } catch (e: CancellationException) {
                AgentExecutionSession.onStopRequested("Fast agent cancelled")
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Fast run cancelled.",
                    ),
                )
            } catch (e: Exception) {
                val message = e.message ?: "Fast run failed"
                AgentExecutionSession.appendMessage(
                    AgentMessage(
                        AgentExecutionSession.newMessageId(),
                        MessageRole.SYSTEM,
                        "Fast run failed: $message",
                    ),
                )
            } finally {
                AgentRunContext.reset(null)
                AgentExecutionSession.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun isRunActive(): Boolean =
        runJob?.isActive == true && AgentRunController.isActive()

    private fun beginForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        AgentExecutionSession.setNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, AgentExecutionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ProotCoworkApp.CHANNEL_AGENT)
            .setContentTitle(getString(R.string.agent_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.stop_agent), stop)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_EXECUTE_SWARM = "com.proot.cowork.action.EXECUTE_SWARM"
        const val ACTION_EXECUTE_FAST = "com.proot.cowork.action.EXECUTE_FAST"
        const val ACTION_STOP = "com.proot.cowork.action.STOP_AGENT"
        const val ACTION_CANCEL_SUBTASK = "com.proot.cowork.action.CANCEL_SUBTASK"
        const val EXTRA_PLAN_JSON = "plan_json"
        const val EXTRA_USER_TASK = "user_task"
        const val EXTRA_HISTORY_JSON = "history_json"
        const val EXTRA_MAX_POOL = "max_pool"
        const val EXTRA_MAX_TOOL_CALLS = "max_tool_calls"
        const val EXTRA_SUBTASK_ID = "subtask_id"
        const val EXTRA_THREAD_ID = "thread_id"
        private const val NOTIFICATION_ID = 77

        fun startSwarm(context: Context, plan: TaskPlan, history: List<AgentMessage>, maxPool: Int, maxToolCalls: Int) {
            context.startForegroundService(
                Intent(context, AgentExecutionService::class.java).apply {
                    action = ACTION_EXECUTE_SWARM
                    putExtra(EXTRA_PLAN_JSON, planToJson(plan).toString())
                    putExtra(EXTRA_HISTORY_JSON, historyToJson(history).toString())
                    putExtra(EXTRA_MAX_POOL, maxPool)
                    putExtra(EXTRA_MAX_TOOL_CALLS, maxToolCalls)
                },
            )
        }

        fun startFast(
            context: Context,
            task: String,
            history: List<AgentMessage>,
            maxToolCalls: Int,
            threadId: String?,
        ) {
            context.startForegroundService(
                Intent(context, AgentExecutionService::class.java).apply {
                    action = ACTION_EXECUTE_FAST
                    putExtra(EXTRA_USER_TASK, task)
                    putExtra(EXTRA_HISTORY_JSON, historyToJson(history).toString())
                    putExtra(EXTRA_MAX_TOOL_CALLS, maxToolCalls)
                    putExtra(EXTRA_THREAD_ID, threadId)
                },
            )
        }

        fun stop(context: Context) {
            AgentRunController.requestStop()
            context.startService(
                Intent(context, AgentExecutionService::class.java).setAction(ACTION_STOP),
            )
        }

        fun cancelSubtask(context: Context, taskId: String) {
            context.startService(
                Intent(context, AgentExecutionService::class.java).apply {
                    action = ACTION_CANCEL_SUBTASK
                    putExtra(EXTRA_SUBTASK_ID, taskId)
                },
            )
        }

        private fun planToJson(plan: TaskPlan) = org.json.JSONObject().apply {
            put("id", plan.id)
            put("summary", plan.summary)
            put("userTask", plan.userTask)
            put("subtasks", org.json.JSONArray().apply {
                plan.subtasks.forEach { t ->
                    put(
                        org.json.JSONObject()
                            .put("id", t.id)
                            .put("title", t.title)
                            .put("agent", t.agent.name)
                            .put("parallelizable", t.parallelizable)
                            .put("status", t.status.name),
                    )
                }
            })
        }

        private fun parsePlan(json: String): TaskPlan {
            val obj = org.json.JSONObject(json)
            val subtasks = obj.optJSONArray("subtasks")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val item = arr.getJSONObject(i)
                    com.proot.cowork.domain.agent.SwarmTask(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        agent = com.proot.cowork.domain.agent.SwarmAgentType.fromString(item.optString("agent")),
                        parallelizable = item.optBoolean("parallelizable", true),
                    )
                }
            }.orEmpty()
            return TaskPlan(
                id = obj.optString("id"),
                summary = obj.optString("summary"),
                userTask = obj.optString("userTask"),
                subtasks = subtasks,
            )
        }

        private fun historyToJson(history: List<AgentMessage>): JSONArray = JSONArray().apply {
            history.forEach { msg ->
                put(
                    org.json.JSONObject()
                        .put("id", msg.id)
                        .put("role", msg.role.name)
                        .put("content", msg.content),
                )
            }
        }

        private fun parseHistory(json: String): List<AgentMessage> {
            if (json.isBlank()) return emptyList()
            val arr = JSONArray(json)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AgentMessage(
                    id = o.optString("id"),
                    role = MessageRole.valueOf(o.optString("role", "USER")),
                    content = o.optString("content"),
                )
            }
        }
    }
}
