package com.proot.cowork.domain.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.feature.EventHandler
import ai.koog.agents.core.dsl.builder.strategy.streamingStrategy
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.streaming.StreamFrame
import com.proot.cowork.data.llm.LlmEndpoint
import com.proot.cowork.data.llm.OpenAiCompatibleLlmClient
import com.proot.cowork.data.prefs.LlmConfig
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs Cowork chat through Koog [AIAgent] agents (Swarm vs Fast system prompts).
 * Streaming uses the OpenAI-compatible SSE path; Koog handles agent orchestration.
 */
object CoworkKoogAgentRunner {

  suspend fun streamChat(
      config: LlmConfig,
      mode: ExecutionMode,
      history: List<AgentMessage>,
      userMessage: String,
      isActive: () -> Boolean,
      onDelta: (String) -> Unit,
  ): String {
    require(LlmEndpoint.isConfigured(config)) { "Configure API key, base URL, and model in Settings" }
    val systemPrompt = systemPromptFor(mode)
    val temperature = if (mode == ExecutionMode.FAST) 0.3 else 0.5
    val streamedText = AtomicReference("")

    try {
      val endpoint = LlmEndpoint.from(config)
      val settings = OpenAIClientSettings(
          baseUrl = endpoint.baseUrl,
          chatCompletionsPath = endpoint.chatCompletionsPath,
      )
      val llmClient = OpenAILLMClient(apiKey = config.apiKey.trim(), settings = settings)
      SingleLLMPromptExecutor(llmClient).use { executor ->
        val model = LLModel(
            provider = LLMProvider.OpenAI,
            id = config.model.trim(),
            capabilities = listOf(LLMCapability.Temperature),
        )
        val agent = AIAgent.builder<String, String>()
            .promptExecutor(executor)
            .systemPrompt(systemPrompt)
            .llmModel(model)
            .temperature(temperature)
            .maxIterations(8)
            .strategy(streamingStrategy())
            .install(EventHandler) { handler ->
              handler.onLLMStreamingFrameReceived { context ->
                if (!isActive()) return@onLLMStreamingFrameReceived
                when (val frame = context.streamFrame) {
                  is StreamFrame.TextDelta -> {
                    streamedText.updateAndGet { it + frame.text }
                    onDelta(frame.text)
                  }
                  else -> Unit
                }
              }
            }
            .build()

        agent.use { runningAgent ->
          val result = runningAgent.run(userMessage)
          val combined = streamedText.get().ifBlank { result }
          if (combined.isNotBlank()) return combined
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // Fallback: direct OpenAI-compatible streaming if Koog streaming strategy is unavailable at runtime.
      return OpenAiCompatibleLlmClient.streamChat(
          config = config,
          systemPrompt = systemPrompt,
          history = history,
          userMessage = userMessage,
          temperature = temperature,
          isActive = isActive,
          onDelta = onDelta,
      )
    }

    val fromStream = streamedText.get()
    if (fromStream.isNotBlank()) return fromStream

    return OpenAiCompatibleLlmClient.streamChat(
        config = config,
        systemPrompt = systemPrompt,
        history = history,
        userMessage = userMessage,
        temperature = temperature,
        isActive = isActive,
        onDelta = onDelta,
    )
  }

  suspend fun testConnection(config: LlmConfig): Result<String> {
    return OpenAiCompatibleLlmClient.testConnection(config)
  }

  private fun systemPromptFor(mode: ExecutionMode): String = when (mode) {
    ExecutionMode.SWARM -> """
      You are the Cowork Swarm orchestrator for a mobile Ubuntu proot desktop environment.
      Decompose the user's task into a clear numbered plan for specialized agents (Planner, Researcher, Executor, Coder, Validator).
      Be concise, actionable, and mention which agent handles each step.
      Do not claim you already executed commands unless tool results are provided.
    """.trimIndent()
    ExecutionMode.FAST -> """
      You are the Cowork Fast agent for a mobile Ubuntu proot desktop environment.
      Respond directly and concisely. Prefer immediate, practical steps the user can run.
      Do not produce a multi-agent plan unless the user explicitly asks for one.
    """.trimIndent()
  }

  fun parseSwarmTasks(response: String, userTask: String): List<SwarmTask> {
    val lines = response.lines()
    val numbered = lines.mapNotNull { line ->
      val trimmed = line.trim()
      val match = Regex("^(\\d+)[.)]\\s+(.+)").find(trimmed)
      match?.let { it.groupValues[1] to it.groupValues[2].trim() }
    }
    if (numbered.isEmpty()) {
      return listOf(
          SwarmTask("1", "Analyze: $userTask"),
          SwarmTask("2", "Plan execution in proot"),
          SwarmTask("3", "Execute and verify"),
      )
    }
    return numbered.take(6).map { (id, title) -> SwarmTask(id, title) }
  }
}
