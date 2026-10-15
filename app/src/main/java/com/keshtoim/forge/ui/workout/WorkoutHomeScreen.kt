package com.keshtoim.forge.ui.workout

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.Upcoming
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.db.Workout
import com.keshtoim.forge.data.fallsOn
import com.keshtoim.forge.data.occurrences
import com.keshtoim.forge.data.repeats
import com.keshtoim.forge.data.time
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.exercises.MuscleMapDefaults
import com.keshtoim.forge.ui.relativeDay
import com.keshtoim.forge.ui.theme.ForgeTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun WorkoutHomeScreen(
    onOpenWorkout: (Long) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onPlan: (planId: Long, epochDay: Long) -> Unit,
    viewModel: WorkoutHomeViewModel = viewModel(factory = WorkoutHomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val home = state ?: return
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val active = home.active
    val templates = (listOfNotNull(home.next) + home.others).associateBy { it.template.id }

    Box(Modifier.fillMaxSize()) {
        Column {
            ScreenHeader(stringResource(R.string.tab_home)) {
                IconButton(onClick = onOpenProgress) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = stringResource(R.string.more_progress))
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    MonthCalendar(
                        month = month,
                        done = home.workoutsByDay.keys,
                        plans = home.plans,
                        activeDay = active?.startedAt?.toDate(ZoneId.systemDefault()),
                        onMonthChange = { month = it },
                        onDayClick = { selectedDay = it.toEpochDay() },
                    )
                }
                if (active != null) {
                    item {
                        ActiveCard(active, onResume = { onOpenWorkout(active.id) }, onFinish = { confirmFinish = true })
                    }
                }
                home.upcoming?.let { next ->
                    templates[next.plan.templateId]?.let { summary ->
                        item { UpcomingCard(next, summary.template.name) { selectedDay = next.date.toEpochDay() } }
                    }
                }
                item { TotalsCard(home.totals) }
                item { WeeksCard(home.weeks) }
                item { MusclesCard(home.heat) }
            }
        }
        if (active == null) {
            FloatingActionButton(
                onClick = { picking = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).size(60.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.workout_new))
            }
        }
    }

    if (picking && active == null) {
        val defaultName = stringResource(R.string.workout_default_name)
        StartSheet(
            next = home.next,
            others = home.others,
            onDismiss = { picking = false },
            onStartTemplate = { picking = false; viewModel.startTemplate(it, onOpenWorkout) },
            onStartEmpty = { picking = false; viewModel.startEmpty(defaultName, onOpenWorkout) },
            onPlan = { picking = false; onPlan(0, LocalDate.now().toEpochDay()) },
        )
    }
    selectedDay?.let { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        val today = LocalDate.now()
        DaySheet(
            day = day,
            plans = if (day >= today) home.plans.filter { it.fallsOn(day) }.sortedBy { it.minuteOfDay } else emptyList(),
            workouts = home.workoutsByDay[day].orEmpty(),
            templates = templates,
            canStart = active == null && day == today,
            canPlan = day >= today,
            onDismiss = { selectedDay = null },
            onStart = { selectedDay = null; viewModel.startTemplate(it, onOpenWorkout) },
            onEdit = { selectedDay = null; onPlan(it.id, it.epochDay) },
            onPlanMore = { selectedDay = null; onPlan(0, epochDay) },
            onOpenWorkout = { selectedDay = null; onOpenDetail(it) },
        )
    }
    if (confirmFinish && active != null) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.workout_finish_confirm)) },
            text = { Text(stringResource(R.string.workout_finish_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; viewModel.finish(active.id, onOpenDetail) }) {
                    Text(stringResource(R.string.workout_finish))
                }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    done: Set<LocalDate>,
    plans: List<PlannedWorkout>,
    activeDay: LocalDate?,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val today = LocalDate.now()
    val title = DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }
    val grid = monthGrid(month, firstDay)
    // Past plans are not shown: a missed day simply stays empty.
    val planned = remember(plans, month) { plans.flatMapTo(HashSet()) { it.occurrences(maxOf(grid.first(), today), grid.last()) } }

    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.month_prev), tint = colors.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.month_next), tint = colors.primary)
                }
            }
            Row(Modifier.padding(top = 4.dp)) {
                repeat(7) { i ->
                    Text(
                        firstDay.plus(i.toLong()).getDisplayName(TextStyle.SHORT, locale).lowercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            grid.chunked(7).forEach { week ->
                Row(Modifier.padding(top = 4.dp)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            done = date in done,
                            planned = date in planned,
                            today = date == today,
                            active = date == activeDay,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                Legend(colors.primary, 12.dp, stringResource(R.string.calendar_done))
                Legend(colors.primary, 12.dp, stringResource(R.string.calendar_planned), outlined = true)
                Legend(colors.secondary, 8.dp, stringResource(R.string.calendar_active))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    done: Boolean,
    planned: Boolean,
    today: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val filled = done && inMonth
    val ring = when {
        filled -> null
        planned -> colors.primary
        today -> colors.onSurface
        else -> null
    }
    Column(
        modifier.height(44.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (filled) colors.primary else Color.Transparent)
                .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (inMonth && (done || planned || today)) FontWeight.Bold else FontWeight.Normal),
                color = when {
                    !inMonth -> colors.outline.copy(alpha = 0.6f)
                    filled -> colors.onPrimary
                    planned -> colors.primary
                    else -> colors.onSurface
                },
            )
        }
        Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (active) colors.secondary else Color.Transparent))
    }
}

