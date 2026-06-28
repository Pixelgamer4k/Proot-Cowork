package com.proot.cowork.domain.agent.orchestration

enum class TaskComplexity {
    TRIVIAL,
    SIMPLE,
    MODERATE,
    COMPLEX,
    ;

    fun requiresPlan(): Boolean = ordinal >= MODERATE.ordinal

    fun requiresStagedExecution(): Boolean = ordinal >= MODERATE.ordinal

    fun suggestsTodos(): Boolean = ordinal >= SIMPLE.ordinal
}

data class TaskClassification(
    val complexity: TaskComplexity,
    val summary: String,
    val suggestedStages: List<ExecutionStage>,
    val rationale: String,
)

enum class ExecutionStage(val label: String, val agentHint: String) {
    RESEARCH("Research", "Gather facts and inspect the environment. Do not write final deliverables yet."),
    EXECUTE("Execute", "Implement the task using shell and file tools. Produce outputs in the output directory."),
    VALIDATE("Validate", "Verify outputs exist and meet the request. Fix gaps if needed."),
    INTEGRATE("Integrate", "Summarize results for the user in clear markdown with paths to artifacts."),
}

data class ValidationResult(
    val passed: Boolean,
    val feedback: String,
)

data class ExecutionPlan(
    val taskSummary: String,
    val complexity: TaskComplexity,
    val stages: List<ExecutionStage>,
    val validationCriteria: List<String>,
    val markdown: String,
)
