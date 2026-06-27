package com.proot.cowork.domain.agent.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SlackTool {
    const val NAME = "slack_notify"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun definition(): JSONObject = JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", NAME)
            put("description", "Post a message to Slack via incoming webhook. Use for progress updates and summaries.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("message", JSONObject().put("type", "string").put("description", "Message text to send"))
                })
                put("required", org.json.JSONArray().put("message"))
            })
        })
    }

    suspend fun execute(webhookUrl: String, args: JSONObject): String = withContext(Dispatchers.IO) {
        val url = webhookUrl.trim()
        if (url.isBlank()) {
            return@withContext "Error: Slack webhook URL is not configured. Add it in Settings → Integrations."
        }
        val message = args.optString("message").trim()
        if (message.isBlank()) return@withContext "Error: message is required"
        val payload = JSONObject().put("text", message.take(4000)).toString()
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(jsonMedia))
            .build()
        runCatching {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) "Slack message sent."
                else "Slack HTTP ${response.code}: ${body.take(200)}"
            }
        }.getOrElse { "Slack failed: ${it.message}" }
    }
}
