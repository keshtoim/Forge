package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class WarmupTest {
    @Test
    fun `a barbell ramps up in plate steps and never below the bar`() {
        assertEquals(listOf(WarmupSet(40.0, 8), WarmupSet(60.0, 5), WarmupSet(80.0, 3)), warmupSets(100.0, 2.5, 20.0))
        // For 30 kg both lighter steps fall below the bar, so they become one set on the empty bar.
        assertEquals(listOf(WarmupSet(20.0, 8), WarmupSet(25.0, 3)), warmupSets(30.0, 2.5, 20.0))
    }

    @Test
    fun `dumbbells round to whole kilograms and a light weight needs no warm-up`() {
        assertEquals(listOf(WarmupSet(10.0, 8), WarmupSet(14.0, 5), WarmupSet(19.0, 3)), warmupSets(24.0, 1.0, null))
        assertEquals(emptyList<WarmupSet>(), warmupSets(2.0, 2.5, null))
    }

    @Test
    fun `warm-ups compare with last time's warm-ups and working sets with working sets`() {
        fun set(kg: Double, warmup: Boolean = false) =
            WorkoutSet(workoutExerciseId = 1, position = 0, weightKg = kg, effort = if (warmup) SetEffort.WARMUP else null)
        val previous = listOf(set(100.0), set(100.0))
        val today = listOf(set(40.0, warmup = true), set(60.0, warmup = true), set(102.5), set(102.5))

        assertEquals(listOf(null, null, 100.0, 100.0), ghosts(today, previous).map { it?.weightKg })
    }
}
