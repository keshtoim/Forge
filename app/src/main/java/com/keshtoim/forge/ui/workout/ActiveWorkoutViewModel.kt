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
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.data.db.WorkoutExerciseWithSets
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.rest.RestTimer
import com.keshtoim.forge.ui.ActiveWorkoutRoute
import com.keshtoim.forge.ui.exercises.PICKED_EXERCISES
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
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

    init {
        viewModelScope.launch {
            detail.filterNotNull().collect { d ->
                d.exercises.map { it.workoutExercise.exerciseId }
                    .filter { it !in previous }
                    .forEach { previous[it] = workoutDao.previousSets(it) }
            }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<LongArray?>(PICKED_EXERCISES, null).filterNotNull().collect { ids ->
                workoutDao.addExercises(workoutId, ids.toList())
                savedStateHandle[PICKED_EXERCISES] = null
            }
        }
    }

    fun updateSet(set: WorkoutSet) {
        viewModelScope.launch { workoutDao.update(set) }
    }

    fun toggleCompleted(set: WorkoutSet, ghost: WorkoutSet?) {
        val updated = if (set.completed) {
            set.copy(completed = false)
        } else {
            val filled = set.copy(
                weightKg = set.weightKg ?: ghost?.weightKg,
                reps = set.reps ?: ghost?.reps,
                durationSec = set.durationSec ?: ghost?.durationSec,
                distanceM = set.distanceM ?: ghost?.distanceM,
            )
            if (filled.reps == null && filled.durationSec == null && filled.distanceM == null) return
            restTimer.start()
            filled.copy(completed = true)
        }
        updateSet(updated)
    }

    fun addSet(exercise: WorkoutExerciseWithSets) {
        val last = exercise.sets.maxByOrNull { it.position }
        val base = last?.copy(id = 0, completed = false) ?: WorkoutSet(workoutExerciseId = exercise.workoutExercise.id, position = 0)
        viewModelScope.launch { workoutDao.insert(base.copy(position = (last?.position ?: -1) + 1)) }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch { workoutDao.delete(set) }
    }

    fun removeExercise(exercise: WorkoutExerciseWithSets) {
        viewModelScope.launch { workoutDao.delete(exercise.workoutExercise) }
    }

    fun adjustRest(deltaSec: Int) = restTimer.adjust(deltaSec)

    fun skipRest() = restTimer.stop()

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.stop()
            workoutDao.finish(workoutId, System.currentTimeMillis())
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
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
