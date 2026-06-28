package com.proot.cowork.domain.agent.orchestration

import com.proot.cowork.data.files.GuestPaths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object SystemPromptBuilder {

    fun build(
        skillsSuffix: String,
        classification: TaskClassification,
        planPath: String,
    ): String {
        if (classification.complexity.isToolFree()) {
            return buildTrivialPrompt(classification)
        }
        return buildString {
            appendLine(layerIdentity())
            appendLine()
            if (skillsSuffix.isNotBlank()) {
                appendLine(skillsSuffix)
                appendLine()
            }
            appendLine(layerToolGuidance())
            appendLine()
            appendLine(layerEmphasisRules(classification, planPath))
            appendLine()
            appendLine(layerOrchestration(classification))
            appendLine()
            appendLine(layerSkillRules())
        }.trim()
    }

    private fun buildTrivialPrompt(classification: TaskClassification): String {
        val now = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z"))
        return """
            |You are Cowork Agent. The user's question is trivial (${classification.rationale}).
            |
            |Rules — follow strictly:
            |- Answer directly in one short message. No markdown headers unless helpful.
            |- Do NOT use tools. Do NOT run shell commands. Do NOT read or write files.
            |- Do NOT create todos or plans. Do NOT mention proot, tools, or your environment.
            |- For arithmetic, compute mentally and give the numeric result.
            |- Current date/time (if relevant): $now
        """.trimMargin().trim()
    }

    private fun layerIdentity(): String = """
        |# Layer 1 — Core identity
        |You are Cowork Agent, an orchestrator with direct access to an Ubuntu proot environment on the user's phone-linked desktop.
        |Communicate in clear markdown. Be concise, direct, and actionable. Match the user's language.
    """.trimMargin()

    private fun layerToolGuidance(): String = """
        |# Layer 5 — Tools (subset)
        |Available: proot_shell, read_file, write_file, edit_file, web_fetch, edit_and_test_code, skills_*, slack_notify, todo_write, todo_read.
        |Read paths under ${GuestPaths.HOME}. Write deliverables to ${GuestPaths.AGENT_OUTPUT_DIR} (also Desktop/Artifacts when sharing with user).
        |Always read_file before edit_file on the same path.
    """.trimMargin()

    private fun layerEmphasisRules(
        classification: TaskClassification,
        planPath: String,
    ): String {
        val now = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z"))
        return buildString {
            appendLine("# Layer 7 — Emphasis rules")
            appendLine("- Current date/time: $now")
            appendLine("- Task complexity: ${classification.complexity.name} (${classification.rationale})")
            if (classification.complexity.requiresPlan()) {
                appendLine("- A plan was written to `$planPath` before execution. Follow it efficiently.")
                appendLine("- Use tools sparingly; avoid retry loops. Write deliverables to ${GuestPaths.AGENT_OUTPUT_DIR}.")
            }
            if (classification.complexity == TaskComplexity.MODERATE) {
                appendLine("- Moderate task: research briefly (≤2 web_fetch), then write summary.md to output.")
            }
            if (classification.complexity == TaskComplexity.COMPLEX) {
                appendLine("- Do not skip validation; fix failures before proceeding.")
            }
            if (classification.complexity.suggestsTodos()) {
                appendLine("- Use todo_write before todo_read. Keep exactly one todo in_progress.")
            }
            appendLine("- Use tools when the task requires actions in the environment; skip tools for pure Q&A.")
            appendLine("- Final answers: **bold** key results, bullet steps, `code` for paths/commands.")
            appendLine("- Summarize what you did and where outputs live.")
        }
    }

    private fun layerOrchestration(classification: TaskClassification): String {
        val stages = classification.suggestedStages.joinToString(" → ") { it.label }
        return """
            |# Layer 9 — Orchestration
            |Execution pattern for this task: $stages
            |${ExecutionStage.RESEARCH.agentHint}
            |${ExecutionStage.EXECUTE.agentHint}
            |Research and writing must not be merged in a single stage when both are required.
        """.trimMargin()
    }

    private fun layerSkillRules(): String = """
        |# Layer 10 — Skills
        |Check enabled skills before improvising. Use skills_list and skill_view to load workflows on demand.
        |User skills override built-in skills with the same name.
    """.trimMargin()
}
