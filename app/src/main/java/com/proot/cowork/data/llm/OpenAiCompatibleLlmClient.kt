package com.proot.cowork.data.llm

import com.proot.cowork.data.prefs.LlmConfig
import com.proot.cowork.domain.agent.AgentMessage
import com.proot.cowork.domain.agent.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAiCompatibleLlmClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(config: LlmConfig): Result<String> = withContext(Dispatchers.IO) {
        if (!LlmEndpoint.isConfigured(config)) {
            return@withContext Result.failure(IllegalStateException("API key, base URL, and model are required"))
        }
        runCatching {
            val endpoint = LlmEndpoint.from(config)
            val payload = JSONObject().apply {
                put("model", config.model.trim())
                put("max_tokens", 8)
                put("stream", false)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
            }
            val request = buildRequest(endpoint, config.apiKey, payload.toString())
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: ${body.take(240)}")
                }
                "Connected to ${endpoint.baseUrl} (${response.code})"
            }
        }
    }

    suspend fun streamChat(
        config: LlmConfig,
        systemPrompt: String,
        history: List<AgentMessage>,
        userMessage: String,
        temperature: Double,
        isActive: () -> Boolean,
        onDelta: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val endpoint = LlmEndpoint.from(config)
        val payload = JSONObject().apply {
            put("model", config.model.trim())
            put("stream", true)
            put("temperature", temperature)
            put("messages", buildMessagesArray(systemPrompt, history, userMessage))
        }
        val request = buildRequest(endpoint, config.apiKey, payload.toString())
        val buffer = StringBuilder()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                error("HTTP ${response.code}: ${err.take(300)}")
            }
            val source = response.body?.source() ?: error("Empty response body")
            while (!source.exhausted()) {
                if (!isActive()) break
                val line = source.readUtf8Line() ?: continue
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val delta = runCatching {
                    JSONObject(data)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                }.getOrDefault("")
                if (delta.isNotEmpty()) {
                    buffer.append(delta)
                    onDelta(delta)
                }
            }
        }
        buffer.toString()
    }

    private fun buildRequest(endpoint: ResolvedLlmEndpoint, apiKey: String, payload: String): Request {
        return Request.Builder()
            .url(endpoint.chatCompletionsUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/Pixelgamer4k/Proot-Cowork")
            .addHeader("X-Title", "Proot Cowork")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
    }

    private fun buildMessagesArray(
        systemPrompt: String,
        history: List<AgentMessage>,
        userMessage: String,
    ): JSONArray {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(12).forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
                MessageRole.TOOL -> "assistant"
            }
            if (msg.content.isNotBlank()) {
                messages.put(JSONObject().put("role", role).put("content", msg.content))
            }
        }
        messages.put(JSONObject().put("role", "user").put("content", userMessage))
        return messages
    }
}
