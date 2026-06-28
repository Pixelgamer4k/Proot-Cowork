package com.proot.cowork.domain.agent.orchestration

import com.proot.cowork.data.llm.LlmEndpoint
import com.proot.cowork.data.llm.OpenAiCompatibleLlmClient
import com.proot.cowork.data.prefs.LlmConfig
import com.proot.cowork.domain.agent.orchestration.StageValidator

class StageValidator {

    suspend fun validate(
        config: LlmConfig,
        stage: ExecutionStage,
        stageOutput: String,
        userTask: String,
        isActive: () -> Boolean,
    ): ValidationResult {
        if (!isActive()) {
            return ValidationResult(passed = false, feedback = "Run cancelled")
        }

        val heuristic = heuristicValidate(stage, stageOutput)
        if (!heuristic.passed) return heuristic

        if (!LlmEndpoint.isConfigured(config)) {
            return ValidationResult(passed = true, feedback = "Skipped LLM validation (API not configured)")
        }

        if (stage == ExecutionStage.INTEGRATE) {
            return ValidationResult(passed = stageOutput.isNotBlank(), feedback = "Integration output required")
        }

        val system = """
            You are a strict stage validator. Reply with ONLY one line:
            PASS
            or
            FAIL: <one sentence reason>
        """.trimIndent()
        val user = """
            User task: $userTask
            Stage: ${stage.label}
            Stage output (truncated):
            ${stageOutput.take(3000)}
        """.trimIndent()
        val messages = OpenAiCompatibleLlmClient.buildMessagesArray(system, emptyList(), user)
        val result = runCatching {
            OpenAiCompatibleLlmClient.complete(config, messages, temperature = 0.0)
        }.getOrElse {
            return ValidationResult(passed = true, feedback = "Validator unavailable: ${it.message}")
        }
        val line = result.content.trim().lineSequence().firstOrNull().orEmpty()
        return if (line.equals("PASS", ignoreCase = true) || line.startsWith("PASS ", ignoreCase = true)) {
            ValidationResult(passed = true, feedback = "Stage passed")
        } else {
            val reason = line.removePrefix("FAIL:").removePrefix("FAIL").trim()
            ValidationResult(
                passed = false,
                feedback = reason.ifBlank { "Stage validation failed" },
            )
        }
    }

    private fun heuristicValidate(stage: ExecutionStage, output: String): ValidationResult {
        val trimmed = output.trim()
        if (trimmed.startsWith("Error:", ignoreCase = true) || trimmed.startsWith("Tool error:", ignoreCase = true)) {
            return ValidationResult(passed = false, feedback = "Tool error in stage output")
        }
        if (stage == ExecutionStage.RESEARCH && trimmed.length < 20) {
            return ValidationResult(passed = false, feedback = "Research stage produced insufficient output")
        }
        if (stage == ExecutionStage.EXECUTE && trimmed.isBlank()) {
            return ValidationResult(passed = false, feedback = "Execute stage produced no output")
        }
        return ValidationResult(passed = true, feedback = "Heuristic pass")
    }
}
