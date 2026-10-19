package com.keshtoim.forge.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(AndroidJUnit4::class)
class AiConnectionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer().apply { start() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cipher = object : SecretCipher {
        override fun encrypt(plain: String) = plain
        override fun decrypt(stored: String) = stored
    }
    private val settings = AiSettings(context, cipher)

    // Robolectric's default network has no internet capability, which the status would read as offline.
    init {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance().also { shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
    }
    private val connection = AiConnection(context, settings, scope, server.url("/").toString())

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun awaitStatus(expected: AiStatus) = runBlocking { withTimeout(10_000) { connection.status.first { it == expected } } }

    private fun awaitFailure(failure: AiFailure) =
        runBlocking { withTimeout(10_000) { connection.status.first { it is AiStatus.Failed && it.error.failure == failure } } }

    // The check asks the model for a real one-word answer.
    private fun answer(code: Int) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(
        if (code == 200) {
            """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5","content":[{"type":"text","text":"OK"}],""" +
                """"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":1}}"""
        } else {
            """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        },
    )

    @Test
    fun `status stays off until the assistant is on with a key, then reflects the check`() {
        awaitStatus(AiStatus.Off)
        settings.setProvider(AiProvider.CLAUDE)
        settings.setKey(AiProvider.CLAUDE, "bad")
        settings.setEnabled(true)
        awaitStatus(AiStatus.Checking)

        server.enqueue(answer(401))
        connection.verify()
        awaitFailure(AiFailure.BAD_KEY)

        settings.setKey(AiProvider.CLAUDE, "good")
        awaitStatus(AiStatus.Checking)
        server.enqueue(answer(200))
        connection.verify()
        awaitStatus(AiStatus.Ready)
        assertEquals("/v1/messages?beta=true", run { server.takeRequest(); server.takeRequest().path })
    }

    @Test
    fun `an unreachable service is reported even while the device is online`() {
        settings.setProvider(AiProvider.CLAUDE)
        settings.setKey(AiProvider.CLAUDE, "key")
        settings.setEnabled(true)
        server.shutdown()
        connection.verify()
        awaitFailure(AiFailure.NO_CONNECTION)
    }

    @Test
    fun `a check loads the list, moves to an offered model and confirms it`() {
        settings.setProvider(AiProvider.CLAUDE)
        settings.setKey(AiProvider.CLAUDE, "key")
        settings.setModel(AiProvider.CLAUDE, "claude-retired-1")
        settings.setEnabled(true)
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(
                """{"data":[{"type":"model","id":"claude-sonnet-5-5","display_name":"Claude Sonnet 5.5","created_at":"2026-01-01T00:00:00Z"},""" +
                    """{"type":"model","id":"claude-opus-5-5","display_name":"Claude Opus 5.5","created_at":"2026-01-01T00:00:00Z"}],"has_more":false,"first_id":"a","last_id":"b"}""",
            ),
        )
        server.enqueue(answer(200))

        assertEquals(2, runBlocking { connection.check() })
        assertEquals("claude-opus-5-5", settings.state.value.model(AiProvider.CLAUDE))
        assertEquals(listOf("claude-sonnet-5-5", "claude-opus-5-5"), settings.state.value.choices(AiProvider.CLAUDE).map { it.id })
        awaitStatus(AiStatus.Ready)
        server.takeRequest()
        assertEquals("/v1/messages?beta=true", server.takeRequest().path)
    }
}
