package com.keshtoim.forge.ui

import android.content.Context
import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BuiltInExercise
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup

fun Exercise.displayName(context: Context): String =
    builtInKey?.let { context.getString(BuiltInExercise.valueOf(it).nameRes) } ?: name.orEmpty()

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
