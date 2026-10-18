package com.keshtoim.forge.data.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Robolectric has no Android Keystore; reversing the text is enough to see that nothing is stored in the clear.
    private val cipher = object : SecretCipher {
        override fun encrypt(plain: String) = plain.reversed()
        override fun decrypt(stored: String) = stored.reversed()
    }

    @Test
    fun `nothing is sent until the assistant is on and the chosen service has a key`() {
        val settings = AiSettings(context, cipher)
        assertNull(settings.state.value.config)

        settings.setKey(AiProvider.CLAUDE, "  sk-ant-123 ")
        settings.setProvider(AiProvider.CLAUDE)
        assertNull(settings.state.value.config)

        settings.setEnabled(true)
        assertEquals(AiConfig(AiProvider.CLAUDE, "sk-ant-123", AiProvider.CLAUDE.defaultModel), settings.state.value.config)

        settings.setProvider(AiProvider.GEMINI)
        assertNull(settings.state.value.config)
    }

    @Test
    fun `keys and models survive a restart and keys are stored encrypted`() {
        AiSettings(context, cipher).apply {
            setEnabled(true)
            setProvider(AiProvider.OPENAI)
            setKey(AiProvider.OPENAI, "sk-proj-abc")
            setModel(AiProvider.OPENAI, "gpt-5-mini")
        }

        val restored = AiSettings(context, cipher).state.value
        assertEquals(AiConfig(AiProvider.OPENAI, "sk-proj-abc", "gpt-5-mini"), restored.config)
        val raw = context.getSharedPreferences("ai", Context.MODE_PRIVATE).all.values.map { it.toString() }
        assertFalse(raw.any { "sk-proj-abc" in it })
    }

    @Test
    fun `a loaded model list is kept per key and moves the selection to an offered model`() {
        val settings = AiSettings(context, cipher)
        settings.setKey(AiProvider.OPENAI, "sk-one")
        settings.setModel(AiProvider.OPENAI, "gpt-4-turbo")
        assertEquals(AiProvider.OPENAI.knownModels, settings.state.value.choices(AiProvider.OPENAI).map { it.id })

        val listed = listOf(AiModel("gpt-5-mini", "gpt-5-mini"), AiModel("gpt-5", "gpt-5"))
        settings.setCatalog(AiProvider.OPENAI, listed)
        assertEquals("gpt-5", settings.state.value.model(AiProvider.OPENAI))
        assertEquals(listed, AiSettings(context, cipher).state.value.choices(AiProvider.OPENAI))

        settings.setKey(AiProvider.OPENAI, "sk-two")
        assertEquals(AiProvider.OPENAI.knownModels, settings.state.value.choices(AiProvider.OPENAI).map { it.id })
        assertEquals(AiProvider.OPENAI.knownModels, AiSettings(context, cipher).state.value.choices(AiProvider.OPENAI).map { it.id })
    }
}
