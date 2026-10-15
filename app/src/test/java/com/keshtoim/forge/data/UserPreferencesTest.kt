package com.keshtoim.forge.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `theme follows the system until chosen and the choice survives a restart`() {
        val preferences = UserPreferences(context)
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.value)

        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
        assertEquals(ThemeMode.DARK, UserPreferences(context).themeMode.value)
    }

    @Test
    fun `height and birth date are unknown until set and then persist`() {
        val preferences = UserPreferences(context)
        assertNull(preferences.heightCm.value)
        assertNull(preferences.birthDate.value)

        preferences.setHeightCm(178)
        preferences.setBirthDate(LocalDate.of(1995, 6, 1))

        val reloaded = UserPreferences(context)
        assertEquals(178, reloaded.heightCm.value)
        assertEquals(LocalDate.of(1995, 6, 1), reloaded.birthDate.value)
    }
}
