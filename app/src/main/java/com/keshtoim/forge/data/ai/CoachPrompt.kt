package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.age
import com.keshtoim.forge.data.db.BodyMeasurement
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.data.navyBodyFat
import com.keshtoim.forge.data.series
import java.time.LocalDate
import kotlin.math.roundToInt

data class Athlete(
    val sex: BodyType? = null,
    val heightCm: Int? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    // From the oldest weigh-in of the last 30 days to the latest one.
    val weightChangeKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val leanMassKg: Double? = null,
)

// What the body log says about the athlete; body fat uses the latest tape measurements, as the profile does.
fun athlete(sex: BodyType?, heightCm: Int?, birthDate: LocalDate?, log: List<BodyMeasurement>, today: LocalDate): Athlete {
    val sorted = log.sortedBy { it.epochDay }
    val weights = sorted.series { it.weightKg }
    val latest = weights.lastOrNull()
    val monthAgo = weights.firstOrNull { it.epochDay >= today.minusDays(30).toEpochDay() }
    fun lastOf(of: (BodyMeasurement) -> Double?) = sorted.series(of).lastOrNull()?.value
    val neck = lastOf { it.neckCm }
    val waist = lastOf { it.waistCm }
    val fat = if (sex != null && heightCm != null && neck != null && waist != null) navyBodyFat(sex, heightCm, neck, waist, lastOf { it.hipsCm }) else null
    return Athlete(
        sex = sex,
        heightCm = heightCm,
        age = birthDate?.let { age(it, today) },
        weightKg = latest?.value,
        weightChangeKg = if (latest != null && monthAgo != null && monthAgo.epochDay < latest.epochDay) latest.value - monthAgo.value else null,
        bodyFatPercent = fat,
        leanMassKg = fat?.let { f -> latest?.value?.let { it * (1 - f / 100) } },
    )
}

data class ExerciseLog(
    val name: String,
    val type: ExerciseType,
    val superset: Int?,
    val sets: List<WorkoutSet>,
    val lastTime: List<WorkoutSet>,
    val onScreen: Boolean,
    val note: String? = null,
)

// The coach sees exactly what the athlete logged; the prompt is in English, the answer in the app's language.
fun coachPrompt(
    language: String,
    athlete: Athlete,
    workoutName: String,
    minutesIn: Long,
    exercises: List<ExerciseLog>,
    workoutNote: String? = null,
): String = buildString {
    appendLine("You are a strength coach inside Forge, a workout log app, talking to the athlete between sets of a workout in progress.")
    appendLine("Answer in $language. Keep replies short enough to read between sets: a few sentences or a short list, plain text without headings or tables.")
    appendLine("Base advice on the log below; if something you need isn't there, ask instead of guessing. You can't edit the log, so say what to enter.")
    appendLine("If the athlete mentions pain or injury, advise stopping that exercise and seeing a professional rather than training through it.")
    appendLine()
    listOfNotNull(
        athlete.sex?.let { if (it == BodyType.MALE) "male" else "female" },
        athlete.heightCm?.let { "height $it cm" },
        athlete.age?.let { "age $it" },
        athlete.weightKg?.let { weight ->
            "body weight ${number(weight)} kg" + athlete.weightChangeKg?.let { " (${signed(it)} kg over the last 30 days)" }.orEmpty()
        },
        athlete.bodyFatPercent?.let { "body fat about ${it.roundToInt()}% by the US Navy tape method" },
        athlete.leanMassKg?.let { "lean mass ${number(it)} kg" },
    ).takeIf { it.isNotEmpty() }?.let { appendLine("Athlete: ${it.joinToString()}.") }
    if (!workoutNote.isNullOrBlank()) appendLine("Athlete's note for this workout: $workoutNote")
    appendLine("Workout \"$workoutName\", started $minutesIn min ago. Exercises in order, > marks the one on screen:")
    exercises.forEachIndexed { i, e ->
        val superset = e.superset?.let { " [superset $it: done back to back, rest after the round]" }.orEmpty()
        appendLine("${if (e.onScreen) ">" else " "} ${i + 1}. ${e.name}$superset")
        appendLine("   today: ${e.sets.sortedBy { it.position }.mapIndexed { n, s -> "${n + 1}) ${setText(e.type, s)}" }.joinToString("; ").ifEmpty { "no sets" }}")
        if (e.lastTime.isNotEmpty()) appendLine("   last time: ${e.lastTime.joinToString("; ") { setText(e.type, it) }}")
        if (!e.note.isNullOrBlank()) appendLine("   athlete's note: ${e.note}")
    }
    appendLine()
    append("Set levels: warm-up (not counted in records), light (many reps in reserve), medium (3-4 in reserve), heavy (1-2 in reserve), max (to failure or a record).")
}

private fun setText(type: ExerciseType, set: WorkoutSet): String {
    val value = when (type) {
        ExerciseType.WEIGHT_REPS -> "${set.weightKg?.let(::number) ?: "?"} kg x ${set.reps ?: "?"}"
        ExerciseType.REPS -> "${set.reps ?: "?"} reps"
        ExerciseType.DURATION -> "${set.durationSec ?: "?"} s"
        ExerciseType.DISTANCE -> "${set.distanceM?.let { number(it / 1000) } ?: "?"} km"
    }
    val level = set.effort?.let { ", ${it.label}" }.orEmpty()
    return if (set.completed) "$value$level, done" else "$value, planned"
}

private val SetEffort.label get() = when (this) {
    SetEffort.WARMUP -> "warm-up"
    SetEffort.LIGHT -> "light"
    SetEffort.MEDIUM -> "medium"
    SetEffort.HEAVY -> "heavy"
    SetEffort.MAX -> "max"
}

private fun signed(value: Double) = (if (value > 0) "+" else "") + number(value)

private fun number(value: Double) = "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
