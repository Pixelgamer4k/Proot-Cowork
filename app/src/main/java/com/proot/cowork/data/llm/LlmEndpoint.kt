package com.proot.cowork.data.llm

import com.proot.cowork.data.prefs.LlmConfig

data class ResolvedLlmEndpoint(
    val baseUrl: String,
    val chatCompletionsPath: String,
) {
    val chatCompletionsUrl: String
        get() = "${baseUrl.trimEnd('/')}/${chatCompletionsPath.trimStart('/')}"
}

object LlmEndpoint {
    fun from(config: LlmConfig): ResolvedLlmEndpoint {
        var baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            baseUrl = "https://openrouter.ai/api/v1"
        }
        val path = when {
            baseUrl.endsWith("/v1") -> "chat/completions"
            else -> "v1/chat/completions"
        }
        return ResolvedLlmEndpoint(baseUrl = baseUrl, chatCompletionsPath = path)
    }

    fun isConfigured(config: LlmConfig): Boolean {
        return config.apiKey.isNotBlank() &&
            config.baseUrl.isNotBlank() &&
            config.model.isNotBlank()
    }
}
