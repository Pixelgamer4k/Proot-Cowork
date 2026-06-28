package com.proot.cowork.domain.agent.orchestration

import android.util.Base64
import com.proot.cowork.data.files.GuestPaths
import com.proot.cowork.data.proot.ProotGuestShellExecutor
import com.proot.cowork.domain.agent.AgentRunContext

class PlanWriter(private val shell: ProotGuestShellExecutor) {

    suspend fun ensureOutputDir(): String {
        shell.run(GuestPaths.ensureArtifactsCmd())
        return GuestPaths.AGENT_OUTPUT_DIR
    }

    suspend fun writePlan(classification: TaskClassification): ExecutionPlan {
        ensureOutputDir()
        val plan = buildPlan(classification)
        val path = GuestPaths.planFilePath()
        val b64 = Base64.encodeToString(plan.markdown.toByteArray(), Base64.NO_WRAP)
        val quoted = "'" + path.replace("'", "'\\''") + "'"
        shell.run("mkdir -p '${GuestPaths.AGENT_OUTPUT_DIR}' && echo '$b64' | base64 -d > $quoted")
        AgentRunContext.planWritten = true
        return plan
    }

    fun buildPlan(classification: TaskClassification): ExecutionPlan {
        val criteria = listOf(
            "All requested deliverables exist in ${GuestPaths.AGENT_OUTPUT_DIR} or ${GuestPaths.ARTIFACTS_DIR}",
            "Commands completed without unresolved errors",
            "Final response summarizes paths and outcomes",
        )
        val stageLines = classification.suggestedStages.mapIndexed { index, stage ->
            "${index + 1}. **${stage.label}** — ${stage.agentHint}"
        }
        val markdown = buildString {
            appendLine("# Execution Plan")
            appendLine()
            appendLine("## Task summary")
            appendLine(classification.summary)
            appendLine()
            appendLine("## Classification")
            appendLine("- Complexity: `${classification.complexity.name}`")
            appendLine("- Rationale: ${classification.rationale}")
            appendLine()
            appendLine("## Stages")
            stageLines.forEach { appendLine(it) }
            appendLine()
            appendLine("## Validation criteria")
            criteria.forEach { appendLine("- $it") }
            appendLine()
            appendLine("## Output directory")
            appendLine(GuestPaths.AGENT_OUTPUT_DIR)
            appendLine()
            appendLine("## Dependency graph")
            appendLine("Sequential stage-gate: each stage must pass validation before the next.")
        }
        return ExecutionPlan(
            taskSummary = classification.summary,
            complexity = classification.complexity,
            stages = classification.suggestedStages,
            validationCriteria = criteria,
            markdown = markdown,
        )
    }
}
