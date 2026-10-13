package com.keshtoim.forge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.keshtoim.forge.R

private val Ink = Color(0xFF16121F)
private val Violet = Color(0xFF6C4CF5)

// Light lilac shell with an athletic core: dark ink panels (inverseSurface), violet for actions,
// coral (secondary) for energy and records, mint (tertiary) for rest.
private val ForgeColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE6FF),
    onPrimaryContainer = Color(0xFF5235D6),
    inversePrimary = Color(0xFFB6A4FF),
    secondary = Color(0xFFFF5A47),
    onSecondary = Ink,
    secondaryContainer = Color(0xFFFFE4DF),
    onSecondaryContainer = Color(0xFFB5321F),
    tertiary = Color(0xFF1E8C6E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF4EC),
    onTertiaryContainer = Color(0xFF0F5A47),
    background = Color(0xFFF6F2FC),
    onBackground = Ink,
    surface = Color(0xFFF6F2FC),
    onSurface = Ink,
    surfaceVariant = Color(0xFFDCD4F0),
    onSurfaceVariant = Color(0xFF4E4862),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1ECFA),
    surfaceContainerHighest = Color(0xFFECE6FF),
    inverseSurface = Ink,
    inverseOnSurface = Color.White,
    outline = Color(0xFF8E88A6),
    outlineVariant = Color(0xFFE4DDF2),
    error = Color(0xFFC62828),
    errorContainer = Color(0xFFFFDAD6),
)

// Variable fonts: one file per family, weights picked through the wght axis.
@OptIn(ExperimentalTextApi::class)
private fun exo2(weight: Int) = Font(
    R.font.exo2_italic,
    FontWeight(weight),
    FontStyle.Italic,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(R.font.nunito, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

// Display face: always heavy italic. Text using it is upper-cased at the call site.
val Exo2 = FontFamily(exo2(800), exo2(900))
private val Nunito = FontFamily(nunito(600), nunito(700), nunito(800), nunito(900))

private fun display(size: Int, lineHeight: Float = 1f) = TextStyle(
    fontFamily = Exo2,
    fontWeight = FontWeight.Black,
    fontStyle = FontStyle.Italic,
    fontSize = size.sp,
    lineHeight = lineHeight.em,
)

private fun body(size: Int, weight: Int, lineHeight: Float = 1.35f) =
    TextStyle(fontFamily = Nunito, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = lineHeight.em)

private val ForgeTypography = Typography(
    displayLarge = display(64, 0.92f),
    displayMedium = display(56, 1f),
    displaySmall = display(40, 0.95f),
    headlineLarge = display(32, 0.95f),
    headlineMedium = display(30),
    headlineSmall = display(22),
    titleLarge = display(20),
    titleMedium = body(16, 800),
    titleSmall = body(14, 800),
    bodyLarge = body(16, 700),
    bodyMedium = body(14, 600),
    bodySmall = body(13, 600),
    labelLarge = body(14, 800),
    labelMedium = body(12, 800, 1.2f).copy(letterSpacing = 0.06.em),
    labelSmall = body(11, 700, 1.2f),
)

private val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ForgeColors, typography = ForgeTypography, shapes = ForgeShapes, content = content)
}
