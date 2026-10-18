package com.keshtoim.forge.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiException
import com.keshtoim.forge.data.ai.AiFailure
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.data.ai.Athlete
import com.keshtoim.forge.data.ai.ExerciseLog
import com.keshtoim.forge.data.ai.coachPrompt
import com.keshtoim.forge.data.db.BodyMeasurementDao
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.data.db.WorkoutDetail
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.rest.RestTimer
import com.keshtoim.forge.ui.ActiveWorkoutRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.Period
import java.util.Locale

class ActiveWorkoutViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val restTimer: RestTimer,
    private val ai: AiConnection,
    private val preferences: UserPreferences,
    private val measurements: BodyMeasurementDao,
) : ViewModel() {
    private val workoutId = savedStateHandle.toRoute<ActiveWorkoutRoute>().id

    val detail = workoutDao.observeDetail(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val rest = restTimer.state

    val aiStatus = ai.status

    fun verifyAi() = ai.verify()

    val chat = mutableStateListOf<AiTurn>()
    var coachBusy by mutableStateOf(false)
        private set
    var coachFailure by mutableStateOf<AiFailure?>(null)
        private set

    // [names] are display names by exerciseId; the prompt is rebuilt on every question so the coach sees the latest sets.
    fun askCoach(text: String, names: Map<Long, String>, focusId: Long?) {
        val question = text.trim()
        if (question.isEmpty() || coachBusy) return
        // After a failed request the question is still unanswered: append to it so turns keep alternating.
        val last = chat.lastOrNull()
        if (last?.fromUser == true) chat[chat.lastIndex] = last.copy(text = last.text + "\n" + question) else chat += AiTurn(true, question)
        sendCoach(names, focusId)
    }

    fun retryCoach(names: Map<Long, String>, focusId: Long?) {
        if (chat.lastOrNull()?.fromUser == true && !coachBusy) sendCoach(names, focusId)
    }

    private fun sendCoach(names: Map<Long, String>, focusId: Long?) {
        val workout = detail.value ?: return
        coachBusy = true
        coachFailure = null
        viewModelScope.launch {
            try {
                chat += AiTurn(false, ai.ask(coachSystem(workout, names, focusId), chat.toList()))
            } catch (e: AiException) {
                coachFailure = e.failure
            }
            coachBusy = false
        }
    }

    private suspend fun coachSystem(workout: WorkoutDetail, names: Map<Long, String>, focusId: Long?): String {
        val logs = workout.exercises.ordered().map { e ->
            val exerciseId = e.workoutExercise.exerciseId
            ExerciseLog(
                name = names[exerciseId].orEmpty(),
                type = exercises.value[exerciseId]?.type ?: ExerciseType.WEIGHT_REPS,
                superset = e.workoutExercise.superset,
                sets = e.sets,
                lastTime = previous[exerciseId].orEmpty(),
                onScreen = e.workoutExercise.id == focusId,
            )
        }
        val athlete = Athlete(
            heightCm = preferences.heightCm.value,
            age = preferences.birthDate.value?.let { Period.between(it, LocalDate.now()).years },
            weightKg = measurements.latestWeight(),
        )
        val minutes = (System.currentTimeMillis() - workout.workout.startedAt) / 60_000
        return coachPrompt(Locale.getDefault().getDisplayLanguage(Locale.ENGLISH), athlete, workout.workout.name, minutes, logs)
    }

    // Sets of the last finished workout per exerciseId, shown as placeholders.
    val previous = mutableStateMapOf<Long, List<WorkoutSet>>()

    private val setWrites = Mutex()

    init {
        viewModelScope.launch {
            detail.filterNotNull().collect { d ->
                d.exercises.map { it.workoutExercise.exerciseId }
                    .filter { it !in previous }
                    .forEach { previous[it] = workoutDao.previousSets(it) }
            }
        }
    }

    fun addExercises(ids: LongArray) {
        viewModelScope.launch { workoutDao.addExercises(workoutId, ids.toList()) }
    }

    // The UI's copy of a set lags behind pending writes, so every edit re-reads the row under one lock;
    // otherwise quick successive edits (type, then tap ✓) would overwrite each other with stale values.
    fun editSet(id: Long, transform: (WorkoutSet) -> WorkoutSet?) {
        viewModelScope.launch {
            setWrites.withLock {
                val current = workoutDao.getSet(id) ?: return@withLock
                transform(current)?.let { workoutDao.update(it) }
            }
        }
    }

    fun toggleCompleted(id: Long, ghost: WorkoutSet?) = editSet(id) { set ->
        if (set.completed) return@editSet set.copy(completed = false)
        set.completedWith(ghost)?.also { restTimer.start() }
    }

    // Completes every set of a superset round; rest starts once, after the whole round.
    fun completeRound(sets: List<Pair<Long, WorkoutSet?>>) {
        viewModelScope.launch {
            setWrites.withLock {
                val done = sets.mapNotNull { (id, ghost) -> workoutDao.getSet(id)?.takeIf { !it.completed }?.completedWith(ghost) }
                done.forEach { workoutDao.update(it) }
                if (done.isNotEmpty()) restTimer.start()
            }
        }
    }

    // Missing values come from last time; a set with nothing to log stays open.
    private fun WorkoutSet.completedWith(ghost: WorkoutSet?): WorkoutSet? {
        val filled = copy(
            weightKg = weightKg ?: ghost?.weightKg,
            reps = reps ?: ghost?.reps,
            durationSec = durationSec ?: ghost?.durationSec,
            distanceM = distanceM ?: ghost?.distanceM,
        )
        if (filled.reps == null && filled.durationSec == null && filled.distanceM == null) return null
        return filled.copy(completed = true)
    }

    fun addSet(workoutExerciseId: Long) = addSets(listOf(workoutExerciseId))

    // A new set starts from the values of the exercise's last one, so another round repeats the previous.
    fun addSets(workoutExerciseIds: List<Long>) {
        viewModelScope.launch {
            setWrites.withLock {
                workoutExerciseIds.forEach { workoutExerciseId ->
                    val last = workoutDao.lastSet(workoutExerciseId)
                    val base = last?.copy(id = 0, completed = false) ?: WorkoutSet(workoutExerciseId = workoutExerciseId, position = 0)
                    workoutDao.insert(base.copy(position = (last?.position ?: -1) + 1))
                }
            }
        }
    }

    fun setBar(exercise: Exercise, barKg: Double?) {
        viewModelScope.launch { exerciseDao.update(exercise.copy(barKg = barKg)) }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch { workoutDao.delete(set) }
    }

    fun removeExercise(exercise: WorkoutExerciseWithSets) {
        viewModelScope.launch { workoutDao.delete(exercise.workoutExercise) }
    }

    fun adjustRest(deltaSec: Int) = restTimer.adjust(deltaSec)

    fun skipRest() = restTimer.stop()

    private var closing = false

    fun finish(onDone: () -> Unit) {
        if (closing) return
        closing = true
        viewModelScope.launch {
            restTimer.stop()
            workoutDao.finish(workoutId, System.currentTimeMillis())
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        if (closing) return
        closing = true
        viewModelScope.launch {
            restTimer.stop()
            workoutDao.deleteWorkout(workoutId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                ActiveWorkoutViewModel(
                    createSavedStateHandle(), db.workoutDao(), db.exerciseDao(), app.restTimer, app.aiConnection, app.preferences, db.bodyMeasurementDao(),
                )
            }
        }
    }
}
