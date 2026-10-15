package com.keshtoim.forge.data

import com.keshtoim.forge.data.db.PlannedWorkout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

val PlannedWorkout.date: LocalDate get() = LocalDate.ofEpochDay(epochDay)

val PlannedWorkout.time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

val PlannedWorkout.repeats: Set<DayOfWeek>
    get() = DayOfWeek.entries.filterTo(LinkedHashSet()) { repeatDays and (1 shl it.ordinal) != 0 }

fun repeatMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl day.ordinal) }

// The first day always counts; a repeating plan then also falls on its weekdays after it.
fun PlannedWorkout.fallsOn(day: LocalDate): Boolean = day == date || (day > date && day.dayOfWeek in repeats)

fun PlannedWorkout.occurrences(from: LocalDate, to: LocalDate): List<LocalDate> =
    generateSequence(maxOf(from, date)) { it.plusDays(1) }.takeWhile { it <= to }.filter(::fallsOn).toList()

fun PlannedWorkout.nextOccurrence(from: LocalDate): LocalDate? = occurrences(from, maxOf(from, date).plusDays(7)).firstOrNull()

data class Upcoming(val plan: PlannedWorkout, val date: LocalDate)

// The next occurrence of every plan from [today] on, soonest first.
fun upcoming(plans: List<PlannedWorkout>, today: LocalDate): List<Upcoming> =
    plans.mapNotNull { plan -> plan.nextOccurrence(today)?.let { Upcoming(plan, it) } }
        .sortedWith(compareBy({ it.date }, { it.plan.minuteOfDay }))
