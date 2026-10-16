package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
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
import kotlin.math.max

// Each view is drawn in a 100×220 box as its left half (x ≤ 50) and mirrored around x = 50.
private const val VIEW_WIDTH = 100f
private const val VIEW_HEIGHT = 220f
private const val VIEW_GAP = 15f
private const val MAP_WIDTH = 2 * VIEW_WIDTH + VIEW_GAP

val LocalBodyType = staticCompositionLocalOf { BodyType.MALE }

private fun path(data: String) = PathParser().parsePathString(data).toPath()

// The male reference figure; the female one is derived from it by warpFemale().
private const val OUTLINE =
    "M50 3 C44.5 3 41.5 7 41.5 13 C41.5 16 41.2 17 41 18 C39.8 17.5 39.3 19 39.8 21 C40.2 22.6 41 23 41.8 22.5 " +
        "C42.5 25.5 44 27.5 45.2 28.6 L44.6 31.5 C41 34 36 36 31.5 37.5 C26 38.5 22.5 41.5 21.6 47 C20.8 52 20.4 57 19.8 62 " +
        "C19 70 18.6 78 18.4 84 C17.8 87 17 90 16.4 93 C15 100 13.8 108 13 115 C11.4 116.5 10.6 119 10.8 123 " +
        "C11 127.5 12.6 130.5 15.6 130.6 C18.3 130.6 19.4 128 19.2 124.5 C19 121.5 18.4 119 17.6 117 C19.6 109 22.2 100 23.4 93 " +
        "C24.2 90 25 88 25.3 86 C26.4 78 27.6 70 28.8 63 C29.6 60.5 30.6 59.5 31.4 60.4 C31.6 66 32.4 72 33.2 78 " +
        "C33.8 84 34.6 89 34.8 93 C34.6 97 33.4 101 32.6 105 C31 112 30 120 30.2 128 C30.6 138 32.2 148 33.4 155 " +
        "C33.8 158 33.8 161 33.6 164 C32.6 170 32.4 176 33.2 183 C34 190 35.6 196 36.6 202 C36.4 205 35 207 34.4 209.5 " +
        "C33.8 212 35 213.6 37.6 213.6 L44.4 213.6 C46 213.6 46.4 212 46 210 C45.6 207.5 45 205 44.8 202 " +
        "C44.6 196 45.4 190 45.6 184 C45.8 176 45 168 45.2 163 C45.4 160 45.6 158 46 155 C47.2 146 48.4 132 48.8 120 " +
        "C49 117 49.2 115 50 114"

// The fill runs a little past the centre line so the two mirrored halves leave no seam.
private const val FILL_CLOSE = " L50.8 114 L50.8 3 Z"

private class Region(val muscle: MuscleGroup, val data: String)

private const val FOREARM_OUTER = "M17.8 86.6 C16.6 92 15.4 100 14.4 108 C14 111 13.8 113.4 13.8 115.4 L15.2 115.8 C16.6 107 19 98 21.6 90.6 C20.8 88.6 19.4 87.2 17.8 86.6 Z"
private const val FOREARM_INNER = "M22.6 89.6 C20 97 17.6 106 16.2 115.8 L17.4 116.6 C19.4 108 21.8 100 23.6 92.6 C23.4 91.4 23.1 90.4 22.6 89.6 Z"
private const val KNUCKLES = "M12.4 122.6 C14 121.6 16 121.6 17.8 122.4"
private const val PECTORAL =
    "M49.4 41.2 C44 40 38.4 40 34.6 41.4 C31.6 44 29.8 48 29.4 52.6 C29.4 56 30.6 59 32.6 60.6 C36 62.6 41 63.2 45.4 62.6 C47.6 62.2 49 61.4 49.4 60 Z"
// Drawn in final female coordinates, so it is swapped in after the warp.
private const val BREAST =
    "M49.2 44.6 C44.6 43 39.4 43.4 35.6 45.8 C33 48 32.2 51.6 32.8 55 C33.6 59.2 36.8 62.2 41 62.6 C44.8 63 47.8 61.2 49 58.6 Z"

