package com.keshtoim.forge.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.data.Reading
import com.keshtoim.forge.ui.profile.one
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// A line through at least two readings, spaced by date, with the first and last values and dates labelled.
@Composable
fun ReadingsChart(points: List<Reading>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val format = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val firstDay = points.first().epochDay
    val span = (points.last().epochDay - firstDay).coerceAtLeast(1)
    val label = MaterialTheme.typography.labelSmall
    Column(modifier) {
        Row {
            Text(one(points.first().value), style = label, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(one(points.last().value), style = label, color = colors.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
            val inset = 6.dp.toPx()
            fun at(p: Reading) = Offset(
                inset + (size.width - 2 * inset) * (p.epochDay - firstDay) / span,
                inset + (size.height - 2 * inset) * (if (max > min) ((max - p.value) / (max - min)).toFloat() else 0.5f),
            )
            drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val path = Path().apply {
                points.forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
            }
            drawPath(path, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.dropLast(1).forEach { drawCircle(colors.primary, 2.5.dp.toPx(), at(it)) }
            val last = at(points.last())
            drawCircle(colors.surfaceContainer, 7.dp.toPx(), last)
            drawCircle(colors.primary, 5.dp.toPx(), last)
        }
        Row {
            Text(LocalDate.ofEpochDay(firstDay).format(format), style = label, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(LocalDate.ofEpochDay(points.last().epochDay).format(format), style = label, color = colors.onSurfaceVariant)
        }
    }
}
