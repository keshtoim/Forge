package com.keshtoim.forge.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.rest.Rest
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.summary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onClose: () -> Unit,
    onFinished: () -> Unit,
    onAddExercises: () -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = ActiveWorkoutViewModel.Factory),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val workout = detail ?: return
    val ordered = workout.exercises.ordered()
    val focus = ordered.firstOrNull { it.workoutExercise.id == focusedId } ?: workout.exercises.defaultFocus()
    val finish = { if (workout.exercises.any { e -> e.sets.any { it.completed } }) viewModel.finish(onFinished) else confirmDiscard = true }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.back))
            }
            Text(rememberElapsed(workout.workout.startedAt), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = finish,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.height(44.dp),
            ) {
                Text(stringResource(R.string.workout_finish_short), style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        rest?.let { RestPill(it, onAdd = { viewModel.adjustRest(15) }, onSkip = viewModel::skipRest) }

        if (focus == null) {
            EmptyWorkout(onAddExercises, Modifier.weight(1f))
            return@Column
        }
        val exercise = exercises[focus.workoutExercise.exerciseId] ?: return@Column
        val previous = viewModel.previous[exercise.id].orEmpty()
        val sets = focus.sets.sortedBy { it.position }
        val current = focus.currentSet()
        val currentIndex = sets.indexOf(current)
        val next = workout.exercises.nextUnfinished(focus.workoutExercise.id)
        val nextName = next?.let { exercises[it.workoutExercise.exerciseId]?.displayName(context) }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 16.dp),
        ) {
            ExerciseProgress(ordered, focus.workoutExercise.id) { listOpen = true }
            BasicText(
                exercise.displayName(context),
                style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 40.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
            SetChips(sets, current, exercise, onEdit = { viewModel.toggleCompleted(it.id, null) })
            if (current != null) {
                SetCard(
                    number = currentIndex + 1,
                    set = current,
                    ghost = previous.getOrNull(currentIndex),
                    exercise = exercise,
                    onEdit = { transform -> viewModel.editSet(current.id, transform) },
                    onBarChange = { viewModel.setBar(exercise, it) },
                )
            } else {
                Text(
                    stringResource(R.string.all_sets_done),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)) {
            val ctaLabel = when {
                current != null -> stringResource(R.string.set_done_cta)
                nextName != null -> stringResource(R.string.next_exercise, nextName)
                else -> stringResource(R.string.workout_finish_short)
            }
            Button(
                onClick = {
                    when {
                        current != null -> {
                            viewModel.toggleCompleted(current.id, previous.getOrNull(currentIndex))
                            // Finishing the last set moves on to the next exercise that still has sets left.
                            if (sets.count { !it.completed } == 1) next?.let { focusedId = it.workoutExercise.id }
                        }
                        next != null -> focusedId = next.workoutExercise.id
                        else -> finish()
                    }
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(
                    ctaLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, letterSpacing = 0.04.em),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.addSet(focus.workoutExercise.id) }) {
                    Text(stringResource(R.string.set_add_short), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (current != null && nextName != null) {
                    TextButton(onClick = { focusedId = next.workoutExercise.id }) {
                        Text(stringResource(R.string.next_exercise, nextName.lowercase()) + " →", color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (listOpen) {
        ModalBottomSheet(onDismissRequest = { listOpen = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            ExerciseList(
                exercises = ordered,
                names = { id -> exercises[id]?.displayName(context).orEmpty() },
                focusedId = focus?.workoutExercise?.id,
                onSelect = { focusedId = it; listOpen = false },
                onAddSet = viewModel::addSet,
                onRemoveSet = { e -> e.sets.filter { !it.completed }.maxByOrNull { it.position }?.let(viewModel::deleteSet) },
                onRemove = viewModel::removeExercise,
                onAddExercises = { listOpen = false; onAddExercises() },
                onDiscard = { listOpen = false; confirmDiscard = true },
            )
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
private fun RestPill(rest: Rest, onAdd: () -> Unit, onSkip: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val now = rememberNow()
    val left = ((rest.endsAt - now).toFloat() / (rest.totalSec * 1000)).coerceIn(0f, 1f)
    Box(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(colors.tertiaryContainer),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(left).background(lerp(colors.tertiaryContainer, colors.tertiary, 0.12f)))
        Row(Modifier.fillMaxSize().padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rest_title), style = MaterialTheme.typography.titleSmall, color = colors.onTertiaryContainer)
            Text(
                rememberRemaining(rest.endsAt),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = colors.onTertiaryContainer,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Surface(onClick = onAdd, shape = CircleShape, color = colors.surfaceContainer, modifier = Modifier.height(44.dp)) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.rest_add), style = MaterialTheme.typography.labelLarge, color = colors.onTertiaryContainer)
                }
            }
            Surface(onClick = onSkip, shape = CircleShape, color = colors.tertiary, modifier = Modifier.padding(start = 6.dp).height(44.dp)) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.rest_skip), style = MaterialTheme.typography.labelLarge, color = colors.onTertiary)
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgress(exercises: List<WorkoutExerciseWithSets>, focusedId: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val index = exercises.indexOfFirst { it.workoutExercise.id == focusedId }
    Row(
        Modifier.clip(CircleShape).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        exercises.forEachIndexed { i, e ->
            val color = when {
                i == index -> colors.onSurface
                e.currentSet() == null && e.sets.isNotEmpty() -> colors.primary
                else -> colors.surfaceVariant
            }
            Box(Modifier.size(width = if (i == index) 26.dp else 10.dp, height = 10.dp).clip(CircleShape).background(color))
        }
        Text(
            stringResource(R.string.exercise_position, index + 1, exercises.size),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun SetChips(sets: List<WorkoutSet>, current: WorkoutSet?, exercise: Exercise, onEdit: (WorkoutSet) -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val done = sets.withIndex().filter { it.value.completed }
    val later = sets.count { !it.completed && it != current }
    if (done.isEmpty() && later == 0) return
    Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        done.forEach { (index, set) ->
            val summary = set.summary(exercise.type, context)
            val hint = stringResource(R.string.set_chip_hint, index + 1, summary)
            Surface(
                onClick = { onEdit(set) },
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier.height(36.dp).semantics { contentDescription = hint },
            ) {
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("✓ $summary", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                }
            }
        }
        if (later > 0) {
            Surface(shape = CircleShape, color = colors.background, border = BorderStroke(2.dp, colors.surfaceVariant), modifier = Modifier.height(36.dp)) {
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(pluralStringResource(R.plurals.sets_left, later, later), style = MaterialTheme.typography.labelLarge, color = colors.outline)
                }
            }
        }
    }
}

@Composable
private fun SetCard(
    number: Int,
    set: WorkoutSet,
    ghost: WorkoutSet?,
    exercise: Exercise,
    onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit,
    onBarChange: (Double?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.set_label, number),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 10.dp, vertical = 5.dp),
                )
                Spacer(Modifier.weight(1f))
                ghost?.let {
                    Text(
                        stringResource(R.string.last_time, it.summary(exercise.type, context)),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            val plates = exercise.barKg?.let(::PlateLoading)
            SetField.forType(exercise.type).forEach { field ->
                key(set.id, field) { BigField(field, set, ghost, plates.takeIf { field == SetField.WEIGHT }, onEdit) }
            }
            if (exercise.type == ExerciseType.WEIGHT_REPS) BarSelector(exercise.barKg, onBarChange)
        }
    }
}

@Composable
private fun BarSelector(barKg: Double?, onSelect: (Double?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = { open = true }) {
            Text(barLabel(barKg))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf(null) + PlateLoading.BAR_WEIGHTS).forEach { option ->
                DropdownMenuItem(
                    text = { Text(barLabel(option)) },
                    onClick = { open = false; onSelect(option) },
                    trailingIcon = { if (option == barKg) Icon(Icons.Filled.Check, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun barLabel(barKg: Double?) =
    if (barKg == null) stringResource(R.string.bar_none) else stringResource(R.string.bar_weight, formatNumber(barKg))

@Composable
private fun BigField(field: SetField, set: WorkoutSet, ghost: WorkoutSet?, plates: PlateLoading?, onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // With plates the field shows one side while the stored set and every edit keep the total.
    val view = { s: WorkoutSet -> plates?.toSide(s) ?: s }
    val back = { s: WorkoutSet -> plates?.toTotal(s) ?: s }
    val shownGhost = ghost?.let(view)
    val stored = field.read(view(set))
    var text by remember { mutableStateOf(stored) }
    var focused by remember { mutableStateOf(false) }
    // While typing the DB round-trip lags behind the input, so only sync external changes when unfocused.
    LaunchedEffect(stored, focused) { if (!focused) text = stored }
    val focusManager = LocalFocusManager.current
    val unit = stringResource(if (plates != null) R.string.unit_kg_per_side else field.unitRes)
    val style = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton(Icons.Filled.Remove, "−${formatNumber(field.step)} $unit") {
            focusManager.clearFocus()
            onEdit { back(field.step(view(it), shownGhost, -1)) }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    text = input
                    onEdit { back(field.write(view(it), input)) }
                },
                textStyle = style.copy(color = colors.onSurface),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = field.keyboard, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = unit },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) Text(shownGhost?.let(field::read).orEmpty(), style = style, color = lerp(colors.surfaceVariant, colors.outline, 0.6f))
                        inner()
                    }
                },
            )
            Text(unit, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colors.onSurfaceVariant)
            if (plates != null) {
                (set.weightKg ?: ghost?.weightKg)?.let { total ->
                    Text(
                        stringResource(R.string.plates_total, formatNumber(total)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        StepButton(Icons.Filled.Add, "+${formatNumber(field.step)} $unit") {
            focusManager.clearFocus()
            onEdit { back(field.step(view(it), shownGhost, 1)) }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description) }
    }
}

@Composable
private fun EmptyWorkout(onAddExercises: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onAddExercises, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text(stringResource(R.string.add_exercises), style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp))
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<WorkoutExerciseWithSets>,
    names: (Long) -> String,
    focusedId: Long?,
    onSelect: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemoveSet: (WorkoutExerciseWithSets) -> Unit,
    onRemove: (WorkoutExerciseWithSets) -> Unit,
    onAddExercises: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            stringResource(R.string.workout_exercises),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        exercises.forEach { e ->
            val selected = e.workoutExercise.id == focusedId
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(e.workoutExercise.id) }
                    .background(if (selected) colors.primaryContainer else colors.surfaceContainer)
                    .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(names(e.workoutExercise.exerciseId), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.sets_progress, e.sets.count { it.completed }, e.sets.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onRemoveSet(e) }, enabled = e.sets.any { !it.completed }) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.set_remove))
                }
                IconButton(onClick = { onAddSet(e.workoutExercise.id) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.set_add))
                }
                IconButton(onClick = { onRemove(e) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.exercise_remove), tint = colors.error)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = colors.outlineVariant)
        TextButton(onClick = onAddExercises, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.add_exercises), modifier = Modifier.padding(start = 8.dp))
        }
        TextButton(onClick = onDiscard, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.workout_discard), color = colors.error)
        }
    }
}
