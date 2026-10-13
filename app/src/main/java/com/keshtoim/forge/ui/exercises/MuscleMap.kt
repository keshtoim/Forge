package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BodyType
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
import com.keshtoim.forge.ui.labelRes

// Each view is drawn in a 100×220 box as its left half (x ≤ 50) and mirrored around x = 50.
private const val VIEW_WIDTH = 100f
private const val VIEW_HEIGHT = 220f
private const val VIEW_GAP = 15f
private const val MAP_WIDTH = 2 * VIEW_WIDTH + VIEW_GAP

val LocalBodyType = staticCompositionLocalOf { BodyType.MALE }

private fun path(data: String) = PathParser().parsePathString(data).toPath()

private class Region(val muscle: MuscleGroup, val data: String)

// Body and muscle outlines below share one reference figure; warp() fits it to each body type's proportions.
private const val BODY =
    "M50 27 C47 27 45 28 45 31 C45 34 43 35 39 36 C30 37 22 38 19 46 C17 52 17 60 17 68 C16 76 15 82 14 86 " +
        "C12 96 10 106 10 116 C9 120 8 126 10 131 C12 133 15 132 16 128 C16 124 16 120 17 117 C19 106 22 96 24 86 " +
        "C25 80 26 74 27 66 L28 60 C29 72 31 86 32 98 C31 106 30 112 30 120 C29 134 30 148 32 160 " +
        "C31 172 31 186 34 200 L33 210 C33 213 36 214 40 214 L45 214 C46 210 45 206 44 202 C46 190 47 176 45 164 " +
        "C45 152 46 138 47 128 L50 123 Z"

private const val LOWER_CHEST = "M49 49 L49 61 C44 64 37 63 32 59 C29.5 56 29.5 52 30.4 50 C36 48.5 43 48.5 49 49 Z"
private const val LOWER_CHEST_FEMALE =
    "M48.4 49.5 C48.6 55 48.4 60 46.5 63.5 C44 67.5 37.5 68 34 65 C30.5 62 29.6 56.5 30.4 50 C36 48.8 43 48.8 48.4 49.5 Z"

// Each group is split into its visible heads/parts; all parts of a group light up together.
private const val LATERAL_DELT = "M27 38 C22 39 19 43 18.5 49 C18 54 18.5 58 19 61 C20.5 56 22 52 25 48 C25.5 44 26 41 27 38 Z"
private const val FOREARM_OUTER = "M15 88 C13 96 11.5 104 11 112 L13.5 113 C14.5 104 16.5 95 19 87.5 C17.5 87 16 87.2 15 88 Z"
private const val FOREARM_INNER = "M19.8 87.5 C17.5 95 15.5 104 14.3 113 L16 115 C18 106 21 97 23 88 C22 87 21 87.2 19.8 87.5 Z"

private val FRONT = listOf(
    Region(BACK, "M45.5 31 C45 33.5 43 35 39.5 36 L46.5 36.2 C47.2 34 47.5 31.5 47.5 29.5 Z"),
    Region(SHOULDERS, LATERAL_DELT),
    Region(SHOULDERS, "M38 37 C34 37 30 37.4 27.8 38 C26.8 41 26.2 44 25.8 48 C28 46 30 45 32 44 C35 42 37 40 38 37 Z"),
    Region(CHEST, "M49 40 L49 48 C43 47.5 36 47.5 30.5 49 C30 47 30.2 46 31 45 C35 41 42 39 49 40 Z"),
    Region(CHEST, LOWER_CHEST),
    Region(BICEPS, "M20 62 C18 68 18 76 19 82 C20 83 21.5 83 22.3 82 C22 75 22 68 22.3 61 C21.5 60.7 20.6 61.2 20 62 Z"),
    Region(BICEPS, "M23.1 60.5 C22.9 68 22.9 75 23.2 81.8 C24.2 81 24.8 79.5 25 78 C26 72 26 66 25 60 C24.4 59.8 23.7 60 23.1 60.5 Z"),
    Region(FOREARMS, FOREARM_OUTER),
    Region(FOREARMS, FOREARM_INNER),
    Region(ABS, "M31.5 61 C33 62 35 63 37 64 L37.5 68 C35.5 68 33.5 68 32 68.5 C31.5 66 31.3 63 31.5 61 Z"),
    Region(ABS, "M49 64 L49 73 L40.5 73 L40.5 67 C43 65 46 64 49 64 Z"),
    Region(ABS, "M49 74 L49 83 L40.5 83 L40.5 74 Z"),
    Region(ABS, "M49 84 L49 93 L40.5 93 L40.5 84 Z"),
    Region(ABS, "M49 94 L49 107 C46.5 108.5 43 108 41.5 105 L40.5 94 Z"),
    Region(ABS, "M38 70 L39 102 C36 100 33 94 32 86 C31 80 31.5 74 32.5 70 C34.5 69.5 36.5 69.5 38 70 Z"),
    Region(QUADS, "M32 118 C30 130 30 146 33.5 156 C35 157 36.5 157 37.5 156 C36.5 145 36 132 36.5 120 C35 118.5 33.5 117.5 32 118 Z"),
    Region(QUADS, "M37.5 119.5 C37 132 37.5 145 38.5 154 C40 155 41.5 155 42.5 153 C43.5 143 44 131 43.5 121 C41.5 119.5 39.5 119 37.5 119.5 Z"),
    Region(QUADS, "M43.5 145 C43 150 42.5 155 41 158.5 C42.5 159.5 44 158.5 44.8 156.5 C45.5 152 45.5 148 44.7 144 Z"),
    Region(CALVES, "M32 166 C29 177 31 188 34 195 L35.5 195 C36 185 36.5 175 36 166 C34.5 165 33 165 32 166 Z"),
    Region(CALVES, "M36.6 166 C37 176 37 187 37.2 198 L40 198 C40.3 187 40.4 176 40.2 166 Z"),
    Region(CALVES, "M42 165 C45 172 45.5 182 44 192 L41.5 192 C41 183 41 174 41 166 Z"),
)

