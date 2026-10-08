package com.keshtoim.forge.ui.workout

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardType
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutSet
import java.math.BigDecimal
import java.math.RoundingMode

enum class SetField(@StringRes val unitRes: Int, val keyboard: KeyboardType, val step: Double, private val max: Double) {
    WEIGHT(R.string.unit_kg, KeyboardType.Decimal, 2.5, 1_000.0) {
        override fun value(set: WorkoutSet) = set.weightKg
        override fun with(set: WorkoutSet, value: Double?) = set.copy(weightKg = value)
    },
    REPS(R.string.unit_reps, KeyboardType.Number, 1.0, 1_000.0) {
        override fun value(set: WorkoutSet) = set.reps?.toDouble()
        override fun with(set: WorkoutSet, value: Double?) = set.copy(reps = value?.toInt())
    },
    MINUTES(R.string.unit_min, KeyboardType.Number, 1.0, 999.0) {
        override fun value(set: WorkoutSet) = set.durationSec?.let { (it / 60).toDouble() }
        override fun with(set: WorkoutSet, value: Double?): WorkoutSet {
            val seconds = (set.durationSec ?: 0) % 60
            return set.copy(durationSec = if (value == null && seconds == 0) null else (value ?: 0.0).toInt() * 60 + seconds)
        }
    },
    SECONDS(R.string.unit_sec, KeyboardType.Number, 5.0, 59.0) {
        override fun value(set: WorkoutSet) = set.durationSec?.let { (it % 60).toDouble() }
        override fun with(set: WorkoutSet, value: Double?): WorkoutSet {
            val minutes = (set.durationSec ?: 0) / 60
            return set.copy(durationSec = if (value == null && minutes == 0) null else minutes * 60 + (value ?: 0.0).toInt())
        }
    },
    DISTANCE(R.string.unit_km, KeyboardType.Decimal, 0.5, 1_000.0) {
        override fun value(set: WorkoutSet) = set.distanceM?.div(1000)
        override fun with(set: WorkoutSet, value: Double?) = set.copy(distanceM = value?.times(1000))
    };

    abstract fun value(set: WorkoutSet): Double?
    abstract fun with(set: WorkoutSet, value: Double?): WorkoutSet

    fun read(set: WorkoutSet): String = value(set)?.let(::formatNumber).orEmpty()

    fun write(set: WorkoutSet, text: String): WorkoutSet {
        // toDoubleOrNull accepts "Infinity" and "1e999", which would break formatting and Int math.
        val parsed = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.coerceAtMost(max)
        return with(set, if (keyboard == KeyboardType.Number) parsed?.toInt()?.toDouble() else parsed)
    }

    fun step(set: WorkoutSet, ghost: WorkoutSet?, direction: Int): WorkoutSet {
        val base = value(set) ?: ghost?.let(::value) ?: 0.0
        return with(set, (base + step * direction).coerceIn(0.0, max))
    }

    companion object {
        fun forType(type: ExerciseType) = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(WEIGHT, REPS)
            ExerciseType.REPS -> listOf(REPS)
            ExerciseType.DURATION -> listOf(MINUTES, SECONDS)
            ExerciseType.DISTANCE -> listOf(DISTANCE, MINUTES)
        }
    }
}

// Rounds away floating point noise such as 10.1 * 3 = 30.299999999999997.
fun formatNumber(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
