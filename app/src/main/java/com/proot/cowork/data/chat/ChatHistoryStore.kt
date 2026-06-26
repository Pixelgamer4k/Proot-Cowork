package com.proot.cowork.data.chat

import android.content.Context
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.ExecutionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class SavedConversation(
    val messages: List<AgentMessage> = emptyList(),
    val executionMode: ExecutionMode = ExecutionMode.SWARM,
)

class ChatHistoryStore(private val context: Context) {

    private val storeFile: File
        get() = File(context.filesDir, "chat/conversation.json").also {
            it.parentFile?.mkdirs()
        }

    suspend fun load(): SavedConversation = withContext(Dispatchers.IO) {
        val file = storeFile
        if (!file.exists()) return@withContext SavedConversation()
        runCatching {
            val obj = JSONObject(file.readText())
            val messages = ChatMessageJson.messagesFromJson(obj.optJSONArray("messages") ?: org.json.JSONArray())
                .filter(ChatMessageJson::isPersistable)
                .takeLast(MAX_STORED_MESSAGES)
            val mode = runCatching {
                ExecutionMode.valueOf(obj.optString("executionMode", ExecutionMode.SWARM.name))
            }.getOrDefault(ExecutionMode.SWARM)
            SavedConversation(messages = messages, executionMode = mode)
        }.getOrDefault(SavedConversation())
    }

    suspend fun save(messages: List<AgentMessage>, executionMode: ExecutionMode) = withContext(Dispatchers.IO) {
        val persistable = messages.filter(ChatMessageJson::isPersistable).takeLast(MAX_STORED_MESSAGES)
        val payload = JSONObject().apply {
            put("version", 1)
            put("executionMode", executionMode.name)
            put("messages", ChatMessageJson.messagesToJson(persistable))
        }
        storeFile.writeText(payload.toString())
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        storeFile.delete()
    }

    companion object {
        const val MAX_STORED_MESSAGES = 200
    }
}
