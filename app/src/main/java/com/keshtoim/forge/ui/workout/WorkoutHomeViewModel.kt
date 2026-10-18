package com.keshtoim.forge.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.Upcoming
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.PlanDao
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.db.StartItem
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.data.db.Workout
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.data.db.toStartItem
import com.keshtoim.forge.data.upcoming
import com.keshtoim.forge.rest.RestTimer
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
data class HomeState(
    val active: Workout?,
    val next: TemplateSummary?,
    val others: List<TemplateSummary>,
    // Every template by id, hidden ones too: plans with their own exercise list point at those.
    val templates: Map<Long, TemplateSummary>,
    val workoutsByDay: Map<LocalDate, List<Workout>>,
    val plans: List<PlannedWorkout>,
    val upcoming: Upcoming?,
    val totals: Totals,
    val weeks: List<WeekBar>,
    val heat: Map<MuscleGroup, Int>,
)

class WorkoutHomeViewModel(
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val restTimer: RestTimer,
    planDao: PlanDao,
) : ViewModel() {
    val state = combine(
        templateDao.observeAll(),
        exerciseDao.observeAll(),
        workoutDao.observeFinished(),
        workoutDao.observeActive(),
        planDao.observeAll(),
    ) { templates, exercises, details, active, plans ->
        val finished = details.map { it.workout }
        val byId = exercises.associateBy { it.id }
        val lastDone = lastDoneByTemplate(finished)
        val summaries = templates
            .map { t ->
                val items = t.items.sortedBy { it.position }
                TemplateSummary(t.template, items, items.mapNotNull { byId[it.exerciseId] }, lastDone[t.template.id])
            }
            .sortedBy { it.lastDoneAt ?: Long.MIN_VALUE }
        val visible = summaries.filter { !it.template.hidden }
        val next = if (active == null) nextInRotation(visible) { it.lastDoneAt } else null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        HomeState(
            active = active,
            next = next,
            others = visible.filter { it != next },
            templates = summaries.associateBy { it.template.id },
            workoutsByDay = workoutsByDay(finished, zone),
            plans = plans,
            upcoming = upcoming(plans, today).firstOrNull(),
            totals = totals(details, today.minusDays(29), zone),
            weeks = weekBars(details, today, zone, WeekFields.of(Locale.getDefault()).firstDayOfWeek),
            heat = recentHeat(details, byId, today.minusDays(4), zone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startEmpty(name: String, onStarted: (Long) -> Unit) = start(name, null, emptyList(), onStarted)

    fun startTemplate(summary: TemplateSummary, onStarted: (Long) -> Unit) =
        start(summary.template.name, summary.template.id, summary.items.map { it.toStartItem() }, onStarted)

    private var busy = false

    private fun start(name: String, templateId: Long?, items: List<StartItem>, onStarted: (Long) -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val id = workoutDao.startOrResume(name, templateId, items, System.currentTimeMillis())
            busy = false
            onStarted(id)
        }
    }

    // A workout without completed sets is dropped by the DAO, so [onFinished] only fires for kept ones.
    fun finish(workoutId: Long, onFinished: (Long) -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            restTimer.stop()
            val kept = workoutDao.finish(workoutId, System.currentTimeMillis())
            busy = false
            if (kept) onFinished(workoutId)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                WorkoutHomeViewModel(db.templateDao(), db.exerciseDao(), db.workoutDao(), app.restTimer, db.planDao())
            }
        }
    }
}
