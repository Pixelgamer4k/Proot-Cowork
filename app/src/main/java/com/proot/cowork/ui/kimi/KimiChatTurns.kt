package com.proot.cowork.ui.kimi

import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.MessageRole

data class KimiChatTurn(
    val user: AgentMessage?,
    val assistant: AgentMessage?,
    val tools: List<AgentMessage>,
    val systemNotices: List<AgentMessage> = emptyList(),
)

fun groupMessagesIntoTurns(messages: List<AgentMessage>): List<KimiChatTurn> {
    val turns = mutableListOf<KimiChatTurn>()
    var user: AgentMessage? = null
    var assistant: AgentMessage? = null
    val tools = mutableListOf<AgentMessage>()
    val systemNotices = mutableListOf<AgentMessage>()

    fun flush() {
        if (user != null || assistant != null || tools.isNotEmpty() || systemNotices.isNotEmpty()) {
            turns.add(
                KimiChatTurn(
                    user = user,
                    assistant = assistant,
                    tools = tools.toList(),
                    systemNotices = systemNotices.toList(),
                ),
            )
            user = null
            assistant = null
            tools.clear()
            systemNotices.clear()
        }
    }

    for (msg in messages) {
        when (msg.role) {
            MessageRole.USER -> {
                flush()
                user = msg
            }
            MessageRole.ASSISTANT -> assistant = msg
            MessageRole.TOOL -> tools.add(msg)
            MessageRole.SYSTEM -> {
                if (user == null && assistant == null && tools.isEmpty()) {
                    turns.add(KimiChatTurn(user = null, assistant = null, tools = emptyList(), systemNotices = listOf(msg)))
                } else {
                    systemNotices.add(msg)
                }
            }
        }
    }
    flush()
    return turns
}

fun thinkingLinesFromTools(tools: List<AgentMessage>): List<String> =
    tools.map { tool ->
        val name = tool.toolName ?: "tool"
        val agent = tool.agentName?.let { "$it · " }.orEmpty()
        when (tool.toolStatus) {
            com.proot.cowork.domain.agent.ToolCallStatus.RUNNING ->
                "$agent$name — running…"
            com.proot.cowork.domain.agent.ToolCallStatus.FAILED ->
                "$agent$name — failed"
            else ->
                "$agent$name — done"
        }
    }
