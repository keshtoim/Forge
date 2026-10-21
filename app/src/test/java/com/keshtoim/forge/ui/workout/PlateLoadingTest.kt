package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateLoadingTest {
    private val plates = PlateLoading(20.0)
    private val empty = WorkoutSet(workoutExerciseId = 1, position = 0)

    @Test
    fun `plates on one side are stored as total with the bar`() {
        val typed = SetField.WEIGHT.write(plates.toSide(empty), "40")

        assertEquals(100.0, plates.toTotal(typed).weightKg!!, 0.0)
    }

    @Test
    fun `stored total is shown as plates per side`() {
        assertEquals("31.25", SetField.WEIGHT.read(plates.toSide(empty.copy(weightKg = 82.5))))
    }

    @Test
    fun `stepping adds plates to both sides`() {
        val stepped = plates.toTotal(SetField.WEIGHT.step(plates.toSide(empty.copy(weightKg = 60.0)), null, 1))

        assertEquals(65.0, stepped.weightKg!!, 0.0)
    }

    @Test
    fun `empty bar and empty field stay distinct`() {
        assertEquals(20.0, plates.toTotal(SetField.WEIGHT.write(empty, "0")).weightKg!!, 0.0)
        assertNull(plates.toTotal(SetField.WEIGHT.write(empty, "")).weightKg)
    }

    @Test
    fun `a total lighter than the bar shows no plates`() {
        assertEquals(0.0, plates.toSide(empty.copy(weightKg = 10.0)).weightKg!!, 0.0)
    }

    @Test
    fun `plates per side come heaviest first from a standard set`() {
        val olympic = PlateLoading(20.0)
        assertEquals(listOf(25.0, 15.0, 2.5), olympic.platesFor(105.0))
        assertEquals(listOf(1.25), olympic.platesFor(22.5))
        assertEquals(emptyList<Double>(), olympic.platesFor(20.0))
        // A weight the set can't make, or lighter than the bar, has no loading.
        assertNull(olympic.platesFor(21.0))
        assertNull(olympic.platesFor(15.0))
    }
}
