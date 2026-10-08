package com.keshtoim.forge.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PickExercisesFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    // Built-in exercises are seeded asynchronously on first launch.
    private fun pickBenchPress() {
        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 10_000)
        rule.onNodeWithText("Bench Press").performClick()
        rule.onNode(hasClickAction() and hasAnyDescendant(hasText("Add (1)")), useUnmergedTree = true).performClick()
    }

    @Test
    fun pickedExerciseIsAddedToTemplate() {
        rule.onNodeWithText("New template").performClick()
        rule.onNode(hasSetTextAction() and hasText("Template name")).performTextInput("Push")
        rule.onNodeWithText("Add exercises").performClick()
        pickBenchPress()

        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 5_000)
        rule.onNodeWithText("Save").performClick()

        rule.waitUntilAtLeastOneExists(hasText("Push"), timeoutMillis = 5_000)
        rule.onNodeWithText("Bench Press").assertIsDisplayed()
    }

    @Test
    fun pickedExerciseIsAddedToActiveWorkout() {
        rule.onNodeWithText("Start empty workout").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Add exercises"), timeoutMillis = 5_000)
        rule.onNodeWithText("Add exercises").performClick()
        pickBenchPress()

        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 5_000)
        rule.waitUntilAtLeastOneExists(hasText("Add set"), timeoutMillis = 5_000)
    }
}
