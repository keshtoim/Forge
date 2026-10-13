package com.keshtoim.forge.ui.exercises

import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.secondary

// Load of a set of exercises: primary muscles count double, then scores are scaled to levels 1..3.
fun muscleHeat(exercises: List<Exercise>): Map<MuscleGroup, Int> {
    val score = mutableMapOf<MuscleGroup, Int>()
    exercises.forEach { e ->
        score.merge(e.muscleGroup, 2, Int::plus)
        e.secondary.forEach { score.merge(it, 1, Int::plus) }
    }
    val max = score.values.maxOrNull() ?: return emptyMap()
    return score.mapValues { (_, s) -> ((s * 3 + max - 1) / max).coerceIn(1, 3) }
}