private val FRONT = listOf(
    Region(BACK, "M44.6 31.6 C41 34 36.5 36 32.2 37.4 C36 38 40.5 38 44.5 37.6 C45.5 35.5 45.2 33.5 44.6 31.6 Z"),
    Region(SHOULDERS, "M32 38.2 C27 38.8 23 41.5 22.2 47 C21.6 51.5 21.6 55.5 22.8 59 C24.6 57 26.4 54.5 27.6 51.5 C28.8 47.5 30.4 43.4 33.6 40.2 C33.2 39.4 32.7 38.7 32 38.2 Z"),
    Region(BICEPS, "M27.8 55 C25.4 58.5 23.4 63 22.4 68.5 C21.6 73.5 21.8 79 23.2 83.6 C24.2 84.6 25.4 84.4 25.8 83.4 C26.8 76.5 27.8 69.5 28.6 62.6 C28.6 60 28.4 57.4 27.8 55 Z"),
    Region(TRICEPS, "M21.6 59.4 C20.8 64 20.2 70 19.8 76 C19.6 79.5 19.8 82.5 20.6 84.6 C21.4 80 21.6 74 21.8 68.5 C22 65 22.2 62 21.6 59.4 Z"),
    Region(FOREARMS, FOREARM_OUTER),
    Region(FOREARMS, FOREARM_INNER),
    // Serratus, obliques and the four rows of the rectus all light up as abs.
    Region(ABS, "M31.6 61.4 C32.4 62.4 33.6 63.2 35 63.6 L34.2 65.6 C33 65.2 32.2 64.6 31.8 64 Z"),
    Region(ABS, "M32 65.6 C33 66.4 34.2 67 35.4 67.4 L34.8 69.4 C33.6 69 32.8 68.6 32.3 68 Z"),
    Region(ABS, "M32.5 69.6 C33.5 70.4 34.6 70.9 35.8 71.2 L35.4 73.2 C34.2 72.9 33.4 72.5 32.9 72 Z"),
    Region(ABS, "M36.6 64.4 C38.6 65 40.4 65.3 41.8 65.4 L42.3 88 C42.4 93 42 97 41 100.6 C38.6 99 36.6 96.6 35.4 93.4 C35 89 34.6 84.5 34.2 80.4 C35 76 36 70 36.6 64.4 Z"),
    Region(ABS, "M43 64.6 C45 63.8 47.4 63.6 49.4 63.8 L49.4 71.4 L43.2 71.6 Z"),
    Region(ABS, "M43.3 72.8 L49.4 72.6 L49.4 80.2 L43.5 80.4 Z"),
    Region(ABS, "M43.6 81.6 L49.4 81.4 L49.4 89 L43.7 89.2 Z"),
    Region(ABS, "M43.8 90.4 L49.4 90.2 L49.4 104 C47 104.6 45 103 44.4 100.4 C44 97 43.8 93.5 43.8 90.4 Z"),
    Region(GLUTES, "M33.2 103.4 C32 107 31.4 110.5 31.6 113 L35 112.4 C35.4 109.4 35.6 106.4 35.2 103.4 Z"),
    Region(QUADS, "M35.6 113.6 C32.6 115 31.2 120 31 127 C31 136 32.4 145 34.4 152.6 C35.6 154.2 36.8 154 37.4 152.6 C36.4 144 35.8 134 36.2 124 C36.4 120 36.6 116.6 35.6 113.6 Z"),
    Region(QUADS, "M41.2 108.8 C38.6 112 37.4 118 37.2 126 C37.2 134 38.2 142 39.6 149 C40.4 150.4 41.6 150.4 42.2 149 C43.4 141 44 133 43.8 124 C43.6 118 42.8 112.6 41.2 108.8 Z"),
    Region(QUADS, "M44.6 136.4 C43.4 141 42.8 146 43 150.4 C43.4 154.2 45 156.4 46.6 156 C47.8 152 47.8 146.6 47.2 141.6 C46.6 139.6 45.8 137.6 44.6 136.4 Z"),
    Region(QUADS, "M47.6 112.8 C45.6 115 44.6 119.6 44.6 124.6 C44.8 128.6 45.6 132.4 46.8 135.2 C47.8 131 48.4 125 48.6 119 C48.6 116.6 48.4 114.6 47.6 112.8 Z"),
    Region(CALVES, "M36.4 165 C35.4 171 35.2 178 35.8 186 C36.2 191 36.8 195.6 37.6 198.4 L38.8 198.2 C38.6 190 38.8 180 39.4 171 C39.4 168.6 38.4 166 36.4 165 Z"),
    Region(CALVES, "M34.4 165.6 C33.2 170 32.8 176 33.4 181.6 C33.9 184 34.6 185.6 35.2 186.4 C35 180 35 172 35.6 166.4 Z"),
    Region(CALVES, "M42.6 165 C44.4 168 45 174 44.6 180.4 C44.2 184.4 43.2 187.6 41.8 189 C41 184 40.8 176 41.2 170 C41.4 167.6 41.9 166 42.6 165 Z"),
    // Chest goes last so it overlaps the serratus rather than the other way round.
    Region(CHEST, PECTORAL),
)

