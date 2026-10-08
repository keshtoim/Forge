package com.keshtoim.forge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForgeColors = darkColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFF2B1400),
    primaryContainer = Color(0xFF5C2E00),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = Color(0xFFE5BFA8),
    onSecondary = Color(0xFF432B1C),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECE0DA),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFECE0DA),
    surfaceVariant = Color(0xFF2A2420),
    onSurfaceVariant = Color(0xFFD7C3B8),
    surfaceContainer = Color(0xFF1E1A18),
    surfaceContainerHigh = Color(0xFF262220),
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ForgeColors, content = content)
}
