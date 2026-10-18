package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet

// The workout screen shows one exercise at a time; its current set is the first one not yet done.
fun WorkoutExerciseWithSets.currentSet(): WorkoutSet? = sets.sortedBy { it.position }.firstOrNull { !it.completed }

fun List<WorkoutExerciseWithSets>.ordered() = sortedBy { it.workoutExercise.position }

fun List<WorkoutExerciseWithSets>.defaultFocus(): WorkoutExerciseWithSets? =
    ordered().let { list -> list.firstOrNull { it.currentSet() != null } ?: list.firstOrNull() }

// Next exercise with sets left after [currentId] and the rest of its superset, wrapping around to ones skipped earlier.
fun List<WorkoutExerciseWithSets>.nextUnfinished(currentId: Long): WorkoutExerciseWithSets? {
    val list = ordered()
    val current = list.firstOrNull { it.workoutExercise.id == currentId }
    val group = current?.let { list.supersetOf(it) }.orEmpty().map { it.workoutExercise.id }.toSet() + currentId
    val last = list.indexOfLast { it.workoutExercise.id in group }
    return (list.drop(last + 1) + list.take(last.coerceAtLeast(0)))
        .firstOrNull { it.workoutExercise.id !in group && it.currentSet() != null }
}

// The superset [focus] belongs to, in order: neighbours sharing its group. Just [focus] outside a superset.
fun List<WorkoutExerciseWithSets>.supersetOf(focus: WorkoutExerciseWithSets): List<WorkoutExerciseWithSets> {
    val group = focus.workoutExercise.superset ?: return listOf(focus)
    val list = ordered()
    val index = list.indexOfFirst { it.workoutExercise.id == focus.workoutExercise.id }
    var start = index
    while (start > 0 && list[start - 1].workoutExercise.superset == group) start--
    var end = index
    while (end < list.lastIndex && list[end + 1].workoutExercise.superset == group) end++
    return list.subList(start, end + 1)
}

// A round is one set position across the superset; the current one is the first with a set still to do.
fun List<WorkoutExerciseWithSets>.currentRound(): Int? {
    val sets = map { e -> e.sets.sortedBy { it.position } }
    return (0 until (sets.maxOfOrNull { it.size } ?: 0)).firstOrNull { r -> sets.any { it.getOrNull(r)?.completed == false } }
}
