package com.keshtoim.forge.rest

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Rest(val endsAt: Long, val totalSec: Int)

class RestTimer(private val context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<Rest?>(null)
    val state = _state.asStateFlow()

    fun start() {
        val seconds = prefs.getInt(KEY_REST_SECONDS, 90)
        _state.value = Rest(System.currentTimeMillis() + seconds * 1000L, seconds)
        ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java))
    }

    fun adjust(deltaSec: Int) {
        val rest = _state.value ?: return
        val total = (rest.totalSec + deltaSec).coerceAtLeast(MIN_REST_SECONDS)
        prefs.edit { putInt(KEY_REST_SECONDS, total) }
        val endsAt = rest.endsAt + (total - rest.totalSec) * 1000L
        _state.value = if (endsAt > System.currentTimeMillis()) Rest(endsAt, total) else null
    }

    fun stop() {
        _state.value = null
    }

    private companion object {
        const val KEY_REST_SECONDS = "rest_seconds"
        const val MIN_REST_SECONDS = 15
    }
}
