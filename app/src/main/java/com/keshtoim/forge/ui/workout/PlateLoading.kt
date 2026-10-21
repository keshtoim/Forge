package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.WorkoutSet

// Barbell sets are entered as plates on one side; the stored weight is always the total including the bar.
class PlateLoading(val barKg: Double) {
    fun toSide(set: WorkoutSet) = set.copy(weightKg = set.weightKg?.let { ((it - barKg) / 2).coerceAtLeast(0.0) })

    fun toTotal(set: WorkoutSet) = set.copy(weightKg = set.weightKg?.let { barKg + it * 2 })

    // The plates for one side, heaviest first, from a standard gym set; null when the weight can't be made from them.
    fun platesFor(totalKg: Double): List<Double>? {
        var left = (totalKg - barKg) / 2
        if (left < -0.001) return null
        val plates = mutableListOf<Double>()
        PLATES.forEach { plate ->
            while (left >= plate - 0.001) {
                plates += plate
                left -= plate
            }
        }
        return plates.takeIf { left < 0.001 }
    }

    companion object {
        val BAR_WEIGHTS = listOf(20.0, 15.0, 10.0)
        val PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    }
}
