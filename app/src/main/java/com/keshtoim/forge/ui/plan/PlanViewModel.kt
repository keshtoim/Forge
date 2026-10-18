package com.keshtoim.forge.ui.plan

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
import com.keshtoim.forge.data.date
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.PlanDao
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.data.repeatMask
import com.keshtoim.forge.data.repeats
import com.keshtoim.forge.data.time
import com.keshtoim.forge.reminder.PlanReminders
import com.keshtoim.forge.ui.PlanRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PlanViewModel(
    savedStateHandle: SavedStateHandle,
    private val planDao: PlanDao,
    private val templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    private val reminders: PlanReminders,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PlanRoute>()
    val planId = route.id

    val templates = templateDao.observeAll()
        .map { list -> list.filter { !it.template.hidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    var templateId by mutableStateOf<Long?>(null)

    // Own exercises live in a hidden template that belongs to this plan alone.
    var own by mutableStateOf(false)
    var ownName by mutableStateOf("")
    val items = mutableStateListOf<TemplateExercise>()
    var saveAsTemplate by mutableStateOf(false)
    private var ownTemplateId = 0L

    var date by mutableStateOf(LocalDate.ofEpochDay(route.epochDay))
    var time by mutableStateOf(LocalTime.of(18, 0))
    var repeat by mutableStateOf(emptySet<DayOfWeek>())
    var remindBefore by mutableStateOf<Int?>(30)

    init {
        if (planId != 0L) {
            viewModelScope.launch {
                planDao.get(planId)?.let { plan ->
                    val ownList = templateDao.get(plan.templateId)?.takeIf { it.template.hidden }
                    if (ownList == null) templateId = plan.templateId
                    if (ownList != null) {
                        own = true
                        ownTemplateId = ownList.template.id
                        ownName = ownList.template.name
                        items.addAll(0, ownList.items.sortedBy { it.position })
                    }
                    date = plan.date
                    time = plan.time
                    repeat = plan.repeats
                    remindBefore = plan.remindBefore
                }
            }
        }
    }

    val canSave get() = if (own) items.isNotEmpty() else templateId != null

    fun addExercises(ids: LongArray) {
        ids.forEach { items += TemplateExercise(templateId = 0, exerciseId = it, position = 0, targetSets = 3) }
    }

    fun toggleRepeat(day: DayOfWeek) {
        repeat = if (day in repeat) repeat - day else repeat + day
    }

    private var busy = false

    fun save(fallbackName: String, onDone: () -> Unit) {
        if (busy || !canSave) return
        busy = true
        viewModelScope.launch {
            val template = if (own) {
                val name = ownName.trim().ifEmpty { fallbackName }
                templateDao.save(Template(id = ownTemplateId, name = name, hidden = !saveAsTemplate), items.toList())
            } else {
                templateId!!
            }
            val plan = PlannedWorkout(
                id = planId,
                templateId = template,
                epochDay = date.toEpochDay(),
                minuteOfDay = time.hour * 60 + time.minute,
                repeatDays = repeatMask(repeat),
                remindBefore = remindBefore,
            )
            reminders.schedule(plan.copy(id = planDao.save(plan)))
            // Switched to a template: the old own list goes only now, since deleting it first would take the plan with it.
            if (!own && ownTemplateId != 0L) templateDao.get(ownTemplateId)?.let { templateDao.delete(it.template) }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            planDao.get(planId)?.let { planDao.delete(it) }
            templateDao.get(ownTemplateId)?.takeIf { it.template.hidden }?.let { templateDao.delete(it.template) }
            reminders.cancel(planId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                PlanViewModel(createSavedStateHandle(), db.planDao(), db.templateDao(), db.exerciseDao(), app.planReminders)
            }
        }
    }
}
