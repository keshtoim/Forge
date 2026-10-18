package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet

data class Athlete(val heightCm: Int?, val age: Int?, val weightKg: Double?)

data class ExerciseLog(
    val name: String,
    val type: ExerciseType,
    val superset: Int?,
    val sets: List<WorkoutSet>,
    val lastTime: List<WorkoutSet>,
    val onScreen: Boolean,
)

// The coach sees exactly what the athlete logged; the prompt is in English, the answer in the app's language.
fun coachPrompt(language: String, athlete: Athlete, workoutName: String, minutesIn: Long, exercises: List<ExerciseLog>): String = buildString {
    appendLine("You are a strength coach inside Forge, a workout log app, talking to the athlete between sets of a workout in progress.")
    appendLine("Answer in $language. Keep replies short enough to read between sets: a few sentences or a short list, plain text without headings or tables.")
    appendLine("Base advice on the log below; if something you need isn't there, ask instead of guessing. You can't edit the log, so say what to enter.")
    appendLine("If the athlete mentions pain or injury, advise stopping that exercise and seeing a professional rather than training through it.")
    appendLine()
    listOfNotNull(
        athlete.heightCm?.let { "height $it cm" },
        athlete.age?.let { "age $it" },
        athlete.weightKg?.let { "body weight ${number(it)} kg" },
    ).takeIf { it.isNotEmpty() }?.let { appendLine("Athlete: ${it.joinToString()}.") }
    appendLine("Workout \"$workoutName\", started $minutesIn min ago. Exercises in order, > marks the one on screen:")
    exercises.forEachIndexed { i, e ->
        val superset = e.superset?.let { " [superset $it: done back to back, rest after the round]" }.orEmpty()
        appendLine("${if (e.onScreen) ">" else " "} ${i + 1}. ${e.name}$superset")
        appendLine("   today: ${e.sets.sortedBy { it.position }.mapIndexed { n, s -> "${n + 1}) ${setText(e.type, s)}" }.joinToString("; ").ifEmpty { "no sets" }}")
        if (e.lastTime.isNotEmpty()) appendLine("   last time: ${e.lastTime.joinToString("; ") { setText(e.type, it) }}")
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

private fun number(value: Double) = "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
