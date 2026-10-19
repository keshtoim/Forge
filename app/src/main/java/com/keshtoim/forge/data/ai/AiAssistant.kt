package com.keshtoim.forge.data.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.beta.messages.BetaBase64ImageSource
import com.anthropic.models.beta.messages.BetaContentBlockParam
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams as ClaudeParams
import com.google.genai.Client
import com.google.genai.errors.ApiException
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.HttpOptions
import com.google.genai.types.ListModelsConfig
import com.google.genai.types.Part
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.errors.OpenAIIoException
import com.openai.errors.OpenAIServiceException
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.IOException
import java.util.Base64

// [knownModels] are offered until the provider's own list is loaded; the first one is the default.
enum class AiProvider(val title: String, val knownModels: List<String>, val keyUrl: String, val modelsUrl: String) {
    GEMINI(
        "Gemini",
        listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-2.5-flash"),
        "https://aistudio.google.com/apikey",
        "https://ai.google.dev/gemini-api/docs/models",
    ),
    OPENAI("ChatGPT", listOf("gpt-5", "gpt-5-mini", "gpt-5-nano"), "https://platform.openai.com/api-keys", "https://platform.openai.com/docs/models"),
    CLAUDE(
        "Claude",
        listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5"),
        "https://platform.claude.com/settings/keys",
        "https://platform.claude.com/docs/en/about-claude/models/overview",
    ),
    ;

    val defaultModel get() = knownModels.first()
}

// [images] are JPEG bytes the user attached; replies never carry any.
data class AiTurn(val fromUser: Boolean, val text: String, val images: List<ByteArray> = emptyList())

private fun ByteArray.base64(): String = Base64.getEncoder().encodeToString(this)

@Serializable
data class AiModel(val id: String, val name: String)

// Keeps the chosen model while the provider still offers it, otherwise falls back to the default, then to the first one listed.
fun selectModel(current: String, available: List<String>, preferred: String): String = when {
    available.isEmpty() || current in available -> current
    preferred in available -> preferred
    else -> available.first()
}

// Providers list models for speech, images, embeddings and live audio next to chat ones; picking one of those breaks every answer.
private val NOT_FOR_CHAT = listOf(
    "embedding", "tts", "whisper", "dall-e", "moderation", "image", "imagen", "veo", "realtime", "audio", "live",
    "transcribe", "search", "instruct", "codex", "computer-use", "robotics", "aqa",
)

fun isChatModel(id: String) = NOT_FOR_CHAT.none { it in id.lowercase() }

// OpenAI lists every model the key can reach; only the ones that answer chat completions are useful here.
fun isOpenAiChatModel(id: String): Boolean {
    val chatFamily = id.startsWith("gpt-") || id.startsWith("chatgpt-") || Regex("^o\\d").containsMatchIn(id)
    return chatFamily && isChatModel(id)
}

enum class AiFailure { NO_CONNECTION, BAD_KEY, BAD_MODEL, RATE_LIMIT, NO_CREDIT, REGION, REFUSED, OTHER }

