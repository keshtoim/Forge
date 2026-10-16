package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.BodyMeasurement
import java.time.LocalDate
import java.time.Period
import kotlin.math.log10
import kotlin.math.pow

enum class BmiFormula {
    // Weight over height squared; WHO categories are defined on it.
    QUETELET,
    // 1.3 × weight / height^2.5: corrects the classic index for very tall and very short people.
    TREFETHEN,
    // Weight over height cubed (Rohrer's index), with its own normal range of 11–15.
    PONDERAL;

    fun of(weightKg: Double, heightCm: Int): Double {
        val m = heightCm / 100.0
        return when (this) {
            QUETELET -> weightKg / m.pow(2)
            TREFETHEN -> 1.3 * weightKg / m.pow(2.5)
            PONDERAL -> weightKg / m.pow(3)
        }
    }

    fun category(value: Double): BmiCategory = when (this) {
        PONDERAL -> when {
            value < 11 -> BmiCategory.UNDERWEIGHT
            value <= 15 -> BmiCategory.NORMAL
            else -> BmiCategory.OVERWEIGHT
        }
        else -> when {
            value < 18.5 -> BmiCategory.UNDERWEIGHT
            value < 25 -> BmiCategory.NORMAL
            value < 30 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }
}

enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

// Weights that keep the classic index within 18.5–25 at this height.
fun normalWeightRange(heightCm: Int): ClosedFloatingPointRange<Double> {
    val m2 = (heightCm / 100.0).pow(2)
    return 18.5 * m2..25 * m2
}

private fun inchesOver5Feet(heightCm: Int) = heightCm / 2.54 - 60

fun devineWeight(heightCm: Int, sex: BodyType): Double =
    (if (sex == BodyType.MALE) 50.0 else 45.5) + 2.3 * inchesOver5Feet(heightCm)

fun robinsonWeight(heightCm: Int, sex: BodyType): Double =
    if (sex == BodyType.MALE) 52 + 1.9 * inchesOver5Feet(heightCm) else 49 + 1.7 * inchesOver5Feet(heightCm)

fun age(birthDate: LocalDate, today: LocalDate): Int = Period.between(birthDate, today).years

data class Reading(val epochDay: Long, val value: Double)

// One parameter's history, skipping days on which it was not measured.
fun List<BodyMeasurement>.series(of: (BodyMeasurement) -> Double?): List<Reading> =
    mapNotNull { m -> of(m)?.let { Reading(m.epochDay, it) } }

// US Navy circumference method (Hodgdon & Beckett); the female formula also needs the hips.
fun navyBodyFat(sex: BodyType, heightCm: Int, neckCm: Double, waistCm: Double, hipsCm: Double?): Double? {
    val h = log10(heightCm.toDouble())
    val fat = when (sex) {
        BodyType.MALE -> {
            if (waistCm <= neckCm) return null
            495 / (1.0324 - 0.19077 * log10(waistCm - neckCm) + 0.15456 * h) - 450
        }
        BodyType.FEMALE -> {
            val girth = (hipsCm ?: return null) + waistCm - neckCm
            if (girth <= 0) return null
            495 / (1.29579 - 0.35004 * log10(girth) + 0.221 * h) - 450
        }
    }
    return fat.takeIf { it in 2.0..70.0 }
}

enum class BodyFatCategory { ESSENTIAL, ATHLETIC, FITNESS, AVERAGE, OBESE }

// American Council on Exercise ranges.
fun bodyFatCategory(sex: BodyType, percent: Double): BodyFatCategory {
    val bounds = if (sex == BodyType.MALE) listOf(6.0, 14.0, 18.0, 25.0) else listOf(14.0, 21.0, 25.0, 32.0)
    return BodyFatCategory.entries[bounds.count { percent >= it }]
}

// Fat-free mass index normalised to 1.8 m: a BMI that counts muscle rather than fat.
fun ffmi(weightKg: Double, heightCm: Int, bodyFatPercent: Double): Double {
    val m = heightCm / 100.0
    return weightKg * (1 - bodyFatPercent / 100) / m.pow(2) + 6.1 * (1.8 - m)
}

enum class WaistRisk { LOW, INCREASED, HIGH }

fun waistToHeightRisk(ratio: Double): WaistRisk = when {
    ratio < 0.5 -> WaistRisk.LOW
    ratio < 0.6 -> WaistRisk.INCREASED
    else -> WaistRisk.HIGH
}

// WHO cut-offs for abdominal obesity.
fun waistToHipRisk(sex: BodyType, ratio: Double): WaistRisk {
    val (increased, high) = if (sex == BodyType.MALE) 0.9 to 1.0 else 0.8 to 0.85
    return when {
        ratio < increased -> WaistRisk.LOW
        ratio < high -> WaistRisk.INCREASED
        else -> WaistRisk.HIGH
    }
}
