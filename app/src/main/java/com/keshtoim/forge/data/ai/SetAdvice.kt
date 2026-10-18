package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SetAdvice(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceM: Double? = null,
    val reason: String = "",
) {
    fun applyTo(set: WorkoutSet) = set.copy(
        weightKg = weightKg ?: set.weightKg,
        reps = reps ?: set.reps,
        durationSec = durationSec ?: set.durationSec,
        distanceM = distanceM ?: set.distanceM,
    )
}

fun setAdviceRequest(exercise: String, type: ExerciseType, setNumber: Int): String {
    val fields = when (type) {
        ExerciseType.WEIGHT_REPS -> "weightKg and reps"
        ExerciseType.REPS -> "reps"
        ExerciseType.DURATION -> "durationSec"
        ExerciseType.DISTANCE -> "distanceM"
    }
    return "Suggest the target for set $setNumber of $exercise, the set on screen now. " +
        "Reply with only a JSON object and no other text: " +
        """{"weightKg": number or null, "reps": integer or null, "durationSec": integer or null, "distanceM": number or null, "reason": "one short sentence"}. """ +
        "Fill only $fields, leave the rest null, and write the reason in the language you were asked to answer in."
}

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

// Models sometimes wrap the object in prose or a code fence, so only the outermost braces are read.
fun parseSetAdvice(reply: String): SetAdvice? {
    val start = reply.indexOf('{')
    val end = reply.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val advice = runCatching { json.decodeFromString(SetAdvice.serializer(), reply.substring(start, end + 1)) }.getOrNull() ?: return null
    val sane = (advice.weightKg ?: 0.0) in 0.0..1000.0 && (advice.reps ?: 1) in 1..200 &&
        (advice.durationSec ?: 1) in 1..36_000 && (advice.distanceM ?: 1.0) in 1.0..1_000_000.0
    val anything = listOf(advice.weightKg, advice.reps, advice.durationSec, advice.distanceM).any { it != null }
    return advice.takeIf { sane && anything }
}
