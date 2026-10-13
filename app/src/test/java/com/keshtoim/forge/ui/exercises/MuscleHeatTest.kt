package com.keshtoim.forge.ui.exercises

import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.MuscleGroup.CHEST
import com.keshtoim.forge.data.db.MuscleGroup.SHOULDERS
import com.keshtoim.forge.data.db.MuscleGroup.TRICEPS
import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleHeatTest {
    private fun exercise(primary: MuscleGroup, vararg secondary: MuscleGroup) =
        Exercise(name = "E", muscleGroup = primary, secondaryMuscles = secondary.toList(), type = ExerciseType.WEIGHT_REPS)

    @Test
    fun `most loaded muscle gets level 3 and the rest scale down`() {
        val heat = muscleHeat(listOf(exercise(CHEST, TRICEPS), exercise(CHEST, SHOULDERS), exercise(TRICEPS)))

        // CHEST 4, TRICEPS 3, SHOULDERS 1.
        assertEquals(mapOf(CHEST to 3, TRICEPS to 3, SHOULDERS to 1), heat)
    }

    @Test
    fun `no exercises means no heat`() {
        assertEquals(emptyMap<MuscleGroup, Int>(), muscleHeat(emptyList()))
    }
}
