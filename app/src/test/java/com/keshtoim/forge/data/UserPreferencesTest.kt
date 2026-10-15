package com.keshtoim.forge.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

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
}
