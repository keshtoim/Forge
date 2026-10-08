package com.keshtoim.forge.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Workout
import com.keshtoim.forge.rest.Rest
import com.keshtoim.forge.ui.exercises.ExercisePickerScreen
import com.keshtoim.forge.ui.exercises.ExercisesScreen
import com.keshtoim.forge.ui.exercises.PICKED_EXERCISES
import com.keshtoim.forge.ui.history.HistoryScreen
import com.keshtoim.forge.ui.history.WorkoutDetailScreen
import com.keshtoim.forge.ui.progress.ExerciseProgressScreen
import com.keshtoim.forge.ui.progress.ProgressScreen
import com.keshtoim.forge.ui.settings.SettingsScreen
import com.keshtoim.forge.ui.templates.TemplateEditorScreen
import com.keshtoim.forge.ui.templates.TemplateEditorViewModel
import com.keshtoim.forge.ui.workout.ActiveWorkoutScreen
import com.keshtoim.forge.ui.workout.ActiveWorkoutViewModel
import com.keshtoim.forge.ui.workout.WorkoutHomeScreen
import com.keshtoim.forge.ui.workout.rememberElapsed
import com.keshtoim.forge.ui.workout.rememberRemaining
import kotlinx.serialization.Serializable

@Serializable object WorkoutRoute
@Serializable object HistoryRoute
@Serializable object ExercisesRoute
@Serializable object ProgressRoute
@Serializable data class TemplateEditorRoute(val id: Long)
@Serializable object ExercisePickerRoute
@Serializable data class ActiveWorkoutRoute(val id: Long)
@Serializable data class WorkoutDetailRoute(val id: Long)
@Serializable data class ExerciseProgressRoute(val exerciseId: Long)
@Serializable object SettingsRoute

private enum class Tab(val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    Workout(WorkoutRoute, R.string.tab_workout, Icons.Filled.FitnessCenter),
    History(HistoryRoute, R.string.tab_history, Icons.Filled.History),
    Exercises(ExercisesRoute, R.string.tab_exercises, Icons.AutoMirrored.Filled.FormatListBulleted),
    Progress(ProgressRoute, R.string.tab_progress, Icons.AutoMirrored.Filled.ShowChart),
}

@Composable
fun ForgeApp(viewModel: ForgeViewModel = viewModel(factory = ForgeViewModel.Factory)) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                Column {
                    activeWorkout?.let { workout ->
                        ActiveWorkoutBar(workout, rest) { navController.openWorkout(workout.id) }
                    }
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        // Screens draw their own top app bars, so only the bottom inset is handled here.
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = WorkoutRoute,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable<WorkoutRoute> { entry ->
                WorkoutHomeScreen(
                    onOpenTemplate = { id -> entry.ifResumed { navController.navigate(TemplateEditorRoute(id)) } },
                    onOpenWorkout = { id -> entry.ifResumed { navController.openWorkout(id) } },
                    onOpenSettings = { entry.ifResumed { navController.navigate(SettingsRoute) } },
                )
            }
            composable<HistoryRoute> { entry ->
                HistoryScreen(onOpen = { id -> entry.ifResumed { navController.navigate(WorkoutDetailRoute(id)) } })
            }
            composable<WorkoutDetailRoute> { entry ->
                WorkoutDetailScreen(onBack = { entry.ifResumed { navController.popBackStack() } })
            }
            composable<ExercisesRoute> { ExercisesScreen() }
            composable<ProgressRoute> { entry ->
                ProgressScreen(onOpen = { id -> entry.ifResumed { navController.navigate(ExerciseProgressRoute(id)) } })
            }
            composable<ExerciseProgressRoute> { entry ->
                ExerciseProgressScreen(onBack = { entry.ifResumed { navController.popBackStack() } })
            }
            composable<TemplateEditorRoute> { entry ->
                val editor: TemplateEditorViewModel = viewModel(factory = TemplateEditorViewModel.Factory)
                PickedExercisesEffect(entry, editor::addExercises)
                TemplateEditorScreen(
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                    onAddExercises = { entry.ifResumed { navController.navigate(ExercisePickerRoute) } },
                    viewModel = editor,
                )
            }
            composable<ActiveWorkoutRoute> { entry ->
                val workoutId = entry.toRoute<ActiveWorkoutRoute>().id
                val workout: ActiveWorkoutViewModel = viewModel(factory = ActiveWorkoutViewModel.Factory)
                PickedExercisesEffect(entry, workout::addExercises)
                ActiveWorkoutScreen(
                    viewModel = workout,
                    onClose = { entry.ifResumed { navController.popBackStack() } },
                    onFinished = {
                        entry.ifResumed {
                            navController.navigate(WorkoutDetailRoute(workoutId)) {
                                popUpTo<ActiveWorkoutRoute> { inclusive = true }
                            }
                        }
                    },
                    onAddExercises = { entry.ifResumed { navController.navigate(ExercisePickerRoute) } },
                )
            }
            composable<SettingsRoute> { entry ->
                SettingsScreen(onBack = { entry.ifResumed { navController.popBackStack() } })
            }
            composable<ExercisePickerRoute> { entry ->
                ExercisePickerScreen(
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                    onDone = { ids ->
                        entry.ifResumed {
                            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISES, ids)
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}

// A screen that is animating out still receives clicks; without this a double tap pops or pushes twice
// (e.g. popping the start destination leaves an empty NavHost).
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

private fun NavController.openWorkout(id: Long) = navigate(ActiveWorkoutRoute(id)) { launchSingleTop = true }

@Composable
private fun ActiveWorkoutBar(workout: Workout, rest: Rest?, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null)
            Text(
                workout.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(
                if (rest != null) stringResource(R.string.rest_remaining, rememberRemaining(rest.endsAt)) else rememberElapsed(workout.startedAt),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

// The picker returns its result through the previous entry's handle, which is not the one ViewModels receive.
@Composable
private fun PickedExercisesEffect(entry: NavBackStackEntry, onPicked: (LongArray) -> Unit) {
    val picked by entry.savedStateHandle.getStateFlow<LongArray?>(PICKED_EXERCISES, null).collectAsStateWithLifecycle()
    LaunchedEffect(picked) {
        picked?.let {
            onPicked(it)
            entry.savedStateHandle[PICKED_EXERCISES] = null
        }
    }
}
