package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInExerciseTest {
    @Test
    fun `secondary muscles are distinct body regions other than the primary one`() {
        BuiltInExercise.entries.forEach { exercise ->
            assertTrue(exercise.name, exercise.muscleGroup !in exercise.secondary)
            assertEquals(exercise.name, exercise.secondary.distinct(), exercise.secondary)
            assertTrue(exercise.name, exercise.secondary.none { it == MuscleGroup.CARDIO || it == MuscleGroup.OTHER })
        }
    }

    @Test
    fun `built-in exercise takes secondary muscles from code, custom one from its column`() {
        val stored = Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS)
        assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), stored.secondary)

        val custom = Exercise(name = "Dip", muscleGroup = MuscleGroup.CHEST, secondaryMuscles = listOf(MuscleGroup.TRICEPS), type = ExerciseType.REPS)
        assertEquals(listOf(MuscleGroup.TRICEPS), custom.secondary)
    }
}
