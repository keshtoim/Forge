package com.keshtoim.forge.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PickExercisesFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun chooseBodyType() {
        // First launch asks which figure the muscle map should show.
        rule.onNodeWithText("MALE", useUnmergedTree = true).performClick()
        rule.onNodeWithText("LET'S GO", useUnmergedTree = true).performClick()
        // Home renders once its data has loaded from Room.
        rule.waitUntilAtLeastOneExists(hasText("HI!"), timeoutMillis = 5_000)
    }

    // Button labels live in the unmerged semantics tree.
    private fun waitForButton(label: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    // Built-in exercises are seeded asynchronously on first launch.
    private fun pickBenchPress() {
        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 10_000)
        rule.onNodeWithText("Bench Press").performClick()
        rule.onNode(hasClickAction() and hasAnyDescendant(hasText("Add (1)")), useUnmergedTree = true).performClick()
    }

    @Test
    fun pickedExerciseIsAddedToTemplate() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("+ New"))
        rule.onNodeWithText("+ New", useUnmergedTree = true).performClick()
        rule.onNode(hasSetTextAction() and hasText("Template name")).performTextInput("Push")
        rule.onNodeWithText("Add exercises").performClick()
        pickBenchPress()

        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 5_000)
        rule.onNodeWithText("Save").performClick()

        // The only template becomes the next one in rotation, shown in the hero card with its exercise count.
        rule.waitUntilAtLeastOneExists(hasText("PUSH"), timeoutMillis = 5_000)
        rule.waitUntilAtLeastOneExists(hasText("1 exercise", substring = true), timeoutMillis = 5_000)
    }

    @Test
    fun pickedExerciseIsAddedToActiveWorkout() {
        rule.onNodeWithText("LET'S GO", useUnmergedTree = true).performClick()
        waitForButton("ADD EXERCISES")
        rule.onNodeWithText("ADD EXERCISES", useUnmergedTree = true).performClick()
        pickBenchPress()

        rule.waitUntilAtLeastOneExists(hasText("BENCH PRESS"), timeoutMillis = 5_000)
        waitForButton("DONE")
    }
}
