package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutSet

enum class ProgressionStep { ADD_WEIGHT, ADD_REP, HOLD }

data class Progression(val weightKg: Double, val reps: Int, val step: ProgressionStep)

// Double progression: work up the rep range at one weight, then add weight and start again from the bottom of the range.
// Compound lifts use 6-10 reps, everything else 10-15; a set taken to the max last time means holding where it was.
fun progression(lastTime: List<WorkoutSet>, mechanics: Mechanics?, equipment: Equipment?): Progression? {
    val working = lastTime.filter { it.completed && it.effort != SetEffort.WARMUP && it.weightKg != null && it.reps != null }
    val top = working.maxOfOrNull { it.weightKg!! } ?: return null
    val atTop = working.filter { it.weightKg == top }
    val range = if (mechanics == Mechanics.COMPOUND) 6..10 else 10..15
    val fewest = atTop.minOf { it.reps!! }
    return when {
        atTop.any { it.effort == SetEffort.MAX } -> Progression(top, fewest, ProgressionStep.HOLD)
        atTop.all { it.reps!! >= range.last } -> Progression(top + if (equipment == Equipment.DUMBBELL) 2.0 else 2.5, range.first, ProgressionStep.ADD_WEIGHT)
        else -> Progression(top, (fewest + 1).coerceIn(range), ProgressionStep.ADD_REP)
    }
}
