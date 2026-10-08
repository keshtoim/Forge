package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory)) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var editing by remember { mutableStateOf<Exercise?>(null) }

    val grouped = remember(exercises, query, group, context) {
        exercises
            .map { it to it.displayName(context) }
            .filter { (e, name) -> (group == null || e.muscleGroup == group) && name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareBy({ it.first.muscleGroup.ordinal }, { it.second.lowercase() }))
            .groupBy { it.first.muscleGroup }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Exercise(name = "", muscleGroup = group ?: MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS)
            }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.exercise_new)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.exercises_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) }
                    }
                },
                singleLine = true,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = group == null, onClick = { group = null }, label = { Text(stringResource(R.string.filter_all)) })
                }
                items(MuscleGroup.entries) { g ->
                    FilterChip(
                        selected = group == g,
                        onClick = { group = if (group == g) null else g },
                        label = { Text(stringResource(g.labelRes)) },
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                grouped.forEach { (muscle, items) ->
                    stickyHeader(key = muscle) {
                        Text(
                            stringResource(muscle.labelRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(items, key = { it.first.id }) { (exercise, name) ->
                        val custom = exercise.builtInKey == null
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(stringResource(exercise.type.labelRes)) },
                            modifier = if (custom) Modifier.clickable { editing = exercise } else Modifier,
                        )
                    }
                }
            }
        }
    }

    editing?.let { exercise ->
        ExerciseDialog(
            initial = exercise,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = { viewModel.archive(exercise); editing = null },
        )
    }
}

@Composable
private fun ExerciseDialog(
    initial: Exercise,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial.name.orEmpty()) }
    var muscle by rememberSaveable { mutableStateOf(initial.muscleGroup) }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.exercise_new else R.string.exercise_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Dropdown(stringResource(R.string.muscle_group), muscle, MuscleGroup.entries, { it.labelRes }) { muscle = it }
                Dropdown(stringResource(R.string.exercise_type), type, ExerciseType.entries, { it.labelRes }) { type = it }
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(initial.copy(name = name.trim(), muscleGroup = muscle, type = type)) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(label: String, selected: T, options: List<T>, labelRes: (T) -> Int, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(labelRes(selected)),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes(option))) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
