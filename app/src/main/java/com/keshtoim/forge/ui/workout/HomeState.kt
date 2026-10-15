package com.keshtoim.forge.ui.workout

import com.keshtoim.forge.data.db.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// Whole weeks covering the month, starting on the locale's first day, so the grid has no gaps.
fun monthGrid(month: YearMonth, firstDay: DayOfWeek): List<LocalDate> {
    val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDay))
    val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(firstDay.minus(1)))
    return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()
}

fun trainedDays(finished: List<Workout>, zone: ZoneId): Set<LocalDate> = finished.mapTo(HashSet()) { it.startedAt.toDate(zone) }

// Templates are done in rotation: the next one is the template done longest ago, never-done ones first.
fun <T> nextInRotation(templates: List<T>, lastDoneAt: (T) -> Long?): T? =
    templates.minByOrNull { lastDoneAt(it) ?: Long.MIN_VALUE }

fun lastDoneByTemplate(finished: List<Workout>): Map<Long, Long> =
    finished.filter { it.templateId != null }.groupBy { it.templateId!! }.mapValues { (_, w) -> w.maxOf { it.startedAt } }

fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
