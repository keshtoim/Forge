package com.keshtoim.forge.ui.progress

import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetPoint

enum class Metric(@StringRes val labelRes: Int, @StringRes val unitRes: Int) {
    E1RM(R.string.metric_e1rm, R.string.unit_kg) {
        // Epley formula; a single rep is the weight itself.
        override fun of(sets: List<SetPoint>) = sets.mapNotNull { s ->
            val w = s.weightKg ?: return@mapNotNull null
            val r = s.reps?.takeIf { it > 0 } ?: return@mapNotNull null
            if (r == 1) w else w * (1 + r / 30.0)
        }.maxOrNull()
    },
    MAX_WEIGHT(R.string.metric_max_weight, R.string.unit_kg) {
        override fun of(sets: List<SetPoint>) = sets.mapNotNull { it.weightKg }.maxOrNull()
    },
    VOLUME(R.string.metric_volume, R.string.unit_kg) {
        override fun of(sets: List<SetPoint>) = sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }.takeIf { it > 0 }
    },
    MAX_REPS(R.string.metric_max_reps, R.string.unit_reps) {
        override fun of(sets: List<SetPoint>) = sets.mapNotNull { it.reps?.toDouble() }.maxOrNull()
    },
    TOTAL_REPS(R.string.metric_total_reps, R.string.unit_reps) {
        override fun of(sets: List<SetPoint>) = sets.sumOf { it.reps ?: 0 }.takeIf { it > 0 }?.toDouble()
    },
    MAX_DURATION(R.string.metric_max_duration, R.string.unit_min) {
        override fun of(sets: List<SetPoint>) = sets.mapNotNull { it.durationSec?.div(60.0) }.maxOrNull()
    },
    MAX_DISTANCE(R.string.metric_max_distance, R.string.unit_km) {
        override fun of(sets: List<SetPoint>) = sets.mapNotNull { it.distanceM?.div(1000) }.maxOrNull()
    };

    // Aggregates the sets of a single workout into one chart point.
    abstract fun of(sets: List<SetPoint>): Double?

    companion object {
        fun forType(type: ExerciseType) = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(E1RM, MAX_WEIGHT, VOLUME)
            ExerciseType.REPS -> listOf(MAX_REPS, TOTAL_REPS)
            ExerciseType.DURATION -> listOf(MAX_DURATION)
            ExerciseType.DISTANCE -> listOf(MAX_DISTANCE)
        }
    }
}

data class Point(val time: Long, val value: Double)

fun Metric.series(sets: List<SetPoint>): List<Point> =
    sets.groupBy { it.workoutId }.values.mapNotNull { workout ->
        of(workout)?.let { Point(workout.first().startedAt, it) }
    }.sortedBy { it.time }
