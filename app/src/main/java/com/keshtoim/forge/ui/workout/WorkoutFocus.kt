package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet

// The workout screen shows one exercise at a time; its current set is the first one not yet done.
fun WorkoutExerciseWithSets.currentSet(): WorkoutSet? = sets.sortedBy { it.position }.firstOrNull { !it.completed }

fun List<WorkoutExerciseWithSets>.ordered() = sortedBy { it.workoutExercise.position }

fun List<WorkoutExerciseWithSets>.defaultFocus(): WorkoutExerciseWithSets? =
    ordered().let { list -> list.firstOrNull { it.currentSet() != null } ?: list.firstOrNull() }

// Next exercise with sets left after [currentId], wrapping around to ones skipped earlier.
fun List<WorkoutExerciseWithSets>.nextUnfinished(currentId: Long): WorkoutExerciseWithSets? {
    val list = ordered()
    val index = list.indexOfFirst { it.workoutExercise.id == currentId }
    return (list.drop(index + 1) + list.take(index.coerceAtLeast(0)))
        .firstOrNull { it.workoutExercise.id != currentId && it.currentSet() != null }
}
