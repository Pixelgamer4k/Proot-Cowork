# Proot Cowork — Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Proot Cowork Android App                     │
├──────────────────────────────┬──────────────────────────────────┤
│   UI Layer (Jetpack Compose) │                                  │
│  ┌────────────────────────┐  │  ┌────────────────────────────┐  │
│  │  Desktop View (16:9)   │  │  │  Agent Chat / Task Panel │  │
│  │  X11 SurfaceView       │  │  │  Plan · Execute · Schedule │  │
│  │  or "Add rootfs" CTA   │  │  │  Swarm progress tree       │  │
│  └────────────────────────┘  │  └────────────────────────────┘  │
├──────────────────────────────┴──────────────────────────────────┤
│                        Domain Layer                              │
│  ┌─────────────┐ ┌──────────────┐ ┌───────────┐ ┌─────────────┐ │
│  │ Orchestrator│ │ SubAgentPool │ │ SkillStore│ │ TaskScheduler│ │
│  │ (Koog)      │ │ (max 8)      │ │ SKILL.md  │ │ WorkManager  │ │
│  └─────────────┘ └──────────────┘ └───────────┘ └─────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      Infrastructure Layer                        │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐ │
│  │ ProotService │ │ X11Service   │ │ LlmClient (OpenAI-compat)│ │
│  │ rootfs mgmt  │ │ termux-x11   │ │ OpenRouter / custom      │ │
│  └──────────────┘ └──────────────┘ └────────────────────────┘ │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐ │
│  │ ShellTool    │ │ FileBrowser  │ │ TerminalSession (PTY)  │ │
│  │ proot exec   │ │ artifacts    │ │ manual override          │ │
│  └──────────────┘ └──────────────┘ └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
app/                          # Main Android application
├── ui/
│   ├── desktop/              # X11 SurfaceView, rootfs import, power controls
│   ├── agent/                # Chat, plan review, swarm tree, mode selector
│   ├── browser/              # In-app file/artifact browser
│   ├── terminal/             # External terminal overlay
│   └── settings/             # API key, model, skills manager
├── domain/
│   ├── agent/                # Orchestrator, sub-agents, modes
│   ├── skills/               # Skill discovery, manage, self-improve
│   └── schedule/             # Deferred task execution
├── data/
│   ├── llm/                  # OpenAI-compatible client (Koog adapter)
│   ├── proot/                # Rootfs import, lifecycle, proot runner
│   └── prefs/                # EncryptedSharedPreferences for API keys
└── service/
    ├── ProotDesktopService   # Foreground service for proot + X11
    └── AgentExecutionService # Long-running agent tasks
```

## Core Components

### 1. Proot Desktop (16:9 upper panel)

**States:**
- `NO_ROOTFS` — blank screen + "Add your rootfs" button
- `IMPORTING` — progress bar for tarball extraction
- `STARTING` — X11 boot + proot login + desktop session
- `RUNNING` — live X11 display in SurfaceView
- `STOPPED` — powered off, show restart button

**Startup sequence (enforced):**
1. Extract/import rootfs to `files/rootfs/`
2. Start X11 server (embedded termux-x11)
3. `proot -r files/rootfs/ ... --shared-tmp` with `DISPLAY=:0`
4. Run guest `start-desktop.sh` (user-provided in rootfs)

**Controls:** Power off, Reboot, Screenshot (screencap from X11 framebuffer)

### 2. Agent Orchestrator

Built on Koog `AIAgent` with custom `SwarmStrategy`:

```kotlin
// Pseudocode
class SwarmOrchestrator {
    suspend fun execute(task: String, mode: ExecutionMode) {
        when (mode) {
            PLAN -> {
                val plan = plannerAgent.decompose(task)
                ui.showPlanForApproval(plan)
                // wait for user approval
                executePlan(plan)
            }
            DIRECT -> executePlan(plannerAgent.decompose(task))
            SCHEDULED -> scheduler.enqueue(task, dateTime)
        }
    }

