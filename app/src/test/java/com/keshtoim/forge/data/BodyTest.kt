package com.keshtoim.forge.data

import org.junit.Assert.assertEquals
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
    fun `age counts only full years`() {
        val birth = LocalDate.of(1998, 3, 12)
        assertEquals(28, age(birth, LocalDate.of(2026, 10, 15)))
        assertEquals(27, age(birth, LocalDate.of(2026, 3, 11)))
    }
}
