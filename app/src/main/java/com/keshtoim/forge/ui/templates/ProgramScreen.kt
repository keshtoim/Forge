package com.keshtoim.forge.ui.templates

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiException
import com.keshtoim.forge.data.ai.AiFailure
import com.keshtoim.forge.data.ai.AiStatus
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.data.ai.CatalogEntry
import com.keshtoim.forge.data.ai.ProgramDraft
import com.keshtoim.forge.data.ai.ProgramGoal
import com.keshtoim.forge.data.ai.ProgramLevel
import com.keshtoim.forge.data.ai.ProgramRequest
import com.keshtoim.forge.data.ai.athlete
import com.keshtoim.forge.data.ai.parseProgram
import com.keshtoim.forge.data.ai.programPrompt
import com.keshtoim.forge.data.db.BodyMeasurementDao
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.runs
import com.keshtoim.forge.data.traits
import com.keshtoim.forge.ui.AiErrorText
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ForgeChip
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.forgeSegmentColors
import com.keshtoim.forge.ui.labelRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

// Equipment the athlete can pick from; NONE stands for bodyweight work.
private val EQUIPMENT_CHOICES = listOf(
    Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.CABLE, Equipment.KETTLEBELL, Equipment.NONE, Equipment.BAND, Equipment.FITBALL,
)

class ProgramViewModel(
    private val ai: AiConnection,
    exerciseDao: ExerciseDao,
    private val templateDao: TemplateDao,
    private val preferences: UserPreferences,
    private val measurements: BodyMeasurementDao,
) : ViewModel() {
    val aiStatus = ai.status

    val exercises = exerciseDao.observeAll()
        .map { list -> list.filter { !it.archived }.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    var goal by mutableStateOf(ProgramGoal.MUSCLE)
    var days by mutableIntStateOf(3)
    var minutes by mutableIntStateOf(60)
    var level by mutableStateOf(ProgramLevel.BEGINNER)
    var equipment by mutableStateOf(setOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.MACHINE, Equipment.CABLE, Equipment.NONE))
    var wishes by mutableStateOf("")

    var draft by mutableStateOf<ProgramDraft?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<AiException?>(null)
        private set

    fun toggleEquipment(item: Equipment) {
        equipment = if (item in equipment) equipment - item else equipment + item
    }

    // Exercises with unknown equipment stay in: excluding them would hide most of the user's own ones.
    fun generate(names: Map<Long, String>) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                val catalog = exercises.value.values
                    .filter { e -> e.traits.equipment.let { it == null || it == Equipment.OTHER || it in equipment } }
                    .map { e -> CatalogEntry(e.id, names[e.id].orEmpty(), e.muscleGroup, e.traits.equipment, e.traits.mechanics, e.type) }
                val athlete = athlete(preferences.bodyType.value, preferences.heightCm.value, preferences.birthDate.value, measurements.observeAll().first(), LocalDate.now())
                val request = ProgramRequest(goal, days, minutes, level, equipment, wishes)
                val system = programPrompt(Locale.getDefault().getDisplayLanguage(Locale.ENGLISH), request, athlete, catalog)
                val reply = ai.ask(system, listOf(AiTurn(true, "Build the program.")))
                draft = parseProgram(reply, catalog.map { it.id }.toSet()) ?: throw AiException(AiFailure.OTHER, detail = reply.take(300))
            } catch (e: AiException) {
                error = e
            }
            busy = false
        }
    }

    fun discard() {
        draft = null
    }

    fun save(onDone: () -> Unit) {
        val program = draft ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            program.workouts.forEach { workout ->
                val name = if (program.name.isBlank()) workout.name else "${program.name} · ${workout.name}"
                templateDao.save(Template(name = name), workout.items)
            }
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                ProgramViewModel(app.aiConnection, db.exerciseDao(), db.templateDao(), app.preferences, db.bodyMeasurementDao())
            }
        }
    }
}

@Composable
fun ProgramScreen(onBack: () -> Unit, viewModel: ProgramViewModel = viewModel(factory = ProgramViewModel.Factory)) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val names = exercises.mapValues { it.value.displayName(context) }
    val draft = viewModel.draft

    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.program_title), onBack = onBack) },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (draft == null) {
                    Button(
                        onClick = { viewModel.generate(names) },
                        enabled = !viewModel.busy && aiStatus != AiStatus.Off && viewModel.equipment.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        if (viewModel.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Text(stringResource(if (viewModel.busy) R.string.program_building else R.string.program_build), modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Button(onClick = { viewModel.save(onBack) }, enabled = !viewModel.busy, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(pluralStringResource(R.plurals.program_save, draft.workouts.size, draft.workouts.size))
                    }
                    OutlinedButton(onClick = viewModel::discard, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.program_again))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (draft == null) {
                item { ProgramForm(viewModel, aiStatus) }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (draft.name.isNotBlank()) Text(draft.name, style = MaterialTheme.typography.headlineSmall)
                        if (draft.notes.isNotBlank()) Text(draft.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(draft.workouts) { workout ->
                    ForgeCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(workout.name, style = MaterialTheme.typography.titleMedium)
                            val supersets = workout.items.runs().filter { it.count() > 1 }
                            workout.items.forEachIndexed { index, item ->
                                val letter = supersets.firstOrNull { index in it }?.let { supersetLetter(index - it.first) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(24.dp).clip(CircleShape)
                                            .background(if (letter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            letter ?: "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (letter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(names[item.exerciseId].orEmpty(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 10.dp))
                                    Text(
                                        pluralStringResource(R.plurals.sets_total, item.targetSets, item.targetSets),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramForm(viewModel: ProgramViewModel, aiStatus: AiStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.program_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Labeled(R.string.program_goal) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgramGoal.entries.forEach { goal ->
                    ForgeChip(selected = viewModel.goal == goal, label = stringResource(goal.labelRes)) { viewModel.goal = goal }
                }
            }
        }
        Labeled(R.string.program_days) {
            Segments((2..6).toList(), viewModel.days, { it.toString() }) { viewModel.days = it }
        }
        Labeled(R.string.program_minutes) {
            Segments(listOf(45, 60, 90), viewModel.minutes, { stringResource(R.string.program_minutes_value, it) }) { viewModel.minutes = it }
        }
        Labeled(R.string.program_level) {
            Segments(ProgramLevel.entries, viewModel.level, { stringResource(it.labelRes) }) { viewModel.level = it }
        }
        Labeled(R.string.program_equipment) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EQUIPMENT_CHOICES.forEach { item ->
                    ForgeChip(
                        selected = item in viewModel.equipment,
                        label = stringResource(if (item == Equipment.NONE) R.string.program_bodyweight else item.labelRes),
                    ) { viewModel.toggleEquipment(item) }
                }
            }
        }
        OutlinedTextField(
            value = viewModel.wishes,
            onValueChange = { viewModel.wishes = it },
            label = { Text(stringResource(R.string.program_wishes)) },
            placeholder = { Text(stringResource(R.string.program_wishes_hint)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        if (aiStatus == AiStatus.Off) {
            Text(stringResource(R.string.program_needs_ai), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        viewModel.error?.let { AiErrorText(it) }
    }
}

@Composable
private fun Labeled(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun <T> Segments(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = forgeSegmentColors(),
                icon = {},
            ) { FitText(label(option)) }
        }
    }
}
