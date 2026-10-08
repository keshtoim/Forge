package com.keshtoim.forge.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    return now
}

@Composable
fun rememberElapsed(startedAt: Long): String = formatDuration(((rememberNow() - startedAt) / 1000).coerceAtLeast(0))

@Composable
fun rememberRemaining(endsAt: Long): String = formatDuration(((endsAt - rememberNow() + 999) / 1000).coerceAtLeast(0))

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
