package com.keshtoim.forge.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExercisesViewModel(private val dao: ExerciseDao) : ViewModel() {
    val exercises = dao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(exercise: Exercise) {
        viewModelScope.launch {
            if (exercise.id == 0L) dao.insert(exercise) else dao.update(exercise)
        }
    }

    suspend fun hasHistory(exerciseId: Long) = dao.isUsedInWorkouts(exerciseId)

    fun archive(exercise: Exercise) {
        viewModelScope.launch { dao.update(exercise.copy(archived = true)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ExercisesViewModel((this[APPLICATION_KEY] as ForgeApplication).database.exerciseDao()) }
        }
    }
}
