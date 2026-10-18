package com.keshtoim.forge.ui.workout

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.WorkoutDao
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

class ActiveWorkoutViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val restTimer: RestTimer,
) : ViewModel() {
    private val workoutId = savedStateHandle.toRoute<ActiveWorkoutRoute>().id

    val detail = workoutDao.observeDetail(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val rest = restTimer.state

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
                ActiveWorkoutViewModel(createSavedStateHandle(), db.workoutDao(), db.exerciseDao(), app.restTimer)
            }
        }
    }
}
