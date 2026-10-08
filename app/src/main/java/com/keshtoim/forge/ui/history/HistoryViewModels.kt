package com.keshtoim.forge.ui.history

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
import com.keshtoim.forge.ui.WorkoutDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(workoutDao: WorkoutDao) : ViewModel() {
    val workouts = workoutDao.observeFinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoryViewModel((this[APPLICATION_KEY] as ForgeApplication).database.workoutDao()) }
        }
    }
}

class WorkoutDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
) : ViewModel() {
    private val workoutId = savedStateHandle.toRoute<WorkoutDetailRoute>().id

    val detail = workoutDao.observeDetail(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            workoutDao.deleteWorkout(workoutId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                WorkoutDetailViewModel(createSavedStateHandle(), db.workoutDao(), db.exerciseDao())
            }
        }
    }
}
