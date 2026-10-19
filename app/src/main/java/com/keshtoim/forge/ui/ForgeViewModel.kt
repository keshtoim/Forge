package com.keshtoim.forge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.data.update.UpdateState
import com.keshtoim.forge.data.update.Updater
import com.keshtoim.forge.rest.RestTimer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ForgeViewModel(workoutDao: WorkoutDao, restTimer: RestTimer, private val preferences: UserPreferences, updater: Updater) : ViewModel() {
    init {
        updater.checkIfDue()
    }

    // Marks the "More" tab, where the update card lives.
    val updateWaiting = updater.state
        .map { it is UpdateState.Available || it is UpdateState.Ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rest = restTimer.state

    val bodyType = preferences.bodyType

    fun setBodyType(type: BodyType) = preferences.setBodyType(type)

    val activeWorkout = workoutDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                ForgeViewModel(app.database.workoutDao(), app.restTimer, app.preferences, app.updater)
            }
        }
    }
}
