package com.keshtoim.forge.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

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

    private val _heightCm = MutableStateFlow(prefs.getInt(KEY_HEIGHT, 0).takeIf { it > 0 })

    val heightCm: StateFlow<Int?> = _heightCm

    fun setHeightCm(cm: Int) {
        prefs.edit { putInt(KEY_HEIGHT, cm) }
        _heightCm.value = cm
    }

    private val _birthDate = MutableStateFlow(prefs.getLong(KEY_BIRTH, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay))

    val birthDate: StateFlow<LocalDate?> = _birthDate

    fun setBirthDate(date: LocalDate) {
        prefs.edit { putLong(KEY_BIRTH, date.toEpochDay()) }
        _birthDate.value = date
    }

    private companion object {
        const val KEY_BODY_TYPE = "body_type"
        const val KEY_THEME = "theme"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_BIRTH = "birth_day"
    }
}
