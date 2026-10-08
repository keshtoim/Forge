package com.keshtoim.forge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.rest.RestTimer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ForgeViewModel(workoutDao: WorkoutDao, restTimer: RestTimer) : ViewModel() {
    val rest = restTimer.state

    val activeWorkout = workoutDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                ForgeViewModel(app.database.workoutDao(), app.restTimer)
            }
        }
    }
}
