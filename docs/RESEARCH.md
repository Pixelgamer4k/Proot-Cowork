# Proot Cowork — Research Report

Deep research on similar apps, proven patterns, and failure modes to avoid.

## 1. Inspiration Apps

### Kimi Work / K2.6 Agent Swarm (Moonshot AI)

**What works:**
- Orchestrator decomposes tasks into parallel subtasks automatically (no hand-crafted workflows)
- Up to 300 sub-agents in cloud; mobile uses single orchestrator + limited parallelism
- Real-time progress UI: task list, sub-agent spawning, parallel execution
- Three modes: chat, single agent, agent swarm
- Skills library (ClawHub) with installable SKILL.md modules

**Lessons for Proot Cowork:**
- Mobile must cap swarm size (3–8 concurrent sub-agents) due to API cost and memory
- Show live task tree in UI — users need visibility into parallel work
- Orchestrator routes subtasks by type (shell → proot, file → filesystem, web → browser)

**References:**
- https://www.kimi.com/help/agent/agent-swarm
- https://github.com/kimi-2-6/kimi-2.6

### Claude Cowork + Dispatch (Anthropic)

**What works:**
- Persistent cross-device conversation thread
- Dispatch agent plans, spawns child tasks, aggregates results
- Three execution surfaces: Cowork (files), Code (workspace), connectors
- Modes: plan-first vs direct execution; scheduled tasks separate from real-time dispatch
- Computer use: agent controls desktop apps with approval gates

**Lessons for Proot Cowork:**
- Plan mode: decompose → show plan → user approves → execute
- Direct mode: skip approval, run immediately
- Schedule mode: WorkManager + AlarmManager for deferred execution
- Child tasks don't spawn further children (prevents runaway recursion on mobile)

**References:**
- https://claude.com/docs/cowork/guide/dispatch
- https://claudelab.net/en/articles/cowork/claude-dispatch-remote-cowork-guide

### Hermes Agent (Nous Research)

**What works:**
- Self-improvement via SKILL.md procedural memory (not model retraining)
- `skill_manage` tool: create/patch/delete skills after complex tasks (5+ tool calls)
- Progressive disclosure: `skills_list` → `skill_view` → optional reference files
- agentskills.io standard for portability
- Human approval gate before skill writes land

**Lessons for Proot Cowork:**
- After successful multi-step tasks, offer to save workflow as skill
- FTS5 or simple index for skill discovery at session start
- Separate declarative memory (preferences) from procedural skills

**References:**
- https://hermes-agent.nousresearch.com/docs/user-guide/features/skills
- https://github.com/NousResearch/hermes-agent

---

## 2. Proot + X11 on Android

### Proven Approaches

| Project | Approach | Key Insight |
|---------|----------|-------------|
| [Phoshdroid](https://github.com/zweck/Phoshdroid) | Embed termux-x11 as library in same process | No cross-process socket dance; SurfaceView for X display |
| [Trierarch](https://github.com/Beauty114514/trierarch) | Native proot + custom Wayland compositor | Downloads proot-distro rootfs on first launch |
| [gabx/linux-on-android](https://github.com/gabx/linux-on-android) | Termux + proot-distro + termux-x11 scripts | `--shared-tmp` critical for X11 socket sharing |
| [DroidDesk](https://github.com/dmitryzenit/DroidDesk) | TUR + proot menu sync | Proot shares display with native Termux desktop |
| [libtermux-android](https://github.com/libtermux/libtermux-android) | Standalone Termux SDK | Kotlin DSL, Flow streaming, TerminalView widget |

### Critical X11 Requirements (from termux-x11 issue #299)

1. **Start X11 server first** on Android host, then launch desktop inside proot
2. **`--shared-tmp`** binds `$PREFIX/tmp/.X11-unix` → `/tmp/.X11-unix` in guest
3. **`DISPLAY=:0`** in both host and guest
4. **`XDG_RUNTIME_DIR=/tmp`** in guest
5. Use **F-Droid Termux builds**, not Play Store (incompatible)

### Rootfs Export Workflow

User builds custom rootfs in Termux with our `rootfs-setup/` scripts, then exports via:

```bash
# Inside Termux after provisioning
cd $PREFIX/var/lib/proot-distro/installed-rootfs/<distro>
tar -czf proot-cowork-rootfs.tar.gz .
```

Proot Cowork imports the tarball to `files/rootfs/` and auto-starts on launch.

---

## 3. Agent Framework Selection

### Winner: [Koog](https://github.com/JetBrains/koog) (JetBrains)

| Criteria | Koog | Raw OpenAI SDK | LangChain4j |
|----------|------|----------------|-------------|
| Kotlin/Android native | ✅ KMP | ❌ Java wrapper | ⚠️ JVM only |
| OpenRouter support | ✅ | Manual | ✅ |
| Tool calling | ✅ Type-safe ToolRegistry | Manual JSON | ✅ |
| Multi-agent graphs | ✅ GraphStrategy | ❌ | ⚠️ |
| MCP support | ✅ agents-mcp module | ❌ | ⚠️ |
| Maturity | 4K+ stars, v1.0 | N/A | Good |

### Complement: [koog-compose](https://github.com/BrianMwas/koog-compose)

- Compose UI bindings, phase engine, streaming tokens to UI
- Optional on-device models (Gemma) — future phase

---

## 4. Skills Standard

Use **agentskills.io** open standard:

```
skills/
└── deploy-proot-app/
    ├── SKILL.md          # Required: YAML frontmatter + instructions
    ├── scripts/
    └── references/
```

Discovery at startup → load full SKILL.md on demand → optional bundled files.

---

## 5. Known Failure Modes & Mitigations

| Failure | Cause | Mitigation |
|---------|-------|------------|
| Black X11 screen | Missing `--shared-tmp` or wrong DISPLAY | Enforce startup sequence in ProotService |
| ptrace overhead | proot syscall interception | Consider proroot library later; cap concurrent shell ops |
| API rate limits | Too many swarm agents | Cap at 8 sub-agents; batch tool calls |
| Runaway agent loops | No step limit | Max 50 tool calls per task; user cancel button |
| Rootfs too large | Full desktop + dev tools | Document minimal rootfs; compress with tar.gz |
| W^X on Android 14+ | Executing from app storage | Use nativeLibraryDir for proot binary |
| Skill pollution | Agent writes bad skills | Human approval gate before skill_manage writes |
| Scheduled task drift | Doze mode kills WorkManager | Use AlarmManager exact alarms + foreground service |

---

## 6. Competitive Positioning

Proot Cowork = **Claude Cowork's planning/execution** + **Kimi's swarm orchestration** + **Hermes self-improvement** + **full Linux desktop in-app** (unique on mobile).

No existing Android app combines embedded proot desktop with multi-agent AI automation.
