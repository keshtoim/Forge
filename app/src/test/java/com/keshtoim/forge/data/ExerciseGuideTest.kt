package com.keshtoim.forge.data

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ExerciseGuideTest {
    private fun resources(locale: Locale) = ApplicationProvider.getApplicationContext<Context>()
        .createConfigurationContext(Configuration().apply { setLocale(locale) })
        .resources

    @Test
    fun `every built-in exercise has technique and mistakes in both languages`() {
        val english = resources(Locale.ENGLISH)
        val russian = resources(Locale.forLanguageTag("ru"))
        BuiltInExercise.entries.forEach { exercise ->
            val guide = exercise.guide
            listOf(guide.steps, guide.mistakes).forEach { res ->
                val en = english.getStringArray(res)
                val ru = russian.getStringArray(res)
                assertTrue(exercise.name, en.isNotEmpty() && en.none { it.isBlank() })
                // Each translation lists the same steps, so the arrays line up item by item.
                assertEquals(exercise.name, en.size, ru.size)
                assertTrue(exercise.name, en.toList() != ru.toList())
            }
        }
    }
}
