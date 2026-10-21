package com.keshtoim.forge.ui.history

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
import com.keshtoim.forge.data.Upcoming
import com.keshtoim.forge.data.UserPreferences
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiException
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.data.ai.ExerciseLog
import com.keshtoim.forge.data.ai.athlete
import com.keshtoim.forge.data.ai.reviewPrompt
import com.keshtoim.forge.data.db.BodyMeasurementDao
import com.keshtoim.forge.data.db.ExerciseDao
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.PlanDao
import com.keshtoim.forge.data.db.TemplateDao
import com.keshtoim.forge.data.db.WorkoutDao
import com.keshtoim.forge.data.upcoming
import com.keshtoim.forge.ui.WorkoutDetailRoute
import com.keshtoim.forge.ui.workout.ordered
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class UpcomingPlan(val upcoming: Upcoming, val name: String)

class HistoryViewModel(workoutDao: WorkoutDao, planDao: PlanDao, templateDao: TemplateDao) : ViewModel() {
    val workouts = workoutDao.observeFinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcoming = combine(planDao.observeAll(), templateDao.observeAll()) { plans, templates ->
        val names = templates.associate { it.template.id to it.template.name }
        upcoming(plans, LocalDate.now()).mapNotNull { next -> names[next.plan.templateId]?.let { UpcomingPlan(next, it) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val db = (this[APPLICATION_KEY] as ForgeApplication).database
                HistoryViewModel(db.workoutDao(), db.planDao(), db.templateDao())
            }
        }
    }
}

class WorkoutDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
    private val ai: AiConnection,
    private val preferences: UserPreferences,
    private val measurements: BodyMeasurementDao,
) : ViewModel() {
    private val workoutId = savedStateHandle.toRoute<WorkoutDetailRoute>().id

    val detail = workoutDao.observeDetail(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises = exerciseDao.observeAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val aiStatus = ai.status

    var reviewBusy by mutableStateOf(false)
        private set
    var reviewError by mutableStateOf<AiException?>(null)
        private set

    // The review is saved with the workout, so it is asked for once and shown from then on.
    fun review(names: Map<Long, String>) {
        val detail = detail.value ?: return
        if (reviewBusy) return
        reviewBusy = true
        reviewError = null
        viewModelScope.launch {
            try {
                val started = detail.workout.startedAt
                val logs = detail.exercises.ordered().map { e ->
                    val exerciseId = e.workoutExercise.exerciseId
                    ExerciseLog(
                        name = names[exerciseId].orEmpty(),
                        type = exercises.value[exerciseId]?.type ?: ExerciseType.WEIGHT_REPS,
                        superset = e.workoutExercise.superset,
                        sets = e.sets.filter { it.completed },
                        lastTime = workoutDao.previousSetsBefore(exerciseId, started),
                        onScreen = false,
                        note = exercises.value[exerciseId]?.note,
                    )
                }
                val athlete = athlete(preferences.bodyType.value, preferences.heightCm.value, preferences.birthDate.value, measurements.observeAll().first(), LocalDate.now())
                val minutes = ((detail.workout.finishedAt ?: started) - started) / 60_000
                val system = reviewPrompt(Locale.getDefault().getDisplayLanguage(Locale.ENGLISH), athlete, detail.workout.name, minutes, logs, detail.workout.note)
                workoutDao.setReview(workoutId, ai.ask(system, listOf(AiTurn(true, "Review this workout."))))
            } catch (e: AiException) {
                reviewError = e
            }
            reviewBusy = false
        }
    }

    fun setNote(note: String) {
        viewModelScope.launch { workoutDao.setNote(workoutId, note.trim().ifEmpty { null }) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            workoutDao.deleteWorkout(workoutId)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                val db = app.database
                WorkoutDetailViewModel(
                    createSavedStateHandle(), db.workoutDao(), db.exerciseDao(), app.aiConnection, app.preferences, db.bodyMeasurementDao(),
                )
            }
        }
    }
}
