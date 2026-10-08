package com.keshtoim.forge.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.formatDate
import com.keshtoim.forge.ui.workout.formatNumber
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onOpen: (Long) -> Unit, viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_progress)) }) }) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.progress_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(items, key = { it.first.id }) { (exercise, history) ->
                ListItem(
                    headlineContent = { Text(exercise.displayName(context)) },
                    supportingContent = {
                        Text(
                            pluralStringResource(R.plurals.plural_workouts, history.sessions, history.sessions) +
                                " · " + formatDate(history.lastAt)
                        )
                    },
                    modifier = Modifier.clickable { onOpen(exercise.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(onBack: () -> Unit, viewModel: ExerciseProgressViewModel = viewModel(factory = ExerciseProgressViewModel.Factory)) {
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val current = exercise ?: return
    val metrics = Metric.forType(current.type)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val metric = metrics[selected.coerceIn(metrics.indices)]
    val series = remember(sets, metric) { metric.series(sets) }
    val unit = stringResource(metric.unitRes)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.displayName(context)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (metrics.size > 1) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    metrics.forEachIndexed { index, m ->
                        SegmentedButton(
                            selected = index == selected,
                            onClick = { selected = index },
                            shape = SegmentedButtonDefaults.itemShape(index, metrics.size),
                        ) { Text(stringResource(m.labelRes), maxLines = 1) }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val last = series.lastOrNull()
                    Text(stringResource(metric.labelRes), style = MaterialTheme.typography.labelLarge)
                    Text(
                        last?.let { "${formatNumber(it.value)} $unit" } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (series.size > 1) {
                        val delta = series.last().value - series.first().value
                        Text(
                            stringResource(R.string.progress_delta, (if (delta >= 0) "+" else "") + formatNumber(delta) + " " + unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (series.isNotEmpty()) {
                        Text(formatNumber(series.maxOf { it.value }), style = MaterialTheme.typography.labelSmall)
                    }
                    LineChart(series, Modifier.fillMaxWidth().height(180.dp))
                    if (series.isNotEmpty()) {
                        Text(formatNumber(series.minOf { it.value }), style = MaterialTheme.typography.labelSmall)
                        Row(Modifier.fillMaxWidth()) {
                            Text(formatDate(series.first().time, FormatStyle.SHORT), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text(formatDate(series.last().time, FormatStyle.SHORT), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Text(stringResource(R.string.records), style = MaterialTheme.typography.titleMedium)
            metrics.forEach { m ->
                val best = m.series(sets).maxByOrNull { it.value } ?: return@forEach
                ListItem(
                    headlineContent = { Text(stringResource(m.labelRes)) },
                    supportingContent = { Text(formatDate(best.time)) },
                    trailingContent = {
                        Text("${formatNumber(best.value)} ${stringResource(m.unitRes)}", style = MaterialTheme.typography.titleMedium)
                    },
                )
            }
        }
    }
}
