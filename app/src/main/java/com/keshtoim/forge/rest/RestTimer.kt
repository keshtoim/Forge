package com.keshtoim.forge.rest

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Rest(val endsAt: Long, val totalSec: Int)

class RestTimer(private val context: Context) {
    private val _state = MutableStateFlow<Rest?>(null)
    val state = _state.asStateFlow()

    fun start(seconds: Int) {
        _state.value = Rest(System.currentTimeMillis() + seconds * 1000L, seconds)
        ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java))
    }

    // Returns the new length of this rest, so the caller can remember it for the exercise.
    fun adjust(deltaSec: Int): Int? {
        val rest = _state.value ?: return null
        val total = (rest.totalSec + deltaSec).coerceAtLeast(MIN_REST_SECONDS)
        val endsAt = rest.endsAt + (total - rest.totalSec) * 1000L
        _state.value = if (endsAt > System.currentTimeMillis()) Rest(endsAt, total) else null
        return total
    }

    fun stop() {
        _state.value = null
    }

    companion object {
        const val MIN_REST_SECONDS = 15
    }
}
