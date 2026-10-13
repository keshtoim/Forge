package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutExercise
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutFocusTest {
    private fun exercise(id: Long, position: Int, vararg done: Boolean) = WorkoutExerciseWithSets(
        WorkoutExercise(id = id, workoutId = 1, exerciseId = id, position = position),
        done.mapIndexed { i, d -> WorkoutSet(id = id * 10 + i, workoutExerciseId = id, position = i, completed = d) }.reversed(),
    )

    @Test
    fun `current set is the first not done by position`() {
        assertEquals(21L, exercise(2, 0, true, false, false).currentSet()?.id)
        assertNull(exercise(2, 0, true, true).currentSet())
    }

    @Test
    fun `default focus is the first exercise with sets left, in position order`() {
        val list = listOf(exercise(3, 2, false), exercise(1, 0, true), exercise(2, 1, false))

        assertEquals(2L, list.defaultFocus()?.workoutExercise?.id)
        assertEquals(1L, listOf(exercise(1, 0, true)).defaultFocus()?.workoutExercise?.id)
    }

    @Test
    fun `next unfinished goes forward and wraps around to skipped exercises`() {
        val list = listOf(exercise(1, 0, false), exercise(2, 1, true), exercise(3, 2, false))

        assertEquals(3L, list.nextUnfinished(1)?.workoutExercise?.id)
        assertEquals(1L, list.nextUnfinished(3)?.workoutExercise?.id)
        assertNull(listOf(exercise(1, 0, false)).nextUnfinished(1))
    }
}