private val FRONT_DETAILS = listOf(
    "M38.2 156.6 C38.2 154.6 39.8 153.8 41 154 C42.4 154.2 43.2 155.6 43 157.4 C42.8 159.6 41.6 161 40.2 160.8 C38.8 160.6 38.2 158.8 38.2 156.6 Z",
    "M45 29.2 C45.8 32 46.8 34.6 48.4 37.2",
    "M41.6 101.6 C43.6 104.6 46 107.8 48.4 110.4",
    KNUCKLES,
    "M17.6 124.6 C16.4 126.4 15 127.4 13.6 127.6",
)

private val BACK_VIEW = listOf(
    Region(BACK, "M49.6 25.6 C48.8 27.8 47 30.4 45 32.4 C41 35 36.4 36.6 32.4 37.6 C35.4 39.6 39.4 41.8 42.6 45.4 C45 48.6 47 53.6 48.4 59 C49 61.6 49.4 64 49.6 66 Z"),
    Region(SHOULDERS, "M32 38.4 C27 39 23.2 41.6 22.2 47 C21.6 51.4 21.8 55.4 23 58.6 C25 56.4 27 53.6 28.8 50.2 C30.4 47 32 44 34 41.6 C33.6 40.2 32.9 39.2 32 38.4 Z"),
    Region(BACK, "M35.6 44 C33.4 46.4 32 49.6 32 53 C32.2 55.6 34.4 57.6 37.4 57.8 C40.2 57.8 42.6 56.4 43.4 54 C42.8 50 41 46.4 38.4 44.2 C37.4 43.6 36.4 43.6 35.6 44 Z"),
    Region(BACK, "M31.6 56.4 C31 58.4 31 60.6 31.6 62.4 C33.6 62.6 35.8 61.8 37.6 60 C36 59.4 34.2 58.4 33 56.8 C32.5 56.4 32 56.3 31.6 56.4 Z"),
    Region(BACK, "M38.4 60.6 C35.6 62.8 33 64 31.8 64.4 C32.2 70 33 76 34.2 81.8 C35.8 87.6 39.2 92.6 43.4 96 C44.6 97 46.2 97 47 95.6 C46.6 88 46.4 78 46.4 69 C43.6 66.6 41 63.8 38.4 60.6 Z"),
    Region(BACK, "M49.6 70 L47.8 72 C47.4 80 47.4 88 47.8 96 C48.4 98.4 49 99.6 49.6 100 Z"),
    Region(TRICEPS, "M22.4 59.6 C20.8 65 19.8 71 19.6 77 C19.6 80.6 20.4 83.4 21.8 84.8 L24.4 84.6 C25.8 78 27 70 28.2 62.6 C26.2 61 24.2 60 22.4 59.6 Z"),
    Region(FOREARMS, FOREARM_OUTER),
    Region(FOREARMS, FOREARM_INNER),
    Region(GLUTES, "M33.4 100.6 C32.6 103 32.4 105.6 32.8 108 C35 106 38 104.4 41.2 103.6 C38.8 101.6 36 100.6 33.4 100.6 Z"),
    Region(GLUTES, "M48.8 102.4 C44.4 102.4 39.4 104 35.6 107.4 C33.4 109.6 32.6 113 33.2 116.6 C34.2 120.6 37.4 123.4 41.6 124 C44.8 124.4 47.4 123.8 49.2 122.8 C49.4 116 49.4 109 48.8 102.4 Z"),
    Region(HAMSTRINGS, "M34.4 124.6 C32.4 130 31.8 137 32.4 144 C33 148.6 34.6 152.6 36.6 155 C37.8 155.4 38.6 154.6 38.8 153.2 C38.2 145 38.2 136 39.2 127.4 C37.6 126.6 36 125.8 34.4 124.6 Z"),
    Region(HAMSTRINGS, "M40.6 127.6 C39.6 136 39.6 145 40.6 153 C41.2 155 42.6 155.6 44 154.4 C45.6 148 46.4 140 46.4 132.6 C46.4 130 46.2 127.6 45.6 125.4 C44 126.6 42.4 127.4 40.6 127.6 Z"),
    Region(CALVES, "M35.2 162.6 C33.6 167 33 172.6 33.6 178 C34.2 182.4 36 185.6 38.4 186.8 C39.4 186.6 39.8 185.2 39.8 183.6 C39.6 177 39.6 170 40.2 164.4 C38.6 163.4 36.8 162.8 35.2 162.6 Z"),
    Region(CALVES, "M41.2 163.6 C40.6 170 40.6 178 41 186 C41.4 188.6 42.8 189.2 44 188 C45.4 184 45.8 178 45.6 172.4 C45.4 168.6 44.8 165.6 43.8 163.6 C43 163.2 42 163.2 41.2 163.6 Z"),
    Region(CALVES, "M36.4 188 C37 192 37.6 196 38.4 199 L43.2 199 C43.6 196 44 192.4 44.4 189.4 C43 190.6 41.6 190.6 40.6 189.4 C39.4 189.4 37.8 189 36.4 188 Z"),
)

