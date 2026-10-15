package com.keshtoim.forge.ui.plan

import androidx.compose.runtime.getValue
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
import com.keshtoim.forge.data.db.PlanDao
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.repeatMask
import com.keshtoim.forge.data.repeats
import com.keshtoim.forge.data.time
import com.keshtoim.forge.reminder.PlanReminders
import com.keshtoim.forge.ui.PlanRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PlanViewModel(
    savedStateHandle: SavedStateHandle,
    private val planDao: PlanDao,
    templateDao: TemplateDao,
    private val reminders: PlanReminders,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PlanRoute>()
    val planId = route.id

    val templates = templateDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var templateId by mutableStateOf<Long?>(null)
    var date by mutableStateOf(LocalDate.ofEpochDay(route.epochDay))
    var time by mutableStateOf(LocalTime.of(18, 0))
    var repeat by mutableStateOf(emptySet<DayOfWeek>())
    var remindBefore by mutableStateOf<Int?>(30)

    init {
        if (planId != 0L) {
            viewModelScope.launch {
                planDao.get(planId)?.let { plan ->
                    templateId = plan.templateId
                    date = plan.date
                    time = plan.time
                    repeat = plan.repeats
                    remindBefore = plan.remindBefore
                }
            }
        }
    }

    fun toggleRepeat(day: DayOfWeek) {
        repeat = if (day in repeat) repeat - day else repeat + day
    }

    private var busy = false

    fun save(onDone: () -> Unit) {
        val template = templateId ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val plan = PlannedWorkout(
                id = planId,
                templateId = template,
                epochDay = date.toEpochDay(),
                minuteOfDay = time.hour * 60 + time.minute,
                repeatDays = repeatMask(repeat),
                remindBefore = remindBefore,
            )
            reminders.schedule(plan.copy(id = planDao.save(plan)))
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            planDao.get(planId)?.let { planDao.delete(it) }
            reminders.cancel(planId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                PlanViewModel(createSavedStateHandle(), db.planDao(), db.templateDao(), app.planReminders)
            }
        }
    }
}
