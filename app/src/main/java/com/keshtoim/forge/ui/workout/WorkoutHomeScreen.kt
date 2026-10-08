package com.keshtoim.forge.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHomeScreen(
    onOpenTemplate: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: WorkoutHomeViewModel = viewModel(factory = WorkoutHomeViewModel.Factory),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultName = stringResource(R.string.workout_default_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_workout)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
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
                val workout = active
                if (workout != null) {
                    Card(
                        onClick = { onOpenWorkout(workout.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(workout.name, style = MaterialTheme.typography.titleMedium)
                                Text(rememberElapsed(workout.startedAt), style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(onClick = { onOpenWorkout(workout.id) }) { Text(stringResource(R.string.workout_resume)) }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.startEmpty(defaultName, onOpenWorkout) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(stringResource(R.string.workout_start_empty)) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.templates), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpenTemplate(0) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.template_new))
                    }
                }
            }
            if (templates.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.templates_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(templates, key = { it.template.id }) { summary ->
                Card(onClick = { onOpenTemplate(summary.template.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                summary.exercises.joinToString { it.displayName(context) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (active == null) {
                            FilledTonalButton(
                                onClick = { viewModel.startTemplate(summary, onOpenWorkout) },
                                modifier = Modifier.padding(start = 8.dp),
                            ) { Text(stringResource(R.string.workout_start)) }
                        }
                    }
                }
            }
        }
    }
}
