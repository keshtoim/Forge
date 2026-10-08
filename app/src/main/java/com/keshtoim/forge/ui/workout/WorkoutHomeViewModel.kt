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
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.data.db.WorkoutDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateSummary(val template: Template, val items: List<TemplateExercise>, val exercises: List<Exercise>)

class WorkoutHomeViewModel(
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
) : ViewModel() {
    val templates = combine(templateDao.observeAll(), exerciseDao.observeAll()) { templates, exercises ->
        val byId = exercises.associateBy { it.id }
        templates.map { t ->
            val items = t.items.sortedBy { it.position }
            TemplateSummary(t.template, items, items.mapNotNull { byId[it.exerciseId] })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeWorkout = workoutDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startEmpty(name: String, onStarted: (Long) -> Unit) = start(name, null, emptyList(), onStarted)

    fun startTemplate(summary: TemplateSummary, onStarted: (Long) -> Unit) =
        start(summary.template.name, summary.template.id, summary.items.map { it.exerciseId to it.targetSets }, onStarted)

    private fun start(name: String, templateId: Long?, items: List<Pair<Long, Int>>, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            // Only one workout may be in progress; resume it instead of starting another.
            val id = workoutDao.getActive()?.id ?: workoutDao.start(name, templateId, items, System.currentTimeMillis())
            onStarted(id)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                WorkoutHomeViewModel(db.templateDao(), db.exerciseDao(), db.workoutDao())
            }
        }
    }
}
