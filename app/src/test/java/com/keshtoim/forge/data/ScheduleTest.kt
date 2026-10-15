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

    @Test
    fun `upcoming lists each plan once by its next day and time and drops past one-offs`() {
        val late = plan(saturday).copy(id = 1, minuteOfDay = 20 * 60)
        val early = plan(saturday).copy(id = 2, minuteOfDay = 9 * 60)
        val weekly = plan(saturday.minusWeeks(2), DayOfWeek.MONDAY).copy(id = 3)
        val past = plan(saturday.minusDays(3)).copy(id = 4)

        val result = upcoming(listOf(late, weekly, past, early), saturday.minusDays(1))

        assertEquals(listOf(2L, 1L, 3L), result.map { it.plan.id })
        assertEquals(LocalDate.of(2026, 10, 19), result.last().date)
    }

    @Test
    fun `reminder goes off before the next occurrence that is still ahead`() {
        val weekly = plan(saturday, DayOfWeek.SATURDAY).copy(remindBefore = 30)

        assertEquals(saturday.atTime(18, 0), weekly.nextReminder(saturday.atTime(17, 0)))
        // Past this Saturday's reminder, the next one is a week later.
        assertEquals(saturday.plusWeeks(1).atTime(18, 0), weekly.nextReminder(saturday.atTime(18, 10)))
        assertNull(plan(saturday).nextReminder(saturday.atTime(9, 0)))
    }

    @Test
    fun `reminder for an early workout falls on the previous evening and ends with a one-off plan`() {
        val early = plan(saturday).copy(minuteOfDay = 15, remindBefore = 60)

        assertEquals(saturday.minusDays(1).atTime(23, 15), early.nextReminder(saturday.minusDays(1).atTime(12, 0)))
        assertNull(early.nextReminder(saturday.atTime(0, 0)))
    }
}
