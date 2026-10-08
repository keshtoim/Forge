package com.keshtoim.forge.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.ui.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onClose: () -> Unit,
    onAddExercises: () -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = ActiveWorkoutViewModel.Factory),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val workout = detail ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    Column {
                        Text(workout.workout.name)
                        Text(
                            rememberElapsed(workout.workout.startedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (workout.exercises.any { e -> e.sets.any { it.completed } }) viewModel.finish(onClose) else confirmDiscard = true
                    }) { Text(stringResource(R.string.workout_finish)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_discard)) },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(workout.exercises.sortedBy { it.workoutExercise.position }, key = { it.workoutExercise.id }) { item ->
                val exercise = exercises[item.workoutExercise.exerciseId] ?: return@items
                ExerciseCard(
                    title = exercise.displayName(context),
                    type = exercise.type,
                    item = item,
                    previous = viewModel.previous[exercise.id].orEmpty(),
                    selectedSetId = selectedSetId,
                    onSelect = { selectedSetId = it },
                    onChange = viewModel::updateSet,
                    onToggle = viewModel::toggleCompleted,
                    onDelete = viewModel::deleteSet,
                    onAddSet = { viewModel.addSet(item) },
                    onRemove = { viewModel.removeExercise(item) },
                )
            }
            item {
                OutlinedButton(onClick = onAddExercises, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_exercises))
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.workout_discard)) },
            text = { Text(stringResource(R.string.workout_discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.discard(onClose) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ExerciseCard(
    title: String,
    type: ExerciseType,
    item: WorkoutExerciseWithSets,
    previous: List<WorkoutSet>,
    selectedSetId: Long?,
    onSelect: (Long) -> Unit,
    onChange: (WorkoutSet) -> Unit,
    onToggle: (WorkoutSet, WorkoutSet?) -> Unit,
    onDelete: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val fields = SetField.forType(type)

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.exercise_remove)) },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
        item.sets.sortedBy { it.position }.forEachIndexed { index, set ->
            val ghost = previous.getOrNull(index)
            key(set.id) {
                SetRow(
                    number = index + 1,
                    set = set,
                    ghost = ghost,
                    fields = fields,
                    onFocus = { onSelect(set.id) },
                    onChange = onChange,
                    onToggle = { onToggle(set, ghost) },
                    onDelete = { onDelete(set) },
                )
                if (selectedSetId == set.id && !set.completed) {
                    StepperBar(set, ghost, fields, onChange)
                }
            }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.padding(start = 4.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.set_add))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetRow(
    number: Int,
    set: WorkoutSet,
    ghost: WorkoutSet?,
    fields: List<SetField>,
    onFocus: () -> Unit,
    onChange: (WorkoutSet) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onDelete()
            it == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
        },
    ) {
        val background = if (set.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        Row(
            Modifier.fillMaxWidth().background(background).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$number", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
            fields.forEach { field ->
                SetFieldInput(field, set, ghost, onChange, onFocus, Modifier.weight(1f))
            }
            FilledTonalIconToggleButton(checked = set.completed, onCheckedChange = { onToggle() }) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.set_done))
            }
        }
    }
}

@Composable
private fun SetFieldInput(
    field: SetField,
    set: WorkoutSet,
    ghost: WorkoutSet?,
    onChange: (WorkoutSet) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier,
) {
    val stored = field.read(set)
    var text by remember { mutableStateOf(stored) }
    var focused by remember { mutableStateOf(false) }
    // While typing the DB round-trip lags behind the input, so only sync external changes when unfocused.
    LaunchedEffect(stored, focused) { if (!focused) text = stored }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(field.write(set, it))
        },
        modifier = modifier.onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocus()
        },
        placeholder = { Text(ghost?.let(field::read).orEmpty()) },
        suffix = { Text(stringResource(field.unitRes)) },
        singleLine = true,
        enabled = !set.completed,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(keyboardType = field.keyboard, imeAction = ImeAction.Next),
    )
}

@Composable
private fun StepperBar(set: WorkoutSet, ghost: WorkoutSet?, fields: List<SetField>, onChange: (WorkoutSet) -> Unit) {
    val focusManager = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fields.forEach { field ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(onClick = { focusManager.clearFocus(); onChange(field.step(set, ghost, -1)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = null)
                }
                Text(
                    "${formatNumber(field.step)} ${stringResource(field.unitRes)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                OutlinedIconButton(onClick = { focusManager.clearFocus(); onChange(field.step(set, ghost, 1)) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
    }
}
