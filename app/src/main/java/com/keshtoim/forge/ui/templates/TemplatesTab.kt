package com.keshtoim.forge.ui.templates

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.relativeDay
import com.keshtoim.forge.ui.workout.TemplateSummary
import com.keshtoim.forge.ui.workout.lastDoneByTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplatesState(val templates: List<TemplateSummary>, val canStart: Boolean)

class TemplatesViewModel(templateDao: TemplateDao, exerciseDao: ExerciseDao, private val workoutDao: WorkoutDao) : ViewModel() {
    val state = combine(
        templateDao.observeAll(),
        exerciseDao.observeAll(),
        workoutDao.observeFinishedWorkouts(),
        workoutDao.observeActive(),
    ) { templates, exercises, finished, active ->
        val byId = exercises.associateBy { it.id }
        val lastDone = lastDoneByTemplate(finished)
        val summaries = templates.map { t ->
            val items = t.items.sortedBy { it.position }
            TemplateSummary(t.template, items, items.mapNotNull { byId[it.exerciseId] }, lastDone[t.template.id])
        }
        TemplatesState(summaries.sortedBy { it.template.name.lowercase() }, canStart = active == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var starting = false

    fun start(summary: TemplateSummary, onStarted: (Long) -> Unit) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            val items = summary.items.map { it.exerciseId to it.targetSets }
            val id = workoutDao.startOrResume(summary.template.name, summary.template.id, items, System.currentTimeMillis())
            starting = false
            onStarted(id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                TemplatesViewModel(db.templateDao(), db.exerciseDao(), db.workoutDao())
            }
        }
    }
}

@Composable
fun TemplatesTab(
    onOpenTemplate: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = viewModel(factory = TemplatesViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    val context = LocalContext.current

    Box(modifier.fillMaxSize()) {
        if (current.templates.isEmpty()) {
            Text(
                stringResource(R.string.templates_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(current.templates, key = { it.template.id }) { summary ->
                ForgeCard(Modifier.fillMaxWidth(), onClick = { onOpenTemplate(summary.template.id) }) {
                    Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                summary.exercises.joinToString { it.displayName(context) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                pluralStringResource(R.plurals.exercises_count, summary.items.size, summary.items.size) +
                                    " · " + relativeDay(context, summary.lastDoneAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        if (current.canStart) {
                            FilledTonalButton(onClick = { viewModel.start(summary, onOpenWorkout) }, modifier = Modifier.padding(start = 8.dp)) {
                                Text(stringResource(R.string.workout_start))
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onOpenTemplate(0) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.template_new)) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}
