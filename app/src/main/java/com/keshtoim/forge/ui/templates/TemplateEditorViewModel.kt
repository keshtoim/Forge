package com.keshtoim.forge.ui.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.ui.TemplateEditorRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
) : ViewModel() {
    val templateId = savedStateHandle.toRoute<TemplateEditorRoute>().id

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    var name by mutableStateOf("")
    val items = mutableStateListOf<TemplateExercise>()

    init {
        if (templateId != 0L) {
            viewModelScope.launch {
                templateDao.get(templateId)?.let { loaded ->
                    name = loaded.template.name
                    // Prepend: exercises picked before loading finished stay at the end.
                    items.addAll(0, loaded.items.sortedBy { it.position })
                }
            }
        }
    }

    fun addExercises(ids: LongArray) {
        ids.forEach { items += TemplateExercise(templateId = templateId, exerciseId = it, position = 0, targetSets = 3) }
    }

    fun setSets(index: Int, sets: Int) {
        items[index] = items[index].copy(targetSets = sets.coerceIn(1, 20))
    }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target in items.indices) items.add(target, items.removeAt(index))
    }

    fun remove(index: Int) {
        items.removeAt(index)
    }

    // The screen stays clickable while the write runs; a second tap would insert a duplicate template.
    private var busy = false

    fun save(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            templateDao.save(Template(id = templateId, name = name.trim()), items.toList())
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            templateDao.delete(Template(id = templateId, name = name))
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                TemplateEditorViewModel(createSavedStateHandle(), db.templateDao(), db.exerciseDao())
            }
        }
    }
}
