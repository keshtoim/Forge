package com.keshtoim.forge.data.ai

import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.data.normalizeSupersets
import com.keshtoim.forge.data.runs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ProgramGoal { MUSCLE, STRENGTH, FAT_LOSS, FITNESS }

enum class ProgramLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class ProgramRequest(
    val goal: ProgramGoal,
    val daysPerWeek: Int,
    val minutes: Int,
    val level: ProgramLevel,
    val equipment: Set<Equipment>,
    val wishes: String,
)

// One exercise the AI may choose from; it answers with these ids, so nothing outside the app's catalog gets in.
data class CatalogEntry(val id: Long, val name: String, val muscle: MuscleGroup, val equipment: Equipment?, val mechanics: Mechanics?, val type: ExerciseType)

data class DraftWorkout(val name: String, val items: List<TemplateExercise>)

data class ProgramDraft(val name: String, val notes: String, val workouts: List<DraftWorkout>)

fun programPrompt(language: String, request: ProgramRequest, athlete: Athlete, catalog: List<CatalogEntry>): String = buildString {
    appendLine("You are a strength coach inside Forge, a workout log app. Build a weekly training program for the athlete.")
    appendLine("Goal: ${request.goal.name.lowercase().replace('_', ' ')}. Level: ${request.level.name.lowercase()}.")
    appendLine("Workouts per week: ${request.daysPerWeek}, about ${request.minutes} minutes each including rest.")
    if (request.wishes.isNotBlank()) appendLine("The athlete's wishes and limits: ${request.wishes.trim()}")
    appendAthlete(athlete)
    appendLine()
    appendLine("Use only exercises from this catalog, by id (id | name | main muscle | equipment | mechanics | logged as):")
    catalog.forEach { e ->
        appendLine("${e.id} | ${e.name} | ${e.muscle.name.lowercase()} | ${e.equipment?.name?.lowercase() ?: "-"} | ${e.mechanics?.name?.lowercase() ?: "-"} | ${e.type.name.lowercase()}")
    }
    appendLine()
    appendLine("Rules: exactly ${request.daysPerWeek} workouts; compound lifts first; 2 to 5 sets per exercise;")
    appendLine("fit the time: roughly 14 working sets in 45 minutes, 18 in 60 and 26 in 90; cover every major muscle group across the week.")
    appendLine("A superset is neighbouring exercises with the same superset number inside one workout; use it only where it saves time sensibly, like opposing muscles.")
    appendLine("Name the workouts and write the notes in $language. Notes: two or three sentences on how to progress and rest.")
    append(
        """Reply with only a JSON object and no other text: {"name": string, "notes": string, "workouts": [{"name": string, "exercises": [{"id": number, "sets": integer, "superset": integer or null}]}]}""",
    )
}

@Serializable
private data class RawProgram(val name: String = "", val notes: String = "", val workouts: List<RawWorkout> = emptyList())

@Serializable
private data class RawWorkout(val name: String = "", val exercises: List<RawItem> = emptyList())

@Serializable
private data class RawItem(val id: Long, val sets: Int = 3, val superset: Int? = null)

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// Ids outside [known] are dropped rather than trusted; supersets are tidied the same way the template editor does.
fun parseProgram(reply: String, known: Set<Long>): ProgramDraft? {
    val start = reply.indexOf('{')
    val end = reply.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val raw = runCatching { json.decodeFromString(RawProgram.serializer(), reply.substring(start, end + 1)) }.getOrNull() ?: return null
    val workouts = raw.workouts.mapIndexedNotNull { index, workout ->
        val items = workout.exercises.filter { it.id in known }.mapIndexed { position, item ->
            TemplateExercise(templateId = 0, exerciseId = item.id, position = position, targetSets = item.sets.coerceIn(1, 10), superset = item.superset)
        }.toMutableList()
        if (items.isEmpty()) return@mapIndexedNotNull null
        items.normalizeSupersets()
        // Members of a superset share the number of rounds; the first exercise decides it.
        items.runs().filter { it.count() > 1 }.forEach { run -> run.forEach { items[it] = items[it].copy(targetSets = items[run.first].targetSets) } }
        DraftWorkout(workout.name.trim().ifEmpty { "${index + 1}" }, items)
    }
    if (workouts.isEmpty()) return null
    return ProgramDraft(raw.name.trim(), raw.notes.trim(), workouts)
}
