package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.ui.exercises.muscleHeat
import com.keshtoim.forge.ui.volumeKg
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class Totals(val workouts: Int, val exercises: Int, val sets: Int, val reps: Int)

data class WeekBar(val start: LocalDate, val minutes: Long, val volumeKg: Double)

private val WorkoutDetail.minutes get() = ((workout.finishedAt ?: workout.startedAt) - workout.startedAt) / 60_000

fun totals(finished: List<WorkoutDetail>, from: LocalDate, zone: ZoneId): Totals {
    val recent = finished.filter { it.workout.startedAt.toDate(zone) >= from }
    val sets = recent.flatMap { w -> w.exercises.flatMap { e -> e.sets.filter { it.completed } } }
    return Totals(
        workouts = recent.size,
        exercises = recent.sumOf { w -> w.exercises.count { e -> e.sets.any { it.completed } } },
        sets = sets.size,
        reps = sets.sumOf { it.reps ?: 0 },
    )
}

// The last [count] calendar weeks, oldest first; the current week is the last bar.
fun weekBars(finished: List<WorkoutDetail>, today: LocalDate, zone: ZoneId, firstDay: DayOfWeek, count: Int = 5): List<WeekBar> {
    val current = today.with(TemporalAdjusters.previousOrSame(firstDay))
    val byWeek = finished.groupBy { it.workout.startedAt.toDate(zone).with(TemporalAdjusters.previousOrSame(firstDay)) }
    return (count - 1 downTo 0).map { ago ->
        val start = current.minusWeeks(ago.toLong())
        val workouts = byWeek[start].orEmpty()
        WeekBar(start, workouts.sumOf { it.minutes }, workouts.sumOf { it.volumeKg })
    }
}

fun recentHeat(finished: List<WorkoutDetail>, exercises: Map<Long, Exercise>, from: LocalDate, zone: ZoneId): Map<MuscleGroup, Int> =
    muscleHeat(
        finished
            .filter { it.workout.startedAt.toDate(zone) >= from }
            .flatMap { w -> w.exercises.filter { e -> e.sets.any { it.completed } } }
            .mapNotNull { exercises[it.workoutExercise.exerciseId] },
    )