    private suspend fun executePlan(plan: TaskPlan) {
        val subtasks = plan.subtasks.filter { it.parallelizable }
        subtasks.chunked(MAX_PARALLEL_AGENTS).forEach { batch ->
            coroutineScope {
                batch.map { subtask ->
                    async { subAgentPool.execute(subtask) }
                }.awaitAll()
            }
        }
        orchestrator.aggregate(results)
    }
}
```

**Sub-agent routing:**
| Subtask type | Executor | Tools |
|--------------|----------|-------|
| Shell/command | ProotShellTool | bash in rootfs |
| File ops | FileSystemTool | proot files + app artifacts |
| Research | WebTool | in-guest browser or HTTP |
| Code | CodeTool | edit files + run tests in proot |

### 3. Skills System (Hermes-inspired)

- Storage: `files/skills/<name>/SKILL.md`
- Discovery: parse frontmatter at app start → inject names into system prompt
- Tools: `skills_list`, `skill_view`, `skill_manage` (with approval gate)
- Self-improve: after task with 5+ tool calls, prompt user to save as skill

### 4. LLM Configuration

```kotlin
data class LlmConfig(
    val baseUrl: String,      // e.g. https://openrouter.ai/api/v1
    val apiKey: String,        // EncryptedSharedPreferences
    val model: String,        // e.g. openrouter/owl-alpha
    val maxTokens: Int = 8192,
)
```

Koog `PromptExecutor` configured for OpenAI-compatible endpoints.

### 5. Execution Modes

| Mode | Behavior | UI |
|------|----------|-----|
| Plan | Decompose → show plan → approve → execute | Editable task tree |
| Direct | Decompose → execute immediately | Live progress only |
| Schedule | Queue for date/time | Calendar + time picker |

### 6. External Tools

- **File Browser:** Storage Access Framework + proot `files/` mount for artifacts
- **Terminal:** Full-screen PTY overlay connected to proot shell
- **Screenshot:** Capture X11 framebuffer → save to `files/artifacts/`

---

## Data Flow: User Task → Proot Execution

```
User: "Install nginx and create a hello page"
    │
    ▼
Orchestrator (Plan Mode)
    ├── Sub-agent 1: apt install nginx (ProotShellTool)
    ├── Sub-agent 2: write index.html (FileSystemTool)  } parallel
    └── Sub-agent 3: research nginx config (WebTool)
    │
    ▼
Aggregate → verify nginx running → offer skill save
    │
    ▼
UI: show results + artifact links + desktop screenshot
```

---

## Phased Implementation

### Phase 1 — Foundation (current)
- [x] Research + architecture docs
- [ ] Jetpack Compose shell with 16:9 desktop placeholder
- [ ] Settings screen (API URL, key, model)
- [ ] GitHub Actions debug APK build
- [ ] Rootfs import UI (file picker + extract)

### Phase 2 — Proot Desktop
- [ ] Bundle proot binary (arm64-v8a)
- [ ] Embed termux-x11 or SurfaceView stub
- [ ] ProotService lifecycle (start/stop/reboot)
- [ ] `rootfs-setup/` scripts for user to build custom rootfs
- [ ] Screenshot capture

### Phase 3 — Agent Core
- [ ] Koog integration with OpenAI-compatible API
- [ ] Basic chat UI with streaming
- [ ] ProotShellTool + FileSystemTool
- [ ] Plan mode with approval UI
- [ ] Direct execution mode

### Phase 4 — Swarm + Skills
- [ ] SubAgentPool (parallel execution, max 8)
- [ ] Swarm progress tree UI
- [ ] Skills discovery + skill_view/manage tools
- [ ] Self-improvement prompt after complex tasks

### Phase 5 — Schedule + Extras
- [ ] Schedule mode (WorkManager + date/time picker)
- [ ] External file browser for artifacts
- [ ] External terminal overlay
- [ ] Power off / reboot controls

### Phase 6 — Polish
- [ ] Error recovery, cancel button, step limits
- [ ] Onboarding flow
- [ ] Performance tuning (proot overhead)
- [ ] Full integration testing on device

**No release builds until Phase 6 complete.**

---

## Security Considerations

- API keys in `EncryptedSharedPreferences`
- Skill writes require user approval
- Proot runs isolated in app sandbox (no root)
- Agent shell commands logged and visible to user
- Max tool call limit per task (50)
- No arbitrary APK installation from agent without user confirm

---

## Dependencies (Phase 1)

| Library | Purpose |
|---------|---------|
| Jetpack Compose + Material3 | UI |
| Koog agents | Agent framework |
| OkHttp | HTTP client |
| Security Crypto | Encrypted prefs |
| WorkManager | Scheduled tasks |
| Navigation Compose | Screen routing |
| DataStore | App preferences |
