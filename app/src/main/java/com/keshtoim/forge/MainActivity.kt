package com.keshtoim.forge

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keshtoim.forge.ui.ForgeApp
import com.keshtoim.forge.ui.theme.ForgeTheme
import com.keshtoim.forge.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = (application as ForgeApplication).preferences
        setContent {
            val mode by preferences.themeMode.collectAsStateWithLifecycle()
            val dark = mode.isDark()
            // The app's theme can differ from the system one, so system bar icons follow the chosen theme.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ForgeTheme(dark) {
                ForgeApp()
            }
        }
    }
}