private val BACK_DETAILS = listOf(
    "M47.6 124.4 C46.8 127 46.8 130.4 47.2 133.6 C48 131.4 48.6 128 48.8 124.2",
    "M22.6 75.6 C23.8 72.4 25.4 71 27 71.4",
    "M37 159.6 C39.6 161.2 42.4 161.2 44.6 159.6",
    "M40.8 199.6 L41 207",
    KNUCKLES,
    "M50 100 L50 66",
)

// Female hair, in final coordinates: a band framing the head in front and a cap with strands behind.
private const val HAIR_FRONT =
    "M50 1.6 C43.4 1.6 39.8 6.2 39.8 13.4 C39.8 20 39.4 26.4 37.8 31.6 C40.4 32.6 42.8 32 44 30.4 C42.8 26.4 42.2 21.4 42.4 16.4 C42.8 10.2 45.8 6.6 50 6.2"
private const val HAIR_BACK = "M50 1.6 C43.4 1.6 39.8 6.2 39.8 13.4 C39.8 20 39.4 26.4 37.8 31.6 C41.4 33.2 45.8 33.6 50 33.6"
private const val HAIR_STRAND = "M44.6 8 C43 14 42.8 22 43.4 31.2"

private class Hair(front: String, frontEnd: Float, back: String, backEnd: Float, val strand: Path) {
    val frontFill = path("$front L50.8 $frontEnd L50.8 1.6 Z")
    val frontLine = path(front)
    val backFill = path("$back L50.8 $backEnd L50.8 1.6 Z")
    val backLine = path(back)
}

private val LONG_HAIR = Hair(HAIR_FRONT, 6.2f, HAIR_BACK, 33.6f, path(HAIR_STRAND))

// Width of the female figure relative to the male one at each height: narrower shoulders and waist, wider hips.
private val FEMALE_WIDTH = listOf(
    0f to 0.93f, 30f to 0.92f, 40f to 0.87f, 62f to 0.86f, 84f to 0.84f, 94f to 0.86f,
    106f to 1f, 118f to 1.05f, 140f to 1f, 162f to 0.93f, 220f to 0.92f,
)
private const val FEMALE_ARM_WIDTH = 0.87f

private fun femaleWidth(y: Float): Float {
    val i = FEMALE_WIDTH.indexOfFirst { y <= it.first }
    if (i <= 0) return if (i == 0) FEMALE_WIDTH.first().second else FEMALE_WIDTH.last().second
    val (y0, k0) = FEMALE_WIDTH[i - 1]
    val (y1, k1) = FEMALE_WIDTH[i]
    val t = (y - y0) / (y1 - y0)
    return k0 + (k1 - k0) * t * t * (3 - 2 * t)
}

private val COORDINATE = Regex("""(-?[\d.]+) (-?[\d.]+)""")

private fun warpFemale(data: String): String = COORDINATE.replace(data) { match ->
    val x = match.groupValues[1].toFloat()
    val y = match.groupValues[2].toFloat()
    // Arms keep one slimming scale: following the hip curve would splay the forearms and fists outwards.
    val arm = y > 56 && y < 132 && x < 31.3f && (y < 105 || x < 26)
    "${50 - (50 - x) * if (arm) FEMALE_ARM_WIDTH else femaleWidth(y)} $y"
}

