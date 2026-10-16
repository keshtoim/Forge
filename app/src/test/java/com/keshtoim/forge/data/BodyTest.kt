package com.keshtoim.forge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyTest {
    @Test
    fun `indices for 62_4 kg at 165 cm`() {
        assertEquals(22.92, BmiFormula.QUETELET.of(62.4, 165), 0.01)
        assertEquals(23.19, BmiFormula.TREFETHEN.of(62.4, 165), 0.01)
        assertEquals(13.89, BmiFormula.PONDERAL.of(62.4, 165), 0.01)
    }

    @Test
    fun `categories follow WHO bounds and the ponderal norm`() {
        assertEquals(BmiCategory.UNDERWEIGHT, BmiFormula.QUETELET.category(18.4))
        assertEquals(BmiCategory.NORMAL, BmiFormula.QUETELET.category(18.5))
        assertEquals(BmiCategory.OVERWEIGHT, BmiFormula.TREFETHEN.category(25.0))
        assertEquals(BmiCategory.OBESE, BmiFormula.QUETELET.category(30.0))
        assertEquals(BmiCategory.NORMAL, BmiFormula.PONDERAL.category(15.0))
        assertEquals(BmiCategory.OVERWEIGHT, BmiFormula.PONDERAL.category(15.1))
    }

    @Test
    fun `normal range and ideal weights at 165 cm`() {
        val range = normalWeightRange(165)
        assertEquals(50.37, range.start, 0.01)
        assertEquals(68.06, range.endInclusive, 0.01)
        assertEquals(56.9, devineWeight(165, BodyType.FEMALE), 0.05)
        assertEquals(57.4, robinsonWeight(165, BodyType.FEMALE), 0.05)
        assertEquals(61.4, devineWeight(165, BodyType.MALE), 0.05)
        assertEquals(61.4, robinsonWeight(165, BodyType.MALE), 0.05)
    }

    @Test
    fun `navy body fat for a man and a woman`() {
        assertEquals(16.4, navyBodyFat(BodyType.MALE, 178, neckCm = 38.0, waistCm = 85.0, hipsCm = null)!!, 0.1)
        assertEquals(26.3, navyBodyFat(BodyType.FEMALE, 165, neckCm = 31.5, waistCm = 71.2, hipsCm = 96.0)!!, 0.1)
        assertNull(navyBodyFat(BodyType.FEMALE, 165, neckCm = 31.5, waistCm = 71.2, hipsCm = null))
        assertNull(navyBodyFat(BodyType.MALE, 178, neckCm = 40.0, waistCm = 39.0, hipsCm = null))
    }

    @Test
    fun `body fat categories differ by sex`() {
        assertEquals(BodyFatCategory.FITNESS, bodyFatCategory(BodyType.MALE, 16.4))
        assertEquals(BodyFatCategory.AVERAGE, bodyFatCategory(BodyType.FEMALE, 26.3))
        assertEquals(BodyFatCategory.ESSENTIAL, bodyFatCategory(BodyType.MALE, 4.0))
        assertEquals(BodyFatCategory.OBESE, bodyFatCategory(BodyType.FEMALE, 32.0))
    }

    @Test
    fun `fat-free mass index is normalised to 1_8 m`() {
        // 80 kg at 180 cm with 15 % fat: 68 kg lean over 3.24 m², no height correction.
        assertEquals(20.99, ffmi(80.0, 180, 15.0), 0.01)
        // 64 kg lean at 170 cm gets 0.61 added for being 10 cm shorter.
        assertEquals(22.76, ffmi(80.0, 170, 20.0), 0.01)
    }

    @Test
    fun `waist ratios use the usual cut-offs`() {
        assertEquals(WaistRisk.LOW, waistToHeightRisk(71.2 / 165))
        assertEquals(WaistRisk.INCREASED, waistToHeightRisk(0.55))
        assertEquals(WaistRisk.HIGH, waistToHeightRisk(0.6))
        assertEquals(WaistRisk.LOW, waistToHipRisk(BodyType.FEMALE, 71.2 / 96))
        assertEquals(WaistRisk.INCREASED, waistToHipRisk(BodyType.MALE, 0.95))
        assertEquals(WaistRisk.HIGH, waistToHipRisk(BodyType.FEMALE, 0.86))
    }

    @Test
    fun `age counts only full years`() {
        val birth = LocalDate.of(1998, 3, 12)
        assertEquals(28, age(birth, LocalDate.of(2026, 10, 15)))
        assertEquals(27, age(birth, LocalDate.of(2026, 3, 11)))
    }
}
