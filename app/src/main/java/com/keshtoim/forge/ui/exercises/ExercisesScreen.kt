package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory)) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_exercises)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = Exercise(name = "", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS)
            }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.exercise_new)) }
        },
    ) { padding ->
        ExerciseCatalog(
            exercises = exercises,
            onClick = { editing = it },
            isClickable = { it.builtInKey == null },
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }

    editing?.let { exercise ->
        // Changing the type would make already logged sets unreadable (e.g. weight×reps shown as time).
        var typeLocked by remember(exercise.id) { mutableStateOf(false) }
        LaunchedEffect(exercise.id) { typeLocked = exercise.id != 0L && viewModel.hasHistory(exercise.id) }
        ExerciseDialog(
            initial = exercise,
            typeLocked = typeLocked,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = { viewModel.archive(exercise); editing = null },
        )
    }
}

@Composable
private fun ExerciseDialog(
    initial: Exercise,
    typeLocked: Boolean,
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
                Dropdown(
                    stringResource(R.string.exercise_type), type, ExerciseType.entries, { it.labelRes },
                    enabled = !typeLocked,
                    hint = if (typeLocked) stringResource(R.string.exercise_type_locked) else null,
                ) { type = it }
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
private fun <T> Dropdown(
    label: String,
    selected: T,
    options: List<T>,
    labelRes: (T) -> Int,
    enabled: Boolean = true,
    hint: String? = null,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = enabled && it }) {
        OutlinedTextField(
            value = stringResource(labelRes(selected)),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            enabled = enabled,
            supportingText = hint?.let { { Text(it) } },
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
