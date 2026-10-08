package com.keshtoim.forge.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TemplateSummary(val template: Template, val exercises: List<Exercise>)

class WorkoutHomeViewModel(templateDao: TemplateDao, exerciseDao: ExerciseDao) : ViewModel() {
    val templates = combine(templateDao.observeAll(), exerciseDao.observeAll()) { templates, exercises ->
        val byId = exercises.associateBy { it.id }
        templates.map { t -> TemplateSummary(t.template, t.items.sortedBy { it.position }.mapNotNull { byId[it.exerciseId] }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                WorkoutHomeViewModel(db.templateDao(), db.exerciseDao())
            }
        }
    }
}
