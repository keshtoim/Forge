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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
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
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.Workout
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
    onWorkoutFinished: (Long) -> Unit,
    viewModel: WorkoutHomeViewModel = viewModel(factory = WorkoutHomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val home = state ?: return
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    val active = home.active

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
                        trained = home.trainedDays,
                        activeDay = active?.startedAt?.toDate(ZoneId.systemDefault()),
                        onMonthChange = { month = it },
                    )
                }
                if (active != null) {
                    item {
                        ActiveCard(active, onResume = { onOpenWorkout(active.id) }, onFinish = { confirmFinish = true })
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
        )
    }
    if (confirmFinish && active != null) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.workout_finish_confirm)) },
            text = { Text(stringResource(R.string.workout_finish_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmFinish = false; viewModel.finish(active.id, onWorkoutFinished) }) {
                    Text(stringResource(R.string.workout_finish))
                }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, trained: Set<LocalDate>, activeDay: LocalDate?, onMonthChange: (YearMonth) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val today = LocalDate.now()
    val title = DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }

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
            monthGrid(month, firstDay).chunked(7).forEach { week ->
                Row(Modifier.padding(top = 4.dp)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            done = date in trained,
                            today = date == today,
                            active = date == activeDay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                Legend(colors.primary, 12.dp, stringResource(R.string.calendar_done))
                Legend(colors.secondary, 8.dp, stringResource(R.string.calendar_active))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, inMonth: Boolean, done: Boolean, today: Boolean, active: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.height(44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (done && inMonth) colors.primary else Color.Transparent)
                .then(if (today && !done) Modifier.border(2.dp, colors.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (inMonth && (done || today)) FontWeight.Bold else FontWeight.Normal),
                color = when {
                    !inMonth -> colors.outline.copy(alpha = 0.6f)
                    done -> colors.onPrimary
                    else -> colors.onSurface
                },
            )
        }
        Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (active) colors.secondary else Color.Transparent))
    }
}

@Composable
private fun Legend(color: Color, size: Dp, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(size).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
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
        }
    }
}
