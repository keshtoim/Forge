package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class HomeStateTest {
    private val zone = ZoneOffset.UTC
    private val friday = LocalDate.of(2026, 10, 9)

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun workout(date: LocalDate, minutes: Long, templateId: Long? = null) =
        Workout(templateId = templateId, name = "W", startedAt = at(date, 18), finishedAt = at(date, 18) + minutes * 60_000)

    @Test
    fun `month grid covers whole weeks from the locale's first day`() {
        val grid = monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 9, 28), grid.first())
        assertEquals(LocalDate.of(2026, 11, 1), grid.last())
        assertEquals(35, grid.size)
    }

    @Test
    fun `sunday-first locales shift the month grid`() {
        val grid = monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 9, 27), grid.first())
        assertEquals(LocalDate.of(2026, 10, 31), grid.last())
    }

    @Test
    fun `workouts are grouped by their local day`() {
        val finished = listOf(workout(friday, 60), workout(friday, 30), workout(friday.minusDays(2), 45))

        assertEquals(mapOf(friday to finished.take(2), friday.minusDays(2) to finished.drop(2)), workoutsByDay(finished, zone))
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
