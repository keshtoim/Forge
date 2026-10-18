package com.keshtoim.forge.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.SetEffort
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.rest.Rest
import com.keshtoim.forge.ui.AiIndicator
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.hintRes
import com.keshtoim.forge.ui.labelRes
import com.keshtoim.forge.ui.shortRes
import com.keshtoim.forge.ui.summary
import com.keshtoim.forge.ui.templates.supersetLetter
import com.keshtoim.forge.ui.theme.ForgeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onClose: () -> Unit,
    onFinished: () -> Unit,
    onAddExercises: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = ActiveWorkoutViewModel.Factory),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(aiStatus) { viewModel.verifyAi() }

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
            AiIndicator(aiStatus, Modifier.padding(end = 10.dp))
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
        val group = ordered.supersetOf(focus)
        val round = group.currentRound().takeIf { group.size > 1 }
        val next = workout.exercises.nextUnfinished(focus.workoutExercise.id)
        val nextName = next?.let { exercises[it.workoutExercise.exerciseId]?.displayName(context) }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 16.dp),
        ) {
            ExerciseProgress(ordered, focus.workoutExercise.id) { listOpen = true }
            if (group.size > 1) {
                SupersetRound(
                    group = group,
                    round = round,
                    exercises = exercises,
                    previous = viewModel.previous,
                    onReopen = { viewModel.toggleCompleted(it.id, null) },
                    onEdit = viewModel::editSet,
                    onBarChange = viewModel::setBar,
                    onAddRound = { viewModel.addSets(group.map { it.workoutExercise.id }) },
                    onOpenExercise = onOpenExercise,
                )
            } else {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        exercise.displayName(context),
                        style = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 2,
                        autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 40.sp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onOpenExercise(exercise.id) }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.exercise_info), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SetList(
                    sets = sets,
                    current = current,
                    previous = previous,
                    exercise = exercise,
                    onReopen = { viewModel.toggleCompleted(it.id, null) },
                    onEdit = { transform -> current?.let { viewModel.editSet(it.id, transform) } },
                    onBarChange = { viewModel.setBar(exercise, it) },
                    onAdd = { viewModel.addSet(focus.workoutExercise.id) },
                )
                if (current == null) {
                    Text(
                        stringResource(R.string.all_sets_done),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)) {
            val pending = if (group.size > 1) round != null else current != null
            val ctaLabel = when {
                round != null -> stringResource(R.string.round_done_cta, round + 1)
                pending -> stringResource(R.string.set_done_cta)
                nextName != null -> stringResource(R.string.next_exercise, nextName)
                else -> stringResource(R.string.workout_finish_short)
            }
            Button(
                onClick = {
                    when {
                        round != null -> {
                            val roundSets = group.mapNotNull { e ->
                                e.sets.sortedBy { it.position }.getOrNull(round)?.takeIf { !it.completed }
                                    ?.let { it.id to viewModel.previous[e.workoutExercise.exerciseId]?.getOrNull(round) }
                            }
                            viewModel.completeRound(roundSets)
                            if (group.sumOf { e -> e.sets.count { !it.completed } } == roundSets.size) next?.let { focusedId = it.workoutExercise.id }
                        }
                        current != null && group.size == 1 -> {
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
            if (pending && nextName != null) {
                TextButton(onClick = { focusedId = next.workoutExercise.id }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
                    Text(stringResource(R.string.next_exercise, nextName.lowercase()), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
fun SetEffort?.color(): Color {
    val palette = ForgeTheme.effort
    return when (this) {
        SetEffort.WARMUP -> palette.warmup
        SetEffort.LIGHT -> palette.light
        SetEffort.MEDIUM -> palette.medium
        SetEffort.HEAVY -> palette.heavy
        SetEffort.MAX -> palette.max
        null -> palette.none
    }
}

private val NUMBER_WIDTH = 36.dp
private val LEVEL_WIDTH = 52.dp

// Sets run top to bottom: done rows, the current set as a full card, then the ones still ahead.
@Composable
private fun SetList(
    sets: List<WorkoutSet>,
    current: WorkoutSet?,
    previous: List<WorkoutSet>,
    exercise: Exercise,
    onReopen: (WorkoutSet) -> Unit,
    onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit,
    onBarChange: (Double?) -> Unit,
    onAdd: () -> Unit,
) {
    val fields = SetField.forType(exercise.type)
    // Rows present when the exercise is opened appear at once; only sets added afterwards animate in.
    val initial = remember(exercise.id) { sets.map { it.id }.toSet() }
    var explaining by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(top = 12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SetHeader(fields)
        sets.forEachIndexed { index, set ->
            key(set.id) {
                AppearOnce(animate = set.id !in initial) {
                    val ghost = previous.getOrNull(index)
                    when {
                        set.completed -> DoneRow(index + 1, set, ghost, fields) { onReopen(set) }
                        set == current -> SetCard(index + 1, set, ghost, exercise, onEdit, onBarChange, onExplain = { explaining = true })
                        else -> UpcomingRow(index + 1, set, ghost, fields)
                    }
                }
            }
        }
        Surface(onClick = onAdd, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.set_add_short), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
    if (explaining) EffortGuide(onDismiss = { explaining = false })
}


// Every exercise of a superset with its set for the current round; the round is logged with one tap.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupersetRound(
    group: List<WorkoutExerciseWithSets>,
    round: Int?,
    exercises: Map<Long, Exercise>,
    previous: Map<Long, List<WorkoutSet>>,
    onReopen: (WorkoutSet) -> Unit,
    onEdit: (Long, (WorkoutSet) -> WorkoutSet?) -> Unit,
    onBarChange: (Exercise, Double?) -> Unit,
    onAddRound: () -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var explaining by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.padding(top = 8.dp).clip(CircleShape).background(colors.primaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.superset),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onPrimaryContainer,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
    Text(
        if (round != null) stringResource(R.string.round_progress, round + 1, group.maxOf { it.sets.size }) else stringResource(R.string.all_sets_done),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Column(Modifier.padding(top = 12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        group.forEachIndexed { i, e ->
            val exercise = exercises[e.workoutExercise.exerciseId] ?: return@forEachIndexed
            val sets = e.sets.sortedBy { it.position }
            val set = round?.let { sets.getOrNull(it) }?.takeIf { !it.completed }
            key(e.workoutExercise.id) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                            Text(supersetLetter(i), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colors.onPrimary)
                        }
                        Text(exercise.displayName(context), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        IconButton(onClick = { onOpenExercise(exercise.id) }) {
                            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.exercise_info), tint = colors.onSurfaceVariant)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sets.forEachIndexed { n, done ->
                            if (done.completed) DoneChip(n + 1, done, exercise) { onReopen(done) }
                        }
                    }
                    if (set != null && round != null) {
                        SetCard(
                            number = round + 1,
                            set = set,
                            ghost = previous[exercise.id]?.getOrNull(round),
                            exercise = exercise,
                            onEdit = { transform -> onEdit(set.id, transform) },
                            onBarChange = { onBarChange(exercise, it) },
                            onExplain = { explaining = true },
                            compact = true,
                        )
                    }
                }
            }
        }
        Surface(onClick = onAddRound, shape = RoundedCornerShape(14.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.round_add_one), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
    if (explaining) EffortGuide(onDismiss = { explaining = false })
}

@Composable
private fun DoneChip(number: Int, set: WorkoutSet, exercise: Exercise, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(set.effort.color()))
            Text(
                "$number: ${set.summary(exercise.type, context)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AppearOnce(animate: Boolean, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(!animate).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(220)) + expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it / 2 },
    ) { content() }
}

@Composable
private fun SetHeader(fields: List<SetField>) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.Bottom) {
        Text("№", style = style, color = color, modifier = Modifier.width(NUMBER_WIDTH))
        fields.forEach { Text(stringResource(it.unitRes), style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        Spacer(Modifier.width(LEVEL_WIDTH))
    }
}

// A row with the effort stripe on its left edge, shared by done and upcoming sets.
@Composable
private fun SetRow(number: Int, stripe: Color, modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(stripe))
        Text(
            "$number",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp).width(NUMBER_WIDTH),
        )
        content()
    }
}

@Composable
private fun DoneRow(number: Int, set: WorkoutSet, ghost: WorkoutSet?, fields: List<SetField>, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val effort = set.effort
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        SetRow(number, effort.color(), Modifier.heightIn(min = 56.dp).padding(end = 4.dp)) {
            fields.forEach { field ->
                Column(Modifier.weight(1f).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(field.read(set).ifEmpty { "—" }, style = MaterialTheme.typography.titleLarge)
                        delta(field, set, ghost)?.let { (text, color) ->
                            Text(text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color, modifier = Modifier.padding(start = 2.dp))
                        }
                    }
                    ghost?.let(field::read)?.takeIf { it.isNotEmpty() }?.let {
                        Text(stringResource(R.string.set_was, it), style = MaterialTheme.typography.labelSmall, color = colors.outline)
                    }
                }
            }
            Text(
                effort?.let { stringResource(it.shortRes).replaceFirstChar { c -> c.titlecase() } }.orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = effort.color(),
                textAlign = TextAlign.End,
                modifier = Modifier.width(LEVEL_WIDTH),
            )
        }
    }
}

// Change against the same set last time; green when it grew, red when it dropped.
@Composable
private fun delta(field: SetField, set: WorkoutSet, ghost: WorkoutSet?): Pair<String, Color>? {
    val now = field.value(set) ?: return null
    val before = ghost?.let(field::value) ?: return null
    val diff = now - before
    val colors = ForgeTheme.effort
    return when {
        diff > 0.001 -> "+${formatNumber(diff)}" to colors.light
        diff < -0.001 -> "−${formatNumber(-diff)}" to colors.max
        else -> "±0" to MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun UpcomingRow(number: Int, set: WorkoutSet, ghost: WorkoutSet?, fields: List<SetField>) {
    val colors = MaterialTheme.colorScheme
    val dash = colors.surfaceVariant
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    dash,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clip(RoundedCornerShape(14.dp)),
    ) {
        SetRow(number, Color.Transparent, Modifier.height(48.dp).padding(end = 4.dp)) {
            fields.forEach { field ->
                Text(
                    field.read(set).ifEmpty { ghost?.let(field::read).orEmpty() }.ifEmpty { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.width(LEVEL_WIDTH))
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
    onExplain: () -> Unit,
    // Superset rounds stack several cards, so the fields share a row and rarer controls stay out.
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val stripe by animateColorAsState(set.effort.color(), label = "effort")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
        border = BorderStroke(2.dp, if (set.effort == null) Color.Transparent else stripe),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(stripe))
            Column(Modifier.padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    if (compact) {
                        IconButton(onClick = onExplain, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = stringResource(R.string.effort_explain), tint = colors.onSurfaceVariant)
                        }
                    }
                }
                val plates = exercise.barKg?.let(::PlateLoading)
                val fields = SetField.forType(exercise.type)
                if (compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fields.forEach { field ->
                            key(set.id, field) { BigField(field, set, ghost, plates.takeIf { field == SetField.WEIGHT }, onEdit, Modifier.weight(1f), compact = true) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        fields.forEach { field ->
                            key(set.id, field) { BigField(field, set, ghost, plates.takeIf { field == SetField.WEIGHT }, onEdit, Modifier.fillMaxWidth()) }
                        }
                    }
                }
                EffortPicker(set.effort, onExplain.takeUnless { compact }) { effort -> onEdit { it.copy(effort = effort) } }
                if (exercise.type == ExerciseType.WEIGHT_REPS && !compact) BarSelector(exercise.barKg, onBarChange)
            }
        }
    }
}

// Tapping the selected level again clears it: most sets need no label.
@Composable
// Without [onExplain] the title row is left out; the caller offers the explanation elsewhere.
private fun EffortPicker(effort: SetEffort?, onExplain: (() -> Unit)?, onSelect: (SetEffort?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        if (onExplain != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.effort_title), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onExplain) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = stringResource(R.string.effort_explain), tint = colors.onSurfaceVariant)
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SetEffort.entries.forEachIndexed { index, level ->
                val label = stringResource(level.labelRes)
                val tint = level.color()
                SegmentedButton(
                    selected = level == effort,
                    onClick = { onSelect(if (level == effort) null else level) },
                    shape = SegmentedButtonDefaults.itemShape(index, SetEffort.entries.size),
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = tint.copy(alpha = 0.22f), activeContentColor = colors.onSurface, activeBorderColor = tint),
                    icon = {},
                    modifier = Modifier.semantics { contentDescription = label },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
                        FitText(stringResource(level.shortRes).replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EffortGuide(onDismiss: () -> Unit) {
    val levels = SetEffort.entries.map { Triple(it.color(), stringResource(it.labelRes), stringResource(it.hintRes)) } +
        Triple(null.color(), stringResource(R.string.effort_none), stringResource(R.string.effort_none_hint))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.effort_guide_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                levels.forEach { (color, name, hint) ->
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(6.dp).fillMaxHeight().clip(CircleShape).background(color))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.effort_guide_ok)) } },
    )
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
private fun BigField(
    field: SetField,
    set: WorkoutSet,
    ghost: WorkoutSet?,
    plates: PlateLoading?,
    onEdit: ((WorkoutSet) -> WorkoutSet) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
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
    val style = (if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium).copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    val stepSize = if (compact) 36.dp else 44.dp

    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Filled.Remove, "−${formatNumber(field.step)} $unit", stepSize) {
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
                        if (text.isEmpty()) Text(shownGhost?.let(field::read).orEmpty(), style = style, maxLines = 1, color = lerp(colors.surfaceVariant, colors.outline, 0.6f))
                        inner()
                    }
                },
            )
            FitText(unit, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colors.onSurfaceVariant)
            if (plates != null) {
                (set.weightKg ?: ghost?.weightKg)?.let { total ->
                    FitText(
                        stringResource(R.string.plates_total, formatNumber(total)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        StepButton(Icons.Filled.Add, "+${formatNumber(field.step)} $unit", stepSize) {
            focusManager.clearFocus()
            onEdit { back(field.step(view(it), shownGhost, 1)) }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, size: Dp, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(size)) {
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
