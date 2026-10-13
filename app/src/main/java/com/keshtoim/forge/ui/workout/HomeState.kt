package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeekDay(val date: LocalDate, val trained: Boolean)

data class Week(val days: List<WeekDay>, val workouts: Int, val minutes: Long)

fun week(finished: List<Workout>, today: LocalDate, zone: ZoneId, firstDay: DayOfWeek): Week {
    val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
    val days = (0L until 7L).map { start.plusDays(it) }
    val thisWeek = finished.filter { it.startedAt.toDate(zone) in days }
    val trained = thisWeek.map { it.startedAt.toDate(zone) }.toSet()
    return Week(
        days = days.map { WeekDay(it, it in trained) },
        workouts = thisWeek.size,
        minutes = thisWeek.sumOf { ((it.finishedAt ?: it.startedAt) - it.startedAt) / 60_000 },
    )
}

// Templates are done in rotation: the next one is the template done longest ago, never-done ones first.
fun <T> nextInRotation(templates: List<T>, lastDoneAt: (T) -> Long?): T? =
    templates.minByOrNull { lastDoneAt(it) ?: Long.MIN_VALUE }

fun lastDoneByTemplate(finished: List<Workout>): Map<Long, Long> =
    finished.filter { it.templateId != null }.groupBy { it.templateId!! }.mapValues { (_, w) -> w.maxOf { it.startedAt } }

fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
