package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutSet

// Barbell sets are entered as plates on one side; the stored weight is always the total including the bar.
class PlateLoading(val barKg: Double) {
    fun toSide(set: WorkoutSet) = set.copy(weightKg = set.weightKg?.let { ((it - barKg) / 2).coerceAtLeast(0.0) })

    fun toTotal(set: WorkoutSet) = set.copy(weightKg = set.weightKg?.let { barKg + it * 2 })

    companion object {
        val BAR_WEIGHTS = listOf(20.0, 15.0, 10.0)
    }
}
