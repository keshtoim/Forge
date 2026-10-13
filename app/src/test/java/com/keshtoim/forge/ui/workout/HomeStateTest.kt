package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class HomeStateTest {
    private val zone = ZoneOffset.UTC
    private val friday = LocalDate.of(2026, 10, 9)

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun workout(date: LocalDate, minutes: Long, templateId: Long? = null) =
        Workout(templateId = templateId, name = "W", startedAt = at(date, 18), finishedAt = at(date, 18) + minutes * 60_000)

    @Test
    fun `week starts on the locale's first day and counts only this week`() {
        val finished = listOf(
            workout(friday.minusDays(4), 60), // Monday
            workout(friday.minusDays(2), 65), // Wednesday
            workout(friday.minusDays(7), 50), // last week
        )

        val week = week(finished, friday, zone, DayOfWeek.MONDAY)

        assertEquals(DayOfWeek.MONDAY, week.days.first().date.dayOfWeek)
        assertEquals(listOf(true, false, true, false, false, false, false), week.days.map { it.trained })
        assertEquals(2, week.workouts)
        assertEquals(125, week.minutes)
    }

    @Test
    fun `sunday-first locales shift the week`() {
        val week = week(emptyList(), friday, zone, DayOfWeek.SUNDAY)

        assertEquals(friday.minusDays(5), week.days.first().date)
    }

    @Test
    fun `next in rotation is the template done longest ago, never done first`() {
        val lastDone = mapOf(1L to 300L, 2L to 100L)

        assertEquals(2L, nextInRotation(listOf(1L, 2L)) { lastDone[it] })
        assertEquals(3L, nextInRotation(listOf(1L, 2L, 3L)) { lastDone[it] })
        assertNull(nextInRotation(emptyList<Long>()) { lastDone[it] })
    }

    @Test
    fun `last done keeps the latest workout per template and skips free workouts`() {
        val finished = listOf(
            workout(friday.minusDays(6), 60, templateId = 1),
            workout(friday.minusDays(1), 60, templateId = 1),
            workout(friday, 30),
        )

        assertEquals(mapOf(1L to at(friday.minusDays(1), 18)), lastDoneByTemplate(finished))
    }
}
