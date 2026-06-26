package com.proot.cowork.data.chat

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatTranscriptExporter(private val context: Context) {

    suspend fun export(messages: List<AgentMessage>): Uri? = withContext(Dispatchers.IO) {
        val visible = messages.filter(ChatMessageJson::isPersistable)
        if (visible.isEmpty()) return@withContext null

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val body = buildString {
            appendLine("# Proot Cowork Chat Transcript")
            appendLine("Exported: ${formatter.format(Date())}")
            appendLine()
            visible.forEach { msg ->
                val role = when (msg.role) {
                    MessageRole.USER -> "You"
                    MessageRole.ASSISTANT -> "Assistant"
                    MessageRole.SYSTEM -> "System"
                    MessageRole.TOOL -> "Tool"
                }
                val time = formatter.format(Date(msg.timestamp))
                appendLine("## $role · $time")
                appendLine(msg.content.trim())
                appendLine()
            }
        }

        val dir = File(context.filesDir, "chat/exports").also { it.mkdirs() }
        val file = File(dir, "cowork-chat-${System.currentTimeMillis()}.md")
        file.writeText(body)

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}
