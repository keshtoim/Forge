package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.db.BodyMeasurement
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.data.navyBodyFat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CoachPromptTest {
    private fun set(position: Int, weight: Double?, reps: Int?, done: Boolean, effort: SetEffort? = null) =
        WorkoutSet(workoutExerciseId = 1, position = position, weightKg = weight, reps = reps, completed = done, effort = effort)

    @Test
    fun `prompt carries the log, the focus, supersets and what is known about the athlete`() {
        val prompt = coachPrompt(
            language = "Russian",
            athlete = Athlete(heightCm = 180, weightKg = 82.5),
            workoutName = "Ноги",
            minutesIn = 24,
            exercises = listOf(
                ExerciseLog("Присед", ExerciseType.WEIGHT_REPS, null, listOf(set(1, 100.0, 5, false), set(0, 97.5, 6, true, SetEffort.HEAVY)), listOf(set(0, 94.999, 6, true)), onScreen = true),
                ExerciseLog("Планка", ExerciseType.DURATION, 1, emptyList(), emptyList(), onScreen = false),
            ),
        )

        assertTrue(prompt.contains("Answer in Russian"))
        assertTrue(prompt.contains("Athlete: height 180 cm, body weight 82.5 kg."))
        assertFalse(prompt.contains("age"))
        assertTrue(prompt.contains("> 1. Присед"))
        assertTrue(prompt.contains("today: 1) 97.5 kg x 6, heavy, done; 2) 100 kg x 5, planned"))
        assertTrue(prompt.contains("last time: 95 kg x 6, done"))
        assertTrue(prompt.contains("  2. Планка [superset 1"))
        assertTrue(prompt.contains("today: no sets"))
    }

    @Test
    fun `the body log gives the coach sex, trend and composition`() {
        val today = LocalDate.of(2026, 10, 21)
        fun day(ago: Long) = today.minusDays(ago).toEpochDay()
        val log = listOf(
            BodyMeasurement(epochDay = day(60), weightKg = 90.0),
            BodyMeasurement(epochDay = day(25), weightKg = 84.0, neckCm = 40.0, waistCm = 90.0),
            BodyMeasurement(epochDay = day(1), weightKg = 82.5, waistCm = 86.0),
        )
        val athlete = athlete(BodyType.MALE, 180, LocalDate.of(1998, 3, 12), log, today)

        assertEquals(28, athlete.age)
        assertEquals(-1.5, athlete.weightChangeKg!!, 0.001)
        // Neck from the older entry, waist from the latest one.
        assertEquals(navyBodyFat(BodyType.MALE, 180, 40.0, 86.0, null)!!, athlete.bodyFatPercent!!, 0.001)

        val prompt = coachPrompt("Russian", athlete, "Ноги", 5, emptyList())
        assertTrue(prompt.contains("Athlete: male, height 180 cm, age 28, body weight 82.5 kg (-1.5 kg over the last 30 days), body fat about"))
        assertTrue(prompt.contains("lean mass"))
    }
}
