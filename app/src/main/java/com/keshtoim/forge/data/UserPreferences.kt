package com.keshtoim.forge.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)

    private val _bodyType = MutableStateFlow(prefs.getString(KEY_BODY_TYPE, null)?.let { name -> BodyType.entries.firstOrNull { it.name == name } })

    // Null until the user picks one on first launch.
    val bodyType: StateFlow<BodyType?> = _bodyType

    fun setBodyType(type: BodyType) {
        prefs.edit { putString(KEY_BODY_TYPE, type.name) }
        _bodyType.value = type
    }

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME, null)?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
    )

    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _themeMode.value = mode
    }

    private companion object {
        const val KEY_BODY_TYPE = "body_type"
        const val KEY_THEME = "theme"
    }
}
