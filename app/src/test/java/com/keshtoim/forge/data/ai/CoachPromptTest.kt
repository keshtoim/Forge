package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachPromptTest {
    private fun set(position: Int, weight: Double?, reps: Int?, done: Boolean, effort: SetEffort? = null) =
        WorkoutSet(workoutExerciseId = 1, position = position, weightKg = weight, reps = reps, completed = done, effort = effort)

    @Test
    fun `prompt carries the log, the focus, supersets and what is known about the athlete`() {
        val prompt = coachPrompt(
            language = "Russian",
            athlete = Athlete(heightCm = 180, age = null, weightKg = 82.5),
            workoutName = "Ноги",
            minutesIn = 24,
            exercises = listOf(
                ExerciseLog("Присед", ExerciseType.WEIGHT_REPS, null, listOf(set(1, 100.0, 5, false), set(0, 97.5, 6, true, SetEffort.HEAVY)), listOf(set(0, 95.0, 6, true)), onScreen = true),
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
}
