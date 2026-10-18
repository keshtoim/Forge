package com.keshtoim.forge.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.ai.AiStatus
import com.keshtoim.forge.ui.theme.ForgeTheme

// A small "AI" tag whose dot tells at a glance whether the assistant can answer right now.
@Composable
fun AiIndicator(status: AiStatus, modifier: Modifier = Modifier) {
    if (status == AiStatus.Off) return
    val colors = MaterialTheme.colorScheme
    val dot = when (status) {
        AiStatus.Ready -> ForgeTheme.effort.light
        AiStatus.Checking -> colors.outline
        else -> colors.error
    }
    val description = when (status) {
        AiStatus.Ready -> stringResource(R.string.ai_status_ready)
        AiStatus.Checking -> stringResource(R.string.ai_status_checking)
        AiStatus.Offline -> stringResource(R.string.ai_error_connection)
        is AiStatus.Failed -> stringResource(status.failure.labelRes)
        AiStatus.Off -> ""
    }
    Row(
        modifier.clip(CircleShape).background(colors.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 6.dp).semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == AiStatus.Checking) PulsingDot(dot) else Dot(dot, Modifier)
        Text(
            stringResource(R.string.ai_short),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun Dot(color: Color, modifier: Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}

// The pulse runs only while checking: the workout screen stays on for an hour and a constant animation would cost battery.
@Composable
private fun PulsingDot(color: Color) {
    val pulse by rememberInfiniteTransition(label = "ai").animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    Dot(color, Modifier.graphicsLayer { alpha = pulse })
}
