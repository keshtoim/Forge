package com.keshtoim.forge.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MeasureFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun measurementsAreLoggedForToday() {
        rule.onNodeWithText("Male", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Let's go", useUnmergedTree = true).performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("New workout"), timeoutMillis = 5_000)
        rule.onNodeWithText("Diary").performClick()
        rule.onNodeWithText("Body").performClick()
        rule.onNodeWithText("Log measurements", useUnmergedTree = true).performClick()

        rule.waitUntilAtLeastOneExists(hasText("Girths"), timeoutMillis = 5_000)
        rule.onNode(hasSetTextAction() and hasText("Weight")).performTextInput("70,5")
        rule.onNode(hasSetTextAction() and hasText("Waist")).performTextInput("80")
        rule.onNodeWithText("Save", useUnmergedTree = true).performClick()

        rule.waitUntilAtLeastOneExists(hasText("waist 80 cm"), timeoutMillis = 5_000)
        val entry = runBlocking { (rule.activity.application as ForgeApplication).database.bodyMeasurementDao().observeAll().first().single() }
        assertEquals(70.5, entry.weightKg)
        assertEquals(80.0, entry.waistCm)
    }
}
