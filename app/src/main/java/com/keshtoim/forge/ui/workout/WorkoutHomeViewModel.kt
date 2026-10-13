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
import com.keshtoim.forge.data.db.Workout
import com.keshtoim.forge.data.db.WorkoutDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class TemplateSummary(
    val template: Template,
    val items: List<TemplateExercise>,
    val exercises: List<Exercise>,
    val lastDoneAt: Long?,
)

// [next] is the template suggested in rotation; it is null while a workout is in progress.
data class HomeState(val active: Workout?, val next: TemplateSummary?, val others: List<TemplateSummary>, val week: Week)

class WorkoutHomeViewModel(
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
) : ViewModel() {
    val state = combine(
        templateDao.observeAll(),
        exerciseDao.observeAll(),
        workoutDao.observeFinishedWorkouts(),
        workoutDao.observeActive(),
    ) { templates, exercises, finished, active ->
        val byId = exercises.associateBy { it.id }
        val lastDone = lastDoneByTemplate(finished)
        val summaries = templates
            .map { t ->
                val items = t.items.sortedBy { it.position }
                TemplateSummary(t.template, items, items.mapNotNull { byId[it.exerciseId] }, lastDone[t.template.id])
            }
            .sortedBy { it.lastDoneAt ?: Long.MIN_VALUE }
        val next = if (active == null) nextInRotation(summaries) { it.lastDoneAt } else null
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        HomeState(active, next, summaries.filter { it != next }, week(finished, LocalDate.now(), ZoneId.systemDefault(), firstDay))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startEmpty(name: String, onStarted: (Long) -> Unit) = start(name, null, emptyList(), onStarted)

    fun startTemplate(summary: TemplateSummary, onStarted: (Long) -> Unit) =
        start(summary.template.name, summary.template.id, summary.items.map { it.exerciseId to it.targetSets }, onStarted)

    private var starting = false

    private fun start(name: String, templateId: Long?, items: List<Pair<Long, Int>>, onStarted: (Long) -> Unit) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            val id = workoutDao.startOrResume(name, templateId, items, System.currentTimeMillis())
            starting = false
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
