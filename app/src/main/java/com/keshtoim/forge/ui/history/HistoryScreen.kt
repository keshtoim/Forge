package com.keshtoim.forge.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.formatDateTime
import com.keshtoim.forge.ui.summary
import com.keshtoim.forge.ui.volumeKg
import com.keshtoim.forge.ui.workout.formatDuration
import com.keshtoim.forge.ui.workout.formatNumber

@Composable
private fun WorkoutStats(detail: WorkoutDetail) {
    val sets = detail.exercises.sumOf { e -> e.sets.count { it.completed } }
    val duration = ((detail.workout.finishedAt ?: detail.workout.startedAt) - detail.workout.startedAt) / 1000
    val parts = listOfNotNull(
        formatDuration(duration),
        pluralStringResource(R.plurals.plural_exercises, detail.exercises.size, detail.exercises.size),
        pluralStringResource(R.plurals.plural_sets, sets, sets),
        detail.volumeKg.takeIf { it > 0 }?.let { "${formatNumber(it)} ${stringResource(R.string.unit_kg)}" },
    )
    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpen: (Long) -> Unit, viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_history)) }) }) { padding ->
        if (workouts.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(workouts, key = { it.workout.id }) { detail ->
                Card(onClick = { onOpen(detail.workout.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(formatDateTime(detail.workout.startedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(detail.workout.name, style = MaterialTheme.typography.titleMedium)
                        WorkoutStats(detail)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(onBack: () -> Unit, viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.Factory)) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val workout = detail ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.workout.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(formatDateTime(workout.workout.startedAt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                WorkoutStats(workout)
            }
            items(workout.exercises.sortedBy { it.workoutExercise.position }, key = { it.workoutExercise.id }) { item ->
                val exercise = exercises[item.workoutExercise.exerciseId] ?: return@items
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(exercise.displayName(context), style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        item.sets.sortedBy { it.position }.forEachIndexed { index, set ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
                                Text(set.summary(exercise.type, context))
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
