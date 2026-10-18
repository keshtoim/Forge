package com.keshtoim.forge.data.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaStopReason
import com.google.genai.Client
import com.google.genai.errors.ApiException
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.errors.OpenAIIoException
import com.openai.errors.OpenAIServiceException
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.anthropic.models.beta.messages.MessageCreateParams as ClaudeParams

enum class AiProvider(val defaultModel: String, val keyUrl: String) {
    GEMINI("gemini-2.5-flash", "https://aistudio.google.com/apikey"),
    OPENAI("gpt-5", "https://platform.openai.com/api-keys"),
    CLAUDE("claude-opus-5-5", "https://platform.claude.com/settings/keys"),
}

data class AiTurn(val fromUser: Boolean, val text: String)

enum class AiFailure { NO_CONNECTION, BAD_KEY, BAD_MODEL, RATE_LIMIT, REFUSED, OTHER }

class AiException(val failure: AiFailure, cause: Throwable? = null) : Exception(failure.name, cause)

data class AiConfig(val provider: AiProvider, val apiKey: String, val model: String)

// One request-response exchange with whichever provider the user picked; every call is a fresh conversation.
object AiAssistant {
    suspend fun reply(config: AiConfig, system: String, turns: List<AiTurn>): String = withContext(Dispatchers.IO) {
        try {
            when (config.provider) {
                AiProvider.GEMINI -> gemini(config, system, turns)
                AiProvider.OPENAI -> openAi(config, system, turns)
                AiProvider.CLAUDE -> claude(config, system, turns)
            }.trim().ifEmpty { throw AiException(AiFailure.OTHER) }
        } catch (e: AiException) {
            throw e
        } catch (e: Exception) {
            throw AiException(classify(e), e)
        }
    }

    private fun claude(config: AiConfig, system: String, turns: List<AiTurn>): String {
        val client = AnthropicOkHttpClient.builder().apiKey(config.apiKey).build()
        try {
            val params = ClaudeParams.builder()
                .model(config.model)
                .maxTokens(16_000L)
                .system(system)
                .apply {
                    turns.forEach { if (it.fromUser) addUserMessage(it.text) else addAssistantMessage(it.text) }
                    // A declined request is retried server-side on the model Anthropic picks for that refusal category.
                    if (config.model in CLAUDE_FALLBACK_MODELS) {
                        addBeta("server-side-fallback-2026-07-01")
                        putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    }
                }
                .build()
            val message = client.beta().messages().create(params)
            if (message.stopReason().orElse(null) == BetaStopReason.REFUSAL) throw AiException(AiFailure.REFUSED)
            return message.content().joinToString("") { block -> block.text().map { it.text() }.orElse("") }
        } finally {
            client.close()
        }
    }

    private fun openAi(config: AiConfig, system: String, turns: List<AiTurn>): String {
        val client = OpenAIOkHttpClient.builder().apiKey(config.apiKey).build()
        try {
            val params = ChatCompletionCreateParams.builder()
                .model(config.model)
                .addSystemMessage(system)
                .apply { turns.forEach { if (it.fromUser) addUserMessage(it.text) else addAssistantMessage(it.text) } }
                .build()
            val choice = client.chat().completions().create(params).choices().firstOrNull() ?: return ""
            return choice.message().content().orElse("")
        } finally {
            client.close()
        }
    }

    private fun gemini(config: AiConfig, system: String, turns: List<AiTurn>): String {
        val client = Client.builder().apiKey(config.apiKey).build()
        try {
            val contents = turns.map { Content.builder().role(if (it.fromUser) "user" else "model").parts(Part.fromText(it.text)).build() }
            val settings = GenerateContentConfig.builder().systemInstruction(Content.fromParts(Part.fromText(system))).build()
            return client.models.generateContent(config.model, contents, settings).text().orEmpty()
        } finally {
            client.close()
        }
    }

    private fun classify(e: Exception): AiFailure = when (e) {
        is UnauthorizedException, is PermissionDeniedException -> AiFailure.BAD_KEY
        is NotFoundException -> AiFailure.BAD_MODEL
        is RateLimitException -> AiFailure.RATE_LIMIT
        is AnthropicServiceException -> status(e.statusCode())
        is OpenAIServiceException -> status(e.statusCode())
        is ApiException -> status(e.code())
        is AnthropicIoException, is OpenAIIoException, is IOException -> AiFailure.NO_CONNECTION
        else -> if (e.cause is IOException) AiFailure.NO_CONNECTION else AiFailure.OTHER
    }

    // Gemini answers a bad key with 400 and the others with 401/403.
    private fun status(code: Int) = when (code) {
        400, 401, 403 -> AiFailure.BAD_KEY
        404 -> AiFailure.BAD_MODEL
        429 -> AiFailure.RATE_LIMIT
        else -> AiFailure.OTHER
    }

    private val CLAUDE_FALLBACK_MODELS = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5")
}