private val BACK_VIEW = listOf(
    Region(BACK, "M50 27 L50 74 C47 66 43 56 37 44 C35 41 36 39 39 37.5 C42 36 44.5 34 45.5 31 C46.5 29 48 27.5 50 27 Z"),
    Region(SHOULDERS, LATERAL_DELT),
    Region(SHOULDERS, "M36 38.5 C33 38.5 29.5 39 27.5 39.6 C26.5 42.5 26 45.5 25.8 48.5 C28.5 46 31.5 44 34.5 43 C35.5 41.5 36 40 36 38.5 Z"),
    Region(BACK, "M35.5 45 C33 47 31 50 30.5 54 C33 55 36 54.5 38.5 52.5 C38 49.5 37 47 35.5 45 Z"),
    Region(BACK, "M30.5 56 C30.5 66 33 80 40 94 C42 96.5 43.5 98 44.3 99 C44.3 91 45 84 46.3 79 C44 71 42 63 39.5 55 C36.5 56.5 33.5 57 30.5 56 Z"),
    Region(BACK, "M49 75 L49 104 C47.5 104 46 103.5 45 102 C45 94 45.5 86 47 79 C47.5 77 48.3 76 49 75 Z"),
    Region(TRICEPS, "M18 62 C17 68 17 75 18 81 C19.5 82 20.5 81.5 21 80 C21 74 21.2 67 21.5 61 C20.2 60.3 19 60.8 18 62 Z"),
    Region(TRICEPS, "M22.3 60.5 C22 67 22 74 22 80 C23.5 80 24.5 78 25 76 C25.5 70 25.5 65 25 61 C24 60.3 23 60.2 22.3 60.5 Z"),
    Region(FOREARMS, FOREARM_OUTER),
    Region(FOREARMS, FOREARM_INNER),
    Region(GLUTES, "M32 106 C31 110 30.8 113 31 116 C33.5 110 37 106.5 41 105 C38 104 35 104.5 32 106 Z"),
    Region(GLUTES, "M49 107 C44 106 38 107 34 112 C31.5 117 32 125 37 130 C42 133 46.5 131.5 49 128 Z"),
    Region(HAMSTRINGS, "M32.5 136 C31.5 146 33 154 35.5 160 L39 160 C38.5 151 38.3 143 38.6 135 C36.5 135.5 34.5 135.8 32.5 136 Z"),
    Region(HAMSTRINGS, "M39.6 134.8 C39.3 143 39.6 151 40.2 160 L44.5 160 C46 152 46 142 45.5 134 C43.5 134.5 41.5 134.8 39.6 134.8 Z"),
    Region(CALVES, "M33 166 C30.5 173 31.5 182 35 188 C36.5 189 38 188.5 38.5 187 C38.5 179 38.4 172 38.6 165 C36.5 164.5 34.5 164.8 33 166 Z"),
    Region(CALVES, "M39.6 165 C39.5 172 39.6 180 40 189 C41.5 191 43.5 191 44.6 189 C45.5 181 45.5 173 44.5 166 C43 164.8 41 164.6 39.6 165 Z"),
    Region(CALVES, "M35.5 191 C36 194 37 197 38 199 L41 199 C41.5 197 42 194 42.2 192 C40 192.5 37.5 192.2 35.5 191 Z"),
)