@Composable
private fun Legend(color: Color, size: Dp, label: String, outlined: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .then(if (outlined) Modifier.border(2.dp, color, CircleShape) else Modifier.background(color)),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun UpcomingCard(next: Upcoming, name: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ForgeCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).border(2.dp, colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.day_planned, planMoment(next.date, next.plan)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.outline)
        }
    }
}

private fun planMoment(date: LocalDate, plan: PlannedWorkout): String {
    val locale = Locale.getDefault()
    return date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)) + " · " + plan.time.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    day: LocalDate,
    plans: List<PlannedWorkout>,
    workouts: List<Workout>,
    templates: Map<Long, TemplateSummary>,
    canStart: Boolean,
    canPlan: Boolean,
    onDismiss: () -> Unit,
    onStart: (TemplateSummary) -> Unit,
    onEdit: (PlannedWorkout) -> Unit,
    onPlanMore: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceContainer) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleLarge,
            )
            plans.forEach { plan ->
                val summary = templates[plan.templateId] ?: return@forEach
                val sets = summary.items.sumOf { it.targetSets }
                Surface(shape = MaterialTheme.shapes.medium, color = colors.background, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.day_planned, plan.time.format(DateTimeFormatter.ofPattern("HH:mm"))),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                        Text(summary.template.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            pluralStringResource(R.plurals.exercises_count, summary.items.size, summary.items.size) + " · " +
                                pluralStringResource(R.plurals.sets_total, sets, sets),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        if (plan.repeatDays != 0) {
                            val firstDay = WeekFields.of(locale).firstDayOfWeek
                            val days = (0L until 7L).map { firstDay.plus(it) }.filter { it in plan.repeats }
                            Text(
                                stringResource(R.string.plan_repeat_on, days.joinToString { it.getDisplayName(TextStyle.SHORT, locale).lowercase(locale) }),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (canStart) {
                                Button(onClick = { onStart(summary) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.workout_start)) }
                            }
                            OutlinedButton(onClick = { onEdit(plan) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.edit)) }
                        }
                    }
                }
            }
            if (workouts.isNotEmpty()) {
                Text(stringResource(R.string.day_done), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                workouts.forEach { workout ->
                    val time = Instant.ofEpochMilli(workout.startedAt).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
                    ListItem(
                        headlineContent = { Text(workout.name) },
                        supportingContent = { Text(time) },
                        leadingContent = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onOpenWorkout(workout.id) },
                    )
                }
            }
            if (plans.isEmpty() && workouts.isEmpty()) {
                Text(stringResource(R.string.day_empty), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (canPlan) {
                OutlinedButton(onClick = onPlanMore, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.day_plan_more), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveCard(workout: Workout, onResume: () -> Unit, onFinish: () -> Unit) {
    val panel = ForgeTheme.panel
    val locale = Locale.getDefault()
    val started = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault())
    val subtitle = stringResource(
        R.string.home_started,
        DateTimeFormatter.ofPattern("EEE, d MMMM", locale).format(started).replaceFirstChar { it.titlecase(locale) },
        DateTimeFormatter.ofPattern("HH:mm", locale).format(started),
    )
    Surface(shape = MaterialTheme.shapes.large, color = panel.panel, contentColor = panel.onPanel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_active), style = MaterialTheme.typography.labelLarge, color = panel.muted, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.home_now),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = panel.panel,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(panel.onPanel.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = panel.accent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(workout.name, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = panel.muted)
                }
                Text(rememberElapsed(workout.startedAt), style = MaterialTheme.typography.headlineSmall)
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onResume, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.workout_resume)) }
                OutlinedButton(
                    onClick = onFinish,
                    border = BorderStroke(1.dp, panel.muted.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(stringResource(R.string.workout_finish), color = panel.onPanel)
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: Totals) {
    val cells = listOf(
        totals.workouts to R.string.stats_workouts,
        totals.exercises to R.string.stats_exercises,
        totals.sets to R.string.stats_sets,
        totals.reps to R.string.stats_reps,
    )
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleMedium)
            cells.chunked(2).forEach { row ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (value, label) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(12.dp),
                        ) {
                            Text("%,d".format(Locale.getDefault(), value), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private enum class ChartMetric(@StringRes val label: Int) { BOTH(R.string.chart_both), TIME(R.string.chart_time), VOLUME(R.string.chart_volume) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeksCard(weeks: List<WeekBar>) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    var metric by rememberSaveable { mutableStateOf(ChartMetric.BOTH) }
    // Each metric is scaled to its own maximum, so minutes and kilograms share the chart height.
    val maxMinutes = weeks.maxOf { it.minutes }.coerceAtLeast(1)
    val maxVolume = weeks.maxOf { it.volumeKg }.coerceAtLeast(1.0)
    val showTime = metric != ChartMetric.VOLUME
    val showVolume = metric != ChartMetric.TIME

    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.weeks_title), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                ChartMetric.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == metric,
                        onClick = { metric = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, ChartMetric.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primaryContainer,
                            activeContentColor = colors.onPrimaryContainer,
                        ),
                    ) { Text(stringResource(entry.label), maxLines = 1) }
                }
            }
            Row(Modifier.fillMaxWidth().height(130.dp).padding(top = 16.dp), verticalAlignment = Alignment.Bottom) {
                weeks.forEach { week ->
                    Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                        if (showTime) Bar(week.minutes.toFloat() / maxMinutes, colors.primary)
                        if (showVolume) Bar((week.volumeKg / maxVolume).toFloat(), colors.secondary)
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.padding(top = 6.dp)) {
                weeks.forEach { week ->
                    Text(
                        DateTimeFormatter.ofPattern("d MMM", locale).format(week.start),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showTime) Legend(colors.primary, 10.dp, stringResource(R.string.chart_time_legend))
                if (showVolume) Legend(colors.secondary, 10.dp, stringResource(R.string.chart_volume_legend))
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    if (fraction <= 0f) return
    Box(Modifier.width(16.dp).fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(color))
}

@Composable
private fun MusclesCard(heat: Map<MuscleGroup, Int>) {
    val mapColors = MuscleMapDefaults.colors()
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) { Text(stringResource(R.string.muscles_title), style = MaterialTheme.typography.titleMedium) }
            MuscleMap(heat, Modifier.padding(top = 10.dp).width(280.dp), mapColors)
            if (heat.isEmpty()) {
                Text(
                    stringResource(R.string.muscles_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.heat_low), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(mapColors.levels)),
                    )
                    Text(stringResource(R.string.heat_high), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSheet(
    next: TemplateSummary?,
    others: List<TemplateSummary>,
    onDismiss: () -> Unit,
    onStartTemplate: (TemplateSummary) -> Unit,
    onStartEmpty: () -> Unit,
    onPlan: () -> Unit,
) {
    val context = LocalContext.current
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(stringResource(R.string.workout_new), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            listOfNotNull(next).plus(others).forEach { summary ->
                val count = pluralStringResource(R.plurals.exercises_count, summary.items.size, summary.items.size)
                val caption = if (summary == next) stringResource(R.string.home_next) else relativeDay(context, summary.lastDoneAt)
                ListItem(
                    headlineContent = { Text(summary.template.name) },
                    supportingContent = { Text("$caption · $count") },
                    leadingContent = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    colors = itemColors,
                    modifier = Modifier.padding(horizontal = 8.dp).clip(MaterialTheme.shapes.small).clickable { onStartTemplate(summary) },
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.workout_no_template)) },
                leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                colors = itemColors,
                modifier = Modifier.padding(horizontal = 8.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onStartEmpty),
            )
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text(stringResource(R.string.plan_title)) },
                leadingContent = { Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                colors = itemColors,
                modifier = Modifier.padding(horizontal = 8.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onPlan),
            )
        }
    }
}
