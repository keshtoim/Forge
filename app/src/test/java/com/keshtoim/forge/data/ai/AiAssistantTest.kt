package com.keshtoim.forge.data.ai

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantTest {
    private val server = MockWebServer().apply { start() }
    private val turns = listOf(AiTurn(true, "Сколько отдыхать?"), AiTurn(false, "2 минуты"), AiTurn(true, "А между кругами?"))

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun ask(provider: AiProvider) =
        runBlocking { AiAssistant.reply(AiConfig(provider, "key-123", provider.defaultModel), "Ты тренер.", turns, server.url("/").toString()) }

    private fun failure(provider: AiProvider) = runCatching { ask(provider) }.exceptionOrNull().let { (it as AiException).failure }

    @Test
    fun `claude gets the system prompt, the dialog and a refusal fallback`() {
        server.enqueue(
            json(
                200,
                """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5","content":[{"type":"text","text":"Полторы минуты"}],""" +
                    """"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}""",
            ),
        )
        assertEquals("Полторы минуты", ask(AiProvider.CLAUDE))

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals("key-123", request.getHeader("x-api-key"))
        assertTrue(request.getHeader("anthropic-beta").orEmpty().contains("server-side-fallback-2026-07-01"))
        assertTrue(body.contains("\"fallbacks\":\"default\"") && body.contains("Ты тренер.") && body.contains("А между кругами?"))
    }

    @Test
    fun `chatgpt answer is read from the first choice`() {
        server.enqueue(
            json(
                200,
                """{"id":"c1","object":"chat.completion","created":1,"model":"gpt-5","choices":[{"index":0,""" +
                    """"message":{"role":"assistant","content":"Полторы минуты","refusal":null},"finish_reason":"stop","logprobs":null}]}""",
            ),
        )
        assertEquals("Полторы минуты", ask(AiProvider.OPENAI))
        assertTrue(server.takeRequest().body.readUtf8().contains("Ты тренер."))
    }

    @Test
    fun `gemini answer is read from the first candidate`() {
        server.enqueue(json(200, """{"candidates":[{"content":{"role":"model","parts":[{"text":"Полторы минуты"}]},"finishReason":"STOP"}]}"""))
        assertEquals("Полторы минуты", ask(AiProvider.GEMINI))
        val request = server.takeRequest()
        assertTrue(request.path.orEmpty().contains("${AiProvider.GEMINI.defaultModel}:generateContent"))
        assertTrue(request.body.readUtf8().contains("Ты тренер."))
    }

    @Test
    fun `a rejected key is reported the same way for every service`() {
        server.enqueue(json(401, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""))
        assertEquals(AiFailure.BAD_KEY, failure(AiProvider.CLAUDE))
        server.enqueue(json(401, """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error","code":"invalid_api_key"}}"""))
        assertEquals(AiFailure.BAD_KEY, failure(AiProvider.OPENAI))
        server.enqueue(json(400, """{"error":{"code":400,"message":"API key not valid.","status":"INVALID_ARGUMENT"}}"""))
        assertEquals(AiFailure.BAD_KEY, failure(AiProvider.GEMINI))
    }

    @Test
    fun `an unknown model and an unreachable server are told apart`() {
        server.enqueue(json(404, """{"type":"error","error":{"type":"not_found_error","message":"model: nope"}}"""))
        assertEquals(AiFailure.BAD_MODEL, failure(AiProvider.CLAUDE))
        server.shutdown()
        assertEquals(AiFailure.NO_CONNECTION, failure(AiProvider.OPENAI))
    }

    @Test
    fun `a chatgpt refusal is told apart from an empty answer`() {
        server.enqueue(
            json(
                200,
                """{"id":"c1","object":"chat.completion","created":1,"model":"gpt-5","choices":[{"index":0,""" +
                    """"message":{"role":"assistant","content":null,"refusal":"I can't help with that."},"finish_reason":"stop","logprobs":null}]}""",
            ),
        )
        assertEquals(AiFailure.REFUSED, failure(AiProvider.OPENAI))
    }

    private fun list(provider: AiProvider) =
        runBlocking { AiAssistant.listModels(AiConfig(provider, "key-123", provider.defaultModel), server.url("/").toString()) }

    @Test
    fun `model lists keep chat models only, with readable names`() {
        server.enqueue(
            json(
                200,
                """{"models":[{"name":"models/gemini-2.5-flash","displayName":"Gemini 2.5 Flash","supportedGenerationMethods":["generateContent","countTokens"]},""" +
                    """{"name":"models/text-embedding-004","displayName":"Embedding","supportedGenerationMethods":["embedContent"]}]}""",
            ),
        )
        assertEquals(listOf(AiModel("gemini-2.5-flash", "Gemini 2.5 Flash")), list(AiProvider.GEMINI))

        server.enqueue(
            json(
                200,
                """{"object":"list","data":[{"id":"gpt-5","object":"model","created":2,"owned_by":"openai"},""" +
                    """{"id":"text-embedding-3-small","object":"model","created":5,"owned_by":"openai"},{"id":"gpt-5-mini","object":"model","created":3,"owned_by":"openai"}]}""",
            ),
        )
        assertEquals(listOf("gpt-5-mini", "gpt-5"), list(AiProvider.OPENAI).map { it.id })

        server.enqueue(
            json(
                200,
                """{"data":[{"type":"model","id":"claude-opus-5-5","display_name":"Claude Opus 5.5","created_at":"2026-01-01T00:00:00Z"}],""" +
                    """"has_more":false,"first_id":"claude-opus-5-5","last_id":"claude-opus-5-5"}""",
            ),
        )
        assertEquals(listOf(AiModel("claude-opus-5-5", "Claude Opus 5.5")), list(AiProvider.CLAUDE))
    }

    @Test
    fun `the chosen model survives a refresh while it is still offered`() {
        val offered = listOf("gpt-5", "gpt-5-mini")
        assertEquals("gpt-5-mini", selectModel("gpt-5-mini", offered, "gpt-5"))
        assertEquals("gpt-5", selectModel("gpt-4", offered, "gpt-5"))
        assertEquals("gpt-5-nano", selectModel("gpt-4", listOf("gpt-5-nano", "gpt-5"), "gpt-9"))
        assertEquals("gpt-4", selectModel("gpt-4", emptyList(), "gpt-5"))
        assertTrue(isOpenAiChatModel("o4-mini") && isOpenAiChatModel("chatgpt-4o-latest"))
        assertTrue(listOf("gpt-4o-realtime-preview", "gpt-image-1", "omni-moderation-latest", "gpt-5-codex", "whisper-1").none(::isOpenAiChatModel))
    }
}
