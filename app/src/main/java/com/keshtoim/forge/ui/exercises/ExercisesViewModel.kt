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

    fun create(exercise: Exercise) {
        viewModelScope.launch {
            dao.insert(exercise)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ExercisesViewModel((this[APPLICATION_KEY] as ForgeApplication).database.exerciseDao()) }
        }
    }
}
