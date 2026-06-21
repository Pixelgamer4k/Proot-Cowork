package com.proot.cowork.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proot.cowork.data.prefs.RootfsState
import com.proot.cowork.data.prefs.SettingsRepository
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.ExecutionMode
import com.proot.cowork.domain.agent.MessageRole
import com.proot.cowork.domain.agent.SwarmTask
import com.proot.cowork.domain.proot.DesktopState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val desktopState: DesktopState = DesktopState.NO_ROOTFS,
    val importProgress: Float = 0f,
    val distroName: String = "",
    val messages: List<AgentMessage> = emptyList(),
    val swarmTasks: List<SwarmTask> = emptyList(),
    val executionMode: ExecutionMode = ExecutionMode.PLAN,
    val inputText: String = "",
    val isExecuting: Boolean = false,
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val localState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.rootfsState,
        localState,
    ) { rootfs, local ->
        local.copy(
            desktopState = when {
                rootfs.isImporting -> DesktopState.IMPORTING
                rootfs.isInstalled -> {
                    if (local.desktopState == DesktopState.STOPPED) DesktopState.STOPPED
                    else DesktopState.RUNNING
                }
                else -> DesktopState.NO_ROOTFS
            },
            importProgress = rootfs.importProgress,
            distroName = rootfs.distroName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onImportRootfsRequested() {
        // Phase 2: wire to SAF file picker via Activity result
        viewModelScope.launch {
            localState.update {
                it.copy(
                    messages = it.messages + AgentMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.SYSTEM,
                        content = "Rootfs import: use Settings or tap Import to select a .tar.gz file. " +
                            "See rootfs-setup/README.md for build instructions.",
                    ),
                )
            }
        }
    }

    fun onPowerOff() {
        localState.update { it.copy(desktopState = DesktopState.STOPPED) }
    }

    fun onReboot() {
        localState.update { it.copy(desktopState = DesktopState.STARTING) }
        // Phase 2: restart ProotDesktopService
    }

    fun onScreenshot() {
        viewModelScope.launch {
            val path = settingsRepository.getArtifactsDir().resolve("screenshot_${System.currentTimeMillis()}.png")
            localState.update {
                it.copy(
                    messages = it.messages + AgentMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.SYSTEM,
                        content = "Screenshot saved to: ${path.absolutePath} (Phase 2: X11 capture)",
                    ),
                )
            }
        }
    }

    fun onModeChange(mode: ExecutionMode) {
        localState.update { it.copy(executionMode = mode) }
    }

    fun onInputChange(text: String) {
        localState.update { it.copy(inputText = text) }
    }

    fun onSend() {
        val text = localState.value.inputText.trim()
        if (text.isEmpty() || localState.value.isExecuting) return

        viewModelScope.launch {
            val userMsg = AgentMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.USER,
                content = text,
            )
            localState.update {
                it.copy(
                    inputText = "",
                    isExecuting = true,
                    messages = it.messages + userMsg,
                )
            }

            val mode = localState.value.executionMode
            val response = when (mode) {
                ExecutionMode.PLAN -> buildPlanResponse(text)
                ExecutionMode.DIRECT -> "Executing directly: \"$text\"\n\n(Phase 3: Koog agent will run tools in proot)"
                ExecutionMode.SCHEDULE -> "Scheduled for later. (Phase 5: date/time picker + WorkManager)"
            }

            localState.update {
                it.copy(
                    isExecuting = false,
                    messages = it.messages + AgentMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.ASSISTANT,
                        content = response,
                    ),
                    swarmTasks = if (mode == ExecutionMode.PLAN) demoSwarmTasks(text) else emptyList(),
                )
            }
        }
    }

    private fun buildPlanResponse(task: String): String {
        return """
            |## Plan for: $task
            |
            |1. **Analyze** — understand requirements and constraints
            |2. **Decompose** — split into parallel subtasks for swarm agents
            |3. **Execute** — run in proot desktop (shell, files, browser tools)
            |4. **Verify** — check outputs and capture screenshot
            |5. **Learn** — offer to save workflow as SKILL.md
            |
            |Tap **Execute** to approve this plan, or edit your request.
            |(Phase 3: real LLM planning via Koog + OpenRouter)
        """.trimMargin()
    }

    private fun demoSwarmTasks(task: String): List<SwarmTask> = listOf(
        SwarmTask("1", "Analyze: $task"),
        SwarmTask("2", "Prepare proot environment"),
        SwarmTask("3", "Execute subtasks in parallel", children = listOf(
            SwarmTask("3a", "Shell commands"),
            SwarmTask("3b", "File operations"),
        )),
        SwarmTask("4", "Verify and report"),
    )

    fun onOpenTerminal() {
        localState.update {
            it.copy(
                messages = it.messages + AgentMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.SYSTEM,
                    content = "External terminal (Phase 5): PTY overlay to proot shell",
                ),
            )
        }
    }

    fun onOpenBrowser() {
        localState.update {
            it.copy(
                messages = it.messages + AgentMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.SYSTEM,
                    content = "File browser (Phase 5): artifacts at ${settingsRepository.getArtifactsDir()}",
                ),
            )
        }
    }

    fun onOpenSkills() {
        localState.update {
            it.copy(
                messages = it.messages + AgentMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.SYSTEM,
                    content = "Skills manager (Phase 4): agentskills.io SKILL.md at ${settingsRepository.getSkillsDir()}",
                ),
            )
        }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(settingsRepository) as T
            }
        }
    }
}
