package com.keshtoim.forge.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.BodyMeasurement
import com.keshtoim.forge.data.db.BodyMeasurementDao
import com.keshtoim.forge.ui.MeasureRoute
import com.keshtoim.forge.ui.workout.formatNumber
import kotlinx.coroutines.launch
import java.time.LocalDate

class MeasureViewModel(savedStateHandle: SavedStateHandle, private val dao: BodyMeasurementDao) : ViewModel() {
    var date by mutableStateOf(LocalDate.ofEpochDay(savedStateHandle.toRoute<MeasureRoute>().epochDay))
        private set

    // Raw input per parameter, so a half-typed "62," stays on screen.
    val input = mutableStateMapOf<BodyParam, String>()

    var exists by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun changeDate(day: LocalDate) {
        date = day
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val entry = dao.getDay(date.toEpochDay())
            exists = entry != null
            input.clear()
            BodyParam.entries.forEach { param -> input[param] = entry?.let(param.get)?.let(::formatNumber).orEmpty() }
        }
    }

    fun parse(param: BodyParam): Double? = input[param].orEmpty().replace(',', '.').toDoubleOrNull()

    fun isValid(param: BodyParam) = input[param].isNullOrBlank() || parse(param)?.let { it in param.range } == true

    val canSave get() = BodyParam.entries.all(::isValid) && BodyParam.entries.any { parse(it) != null }

    private var busy = false

    // Every field is written, so clearing one removes that value from the day.
    fun save(onDone: () -> Unit) {
        if (busy || !canSave) return
        busy = true
        viewModelScope.launch {
            dao.record(date.toEpochDay()) { entry -> BodyParam.entries.fold(entry) { m, param -> param.set(m, parse(param)) } }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            dao.getDay(date.toEpochDay())?.let { dao.delete(it) }
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                MeasureViewModel(createSavedStateHandle(), db.bodyMeasurementDao())
            }
        }
    }
}

internal fun BodyMeasurement.values(): List<Pair<BodyParam, Double>> = BodyParam.entries.mapNotNull { p -> p.get(this)?.let { p to it } }
