package com.proot.cowork.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proot.cowork.data.llm.OpenAiCompatibleLlmClient
import com.proot.cowork.data.prefs.LlmConfig
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.data.proot.ProotGuestShellExecutor
import com.proot.cowork.data.prootcontainer.ProotContainerRepository
import com.proot.cowork.domain.agent.DEFAULT_MAX_AGENT_POOL
import com.proot.cowork.domain.agent.DEFAULT_MAX_TOOL_CALLS
import com.proot.cowork.domain.desktop.TERMUX_STACK_DESKTOP
import com.proot.cowork.domain.proot.DesktopSession
import com.proot.cowork.domain.proot.DesktopState
import com.proot.cowork.termux.proot.ProotXfceLauncher
import com.proot.cowork.termux.x11.X11DisplayConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val savedMessage: String? = null,
    val isTestingConnection: Boolean = false,
    val connectionTestMessage: String? = null,
    val rootfsInstalled: Boolean = false,
    val distroName: String = "ubuntu",
    val isDeletingRootfs: Boolean = false,
    val deleteMessage: String? = null,
    val containerRunning: Boolean = false,
    val cpuLabel: String = "—",
    val memoryLabel: String = "—",
    val uptimeLabel: String = "—",
    val displayLabel: String = X11DisplayConfig.DISPLAY,
    val maxAgentPool: Int = DEFAULT_MAX_AGENT_POOL,
    val maxToolCalls: Int = DEFAULT_MAX_TOOL_CALLS,
    val slackWebhookUrl: String = "",
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val prootContainerRepository: ProotContainerRepository,
    private val guestShell: ProotGuestShellExecutor,
) : ViewModel() {

    private val formState = MutableStateFlow(LlmFormState())
    private val agentFormState = MutableStateFlow(AgentFormState())
    private val savedMessage = MutableStateFlow<String?>(null)
    private val connectionTestMessage = MutableStateFlow<String?>(null)
    private val isTestingConnection = MutableStateFlow(false)
    private val isDeletingRootfs = MutableStateFlow(false)
    private val deleteMessage = MutableStateFlow<String?>(null)
    private val healthState = MutableStateFlow(
        HealthSnapshot(
            containerRunning = false,
            cpuLabel = "—",
            memoryLabel = "—",
            uptimeLabel = "—",
        ),
    )
    private var healthJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.llmConfig,
            settingsRepository.agentSettings,
            settingsRepository.rootfsState,
            formState,
            agentFormState,
        ) { config, agent, rootfs, form, agentForm ->
            Quintuple(config, agent, rootfs, form, agentForm)
        },
        combine(
            savedMessage,
            connectionTestMessage,
            isTestingConnection,
            isDeletingRootfs,
            deleteMessage,
        ) { saved, testMsg, testing, deleting, delMsg ->
            listOf(saved, testMsg, testing, deleting, delMsg)
        },
        healthState,
    ) { packed, flags, health ->
        val config = packed.first
        val agent = packed.second
        val rootfs = packed.third
        val form = packed.fourth
        val agentForm = packed.fifth
        SettingsUiState(
            baseUrl = form.baseUrl.ifEmpty { config.baseUrl },
            apiKey = form.apiKey.ifEmpty { config.apiKey },
            model = form.model.ifEmpty { config.model },
            savedMessage = flags[0] as String?,
            connectionTestMessage = flags[1] as String?,
            isTestingConnection = flags[2] as Boolean,
            isDeletingRootfs = flags[3] as Boolean,
            deleteMessage = flags[4] as String?,
            rootfsInstalled = rootfs.isInstalled,
            distroName = rootfs.distroName,
            containerRunning = health.containerRunning,
            cpuLabel = health.cpuLabel,
            memoryLabel = health.memoryLabel,
            uptimeLabel = health.uptimeLabel,
            maxAgentPool = agentForm.maxAgentPool ?: agent.maxAgentPool,
            maxToolCalls = agentForm.maxToolCalls ?: agent.maxToolCalls,
            slackWebhookUrl = agentForm.slackWebhookUrl.ifEmpty { agent.slackWebhookUrl },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        startHealthPolling()
    }

    fun onBaseUrlChange(value: String) {
        formState.update { it.copy(baseUrl = value) }
    }

    fun onApiKeyChange(value: String) {
        formState.update { it.copy(apiKey = value) }
    }

    fun onModelChange(value: String) {
        formState.update { it.copy(model = value) }
    }

    fun onMaxAgentPoolChange(value: String) {
        val parsed = value.filter { it.isDigit() }.toIntOrNull()
        agentFormState.update { it.copy(maxAgentPool = parsed) }
    }

    fun onMaxToolCallsChange(value: String) {
        val parsed = value.filter { it.isDigit() }.toIntOrNull()
        agentFormState.update { it.copy(maxToolCalls = parsed) }
    }

    fun onSlackWebhookChange(value: String) {
        agentFormState.update { it.copy(slackWebhookUrl = value) }
    }

    fun save() {
        viewModelScope.launch {
            val state = uiState.value
            settingsRepository.saveLlmConfig(state.baseUrl, state.apiKey, state.model)
            settingsRepository.saveAgentSettings(
                maxAgentPool = state.maxAgentPool,
                maxToolCalls = state.maxToolCalls,
                slackWebhookUrl = state.slackWebhookUrl,
            )
            formState.value = LlmFormState()
            agentFormState.value = AgentFormState()
            savedMessage.value = "Settings saved"
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val state = uiState.value
            isTestingConnection.value = true
            connectionTestMessage.value = null
            val result = OpenAiCompatibleLlmClient.testConnection(
                LlmConfig(
                    baseUrl = state.baseUrl,
                    apiKey = state.apiKey,
                    model = state.model,
                ),
            )
            connectionTestMessage.value = result.fold(
                onSuccess = { it },
                onFailure = { "Connection failed: ${it.message}" },
            )
            isTestingConnection.value = false
        }
    }

    fun deleteRootfs() {
        viewModelScope.launch {
            isDeletingRootfs.value = true
            deleteMessage.value = null
            val ok = if (TERMUX_STACK_DESKTOP) {
                prootContainerRepository.deleteContainer()
            } else {
                runCatching {
                    settingsRepository.clearRootfsInstalled()
                    DesktopSession.setState(DesktopState.NO_ROOTFS)
                    true
                }.getOrDefault(false)
            }
            deleteMessage.value = if (ok) "Container removed" else "Delete failed"
            isDeletingRootfs.value = false
        }
    }

    fun clearSavedMessage() {
        savedMessage.value = null
    }

    fun clearConnectionTestMessage() {
        connectionTestMessage.value = null
    }

    fun clearDeleteMessage() {
        deleteMessage.value = null
    }

    private fun startHealthPolling() {
        healthJob?.cancel()
        healthJob = viewModelScope.launch {
            while (isActive) {
                refreshHealth()
                delay(5_000)
            }
        }
    }

    private suspend fun refreshHealth() {
        val installed = prootContainerRepository.isInstalled()
        val running = installed && ProotXfceLauncher.isRunning()
        if (!installed || !running) {
            healthState.value = HealthSnapshot(
                containerRunning = false,
                cpuLabel = "—",
                memoryLabel = "—",
                uptimeLabel = "—",
            )
            return
        }
        val result = guestShell.run(
            """
            python3 -c '
import time
with open("/proc/stat") as f:
    parts = f.readline().split()[1:8]
    total = sum(int(x) for x in parts)
    idle = int(parts[3])
cpu = max(0.0, min(100.0, (1 - idle / total) * 100)) if total else 0.0
mem = {}
with open("/proc/meminfo") as f:
    for line in f:
        if ":" in line:
            k, v = line.split(":", 1)
            mem[k.strip()] = int(v.split()[0])
used = (mem.get("MemTotal", 0) - mem.get("MemAvailable", 0)) // 1024
total = mem.get("MemTotal", 0) // 1024
with open("/proc/uptime") as f:
    up = int(float(f.read().split()[0]))
h, rem = divmod(up, 3600)
m, s = divmod(rem, 60)
print(f"CPU:{cpu:.1f}%|MEM:{used}/{total} MB|UP:{h:02d}:{m:02d}:{s:02d}")
'
            """.trimIndent(),
            timeoutMs = 20_000L,
        )
        if (!result.success) {
            healthState.value = HealthSnapshot(containerRunning = running, cpuLabel = "—", memoryLabel = "—", uptimeLabel = "—")
            return
        }
        val line = result.output.lines().lastOrNull { it.contains('|') }.orEmpty()
        val cpu = line.substringAfter("CPU:").substringBefore("|").ifBlank { "—" }
        val mem = line.substringAfter("MEM:").substringBefore("|").ifBlank { "—" }
        val up = line.substringAfter("UP:").ifBlank { "—" }
        healthState.value = HealthSnapshot(
            containerRunning = running,
            cpuLabel = cpu,
            memoryLabel = "$mem MB",
            uptimeLabel = up,
        )
    }

    private data class HealthSnapshot(
        val containerRunning: Boolean,
        val cpuLabel: String,
        val memoryLabel: String,
        val uptimeLabel: String,
    )

    private data class LlmFormState(
        val baseUrl: String = "",
        val apiKey: String = "",
        val model: String = "",
    )

    private data class AgentFormState(
        val maxAgentPool: Int? = null,
        val maxToolCalls: Int? = null,
        val slackWebhookUrl: String = "",
    )

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
    )

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            prootContainerRepository: ProotContainerRepository,
            guestShell: ProotGuestShellExecutor,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, prootContainerRepository, guestShell) as T
            }
        }
    }
}
