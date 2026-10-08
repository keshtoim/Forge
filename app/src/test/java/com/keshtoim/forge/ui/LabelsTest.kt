package com.keshtoim.forge.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabelsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val set = WorkoutSet(workoutExerciseId = 1, position = 0)

    @Test
    fun `built-in exercise name comes from resources`() {
        val exercise = Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS)
        assertEquals("Bench Press", exercise.displayName(context))
    }

    @Test
    fun `unknown built-in key from a newer backup does not crash`() {
        val exercise = Exercise(builtInKey = "FUTURE_EXERCISE", muscleGroup = MuscleGroup.OTHER, type = ExerciseType.REPS)
        assertEquals("FUTURE_EXERCISE", exercise.displayName(context))
    }

    @Test
    fun `custom exercise uses its own name`() {
        val exercise = Exercise(name = "Zercher squat", muscleGroup = MuscleGroup.QUADS, type = ExerciseType.WEIGHT_REPS)
        assertEquals("Zercher squat", exercise.displayName(context))
    }

    @Test
    fun `set summary depends on exercise type`() {
        assertEquals("62.5 kg × 8", set.copy(weightKg = 62.5, reps = 8).summary(ExerciseType.WEIGHT_REPS, context))
        assertEquals("8", set.copy(reps = 8).summary(ExerciseType.WEIGHT_REPS, context))
        assertEquals("12 reps", set.copy(reps = 12).summary(ExerciseType.REPS, context))
        assertEquals("1:30", set.copy(durationSec = 90).summary(ExerciseType.DURATION, context))
        assertEquals("5 km · 25:00", set.copy(distanceM = 5000.0, durationSec = 1500).summary(ExerciseType.DISTANCE, context))
    }
}
