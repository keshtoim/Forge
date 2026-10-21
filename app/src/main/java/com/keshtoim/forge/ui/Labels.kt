package com.keshtoim.forge.ui

import android.content.Context
import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.ThemeMode
import com.keshtoim.forge.data.Upcoming
import com.keshtoim.forge.data.ai.AiFailure
import com.keshtoim.forge.data.ai.ProgramGoal
import com.keshtoim.forge.data.ai.ProgramLevel
import com.keshtoim.forge.data.builtIn
import com.keshtoim.forge.data.db.Discipline
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.Force
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.data.db.isWorking
import com.keshtoim.forge.data.time
import com.keshtoim.forge.ui.workout.formatDuration
import com.keshtoim.forge.ui.workout.formatNumber
import com.keshtoim.forge.ui.workout.toDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

fun Exercise.displayName(context: Context): String =
    builtIn?.let { context.getString(it.nameRes) }
        ?: name ?: builtInKey.orEmpty()

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
    get() = exercises.sumOf { e -> e.sets.filter { it.isWorking }.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } }

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

// Anatomical name shown under the everyday group label; groups that are not a muscle have none.
@get:StringRes
val MuscleGroup.anatomyRes: Int?
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.anatomy_chest
        MuscleGroup.BACK -> R.string.anatomy_back
        MuscleGroup.SHOULDERS -> R.string.anatomy_shoulders
        MuscleGroup.BICEPS -> R.string.anatomy_biceps
        MuscleGroup.TRICEPS -> R.string.anatomy_triceps
        MuscleGroup.FOREARMS -> R.string.anatomy_forearms
        MuscleGroup.QUADS -> R.string.anatomy_quads
        MuscleGroup.HAMSTRINGS -> R.string.anatomy_hamstrings
        MuscleGroup.GLUTES -> R.string.anatomy_glutes
        MuscleGroup.CALVES -> R.string.anatomy_calves
        MuscleGroup.ABS -> R.string.anatomy_abs
        MuscleGroup.CARDIO, MuscleGroup.OTHER -> null
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

@get:StringRes
val BodyType.labelRes: Int
    get() = when (this) {
        BodyType.MALE -> R.string.body_type_male
        BodyType.FEMALE -> R.string.body_type_female
    }

fun relativeDay(context: Context, epochMillis: Long?, today: LocalDate = LocalDate.now()): String {
    epochMillis ?: return context.getString(R.string.never_done)
    return when (val days = ChronoUnit.DAYS.between(epochMillis.toDate(ZoneId.systemDefault()), today).toInt()) {
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.yesterday)
        else -> context.resources.getQuantityString(R.plurals.days_ago, days, days)
    }
}

fun formatMinutes(context: Context, minutes: Long): String =
    if (minutes < 60) context.getString(R.string.duration_m, minutes.toInt())
    else context.getString(R.string.duration_hm, (minutes / 60).toInt(), (minutes % 60).toInt())

// Volumes of thousands of kilograms read better in tonnes.
fun formatWeight(context: Context, kg: Double): String =
    if (kg >= 1000) "${formatNumber((kg / 100).roundToLong() / 10.0)} ${context.getString(R.string.unit_t)}"
    else "${formatNumber((kg * 10).roundToLong() / 10.0)} ${context.getString(R.string.unit_kg)}"

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

fun Upcoming.moment(): String =
    date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())) + " · " + plan.time.format(DateTimeFormatter.ofPattern("HH:mm"))

@get:StringRes
val SetEffort.labelRes: Int
    get() = when (this) {
        SetEffort.WARMUP -> R.string.effort_warmup
        SetEffort.LIGHT -> R.string.effort_light
        SetEffort.MEDIUM -> R.string.effort_medium
        SetEffort.HEAVY -> R.string.effort_heavy
        SetEffort.MAX -> R.string.effort_max
    }

@get:StringRes
val SetEffort.shortRes: Int
    get() = when (this) {
        SetEffort.WARMUP -> R.string.effort_warmup_short
        SetEffort.LIGHT -> R.string.effort_light_short
        SetEffort.MEDIUM -> R.string.effort_medium_short
        SetEffort.HEAVY -> R.string.effort_heavy_short
        SetEffort.MAX -> R.string.effort_max_short
    }

@get:StringRes
val SetEffort.hintRes: Int
    get() = when (this) {
        SetEffort.WARMUP -> R.string.effort_warmup_hint
        SetEffort.LIGHT -> R.string.effort_light_hint
        SetEffort.MEDIUM -> R.string.effort_medium_hint
        SetEffort.HEAVY -> R.string.effort_heavy_hint
        SetEffort.MAX -> R.string.effort_max_hint
    }

@get:StringRes
val Mechanics.labelRes: Int
    get() = when (this) {
        Mechanics.COMPOUND -> R.string.mechanics_compound
        Mechanics.ISOLATION -> R.string.mechanics_isolation
        Mechanics.OTHER -> R.string.trait_other
    }

@get:StringRes
val Discipline.labelRes: Int
    get() = when (this) {
        Discipline.STRENGTH -> R.string.discipline_strength
        Discipline.CARDIO -> R.string.discipline_cardio
        Discipline.STRETCHING -> R.string.discipline_stretching
        Discipline.PLYOMETRIC -> R.string.discipline_plyometric
        Discipline.POWERLIFTING -> R.string.discipline_powerlifting
        Discipline.WEIGHTLIFTING -> R.string.discipline_weightlifting
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.NONE -> R.string.equipment_none
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.BAND -> R.string.equipment_band
        Equipment.FITBALL -> R.string.equipment_fitball
        Equipment.OTHER -> R.string.trait_other
    }

@get:StringRes
val Force.labelRes: Int
    get() = when (this) {
        Force.PUSH -> R.string.force_push
        Force.PULL -> R.string.force_pull
        Force.STATIC -> R.string.force_static
        Force.OTHER -> R.string.trait_other
    }

@get:StringRes
val AiFailure.labelRes: Int
    get() = when (this) {
        AiFailure.NO_CONNECTION -> R.string.ai_error_connection
        AiFailure.BAD_KEY -> R.string.ai_error_key
        AiFailure.BAD_MODEL -> R.string.ai_error_model
        AiFailure.RATE_LIMIT -> R.string.ai_error_limit
        AiFailure.NO_CREDIT -> R.string.ai_error_credit
        AiFailure.REGION -> R.string.ai_error_region
        AiFailure.REFUSED -> R.string.ai_error_refused
        AiFailure.OTHER -> R.string.ai_error_other
    }

@get:StringRes
val ProgramGoal.labelRes: Int
    get() = when (this) {
        ProgramGoal.MUSCLE -> R.string.goal_muscle
        ProgramGoal.STRENGTH -> R.string.goal_strength
        ProgramGoal.FAT_LOSS -> R.string.goal_fat_loss
        ProgramGoal.FITNESS -> R.string.goal_fitness
    }

@get:StringRes
val ProgramLevel.labelRes: Int
    get() = when (this) {
        ProgramLevel.BEGINNER -> R.string.level_beginner
        ProgramLevel.INTERMEDIATE -> R.string.level_intermediate
        ProgramLevel.ADVANCED -> R.string.level_advanced
    }
