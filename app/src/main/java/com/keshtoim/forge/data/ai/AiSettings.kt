package com.keshtoim.forge.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AiState(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.GEMINI,
    val keys: Map<AiProvider, String> = emptyMap(),
    val models: Map<AiProvider, String> = emptyMap(),
) {
    fun model(provider: AiProvider) = models[provider]?.takeIf { it.isNotBlank() } ?: provider.defaultModel

    // Null while the assistant is off or the chosen provider has no key: nothing may be sent then.
    val config: AiConfig?
        get() = keys[provider]?.takeIf { enabled && it.isNotBlank() }?.let { AiConfig(provider, it, model(provider)) }
}

interface SecretCipher {
    fun encrypt(plain: String): String

    // Null when the stored value can't be read back, e.g. after a restore onto another device.
    fun decrypt(stored: String): String?
}

// API keys are encrypted with a key that never leaves the Android Keystore, so a copied preferences file is useless.
class KeystoreCipher : SecretCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as SecretKey?)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }

    override fun decrypt(stored: String): String? = runCatching {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE)) }
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE))
    }.getOrNull()

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "forge_ai_keys"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}

class AiSettings(context: Context, private val cipher: SecretCipher = KeystoreCipher()) {
    private val prefs = context.getSharedPreferences("ai", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AiState(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            provider = prefs.getString(KEY_PROVIDER, null)?.let { name -> AiProvider.entries.firstOrNull { it.name == name } } ?: AiProvider.GEMINI,
            keys = AiProvider.entries.mapNotNull { p -> prefs.getString(keyOf(p), null)?.let(cipher::decrypt)?.let { p to it } }.toMap(),
            models = AiProvider.entries.mapNotNull { p -> prefs.getString(modelOf(p), null)?.let { p to it } }.toMap(),
        ),
    )
    val state: StateFlow<AiState> = _state

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setProvider(provider: AiProvider) {
        prefs.edit { putString(KEY_PROVIDER, provider.name) }
        _state.value = _state.value.copy(provider = provider)
    }

    fun setKey(provider: AiProvider, key: String) {
        val trimmed = key.trim()
        prefs.edit { if (trimmed.isEmpty()) remove(keyOf(provider)) else putString(keyOf(provider), cipher.encrypt(trimmed)) }
        _state.value = _state.value.copy(keys = _state.value.keys + (provider to trimmed))
    }

    fun setModel(provider: AiProvider, model: String) {
        prefs.edit { putString(modelOf(provider), model.trim()) }
        _state.value = _state.value.copy(models = _state.value.models + (provider to model.trim()))
    }

    private fun keyOf(provider: AiProvider) = "key_${provider.name}"

    private fun modelOf(provider: AiProvider) = "model_${provider.name}"

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_PROVIDER = "provider"
    }
}