private class Figure(type: BodyType) {
    private val warp: (String) -> String = if (type == BodyType.FEMALE) ::warpFemale else { data -> data }
    val bodyFill = path(warp(OUTLINE) + FILL_CLOSE)
    val outline = path(warp(OUTLINE))
    val front = FRONT.map { region ->
        val data = if (type == BodyType.FEMALE && region.data == PECTORAL) BREAST else warp(region.data)
        region.muscle to path(data)
    }
    val back = BACK_VIEW.map { it.muscle to path(warp(it.data)) }
    val frontDetails = FRONT_DETAILS.map { path(warp(it)) }
    val backDetails = BACK_DETAILS.map { path(warp(it)) }
    val hair = if (type == BodyType.FEMALE) LONG_HAIR else null
}

private val FIGURES = BodyType.entries.associateWith(::Figure)

// [levels] colour load levels 1..3; unloaded muscles keep the body colour and show only their outline.
class MuscleMapColors(val body: Color, val line: Color, val levels: List<Color>)

object MuscleMapDefaults {
    @Composable
    fun colors(): MuscleMapColors {
        val scheme = MaterialTheme.colorScheme
        val body = lerp(scheme.surfaceContainer, scheme.surfaceContainerHigh, 0.5f)
        val line = lerp(scheme.onSurfaceVariant, body, 0.15f)
        return MuscleMapColors(body, line, listOf(lerp(body, scheme.primary, 0.3f), lerp(body, scheme.primary, 0.62f), scheme.primary))
    }
}

// Front and back body views; [primary] is highlighted fully and [secondary] muscles faintly.
@Composable
fun MuscleMap(primary: MuscleGroup, secondary: List<MuscleGroup>, modifier: Modifier = Modifier) =
    MuscleMap(secondary.associateWith { 2 } + (primary to 3), modifier)

// Groups without a body region (CARDIO, OTHER) are simply not shown.
@Composable
fun MuscleMap(heat: Map<MuscleGroup, Int>, modifier: Modifier = Modifier, colors: MuscleMapColors = MuscleMapDefaults.colors()) {
    val figure = FIGURES.getValue(LocalBodyType.current)

    fun color(muscle: MuscleGroup) = heat[muscle]?.let { colors.levels.getOrNull(it - 1) } ?: colors.body

    Canvas(modifier.aspectRatio(MAP_WIDTH / VIEW_HEIGHT)) {
        val unit = size.width / MAP_WIDTH
        // Lines stay at least a hairline wide when the map is drawn small.
        val lines = Lines(
            muscle = Stroke(width = max(0.55f, 0.8f / unit), join = StrokeJoin.Round),
            outline = Stroke(width = max(0.8f, 1.2f / unit), join = StrokeJoin.Round, cap = StrokeCap.Round),
        )
        scale(unit, pivot = Offset.Zero) {
            drawView(figure, figure.front, figure.frontDetails, front = true, colors, lines, ::color)
            translate(left = VIEW_WIDTH + VIEW_GAP) { drawView(figure, figure.back, figure.backDetails, front = false, colors, lines, ::color) }
        }
    }
}

private class Lines(val muscle: Stroke, val outline: Stroke)

private fun DrawScope.drawView(
    figure: Figure,
    regions: List<Pair<MuscleGroup, Path>>,
    details: List<Path>,
    front: Boolean,
    colors: MuscleMapColors,
    lines: Lines,
    color: (MuscleGroup) -> Color,
) {
    // Fills overlap across the centre line, so both halves are filled before any line is drawn.
    mirrored { drawPath(figure.bodyFill, colors.body) }
    mirrored {
        regions.forEach { (muscle, path) ->
            drawPath(path, color(muscle))
            drawPath(path, colors.line, style = lines.muscle)
        }
        details.forEach { drawPath(it, colors.line, style = lines.muscle) }
        drawPath(figure.outline, colors.line, style = lines.outline)
    }
    figure.hair?.let { hair ->
        mirrored { drawPath(if (front) hair.frontFill else hair.backFill, colors.body) }
        mirrored {
            drawPath(if (front) hair.frontLine else hair.backLine, colors.line, style = lines.outline)
            if (!front) drawPath(hair.strand, colors.line, style = lines.muscle)
        }
    }
}

private fun DrawScope.mirrored(block: DrawScope.() -> Unit) {
    block()
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(VIEW_WIDTH / 2, 0f), block = block)
}
