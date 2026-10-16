package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.BodyMeasurement
import java.time.LocalDate
import java.time.Period
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
