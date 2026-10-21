package com.keshtoim.forge.ui.workout

import kotlin.math.roundToInt

data class WarmupSet(val weightKg: Double, val reps: Int)

// A classic ramp to the working weight: light and long first, heavier and shorter closer to it.
private val RAMP = listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)

// [stepKg] is the smallest jump the equipment allows; a barbell never goes below its bar.
fun warmupSets(workingKg: Double, stepKg: Double, barKg: Double?): List<WarmupSet> =
    RAMP.map { (share, reps) ->
        val weight = ((workingKg * share) / stepKg).roundToInt() * stepKg
        WarmupSet(maxOf(weight, barKg ?: 0.0), reps)
    }
        .filter { it.weightKg > 0 && it.weightKg < workingKg }
        .distinctBy { it.weightKg }
