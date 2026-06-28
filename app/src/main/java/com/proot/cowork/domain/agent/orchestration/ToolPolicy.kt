package com.proot.cowork.domain.agent.orchestration

/** Whether the tool loop may expose function-calling tools to the model. */
enum class ToolPolicy {
    /** No tools — direct text reply only (trivial Q&A). */
    NONE,
    /** Full agent toolset. */
    FULL,
}
