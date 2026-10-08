package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetFieldTest {
    private val empty = WorkoutSet(workoutExerciseId = 1, position = 0)

    @Test
    fun `weight accepts comma as decimal separator`() {
        val set = SetField.WEIGHT.write(empty, "62,5")
        assertEquals(62.5, set.weightKg!!, 0.0)
        assertEquals("62.5", SetField.WEIGHT.read(set))
    }

    @Test
    fun `weight drops trailing zeros when read`() {
        assertEquals("60", SetField.WEIGHT.read(empty.copy(weightKg = 60.0)))
    }

    @Test
    fun `empty or negative input clears the value`() {
        val set = empty.copy(weightKg = 50.0)
        assertNull(SetField.WEIGHT.write(set, "").weightKg)
        assertNull(SetField.WEIGHT.write(set, "-5").weightKg)
        assertNull(SetField.WEIGHT.write(set, "abc").weightKg)
    }

    @Test
    fun `non-finite input is rejected`() {
        assertNull(SetField.WEIGHT.write(empty, "Infinity").weightKg)
        assertNull(SetField.DISTANCE.write(empty, "1e999").distanceM)
        assertNull(SetField.WEIGHT.write(empty, "NaN").weightKg)
    }

    @Test
    fun `huge input is clamped and does not overflow duration`() {
        assertEquals(1_000.0, SetField.WEIGHT.write(empty, "99999999").weightKg!!, 0.0)
        assertEquals(999 * 60, SetField.MINUTES.write(empty, "99999999999").durationSec)
    }

    @Test
    fun `seconds are clamped instead of rolling into minutes while typing`() {
        val set = SetField.SECONDS.write(empty.copy(durationSec = 60), "75")
        assertEquals(60 + 59, set.durationSec)
        // Re-typing after a clamp must not accumulate into minutes.
        assertEquals(60 + 6, SetField.SECONDS.write(set, "6").durationSec)
    }

    @Test
    fun `step stays within bounds`() {
        assertEquals(59, SetField.SECONDS.step(empty.copy(durationSec = 57), null, 1).durationSec)
    }

    @Test
    fun `reps are truncated to whole numbers`() {
        assertEquals(8, SetField.REPS.write(empty, "8.7").reps)
    }

    @Test
    fun `minutes and seconds combine into duration`() {
        val withMinutes = SetField.MINUTES.write(empty, "2")
        assertEquals(120, withMinutes.durationSec)
        val withSeconds = SetField.SECONDS.write(withMinutes, "30")
        assertEquals(150, withSeconds.durationSec)
        assertEquals("2", SetField.MINUTES.read(withSeconds))
        assertEquals("30", SetField.SECONDS.read(withSeconds))
    }

    @Test
    fun `clearing one part keeps the other`() {
        val set = empty.copy(durationSec = 150)
        assertEquals(30, SetField.MINUTES.write(set, "").durationSec)
        assertEquals(120, SetField.SECONDS.write(set, "").durationSec)
    }

    @Test
    fun `clearing both parts clears duration`() {
        val minutesOnly = empty.copy(durationSec = 120)
        assertNull(SetField.MINUTES.write(minutesOnly, "").durationSec)
        val secondsOnly = empty.copy(durationSec = 45)
        assertNull(SetField.SECONDS.write(secondsOnly, "").durationSec)
    }

    @Test
    fun `distance is entered in km and stored in meters`() {
        val set = SetField.DISTANCE.write(empty, "5.2")
        assertEquals(5200.0, set.distanceM!!, 0.001)
        assertEquals("5.2", SetField.DISTANCE.read(set))
    }

    @Test
    fun `step starts from ghost value when field is empty`() {
        val ghost = empty.copy(weightKg = 60.0)
        assertEquals(62.5, SetField.WEIGHT.step(empty, ghost, 1).weightKg!!, 0.0)
    }

    @Test
    fun `step uses own value over ghost and never goes below zero`() {
        val ghost = empty.copy(reps = 10)
        assertEquals(4, SetField.REPS.step(empty.copy(reps = 5), ghost, -1).reps)
        assertEquals(0.0, SetField.WEIGHT.step(empty, null, -1).weightKg!!, 0.0)
    }

    @Test
    fun `fields match exercise type`() {
        assertEquals(listOf(SetField.WEIGHT, SetField.REPS), SetField.forType(ExerciseType.WEIGHT_REPS))
        assertEquals(listOf(SetField.REPS), SetField.forType(ExerciseType.REPS))
        assertEquals(listOf(SetField.MINUTES, SetField.SECONDS), SetField.forType(ExerciseType.DURATION))
        assertEquals(listOf(SetField.DISTANCE, SetField.MINUTES), SetField.forType(ExerciseType.DISTANCE))
    }

    @Test
    fun `duration formatting`() {
        assertEquals("0:05", formatDuration(5))
        assertEquals("1:30", formatDuration(90))
        assertEquals("1:02:03", formatDuration(3723))
    }
}
