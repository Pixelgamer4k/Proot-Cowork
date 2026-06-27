package com.proot.cowork.data.chat

import android.content.Context
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.ExecutionMode
import com.proot.cowork.domain.agent.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SavedConversation(
    val messages: List<AgentMessage> = emptyList(),
    val executionMode: ExecutionMode = ExecutionMode.SWARM,
)

class ChatHistoryStore(private val context: Context) {

    private val threadsDir: File
        get() = File(context.filesDir, "chat/threads").also { it.mkdirs() }

    private val indexFile: File
        get() = File(threadsDir, "index.json")

    private val legacyFile: File
        get() = File(context.filesDir, "chat/conversation.json")

    suspend fun migrateLegacyIfNeeded() = withContext(Dispatchers.IO) {
        if (!legacyFile.exists()) return@withContext
        if (indexFile.exists() && loadIndex().isNotEmpty()) {
            legacyFile.delete()
            return@withContext
        }
        val legacy = readThreadFile(legacyFile)
        val id = UUID.randomUUID().toString()
        val title = legacy.messages.firstOrNull { it.role == MessageRole.USER }?.content
            ?.lineSequence()?.firstOrNull()?.take(48)
            ?.ifBlank { null }
            ?: "Previous chat"
        val meta = ChatThreadMeta(
            id = id,
            title = title,
            updatedAt = System.currentTimeMillis(),
            executionMode = legacy.executionMode,
        )
        writeThreadFile(id, legacy)
        writeIndex(listOf(meta))
        legacyFile.delete()
    }

    suspend fun listThreads(): List<ChatThreadMeta> = withContext(Dispatchers.IO) {
        migrateLegacyIfNeeded()
        loadIndex().sortedByDescending { it.updatedAt }
    }

    suspend fun loadThread(threadId: String): SavedConversation = withContext(Dispatchers.IO) {
        migrateLegacyIfNeeded()
        val file = threadFile(threadId)
        if (!file.exists()) return@withContext SavedConversation()
        readThreadFile(file)
    }

    suspend fun saveThread(
        threadId: String,
        messages: List<AgentMessage>,
        executionMode: ExecutionMode,
    ) = withContext(Dispatchers.IO) {
        migrateLegacyIfNeeded()
        val persistable = messages.filter(ChatMessageJson::isPersistable).takeLast(MAX_STORED_MESSAGES)
        writeThreadFile(threadId, SavedConversation(persistable, executionMode))
        val index = loadIndex().toMutableList()
        val title = persistable.firstOrNull { it.role == MessageRole.USER }?.content
            ?.lineSequence()?.firstOrNull()?.take(48)
            ?: index.find { it.id == threadId }?.title
            ?: "New chat"
        val existing = index.indexOfFirst { it.id == threadId }
        val meta = ChatThreadMeta(
            id = threadId,
            title = title,
            updatedAt = System.currentTimeMillis(),
            executionMode = executionMode,
        )
        if (existing >= 0) index[existing] = meta else index.add(0, meta)
        writeIndex(index.take(MAX_THREADS))
    }

    suspend fun createThread(title: String = "New chat"): ChatThreadMeta = withContext(Dispatchers.IO) {
        migrateLegacyIfNeeded()
        val meta = ChatThreadMeta(
            id = UUID.randomUUID().toString(),
            title = title,
            updatedAt = System.currentTimeMillis(),
        )
        writeThreadFile(meta.id, SavedConversation())
        writeIndex(listOf(meta) + loadIndex())
        meta
    }

    suspend fun deleteThread(threadId: String) = withContext(Dispatchers.IO) {
        threadFile(threadId).delete()
        writeIndex(loadIndex().filterNot { it.id == threadId })
    }

    /** @deprecated Use thread APIs. Kept for compatibility. */
    suspend fun load(): SavedConversation {
        migrateLegacyIfNeeded()
        val threads = listThreads()
        val id = threads.firstOrNull()?.id ?: return SavedConversation()
        return loadThread(id)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        threadsDir.listFiles()?.forEach { it.delete() }
        legacyFile.delete()
    }

    private fun threadFile(threadId: String) = File(threadsDir, "$threadId.json")

    private fun loadIndex(): List<ChatThreadMeta> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(indexFile.readText())
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val mode = runCatching {
                    ExecutionMode.valueOf(obj.optString("executionMode", ExecutionMode.SWARM.name))
                }.getOrDefault(ExecutionMode.SWARM)
                ChatThreadMeta(
                    id = obj.getString("id"),
                    title = obj.optString("title", "Chat"),
                    updatedAt = obj.optLong("updatedAt", 0L),
                    executionMode = mode,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeIndex(threads: List<ChatThreadMeta>) {
        val arr = JSONArray()
        threads.forEach { meta ->
            arr.put(
                JSONObject()
                    .put("id", meta.id)
                    .put("title", meta.title)
                    .put("updatedAt", meta.updatedAt)
                    .put("executionMode", meta.executionMode.name),
            )
        }
        indexFile.writeText(arr.toString())
    }

    private fun readThreadFile(file: File): SavedConversation {
        val obj = JSONObject(file.readText())
        val messages = ChatMessageJson.messagesFromJson(obj.optJSONArray("messages") ?: JSONArray())
            .filter(ChatMessageJson::isPersistable)
            .takeLast(MAX_STORED_MESSAGES)
        val mode = runCatching {
            ExecutionMode.valueOf(obj.optString("executionMode", ExecutionMode.SWARM.name))
        }.getOrDefault(ExecutionMode.SWARM)
        return SavedConversation(messages = messages, executionMode = mode)
    }

    private fun writeThreadFile(threadId: String, conversation: SavedConversation) {
        val payload = JSONObject().apply {
            put("version", 2)
            put("threadId", threadId)
            put("executionMode", conversation.executionMode.name)
            put("messages", ChatMessageJson.messagesToJson(conversation.messages))
        }
        threadFile(threadId).writeText(payload.toString())
    }

    companion object {
        const val MAX_STORED_MESSAGES = 200
        const val MAX_THREADS = 50
    }
}