// Head, ears and hair are drawn directly in final coordinates (not warped).
private const val HEAD =
    "M50 2 C56.5 2 60 6.5 60 13 C60 16 59.7 18 59.3 19.5 C59 23 57.5 26.5 54.5 28.8 C53 29.8 51.5 30.3 50 30.3 " +
        "C48.5 30.3 47 29.8 45.5 28.8 C42.5 26.5 41 23 40.7 19.5 C40.3 18 40 16 40 13 C40 6.5 43.5 2 50 2 Z"
private const val EAR = "M40.4 14.5 C38.6 14 38 15.5 38.3 17.5 C38.6 19.5 39.5 21 40.9 20.6 Z"

private class Hair(front: String, back: String, behind: String? = null) {
    val front = path(front)
    val back = path(back)
    val behind = behind?.let(::path)
}

private val SHORT_HAIR = Hair(
    front = "M40.2 13 C39.8 5.5 44 1.4 50 1.4 C56 1.4 60.2 5.5 59.8 13 C58.6 9 55.2 7 50 7.2 C44.8 7 41.4 9 40.2 13 Z",
    back = "M40 14 C39.6 5.5 44 1.4 50 1.4 C56 1.4 60.4 5.5 60 14 C59.8 18 59.3 21 58.3 23.5 C55.5 24.6 52.8 25 50 25 " +
        "C47.2 25 44.5 24.6 41.7 23.5 C40.7 21 40.2 18 40 14 Z",
)

private val LONG_HAIR = Hair(
    front = "M40.3 13 C40.5 5.5 44.5 1.6 50 1.6 C55.5 1.6 59.5 5.5 59.7 13 C57.5 9.5 54 8 50.5 8.8 C47 7.8 42.8 9 40.3 13 Z",
    back = "M50 1 C57.5 1 61.6 6 61.4 14 C61.3 22 62 30 63 37 C59 38.5 41 38.5 37 37 C38 30 38.7 22 38.6 14 C38.4 6 42.5 1 50 1 Z",
    behind = "M50 1 C57.5 1 61.6 6 61.4 14 C61.3 22 62 30 63 37 C60 38.4 57.5 38 55.6 36.6 L54.6 29 L45.4 29 L44.4 36.6 " +
        "C42.5 38 40 38.4 37 37 C38 30 38.7 22 38.6 14 C38.4 6 42.5 1 50 1 Z",
)

private fun interpolate(table: List<Pair<Float, Float>>, v: Float): Float {
    if (v <= table.first().first) return table.first().second
    val i = table.indexOfFirst { v <= it.first }
    if (i < 0) return table.last().second
    val (a, b) = table[i - 1]
    val (c, d) = table[i]
    return b + (d - b) * (v - a) / (c - a)
}

// Real proportions: about 7.5 heads tall, legs half the height. Arms move rigidly with the shoulders.
private val TORSO_Y = listOf(27f to 31f, 37f to 40f, 60f to 60f, 98f to 88f, 123f to 110f, 160f to 158f, 202f to 204f, 214f to 214f)
private val ARM_Y = listOf(37f to 40f, 60f to 60f, 214f to 214f)

private class Proportions(val widthByY: List<Pair<Float, Float>>, val armShift: Float, val hair: Hair)

private val PROPORTIONS = mapOf(
    // Broad shoulders and a narrow waist.
    BodyType.MALE to Proportions(
        listOf(27f to 1.18f, 31f to 1.12f, 37f to 0.95f, 45f to 0.9f, 60f to 0.95f, 98f to 0.8f, 112f to 0.83f, 130f to 0.85f, 160f to 0.8f, 214f to 0.8f),
        armShift = 3.2f,
        hair = SHORT_HAIR,
    ),
    // Narrower shoulders, the narrowest waist and wide hips.
    BodyType.FEMALE to Proportions(
        listOf(27f to 1f, 31f to 0.98f, 37f to 0.85f, 45f to 0.8f, 60f to 0.86f, 98f to 0.72f, 112f to 0.95f, 130f to 0.95f, 160f to 0.82f, 214f to 0.8f),
        armShift = 6.3f,
        hair = LONG_HAIR,
    ),
)

private val COORDINATE = Regex("""(-?[\d.]+) (-?[\d.]+)""")

private fun warp(data: String, p: Proportions): String = COORDINATE.replace(data) { match ->
    val x = match.groupValues[1].toFloat()
    val y = match.groupValues[2].toFloat()
    // In the reference figure everything left of x = 27.5 below the armpit belongs to an arm.
    if (y > 58 && x < 27.5f) {
        "${x + p.armShift} ${interpolate(ARM_Y, y)}"
    } else {
        "${50 + (x - 50) * interpolate(p.widthByY, y)} ${interpolate(TORSO_Y, y)}"
    }
}

