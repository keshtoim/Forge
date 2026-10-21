package com.keshtoim.forge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.keshtoim.forge.data.ThemeMode

private val Ink = Color(0xFF16121F)

// Lilac shell with an athletic core: violet for actions, coral (secondary) for energy and records,
// mint (tertiary) for rest. Material fills selected and tonal controls with secondaryContainer, so that one stays violet:
// coral is kept for accents, never for what the user picked.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4CF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECE6FF),
    onPrimaryContainer = Color(0xFF5235D6),
    inversePrimary = Color(0xFFB6A4FF),
    secondary = Color(0xFFFF5A47),
    onSecondary = Ink,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF4A2FC4),
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

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA48FFA),
    onPrimary = Color(0xFF1E0F5C),
    primaryContainer = Color(0xFF3B2B8C),
    onPrimaryContainer = Color(0xFFE4DCFF),
    inversePrimary = Color(0xFF6C4CF5),
    secondary = Color(0xFFFF8A7A),
    onSecondary = Color(0xFF3A0A04),
    secondaryContainer = Color(0xFF34286E),
    onSecondaryContainer = Color(0xFFE4DCFF),
    tertiary = Color(0xFF5FC4A3),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF124A3C),
    onTertiaryContainer = Color(0xFFBFF0DE),
    background = Color(0xFF121016),
    onBackground = Color(0xFFECE6F5),
    surface = Color(0xFF121016),
    onSurface = Color(0xFFECE6F5),
    surfaceVariant = Color(0xFF3A3348),
    onSurfaceVariant = Color(0xFFC6BEDA),
    surfaceContainerLowest = Color(0xFF0D0B11),
    surfaceContainerLow = Color(0xFF1A1721),
    surfaceContainer = Color(0xFF1E1A26),
    surfaceContainerHigh = Color(0xFF2A2535),
    surfaceContainerHighest = Color(0xFF332D40),
    inverseSurface = Color(0xFFECE6F5),
    inverseOnSurface = Color(0xFF1E1A26),
    outline = Color(0xFF8E88A6),
    outlineVariant = Color(0xFF3A3348),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1A1A),
)

// Dark "athletic" panels (active workout, records) stay dark in both themes, so they are not inverseSurface.
@Immutable
data class PanelColors(val panel: Color, val onPanel: Color, val muted: Color, val accent: Color)

private val LightPanel = PanelColors(panel = Ink, onPanel = Color.White, muted = Color(0xFFC9C2DE), accent = Color(0xFFB6A4FF))
private val DarkPanel = PanelColors(panel = Color(0xFF2A2535), onPanel = Color.White, muted = Color(0xFFC6BEDA), accent = Color(0xFFB6A4FF))

private val LocalPanelColors = staticCompositionLocalOf { LightPanel }

// Set effort levels run from cool to hot and also differ in lightness, so they stay apart without colour vision.
@Immutable
data class EffortColors(val warmup: Color, val light: Color, val medium: Color, val heavy: Color, val max: Color, val none: Color)

private val LightEffort = EffortColors(
    warmup = Color(0xFF9A94AE), light = Color(0xFF2FA37F), medium = Color(0xFFE2A91E),
    heavy = Color(0xFFF07A2B), max = Color(0xFFE0453A), none = Color(0xFFDCD4F0),
)
private val DarkEffort = EffortColors(
    warmup = Color(0xFF8E88A6), light = Color(0xFF3FBF96), medium = Color(0xFFF2C04A),
    heavy = Color(0xFFFF8F45), max = Color(0xFFFF6257), none = Color(0xFF3A3348),
)

private val LocalEffortColors = staticCompositionLocalOf { LightEffort }

object ForgeTheme {
    val panel: PanelColors
        @Composable get() = LocalPanelColors.current
    val effort: EffortColors
        @Composable get() = LocalEffortColors.current
}

private fun style(size: Int, weight: FontWeight, lineHeight: Float = 1.3f) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.em)

private val ForgeTypography = Typography(
    displayLarge = style(56, FontWeight.Bold, 1.05f),
    displayMedium = style(48, FontWeight.Bold, 1.05f),
    displaySmall = style(36, FontWeight.Bold, 1.1f),
    headlineLarge = style(30, FontWeight.Bold, 1.15f),
    headlineMedium = style(26, FontWeight.Bold, 1.15f),
    headlineSmall = style(22, FontWeight.Bold, 1.2f),
    titleLarge = style(20, FontWeight.Bold),
    titleMedium = style(16, FontWeight.Medium),
    titleSmall = style(14, FontWeight.Medium),
    bodyLarge = style(16, FontWeight.Normal, 1.4f),
    bodyMedium = style(14, FontWeight.Normal, 1.4f),
    bodySmall = style(13, FontWeight.Normal, 1.35f),
    labelLarge = style(14, FontWeight.Medium),
    labelMedium = style(12, FontWeight.Medium),
    labelSmall = style(11, FontWeight.Medium),
)

private val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ForgeTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPanelColors provides if (dark) DarkPanel else LightPanel,
        LocalEffortColors provides if (dark) DarkEffort else LightEffort,
    ) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = ForgeTypography, shapes = ForgeShapes, content = content)
    }
}
