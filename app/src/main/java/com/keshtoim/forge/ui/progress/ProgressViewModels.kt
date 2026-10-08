package com.keshtoim.forge.ui.progress

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
import com.keshtoim.forge.data.db.ExerciseHistory
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.ui.ExerciseProgressRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(workoutDao: WorkoutDao, exerciseDao: ExerciseDao) : ViewModel() {
    val items = combine(workoutDao.observeExerciseHistory(), exerciseDao.observeAll()) { history, exercises ->
        val byId = exercises.associateBy { it.id }
        history.mapNotNull { h -> byId[h.exerciseId]?.let { it to h } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Pair<Exercise, ExerciseHistory>>())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                ProgressViewModel(db.workoutDao(), db.exerciseDao())
            }
        }
    }
}

class ExerciseProgressViewModel(savedStateHandle: SavedStateHandle, workoutDao: WorkoutDao, exerciseDao: ExerciseDao) : ViewModel() {
    private val exerciseId = savedStateHandle.toRoute<ExerciseProgressRoute>().exerciseId

    val exercise = exerciseDao.observeAll()
        .map { list -> list.firstOrNull { it.id == exerciseId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sets = workoutDao.observeSetPoints(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                ExerciseProgressViewModel(createSavedStateHandle(), db.workoutDao(), db.exerciseDao())
            }
        }
    }
}
