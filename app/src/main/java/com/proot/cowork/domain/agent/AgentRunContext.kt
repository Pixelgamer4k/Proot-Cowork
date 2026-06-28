package com.proot.cowork.domain.agent

/** Per-run context for tools and orchestration (cleared after each agent run). */
object AgentRunContext {
    var threadId: String? = null
    val filesReadThisRun: MutableSet<String> = mutableSetOf()
    var planWritten: Boolean = false
    var todosInitialized: Boolean = false

    fun reset(threadId: String?) {
        this.threadId = threadId
        filesReadThisRun.clear()
        planWritten = false
        todosInitialized = false
    }
}
