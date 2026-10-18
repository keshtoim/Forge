package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutExercise
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutFocusTest {
    private fun exercise(id: Long, position: Int, vararg done: Boolean, superset: Int? = null) = WorkoutExerciseWithSets(
        WorkoutExercise(id = id, workoutId = 1, exerciseId = id, position = position, superset = superset),
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

    @Test
    fun `a superset is the run of neighbours sharing a group`() {
        val list = listOf(
            exercise(1, 0, false),
            exercise(2, 1, true, false, superset = 1),
            exercise(3, 2, true, superset = 1),
            exercise(4, 3, false, superset = 2),
        )
        assertEquals(listOf(2L, 3L), list.supersetOf(list[2]).map { it.workoutExercise.id })
        assertEquals(listOf(1L), list.supersetOf(list[0]).map { it.workoutExercise.id })
        assertEquals(4L, list.nextUnfinished(2)?.workoutExercise?.id)
    }

    @Test
    fun `current round is the first set position with something left`() {
        assertEquals(1, listOf(exercise(1, 0, true, false), exercise(2, 1, true, true, false)).currentRound())
        assertEquals(2, listOf(exercise(1, 0, true, true), exercise(2, 1, true, true, false)).currentRound())
        assertNull(listOf(exercise(1, 0, true), exercise(2, 1, true)).currentRound())
    }
}
