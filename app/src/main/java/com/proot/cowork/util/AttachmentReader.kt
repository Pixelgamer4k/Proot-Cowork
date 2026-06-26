package com.proot.cowork.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AttachmentReader {

    private const val MAX_ATTACHMENT_CHARS = 8_000

    suspend fun readTextSnippet(context: Context, uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
        val name = resolveDisplayName(context, uri) ?: uri.lastPathSegment ?: "attachment"
        val mime = context.contentResolver.getType(uri).orEmpty()
        val text = when {
            mime.startsWith("text/") || name.endsWith(".md") || name.endsWith(".txt") ||
                name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".kt") ||
                name.endsWith(".py") || name.endsWith(".sh") -> {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }.orEmpty()
            }
            else -> "(Binary file — only text attachments are inlined. Name: $name, type: $mime)"
        }
        name to text.take(MAX_ATTACHMENT_CHARS)
    }

    suspend fun readArtifactFile(file: File): Pair<String, String> = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) {
            return@withContext file.name to "(File not found)"
        }
        val text = if (file.length() > 512_000) {
            "(File too large to attach: ${file.length() / 1024} KB)"
        } else {
            runCatching { file.readText() }.getOrElse { "(Could not read file: ${it.message})" }
        }
        file.name to text.take(MAX_ATTACHMENT_CHARS)
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
    }
}
