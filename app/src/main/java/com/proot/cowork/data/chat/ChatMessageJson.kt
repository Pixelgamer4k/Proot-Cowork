package com.proot.cowork.data.chat

import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.MessageRole
import com.proot.cowork.domain.agent.ToolCallStatus
import org.json.JSONArray
import org.json.JSONObject

object ChatMessageJson {

    fun messagesToJson(messages: List<AgentMessage>): JSONArray = JSONArray().apply {
        messages.forEach { put(messageToJson(it)) }
    }

    fun messagesFromJson(array: JSONArray): List<AgentMessage> =
        (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let(::messageFromJson)
        }

    fun messageToJson(msg: AgentMessage): JSONObject = JSONObject().apply {
        put("id", msg.id)
        put("role", msg.role.name)
        put("content", msg.content)
        put("timestamp", msg.timestamp)
        msg.toolName?.let { put("toolName", it) }
        msg.toolCallId?.let { put("toolCallId", it) }
        msg.agentName?.let { put("agentName", it) }
        msg.toolStatus?.let { put("toolStatus", it.name) }
    }

    fun messageFromJson(obj: JSONObject): AgentMessage = AgentMessage(
        id = obj.optString("id"),
        role = runCatching { MessageRole.valueOf(obj.optString("role", "USER")) }
            .getOrDefault(MessageRole.USER),
        content = obj.optString("content", ""),
        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
        toolName = obj.optString("toolName").takeIf { it.isNotBlank() },
        toolCallId = obj.optString("toolCallId").takeIf { it.isNotBlank() },
        agentName = obj.optString("agentName").takeIf { it.isNotBlank() },
        toolStatus = obj.optString("toolStatus").takeIf { it.isNotBlank() }?.let {
            runCatching { ToolCallStatus.valueOf(it) }.getOrNull()
        },
    )

    /** Messages worth persisting and showing in transcript export. */
    fun isPersistable(msg: AgentMessage): Boolean = when (msg.role) {
        MessageRole.TOOL -> false
        MessageRole.ASSISTANT -> msg.content.isNotBlank()
        else -> msg.content.isNotBlank()
    }
}
