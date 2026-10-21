package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionTest {
    private fun set(kg: Double, reps: Int, effort: SetEffort? = null) =
        WorkoutSet(workoutExerciseId = 1, position = 0, weightKg = kg, reps = reps, completed = true, effort = effort)

    @Test
    fun `all working sets at the top of the range add weight and restart the range`() {
        val last = listOf(set(40.0, 8, SetEffort.WARMUP), set(100.0, 10), set(100.0, 11))
        assertEquals(Progression(102.5, 6, ProgressionStep.ADD_WEIGHT), progression(last, Mechanics.COMPOUND, Equipment.BARBELL))
        assertEquals(Progression(22.0, 10, ProgressionStep.ADD_WEIGHT), progression(listOf(set(20.0, 15), set(20.0, 15)), Mechanics.ISOLATION, Equipment.DUMBBELL))
    }

    @Test
    fun `short of the top adds a rep, a max effort set holds`() {
        assertEquals(Progression(100.0, 8, ProgressionStep.ADD_REP), progression(listOf(set(100.0, 9), set(100.0, 7)), Mechanics.COMPOUND, Equipment.BARBELL))
        assertEquals(Progression(100.0, 10, ProgressionStep.HOLD), progression(listOf(set(100.0, 10), set(100.0, 10, SetEffort.MAX)), Mechanics.COMPOUND, Equipment.BARBELL))
        // Lighter back-off sets don't decide the step: only the heaviest weight of last time does.
        assertEquals(Progression(102.5, 6, ProgressionStep.ADD_WEIGHT), progression(listOf(set(100.0, 10), set(80.0, 6)), Mechanics.COMPOUND, Equipment.BARBELL))
    }

    @Test
    fun `nothing to go on gives no hint`() {
        assertNull(progression(listOf(set(40.0, 8, SetEffort.WARMUP)), Mechanics.COMPOUND, Equipment.BARBELL))
        assertNull(progression(emptyList(), null, null))
    }
}
