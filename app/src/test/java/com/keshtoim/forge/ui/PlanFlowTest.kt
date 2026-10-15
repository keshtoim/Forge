package com.keshtoim.forge.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PlanFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val db get() = (rule.activity.application as ForgeApplication).database

    @Test
    fun plannedTemplateIsSavedForToday() {
        rule.onNodeWithText("Male", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Let's go", useUnmergedTree = true).performClick()
        val templateId = runBlocking {
            val bench = db.exerciseDao().insert(Exercise(name = "Bench", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
            db.templateDao().save(Template(name = "Push"), listOf(TemplateExercise(templateId = 0, exerciseId = bench, position = 0, targetSets = 3)))
        }
        rule.waitUntilAtLeastOneExists(hasContentDescription("New workout"), timeoutMillis = 5_000)

        rule.onNode(hasContentDescription("New workout"), useUnmergedTree = true).performClick()
        rule.onNodeWithText("Plan a workout", useUnmergedTree = true).performClick()
        rule.waitUntilAtLeastOneExists(hasText("Push"), timeoutMillis = 5_000)
        rule.onNodeWithText("Plan for", substring = true, useUnmergedTree = true).performClick()

        rule.waitUntil(5_000) { runBlocking { db.planDao().observeAll().first().isNotEmpty() } }
        val plan = runBlocking { db.planDao().observeAll().first().single() }
        assertEquals(templateId, plan.templateId)
        assertEquals(LocalDate.now().toEpochDay(), plan.epochDay)
    }
}
