package com.keshtoim.forge.ui.exercises

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
import com.keshtoim.forge.ui.ExerciseDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ExerciseDetailRoute>()
    val canAddToWorkout = !route.fromWorkout

    val exercise = exerciseDao.observeAll()
        .map { list -> list.firstOrNull { it.id == route.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sets = workoutDao.observeSetPoints(route.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var busy = false

    fun addToWorkout(name: String, onOpened: (Long) -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val workoutId = workoutDao.addToActiveOrStart(name, route.id, System.currentTimeMillis())
            busy = false
            onOpened(workoutId)
        }
    }

    suspend fun hasHistory() = exerciseDao.isUsedInWorkouts(route.id)

    fun save(exercise: Exercise) {
        viewModelScope.launch { exerciseDao.update(exercise) }
    }

    fun archive(exercise: Exercise, onDone: () -> Unit) {
        viewModelScope.launch {
            exerciseDao.update(exercise.copy(archived = true))
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                ExerciseDetailViewModel(createSavedStateHandle(), db.exerciseDao(), db.workoutDao())
            }
        }
    }
}
