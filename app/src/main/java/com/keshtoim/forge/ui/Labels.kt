package com.keshtoim.forge.ui

import android.content.Context
import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BuiltInExercise
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.ui.workout.formatDuration
import com.keshtoim.forge.ui.workout.formatNumber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun Exercise.displayName(context: Context): String =
    builtInKey?.let { context.getString(BuiltInExercise.valueOf(it).nameRes) } ?: name.orEmpty()

fun WorkoutSet.summary(type: ExerciseType, context: Context): String {
    fun unit(value: Double?, @StringRes res: Int) = value?.let { "${formatNumber(it)} ${context.getString(res)}" }
    val duration = durationSec?.let { formatDuration(it.toLong()) }
    return when (type) {
        ExerciseType.WEIGHT_REPS -> listOfNotNull(unit(weightKg, R.string.unit_kg), reps?.toString()).joinToString(" × ")
        ExerciseType.REPS -> unit(reps?.toDouble(), R.string.unit_reps).orEmpty()
        ExerciseType.DURATION -> duration.orEmpty()
        ExerciseType.DISTANCE -> listOfNotNull(unit(distanceM?.div(1000), R.string.unit_km), duration).joinToString(" · ")
    }
}

val WorkoutDetail.volumeKg: Double
    get() = exercises.sumOf { e -> e.sets.filter { it.completed }.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } }

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.FOREARMS -> R.string.muscle_forearms
        MuscleGroup.QUADS -> R.string.muscle_quads
        MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
        MuscleGroup.ABS -> R.string.muscle_abs
        MuscleGroup.CARDIO -> R.string.muscle_cardio
        MuscleGroup.OTHER -> R.string.muscle_other
    }

@get:StringRes
val ExerciseType.labelRes: Int
    get() = when (this) {
        ExerciseType.WEIGHT_REPS -> R.string.type_weight_reps
        ExerciseType.REPS -> R.string.type_reps
        ExerciseType.DURATION -> R.string.type_duration
        ExerciseType.DISTANCE -> R.string.type_distance
    }

fun formatDate(millis: Long, style: FormatStyle = FormatStyle.MEDIUM): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(style))

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
