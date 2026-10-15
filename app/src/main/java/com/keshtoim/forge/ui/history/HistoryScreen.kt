package com.keshtoim.forge.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.theme.ForgeTheme
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.formatDateTime
import com.keshtoim.forge.ui.formatMinutes
import com.keshtoim.forge.ui.formatWeight
import com.keshtoim.forge.ui.summary
import com.keshtoim.forge.ui.volumeKg
import com.keshtoim.forge.ui.workout.formatNumber
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

private val WorkoutDetail.minutes get() = ((workout.finishedAt ?: workout.startedAt) - workout.startedAt) / 60_000
private val WorkoutDetail.doneSets get() = exercises.sumOf { e -> e.sets.count { it.completed } }

@Composable
fun HistoryScreen(onOpen: (Long) -> Unit, viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val months = remember(workouts) { workouts.groupBy { YearMonth.from(Instant.ofEpochMilli(it.workout.startedAt).atZone(zone)) } }

    Scaffold(topBar = { ScreenHeader(stringResource(R.string.tab_history)) }) { padding ->
        if (workouts.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            months.forEach { (month, list) ->
                item(key = month.toString()) { MonthHeader(month, list.size) }
                items(list, key = { it.workout.id }) { detail -> WorkoutRow(detail) { onOpen(detail.workout.id) } }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, count: Int) {
    val locale = Locale.getDefault()
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) +
        if (month.year != LocalDate.now().year) " ${month.year}" else ""
    Row(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(name.uppercase(locale), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.plural_workouts, count, count),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WorkoutRow(detail: WorkoutDetail, onClick: () -> Unit) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val date = Instant.ofEpochMilli(detail.workout.startedAt).atZone(ZoneId.systemDefault())
    ForgeCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).lowercase(locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(detail.workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        formatMinutes(context, detail.minutes),
                        pluralStringResource(R.plurals.plural_sets, detail.doneSets, detail.doneSets),
                        detail.volumeKg.takeIf { it > 0 }?.let { formatWeight(context, it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    pluralStringResource(R.plurals.plural_exercises, detail.exercises.size, detail.exercises.size) +
                        " · " + DateTimeFormatter.ofPattern("HH:mm", locale).format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(onBack: () -> Unit, viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.Factory)) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val workout = detail ?: return

    Scaffold(
        topBar = {
            ScreenHeader(workout.workout.name, onBack = onBack) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.workout_delete))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(formatDateTime(workout.workout.startedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { StatsPanel(workout) }
            items(workout.exercises.sortedBy { it.workoutExercise.position }, key = { it.workoutExercise.id }) { item ->
                val exercise = exercises[item.workoutExercise.exerciseId] ?: return@items
                ForgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(exercise.displayName(context), style = MaterialTheme.typography.titleMedium)
                        item.sets.sortedBy { it.position }.forEachIndexed { index, set ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(set.summary(exercise.type, context), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.workout_delete)) },
            text = { Text(stringResource(R.string.workout_discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StatsPanel(detail: WorkoutDetail) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    // Only numbers go in the display face; units move to the captions so they stay legible.
    val tonnes = detail.volumeKg >= 1000
    val stats = listOf(
        "%d:%02d".format(detail.minutes / 60, detail.minutes % 60) to stringResource(R.string.stat_time),
        detail.doneSets.toString() to stringResource(R.string.stat_sets),
        formatNumber((if (tonnes) detail.volumeKg / 100 else detail.volumeKg * 10).roundToLong() / 10.0) to
            stringResource(R.string.stat_volume, stringResource(if (tonnes) R.string.unit_t else R.string.unit_kg)),
    )
    Surface(shape = MaterialTheme.shapes.large, color = ForgeTheme.panel.panel, contentColor = ForgeTheme.panel.onPanel, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
            stats.forEach { (value, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = ForgeTheme.panel.onPanel.copy(alpha = 0.65f))
                }
            }
        }
    }
}
