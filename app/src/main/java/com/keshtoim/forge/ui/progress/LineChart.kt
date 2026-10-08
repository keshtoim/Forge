package com.keshtoim.forge.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(points: List<Point>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier) {
        val pad = 8.dp.toPx()
        val width = size.width - 2 * pad
        val height = size.height - 2 * pad
        repeat(3) { i ->
            val y = pad + height * i / 2
            drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }
        if (points.isEmpty()) return@Canvas

        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }
        val minT = points.first().time
        val maxT = points.last().time
        val offsets = points.map { p ->
            val x = if (maxT == minT) pad + width / 2 else pad + width * ((p.time - minT).toFloat() / (maxT - minT))
            val y = if (maxV == minV) pad + height / 2 else pad + height * (1 - ((p.value - minV) / (maxV - minV)).toFloat())
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        offsets.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
    }
}
