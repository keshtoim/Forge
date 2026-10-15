package com.keshtoim.forge.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.theme.ForgeTheme
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.exercises.MuscleMapDefaults
import com.keshtoim.forge.ui.exercises.muscleHeat
import com.keshtoim.forge.ui.formatMinutes
import com.keshtoim.forge.ui.relativeDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WorkoutHomeScreen(
    onOpenTemplate: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WorkoutHomeViewModel = viewModel(factory = WorkoutHomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val home = state ?: return
    val defaultName = stringResource(R.string.workout_default_name)
    var sheetFor by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Header(onOpenSettings) }
        item(span = { GridItemSpan(2) }) {
            val active = home.active
            val next = home.next
            when {
                active != null -> Hero(
                    label = stringResource(R.string.home_now),
                    title = active.name,
                    subtitle = rememberElapsed(active.startedAt),
                    heat = home.others.firstOrNull { it.template.id == active.templateId }?.let { muscleHeat(it.exercises) }.orEmpty(),
                    action = stringResource(R.string.workout_resume),
                    onAction = { onOpenWorkout(active.id) },
                )
                next != null -> Hero(
                    label = stringResource(R.string.home_next),
                    title = next.template.name,
                    subtitle = pluralStringResource(R.plurals.exercises_count, next.items.size, next.items.size) + "\n" +
                        pluralStringResource(R.plurals.sets_total, next.items.sumOf { it.targetSets }, next.items.sumOf { it.targetSets }),
                    heat = muscleHeat(next.exercises),
                    action = stringResource(R.string.home_go),
                    onAction = { viewModel.startTemplate(next, onOpenWorkout) },
                )
                else -> Hero(
                    label = stringResource(R.string.home_start_now),
                    title = defaultName,
                    subtitle = stringResource(R.string.templates_empty),
                    heat = emptyMap(),
                    action = stringResource(R.string.home_go),
                    onAction = { viewModel.startEmpty(defaultName, onOpenWorkout) },
                )
            }
        }
        item(span = { GridItemSpan(2) }) { WeekCard(home.week) }
        item(span = { GridItemSpan(2) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.templates).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onOpenTemplate(0) }) {
                    Text("+ " + stringResource(R.string.home_new), style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                }
            }
        }
        items(home.others, key = { it.template.id }) { summary ->
            TemplateTile(summary.template.name, relativeDay(LocalContext.current, summary.lastDoneAt)) { sheetFor = summary.template.id }
        }
        if (home.active == null && (home.next != null || home.others.isNotEmpty())) {
            item(key = "empty") { FreeWorkoutTile { viewModel.startEmpty(defaultName, onOpenWorkout) } }
        }
    }

    val summary = sheetFor?.let { id -> (listOfNotNull(home.next) + home.others).firstOrNull { it.template.id == id } }
    if (summary != null) {
        TemplateSheet(
            summary = summary,
            canStart = home.active == null,
            onDismiss = { sheetFor = null },
            onEdit = { sheetFor = null; onOpenTemplate(summary.template.id) },
            onStart = { sheetFor = null; viewModel.startTemplate(summary, onOpenWorkout) },
        )
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val locale = Locale.getDefault()
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(date, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.home_greeting).uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 2.dp))
        }
        Surface(
            onClick = onOpenSettings,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun Hero(label: String, title: String, subtitle: String, heat: Map<MuscleGroup, Int>, action: String, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = ForgeTheme.panel.panel, contentColor = ForgeTheme.panel.onPanel, modifier = Modifier.fillMaxWidth().height(300.dp)) {
        Box {
            MuscleMap(heat, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = 14.dp).width(196.dp), MuscleMapDefaults.onDark())
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.onSecondary,
                    modifier = Modifier.clip(CircleShape).background(colors.secondary).padding(horizontal = 12.dp, vertical = 6.dp),
                )
                // Long template names shrink so they never run into the body map on the right.
                BasicText(
                    title.uppercase(),
                    style = MaterialTheme.typography.displayLarge.copy(color = ForgeTheme.panel.onPanel),
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = 26.sp, maxFontSize = 64.sp),
                    modifier = Modifier.fillMaxWidth(0.58f).padding(top = 12.dp),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ForgeTheme.panel.onPanel.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.62f).padding(top = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                    Text(action.uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = 0.04.em))
                }
            }
        }
    }
}

@Composable
private fun WeekCard(week: Week) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer, shadowElevation = 1.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.days.forEach { day ->
                        val isToday = day.date == today
                        Box(
                            Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (day.trained) colors.primary else colors.surfaceContainerHigh)
                                .then(if (isToday && !day.trained) Modifier.border(2.dp, colors.onSurface, RoundedCornerShape(10.dp)) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).lowercase(locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    day.trained -> colors.onPrimary
                                    isToday -> colors.onSurface
                                    else -> colors.outline
                                },
                            )
                        }
                    }
                }
                Text(
                    pluralStringResource(R.plurals.workouts_this_week, week.workouts, week.workouts) +
                        if (week.minutes > 0) " · " + formatMinutes(LocalContext.current, week.minutes) else "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                week.workouts.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

@Composable
private fun TemplateTile(name: String, lastDone: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp,
        modifier = Modifier.height(96.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                name.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, lineHeight = 1.05.em),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(lastDone, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FreeWorkoutTile(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.height(96.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.workout_no_template), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSheet(summary: TemplateSummary, canStart: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onStart: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(summary.template.name.uppercase(), style = MaterialTheme.typography.headlineSmall)
                    Text(relativeDay(context, summary.lastDoneAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MuscleMap(muscleHeat(summary.exercises), Modifier.width(96.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                summary.items.forEach { item ->
                    val exercise = summary.exercises.firstOrNull { it.id == item.exerciseId } ?: return@forEach
                    Row {
                        Text(exercise.displayName(context), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("× ${item.targetSets}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.edit)) }
                if (canStart) {
                    Button(onClick = onStart, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.workout_start)) }
                }
            }
        }
    }
}
