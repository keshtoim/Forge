package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.builtIn
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.SetPoint
import com.keshtoim.forge.data.guide
import com.keshtoim.forge.data.secondary
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.anatomyRes
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.labelRes
import com.keshtoim.forge.ui.progress.Metric
import com.keshtoim.forge.ui.progress.formatValue
import com.keshtoim.forge.ui.progress.series

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onOpenProgress: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel(factory = ExerciseDetailViewModel.Factory),
) {
    val loaded by viewModel.exercise.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val exercise = loaded ?: return
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }
    val defaultName = stringResource(R.string.workout_default_name)

    Scaffold(
        topBar = {
            ScreenHeader(exercise.displayName(context), onBack = onBack) {
                if (exercise.builtInKey == null) {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.exercise_edit))
                    }
                }
            }
        },
        bottomBar = {
            if (viewModel.canAddToWorkout) {
                Button(
                    onClick = { viewModel.addToWorkout(defaultName, onOpenWorkout) },
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(56.dp),
                ) { Text(stringResource(R.string.exercise_add_to_workout)) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MapCard(exercise) }
            item { MusclesCard(exercise) }
            exercise.builtIn?.guide?.let { guide ->
                item { TechniqueCard(stringArrayResource(guide.steps)) }
                item { MistakesCard(stringArrayResource(guide.mistakes)) }
            }
            item { HistoryCard(exercise, sets, onAllProgress = { onOpenProgress(exercise.id) }) }
        }
    }

    if (editing) {
        // Changing the type would make already logged sets unreadable (e.g. weight×reps shown as time).
        var typeLocked by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(exercise.id) { typeLocked = viewModel.hasHistory() }
        ExerciseDialog(
            initial = exercise,
            typeLocked = typeLocked,
            onDismiss = { editing = false },
            onSave = { viewModel.save(it); editing = false },
            onDelete = { editing = false; viewModel.archive(exercise, onBack) },
        )
    }
}

@Composable
private fun MapCard(exercise: Exercise) {
    val colors = MuscleMapDefaults.colors()
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MuscleMap(exercise.muscleGroup, exercise.secondary, Modifier.width(280.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(colors.levels[2], stringResource(R.string.exercise_legend_primary))
                if (exercise.secondary.isNotEmpty()) LegendDot(colors.levels[1], stringResource(R.string.exercise_legend_secondary))
            }
            Text(
                stringResource(exercise.type.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun MusclesCard(exercise: Exercise) {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            SectionTitle(stringResource(R.string.exercise_primary))
            MuscleRow(exercise.muscleGroup)
            if (exercise.secondary.isNotEmpty()) {
                SectionTitle(stringResource(R.string.exercise_secondary), Modifier.padding(top = 8.dp))
                exercise.secondary.forEach { MuscleRow(it) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp))
}

@Composable
private fun MuscleRow(group: MuscleGroup) {
    val label = stringResource(group.labelRes)
    val anatomy = group.anatomyRes?.let { stringResource(it) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(anatomy ?: label, style = MaterialTheme.typography.bodyMedium)
        if (anatomy != null) Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TechniqueCard(steps: Array<String>) {
    val colors = MaterialTheme.colorScheme
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.exercise_technique), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            steps.forEachIndexed { index, step ->
                Row {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
                    }
                    Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 12.dp, top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun MistakesCard(mistakes: Array<String>) {
    val colors = MaterialTheme.colorScheme
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.exercise_mistakes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            mistakes.forEach { mistake ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(colors.secondary))
                    Text(mistake, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(exercise: Exercise, sets: List<SetPoint>, onAllProgress: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val workouts = sets.distinctBy { it.workoutId }.size
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.exercise_history), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                if (workouts > 0) TextButton(onClick = onAllProgress) { Text(stringResource(R.string.exercise_all_progress)) }
            }
            if (workouts == 0) {
                Text(
                    stringResource(R.string.exercise_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric.forType(exercise.type).take(2).forEach { metric ->
                    metric.series(sets).maxOfOrNull { it.value }?.let { best ->
                        Stat(formatValue(best) + " " + stringResource(metric.unitRes), stringResource(metric.labelRes), Modifier.weight(1f))
                    }
                }
                Stat(workouts.toString(), stringResource(R.string.stats_workouts), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.background).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