private class Figure(type: BodyType) {
    private val proportions = PROPORTIONS.getValue(type)
    val hair = proportions.hair
    val body = path(warp(BODY, proportions))
    val front = shapes(FRONT, type)
    val back = shapes(BACK_VIEW, type)

    // Chest goes last so the rounder female chest overlaps the upper abs rather than the other way round.
    private fun shapes(regions: List<Region>, type: BodyType) = regions
        .sortedBy { it.muscle == CHEST }
        .map { region ->
            val data = if (type == BodyType.FEMALE && region.data == LOWER_CHEST) LOWER_CHEST_FEMALE else region.data
            region.muscle to path(warp(data, proportions))
        }
}

private val FIGURES = BodyType.entries.associateWith(::Figure)
private val HEAD_PATH = path(HEAD)
private val EAR_PATH = path(EAR)

@Composable
fun ExerciseMuscles(primary: MuscleGroup, secondary: List<MuscleGroup>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuscleMap(primary, secondary, Modifier.fillMaxWidth(0.7f))
        Text(stringResource(R.string.muscles_primary, stringResource(primary.labelRes)), style = MaterialTheme.typography.bodyMedium)
        if (secondary.isNotEmpty()) {
            Text(
                stringResource(R.string.muscles_secondary, secondary.joinToString { context.getString(it.labelRes) }),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// [levels] colour load levels 1..3; anything else is drawn idle.
class MuscleMapColors(val body: Color, val idle: Color, val levels: List<Color>, val hair: Color)

object MuscleMapDefaults {
    @Composable
    fun colors(): MuscleMapColors {
        val scheme = MaterialTheme.colorScheme
        val body = scheme.surfaceVariant
        val idle = lerp(body, scheme.onSurfaceVariant, 0.15f)
        return MuscleMapColors(body, idle, levels(idle, scheme.primary), hair = lerp(body, Color.Black, 0.35f))
    }

    // For maps placed on the dark inverseSurface panels.
    @Composable
    fun onDark(): MuscleMapColors {
        val scheme = MaterialTheme.colorScheme
        val body = lerp(scheme.inverseSurface, scheme.inverseOnSurface, 0.1f)
        val idle = lerp(scheme.inverseSurface, scheme.inverseOnSurface, 0.18f)
        return MuscleMapColors(body, idle, levels(idle, scheme.inversePrimary), hair = lerp(scheme.inverseSurface, Color.Black, 0.4f))
    }

    private fun levels(idle: Color, strong: Color) = listOf(lerp(idle, strong, 0.3f), lerp(idle, strong, 0.6f), strong)
}

// Front and back body views; [primary] is highlighted fully and [secondary] muscles faintly.
@Composable
fun MuscleMap(primary: MuscleGroup, secondary: List<MuscleGroup>, modifier: Modifier = Modifier) =
    MuscleMap(secondary.associateWith { 2 } + (primary to 3), modifier)

// Groups without a body region (CARDIO, OTHER) are simply not shown.
@Composable
fun MuscleMap(heat: Map<MuscleGroup, Int>, modifier: Modifier = Modifier, colors: MuscleMapColors = MuscleMapDefaults.colors()) {
    val figure = FIGURES.getValue(LocalBodyType.current)

    fun color(muscle: MuscleGroup) = heat[muscle]?.let { colors.levels.getOrNull(it - 1) } ?: colors.idle

    Canvas(modifier.aspectRatio(MAP_WIDTH / VIEW_HEIGHT)) {
        scale(size.width / MAP_WIDTH, pivot = Offset.Zero) {
            drawView(figure, figure.front, front = true, colors, ::color)
            translate(left = VIEW_WIDTH + VIEW_GAP) { drawView(figure, figure.back, front = false, colors, ::color) }
        }
    }
}

private fun DrawScope.drawView(
    figure: Figure,
    regions: List<Pair<MuscleGroup, Path>>,
    front: Boolean,
    colors: MuscleMapColors,
    color: (MuscleGroup) -> Color,
) {
    mirrored { drawPath(figure.body, colors.body) }
    if (front) figure.hair.behind?.let { drawPath(it, colors.hair) }
    mirrored { drawPath(EAR_PATH, colors.body) }
    drawPath(HEAD_PATH, colors.body)
    drawPath(if (front) figure.hair.front else figure.hair.back, colors.hair)
    mirrored {
        regions.forEach { (muscle, path) ->
            drawPath(path, color(muscle))
            // Outlining in the body colour leaves thin seams between adjacent muscles.
            drawPath(path, colors.body, style = Stroke(width = 0.6f))
        }
    }
}

private fun DrawScope.mirrored(block: DrawScope.() -> Unit) {
    block()
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(VIEW_WIDTH / 2, 0f), block = block)
}
