package com.keshtoim.forge.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.exercises.ExercisePickerScreen
import com.keshtoim.forge.ui.exercises.ExercisesScreen
import com.keshtoim.forge.ui.exercises.PICKED_EXERCISES
import com.keshtoim.forge.ui.templates.TemplateEditorScreen
import com.keshtoim.forge.ui.workout.WorkoutHomeScreen
import kotlinx.serialization.Serializable

@Serializable object WorkoutRoute
@Serializable object HistoryRoute
@Serializable object ExercisesRoute
@Serializable object ProgressRoute
@Serializable data class TemplateEditorRoute(val id: Long)
@Serializable object ExercisePickerRoute

private enum class Tab(val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    Workout(WorkoutRoute, R.string.tab_workout, Icons.Filled.FitnessCenter),
    History(HistoryRoute, R.string.tab_history, Icons.Filled.History),
    Exercises(ExercisesRoute, R.string.tab_exercises, Icons.AutoMirrored.Filled.FormatListBulleted),
    Progress(ProgressRoute, R.string.tab_progress, Icons.AutoMirrored.Filled.ShowChart),
}

@Composable
fun ForgeApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
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
        },
    ) { padding ->
        // Screens draw their own top app bars, so only the bottom inset is handled here.
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = WorkoutRoute,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable<WorkoutRoute> {
                WorkoutHomeScreen(onOpenTemplate = { navController.navigate(TemplateEditorRoute(it)) })
            }
            composable<HistoryRoute> { TabTitle(R.string.tab_history) }
            composable<ExercisesRoute> { ExercisesScreen() }
            composable<ProgressRoute> { TabTitle(R.string.tab_progress) }
            composable<TemplateEditorRoute> {
                TemplateEditorScreen(
                    onBack = { navController.popBackStack() },
                    onAddExercises = { navController.navigate(ExercisePickerRoute) },
                )
            }
            composable<ExercisePickerRoute> {
                ExercisePickerScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { ids ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISES, ids)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun TabTitle(@StringRes title: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
    }
}
