package com.keshtoim.forge.ui.profile

import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.db.BodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProfileStateTest {
    private val today = LocalDate.of(2026, 10, 15)

    private fun state(vararg log: Pair<LocalDate, Double>) =
        ProfileState(BodyType.FEMALE, 165, null, log.map { (day, kg) -> BodyMeasurement(epochDay = day.toEpochDay(), weightKg = kg) })

    @Test
    fun `trend covers only the last three months`() {
        val profile = state(today.minusMonths(5) to 70.0, today.minusMonths(3) to 64.0, today.minusDays(10) to 63.0, today to 62.4)

        assertEquals(-1.6, profile.recentChange(today)!!, 1e-9)
        assertEquals(62.4, profile.weightKg!!, 0.0)
    }

    @Test
    fun `a single recent weigh-in gives no trend`() {
        assertNull(state(today.minusMonths(4) to 70.0, today to 62.4).recentChange(today))
    }
}
