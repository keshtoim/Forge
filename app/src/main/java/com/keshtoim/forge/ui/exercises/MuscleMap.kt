package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.MuscleGroup.ABS
import com.keshtoim.forge.data.db.MuscleGroup.BACK
import com.keshtoim.forge.data.db.MuscleGroup.BICEPS
import com.keshtoim.forge.data.db.MuscleGroup.CALVES
import com.keshtoim.forge.data.db.MuscleGroup.CHEST
import com.keshtoim.forge.data.db.MuscleGroup.FOREARMS
import com.keshtoim.forge.data.db.MuscleGroup.GLUTES
import com.keshtoim.forge.data.db.MuscleGroup.HAMSTRINGS
import com.keshtoim.forge.data.db.MuscleGroup.QUADS
import com.keshtoim.forge.data.db.MuscleGroup.SHOULDERS
import com.keshtoim.forge.data.db.MuscleGroup.TRICEPS

// Each view is drawn in a 100×220 box as its left half (x ≤ 50) and mirrored around x = 50.
private const val VIEW_WIDTH = 100f
private const val VIEW_HEIGHT = 220f
private const val VIEW_GAP = 15f
private const val MAP_WIDTH = 2 * VIEW_WIDTH + VIEW_GAP

private fun path(data: String) = PathParser().parsePathString(data).toPath()

private class Region(val muscle: MuscleGroup, data: String) {
    val path = path(data)
}

private val BODY = path(
    "M50 27 C47 27 45 28 45 31 C45 34 43 35 39 36 C30 37 22 38 19 46 C17 52 17 60 17 68 C16 76 15 82 14 86 " +
        "C12 96 10 106 10 116 C9 120 8 126 10 131 C12 133 15 132 16 128 C16 124 16 120 17 117 C19 106 22 96 24 86 " +
        "C25 80 26 74 27 66 L28 60 C29 72 31 86 32 98 C31 106 30 112 30 120 C29 134 30 148 32 160 " +
        "C31 172 31 186 34 200 L33 210 C33 213 36 214 40 214 L45 214 C46 210 45 206 44 202 C46 190 47 176 45 164 " +
        "C45 152 46 138 47 128 L50 123 Z",
)

private const val DELTOID = "M38 37 C30 37 22 39 19 47 C18 53 18 58 19 61 C22 55 26 49 32 44 C35 42 37 40 38 37 Z"
private const val FOREARM = "M15 88 C13 96 11 106 11 114 L16 115 C18 106 21 97 23 88 C21 86 17 86 15 88 Z"

private val FRONT = listOf(
    Region(SHOULDERS, DELTOID),
    Region(CHEST, "M49 40 L49 61 C44 64 37 63 32 59 C29 55 29 49 31 45 C35 41 42 39 49 40 Z"),
    Region(BICEPS, "M20 62 C18 68 18 76 19 82 C21 84 24 82 25 78 C26 72 26 66 25 60 C23 59 21 60 20 62 Z"),
    Region(FOREARMS, FOREARM),
    Region(ABS, "M49 64 L49 77 L40 77 L40 68 C42 65 46 64 49 64 Z"),
    Region(ABS, "M49 78 L49 91 L40 91 L40 78 Z"),
    Region(ABS, "M49 92 L49 106 C46 108 42 107 41 104 L40 92 Z"),
    Region(ABS, "M38 66 L39 102 C36 100 33 94 32 86 C31 78 32 70 33 64 C35 64 37 65 38 66 Z"),
    Region(QUADS, "M32 118 C30 130 30 146 34 157 C38 160 42 160 44.5 157 C46 146 46 134 45.5 124 C42 119 37 116 32 118 Z"),
    Region(CALVES, "M32 166 C29 177 31 190 35 199 L38 199 C39 188 39 176 38 165 C36 164 34 164 32 166 Z"),
    Region(CALVES, "M42 165 C45 172 45.5 184 44 196 L41 196 C40 186 40 175 40 166 C41 165 41 165 42 165 Z"),
)

private val BACK_VIEW = listOf(
    Region(BACK, "M50 27 L50 62 C46 56 41 48 35 41 C40 39 44 36 45 31 C46 29 48 27 50 27 Z"),
    Region(BACK, "M32 47 C30 58 32 76 40 94 C43 97 46 99 49 100 L49 66 C44 60 38 52 32 47 Z"),
    Region(SHOULDERS, DELTOID),
    Region(TRICEPS, "M18 62 C17 68 17 76 18 82 C20 84 23 82 24 78 C25 72 25 66 25 61 C23 59 20 59 18 62 Z"),
    Region(FOREARMS, FOREARM),
    Region(GLUTES, "M49 104 C43 102 36 104 32 112 C30 120 32 128 38 131 C43 133 47 131 49 128 Z"),
    Region(HAMSTRINGS, "M32 136 C31 146 33 154 35 160 L44.5 160 C46 152 46 142 45.5 134 C41 135 37 135 32 136 Z"),
    Region(CALVES, "M33 166 C30 174 32 186 37 192 C40 194 44 193 45 190 C45.5 182 45.5 174 44.5 166 C41 163 36 163 33 166 Z"),
)

// Front and back body views; [primary] is highlighted fully and [secondary] muscles faintly.
// Groups without a body region (CARDIO, OTHER) are simply not shown.
@Composable
fun MuscleMap(primary: MuscleGroup, secondary: List<MuscleGroup>, modifier: Modifier = Modifier) {
    val body = MaterialTheme.colorScheme.surfaceVariant
    val idle = lerp(body, MaterialTheme.colorScheme.onSurfaceVariant, 0.3f)
    val strong = MaterialTheme.colorScheme.primary
    val weak = lerp(idle, strong, 0.5f)

    fun color(muscle: MuscleGroup) = when (muscle) {
        primary -> strong
        in secondary -> weak
        else -> idle
    }

    Canvas(modifier.aspectRatio(MAP_WIDTH / VIEW_HEIGHT)) {
        scale(size.width / MAP_WIDTH, pivot = Offset.Zero) {
            drawView(FRONT, body, ::color)
            translate(left = VIEW_WIDTH + VIEW_GAP) { drawView(BACK_VIEW, body, ::color) }
        }
    }
}

private fun DrawScope.drawView(regions: List<Region>, body: Color, color: (MuscleGroup) -> Color) {
    drawOval(body, topLeft = Offset(40f, 3f), size = Size(20f, 24f))
    mirrored { drawPath(BODY, body) }
    mirrored {
        regions.forEach {
            drawPath(it.path, color(it.muscle))
            // Outlining in the body colour leaves thin seams between adjacent muscles.
            drawPath(it.path, body, style = Stroke(width = 0.6f))
        }
    }
}

private fun DrawScope.mirrored(block: DrawScope.() -> Unit) {
    block()
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(VIEW_WIDTH / 2, 0f), block = block)
}
