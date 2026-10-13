package com.keshtoim.forge.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyTypeFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val preferences get() = (rule.activity.application as ForgeApplication).preferences

    @Test
    fun firstLaunchAsksForFigureAndRemembersIt() {
        assertNull(preferences.bodyType.value)
        rule.onNodeWithText("Which figure should the muscle map show?").assertIsDisplayed()

        rule.onNodeWithText("Female").performClick()

        rule.onNodeWithText("New template").assertIsDisplayed()
        assertEquals(BodyType.FEMALE, preferences.bodyType.value)
        // A fresh instance reads the stored choice, so the question is not asked again.
        assertEquals(BodyType.FEMALE, UserPreferences(rule.activity).bodyType.value)
    }

    @Test
    fun figureCanBeChangedInSettings() {
        rule.onNodeWithText("Male").performClick()
        rule.onNodeWithContentDescription("Settings").performClick()

        rule.onNodeWithText("Female").performClick()

        assertEquals(BodyType.FEMALE, preferences.bodyType.value)
    }
}
