package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.PlannedWorkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTest {
    private val saturday = LocalDate.of(2026, 10, 17)

    private fun plan(date: LocalDate, vararg repeat: DayOfWeek) =
        PlannedWorkout(templateId = 1, epochDay = date.toEpochDay(), minuteOfDay = 18 * 60 + 30, repeatDays = repeatMask(repeat.toSet()))

    @Test
    fun `repeat days round trip through the mask`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)

        assertEquals(days, plan(saturday, *days.toTypedArray()).repeats)
        assertEquals(LocalTime.of(18, 30), plan(saturday).time)
    }

    @Test
    fun `one-off plan falls only on its day`() {
        val oneOff = plan(saturday)

        assertEquals(listOf(saturday), oneOff.occurrences(saturday.minusDays(10), saturday.plusDays(10)))
        assertEquals(saturday, oneOff.nextOccurrence(saturday.minusDays(30)))
        assertNull(oneOff.nextOccurrence(saturday.plusDays(1)))
    }

    @Test
    fun `repeating plan starts on its first day and then follows its weekdays`() {
        // Starts on a Saturday, then repeats on Tuesdays and Thursdays only.
        val repeating = plan(saturday, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)

        assertEquals(
            listOf(saturday, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22)),
            repeating.occurrences(saturday.minusDays(7), LocalDate.of(2026, 10, 24)),
        )
        assertEquals(LocalDate.of(2026, 10, 27), repeating.nextOccurrence(LocalDate.of(2026, 10, 23)))
    }
}
