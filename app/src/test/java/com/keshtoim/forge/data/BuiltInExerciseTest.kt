package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.Discipline
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.Force
import com.keshtoim.forge.data.db.Mechanics
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
    fun `exercises loaded with plates are barbell exercises`() {
        BuiltInExercise.entries.filter { it.barKg != null }.forEach { exercise ->
            assertEquals(exercise.name, Equipment.BARBELL, exercise.traits.equipment)
        }
    }

    @Test
    fun `built-in characteristics come from code, custom ones from their columns`() {
        val stored = Exercise(builtInKey = "LAT_PULLDOWN", muscleGroup = MuscleGroup.BACK, type = ExerciseType.WEIGHT_REPS, equipment = Equipment.OTHER)
        assertEquals(ExerciseTraits(Mechanics.COMPOUND, Discipline.STRENGTH, Equipment.CABLE, Force.PULL), stored.traits)

        val custom = Exercise(name = "Swing", muscleGroup = MuscleGroup.GLUTES, type = ExerciseType.REPS, equipment = Equipment.KETTLEBELL)
        assertEquals(ExerciseTraits(null, null, Equipment.KETTLEBELL, null), custom.traits)
    }

    @Test
    fun `trait filter ors within a characteristic and ands across them`() {
        val bench = BuiltInExercise.BENCH_PRESS.traits
        val curl = BuiltInExercise.DUMBBELL_CURL.traits
        val pushdown = BuiltInExercise.TRICEPS_PUSHDOWN.traits

        val freeWeights = TraitFilter(equipment = setOf(Equipment.BARBELL, Equipment.DUMBBELL))
        assertEquals(listOf(true, true, false), listOf(bench, curl, pushdown).map(freeWeights::matches))

        val freeWeightPresses = freeWeights.copy(force = setOf(Force.PUSH))
        assertEquals(listOf(true, false, false), listOf(bench, curl, pushdown).map(freeWeightPresses::matches))
        assertTrue(TraitFilter().matches(ExerciseTraits(null, null, null, null)))
        assertEquals(3, freeWeightPresses.size)
    }

    @Test
    fun `built-in exercise takes secondary muscles from code, custom one from its column`() {
        val stored = Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS)
        assertEquals(listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), stored.secondary)

        val custom = Exercise(name = "Dip", muscleGroup = MuscleGroup.CHEST, secondaryMuscles = listOf(MuscleGroup.TRICEPS), type = ExerciseType.REPS)
        assertEquals(listOf(MuscleGroup.TRICEPS), custom.secondary)
    }
}
