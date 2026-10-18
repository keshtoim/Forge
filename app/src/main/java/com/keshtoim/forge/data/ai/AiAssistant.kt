package com.keshtoim.forge.data.ai

import com.anthropic.client.AnthropicClient
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
import com.google.genai.types.HttpOptions
import com.google.genai.types.Part
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.errors.OpenAIIoException
import com.openai.errors.OpenAIServiceException
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.anthropic.models.beta.messages.MessageCreateParams as ClaudeParams

enum class AiProvider(val title: String, val defaultModel: String, val keyUrl: String) {
    GEMINI("Gemini", "gemini-2.5-flash", "https://aistudio.google.com/apikey"),
    OPENAI("ChatGPT", "gpt-5", "https://platform.openai.com/api-keys"),
    CLAUDE("Claude", "claude-opus-5-5", "https://platform.claude.com/settings/keys"),
}

data class AiTurn(val fromUser: Boolean, val text: String)

enum class AiFailure { NO_CONNECTION, BAD_KEY, BAD_MODEL, RATE_LIMIT, REFUSED, OTHER }

class AiException(val failure: AiFailure, cause: Throwable? = null) : Exception(failure.name, cause)

data class AiConfig(val provider: AiProvider, val apiKey: String, val model: String)

// One request-response exchange with whichever provider the user picked; every call is a fresh conversation.
object AiAssistant {
    // [baseUrl] points the SDK at another server; only tests use it.
    suspend fun reply(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String? = null): String = call(config) {
        when (config.provider) {
            AiProvider.GEMINI -> gemini(config, system, turns, baseUrl)
            AiProvider.OPENAI -> openAi(config, system, turns, baseUrl)
            AiProvider.CLAUDE -> claude(config, system, turns, baseUrl)
        }.trim().ifEmpty { throw AiException(AiFailure.OTHER) }
    }

    // Looks the configured model up: costs no tokens, yet fails like a real request on a wrong key or model name.
    suspend fun ping(config: AiConfig, baseUrl: String? = null) = call(config) {
        when (config.provider) {
            AiProvider.GEMINI -> withGemini(config, baseUrl) { it.models.get(config.model, null) }
            AiProvider.OPENAI -> withOpenAi(config, baseUrl) { it.models().retrieve(config.model) }
            AiProvider.CLAUDE -> withClaude(config, baseUrl) { it.models().retrieve(config.model) }
        }
        Unit
    }

    private suspend fun <T> call(config: AiConfig, block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: AiException) {
            throw e
        } catch (e: Exception) {
            throw AiException(classify(e, config.provider), e)
        }
    }

    private fun claude(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String?): String = withClaude(config, baseUrl) { client ->
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
        message.content().joinToString("") { block -> block.text().map { it.text() }.orElse("") }
    }

    private fun openAi(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String?): String = withOpenAi(config, baseUrl) { client ->
        val params = ChatCompletionCreateParams.builder()
            .model(config.model)
            .addSystemMessage(system)
            .apply { turns.forEach { if (it.fromUser) addUserMessage(it.text) else addAssistantMessage(it.text) } }
            .build()
        val message = client.chat().completions().create(params).choices().firstOrNull()?.message() ?: return@withOpenAi ""
        if (message.refusal().isPresent) throw AiException(AiFailure.REFUSED)
        message.content().orElse("")
    }

    private fun gemini(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String?): String = withGemini(config, baseUrl) { client ->
        val contents = turns.map { Content.builder().role(if (it.fromUser) "user" else "model").parts(Part.fromText(it.text)).build() }
        val settings = GenerateContentConfig.builder().systemInstruction(Content.fromParts(Part.fromText(system))).build()
        client.models.generateContent(config.model, contents, settings).text().orEmpty()
    }

    private fun <T> withClaude(config: AiConfig, baseUrl: String?, block: (AnthropicClient) -> T): T {
        val client = AnthropicOkHttpClient.builder().apiKey(config.apiKey).apply { baseUrl?.let(::baseUrl) }.build()
        try {
            return block(client)
        } finally {
            client.close()
        }
    }

    private fun <T> withOpenAi(config: AiConfig, baseUrl: String?, block: (OpenAIClient) -> T): T {
        val client = OpenAIOkHttpClient.builder().apiKey(config.apiKey).apply { baseUrl?.let(::baseUrl) }.build()
        try {
            return block(client)
        } finally {
            client.close()
        }
    }

    private fun <T> withGemini(config: AiConfig, baseUrl: String?, block: (Client) -> T): T {
        val client = Client.builder().apiKey(config.apiKey).apply { baseUrl?.let { httpOptions(HttpOptions.builder().baseUrl(it).build()) } }.build()
        try {
            return block(client)
        } finally {
            client.close()
        }
    }

    private fun classify(e: Exception, provider: AiProvider): AiFailure = when (e) {
        is UnauthorizedException, is PermissionDeniedException -> AiFailure.BAD_KEY
        is NotFoundException -> AiFailure.BAD_MODEL
        is RateLimitException -> AiFailure.RATE_LIMIT
        is AnthropicServiceException -> status(e.statusCode(), provider)
        is OpenAIServiceException -> status(e.statusCode(), provider)
        is ApiException -> status(e.code(), provider)
        is AnthropicIoException, is OpenAIIoException, is IOException -> AiFailure.NO_CONNECTION
        else -> if (e.cause is IOException) AiFailure.NO_CONNECTION else AiFailure.OTHER
    }

    // Gemini answers a bad key with 400, the others with 401 or 403.
    private fun status(code: Int, provider: AiProvider) = when {
        code == 401 || code == 403 || (code == 400 && provider == AiProvider.GEMINI) -> AiFailure.BAD_KEY
        code == 404 -> AiFailure.BAD_MODEL
        code == 429 -> AiFailure.RATE_LIMIT
        else -> AiFailure.OTHER
    }

    private val CLAUDE_FALLBACK_MODELS = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5")
}
