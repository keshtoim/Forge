package com.keshtoim.forge.ui.exercises

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import com.keshtoim.forge.data.db.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ExerciseMusclesFlowTest {
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

    private fun openExercisesTab() = rule.onNodeWithContentDescription("Exercises").performClick()

    @Test
    fun builtInExerciseShowsItsMuscles() {
        openExercisesTab()
        rule.waitUntilAtLeastOneExists(hasText("Bench Press"), timeoutMillis = 10_000)
        rule.onNodeWithText("Bench Press").performClick()

        rule.onNodeWithText("Primary: Chest").assertIsDisplayed()
        rule.onNodeWithText("Secondary: Triceps, Shoulders").assertIsDisplayed()
    }

    @Test
    fun secondaryMusclesOfCustomExerciseAreSaved() {
        openExercisesTab()
        rule.onNodeWithContentDescription("New exercise").performClick()
        rule.onNode(hasSetTextAction() and hasText("Name")).performTextInput("Dip")
        // The catalog behind the dialog has a filter chip with the same label.
        rule.onAllNodesWithText("Triceps").filterToOne(hasAnyAncestor(isDialog())).performScrollTo().performClick()
        rule.onNodeWithText("Save").performClick()

        val dao = (rule.activity.application as ForgeApplication).database.exerciseDao()
        fun saved() = runBlocking { dao.observeAll().first().singleOrNull { it.name == "Dip" } }
        rule.waitUntil(timeoutMillis = 5_000) { saved() != null }
        assertEquals(listOf(MuscleGroup.TRICEPS), saved()!!.secondaryMuscles)
    }
}
