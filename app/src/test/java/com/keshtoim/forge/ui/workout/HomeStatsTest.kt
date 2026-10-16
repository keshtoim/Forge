package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.Workout
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.data.db.WorkoutExercise
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class HomeStatsTest {
    private val zone = ZoneOffset.UTC
    private val thursday = LocalDate.of(2026, 10, 15)

    private fun set(weight: Double, reps: Int, completed: Boolean = true) =
        WorkoutSet(workoutExerciseId = 0, position = 0, weightKg = weight, reps = reps, completed = completed)

    private fun detail(date: LocalDate, minutes: Long, vararg exercises: Pair<Long, List<WorkoutSet>>): WorkoutDetail {
        val start = date.atTime(18, 0).toInstant(zone).toEpochMilli()
        return WorkoutDetail(
            Workout(name = "W", startedAt = start, finishedAt = start + minutes * 60_000),
            exercises.map { (exerciseId, sets) -> WorkoutExerciseWithSets(WorkoutExercise(workoutId = 0, exerciseId = exerciseId, position = 0), sets) },
        )
    }

    @Test
    fun `totals count only completed work since the given day`() {
        val finished = listOf(
            detail(thursday, 60, 1L to listOf(set(100.0, 5), set(100.0, 5, completed = false)), 2L to listOf(set(0.0, 8, completed = false))),
            detail(thursday.minusDays(29), 50, 1L to listOf(set(80.0, 10), set(80.0, 8))),
            detail(thursday.minusDays(30), 40, 1L to listOf(set(60.0, 10))),
        )

        val totals = totals(finished, thursday.minusDays(29), zone)

        assertEquals(Totals(workouts = 2, exercises = 2, sets = 3, reps = 23), totals)
    }

    @Test
    fun `warm-up sets are not counted as work`() {
        val warmup = set(40.0, 10).copy(effort = SetEffort.WARMUP)
        val finished = listOf(detail(thursday, 60, 1L to listOf(warmup, set(100.0, 5))), detail(thursday, 20, 2L to listOf(warmup)))

        assertEquals(Totals(workouts = 2, exercises = 1, sets = 1, reps = 5), totals(finished, thursday, zone))
    }

    @Test
    fun `week bars go oldest first and include empty weeks`() {
        val finished = listOf(
            detail(thursday, 60, 1L to listOf(set(100.0, 5))),
            detail(thursday.minusDays(2), 30, 1L to listOf(set(50.0, 10))),
            detail(thursday.minusWeeks(2), 45, 1L to listOf(set(40.0, 10))),
        )

        val bars = weekBars(finished, thursday, zone, DayOfWeek.MONDAY, count = 3)

        assertEquals(listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)), bars.map { it.start })
        assertEquals(listOf(45L, 0L, 90L), bars.map { it.minutes })
        assertEquals(listOf(400.0, 0.0, 1000.0), bars.map { it.volumeKg })
    }

    @Test
    fun `recent heat skips old workouts and exercises without completed sets`() {
        val exercises = mapOf(
            1L to Exercise(id = 1, name = "Squat", muscleGroup = MuscleGroup.QUADS, type = ExerciseType.WEIGHT_REPS),
            2L to Exercise(id = 2, name = "Curl", muscleGroup = MuscleGroup.BICEPS, type = ExerciseType.WEIGHT_REPS),
            3L to Exercise(id = 3, name = "Bench", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS),
        )
        val finished = listOf(
            detail(thursday, 60, 1L to listOf(set(100.0, 5)), 2L to listOf(set(10.0, 10, completed = false))),
            detail(thursday.minusDays(5), 60, 3L to listOf(set(80.0, 5))),
        )

        assertEquals(mapOf(MuscleGroup.QUADS to 3), recentHeat(finished, exercises, thursday.minusDays(4), zone))
    }
}