// [detail] is the provider's own explanation, shown next to our message when there is one.
class AiException(val failure: AiFailure, cause: Throwable? = null, val detail: String? = null) : Exception(failure.name, cause)

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

    // A one-word answer along the same path as the chat. Looking a model up is free but passes for models that cannot chat,
    // and for regions and accounts the service refuses to answer, so only a real reply proves the assistant works.
    suspend fun probe(config: AiConfig, baseUrl: String? = null) {
        reply(config, "Reply with the single word OK.", listOf(AiTurn(fromUser = true, text = "Are you there?")), baseUrl)
    }

    // The models the key can use, as the provider lists them; listing is free on all three services.
    suspend fun listModels(config: AiConfig, baseUrl: String? = null): List<AiModel> = call(config) {
        when (config.provider) {
            AiProvider.GEMINI -> withGemini(config, baseUrl) { client ->
                client.models.list(ListModelsConfig.builder().pageSize(100).build())
                    .filter { "generateContent" in it.supportedActions().orElse(emptyList()) && isChatModel(it.name().orElse("")) }
                    .map { model ->
                        val id = model.name().orElse("").removePrefix("models/")
                        AiModel(id, model.displayName().orElse(id))
                    }
            }
            AiProvider.OPENAI -> withOpenAi(config, baseUrl) { client ->
                client.models().list().autoPager().filter { isOpenAiChatModel(it.id()) }
                    .sortedByDescending { it.created() }
                    .map { AiModel(it.id(), it.id()) }
            }
            AiProvider.CLAUDE -> withClaude(config, baseUrl) { client ->
                client.models().list().autoPager().map { AiModel(it.id(), it.displayName()) }
            }
        }.filter { it.id.isNotBlank() }.distinctBy { it.id }
    }

    private suspend fun <T> call(config: AiConfig, block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: AiException) {
            throw e
        } catch (e: Exception) {
            throw classify(e)
        }
    }

    private fun claude(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String?): String = withClaude(config, baseUrl) { client ->
        val params = ClaudeParams.builder()
            .model(config.model)
            .maxTokens(16_000L)
            .system(system)
            .apply {
                turns.forEach { turn ->
                    when {
                        !turn.fromUser -> addAssistantMessage(turn.text)
                        turn.images.isEmpty() -> addUserMessage(turn.text)
                        else -> addUserMessageOfBetaContentBlockParams(
                            turn.images.map { BetaContentBlockParam.ofImage(BetaBase64ImageSource.builder().data(it.base64()).mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG).build()) } +
                                listOfNotNull(turn.text.takeIf { it.isNotBlank() }?.let { BetaContentBlockParam.ofText(it) }),
                        )
                    }
                }
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
            .apply {
                turns.forEach { turn ->
                    when {
                        !turn.fromUser -> addAssistantMessage(turn.text)
                        turn.images.isEmpty() -> addUserMessage(turn.text)
                        else -> addUserMessageOfArrayOfContentParts(
                            turn.images.map { image ->
                                val url = ChatCompletionContentPartImage.ImageUrl.builder().url("data:image/jpeg;base64,${image.base64()}").build()
                                ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder().imageUrl(url).build())
                            } + listOfNotNull(turn.text.takeIf { it.isNotBlank() }?.let { ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder().text(it).build()) }),
                        )
                    }
                }
            }
            .build()
        val message = client.chat().completions().create(params).choices().firstOrNull()?.message() ?: return@withOpenAi ""
        if (message.refusal().isPresent) throw AiException(AiFailure.REFUSED)
        message.content().orElse("")
    }

    private fun gemini(config: AiConfig, system: String, turns: List<AiTurn>, baseUrl: String?): String = withGemini(config, baseUrl) { client ->
        val contents = turns.map { turn ->
            val parts = turn.images.map { Part.fromBytes(it, "image/jpeg") } + listOfNotNull(turn.text.takeIf { it.isNotBlank() }?.let { Part.fromText(it) })
            Content.builder().role(if (turn.fromUser) "user" else "model").parts(parts).build()
        }
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

    private fun classify(e: Exception): AiException = when (e) {
        is AnthropicServiceException -> serviceFailure(e.statusCode(), "${e.message} ${e.body()}", e)
        is OpenAIServiceException -> serviceFailure(e.statusCode(), "${e.message} ${e.body()}", e)
        is ApiException -> serviceFailure(e.code(), e.message.orEmpty(), e)
        is AnthropicIoException, is OpenAIIoException, is IOException -> AiException(AiFailure.NO_CONNECTION, e)
        else -> AiException(if (e.cause is IOException) AiFailure.NO_CONNECTION else AiFailure.OTHER, e, e.message)
    }

    // Region and billing problems arrive under the same status codes as a bad key, so the provider's text decides.
    private fun serviceFailure(code: Int, text: String, cause: Exception): AiException {
        val lower = text.lowercase()
        val failure = when {
            REGION_HINTS.any { it in lower } -> AiFailure.REGION
            CREDIT_HINTS.any { it in lower } || code == 402 -> AiFailure.NO_CREDIT
            code == 401 || code == 403 || (code == 400 && KEY_HINTS.any { it in lower }) -> AiFailure.BAD_KEY
            code == 404 -> AiFailure.BAD_MODEL
            code == 429 -> AiFailure.RATE_LIMIT
            else -> AiFailure.OTHER
        }
        return AiException(failure, cause, readableMessage(text))
    }

    // Error bodies are JSON with a "message" field; that sentence is what the user can act on.
    private fun readableMessage(text: String): String =
        Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(text)?.groupValues?.get(1)?.replace("\\n", " ")?.trim()
            ?.takeIf { it.isNotEmpty() } ?: text.trim().replace(Regex("""^\d{3}\s+[A-Z_]+\.\s*"""), "").take(300)

    private val REGION_HINTS = listOf("location is not supported", "unsupported_country", "country, region, or territory", "request not allowed")
    private val CREDIT_HINTS = listOf("insufficient_quota", "credit balance", "billing", "exceeded your current quota")
    private val KEY_HINTS = listOf("api key", "api_key")

    private val CLAUDE_FALLBACK_MODELS = setOf("claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5")
}
