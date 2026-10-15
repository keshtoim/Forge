package com.keshtoim.forge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.db.BodyWeight
import com.keshtoim.forge.data.db.BodyWeightDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileState(val sex: BodyType, val heightCm: Int?, val birthDate: LocalDate?, val weights: List<BodyWeight>) {
    val weightKg get() = weights.lastOrNull()?.weightKg

    // Weigh-ins of the last three months that the chart and the trend are based on.
    fun recent(today: LocalDate): List<BodyWeight> = weights.filter { it.epochDay >= today.minusMonths(3).toEpochDay() }

    fun recentChange(today: LocalDate): Double? = recent(today).takeIf { it.size >= 2 }?.let { it.last().weightKg - it.first().weightKg }
}

class ProfileViewModel(private val preferences: UserPreferences, private val weights: BodyWeightDao) : ViewModel() {
    val state = combine(preferences.bodyType, preferences.heightCm, preferences.birthDate, weights.observeAll()) { sex, height, birth, log ->
        // The figure is chosen on first launch, so the profile is only reachable once it is set.
        ProfileState(sex ?: BodyType.MALE, height, birth, log)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSex(sex: BodyType) = preferences.setBodyType(sex)

    fun setHeight(cm: Int) = preferences.setHeightCm(cm)

    fun setBirthDate(date: LocalDate) = preferences.setBirthDate(date)

    fun deleteWeight(weight: BodyWeight) {
        viewModelScope.launch { weights.delete(weight) }
    }

    fun recordWeight(kg: Double) {
        viewModelScope.launch { weights.record(LocalDate.now().toEpochDay(), kg) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                ProfileViewModel(app.preferences, app.database.bodyWeightDao())
            }
        }
    }
}
