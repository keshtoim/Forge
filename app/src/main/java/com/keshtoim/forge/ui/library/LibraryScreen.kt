package com.keshtoim.forge.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.exercises.ExercisesTab
import com.keshtoim.forge.ui.templates.TemplatesTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenExercise: (Long) -> Unit, onOpenTemplate: (Long) -> Unit, onOpenWorkout: (Long) -> Unit, onOpenProgram: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column {
        ScreenHeader(stringResource(R.string.tab_library))
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf(R.string.tab_exercises, R.string.templates).forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(label)) })
            }
        }
        when (tab) {
            0 -> ExercisesTab(onOpen = onOpenExercise)
            else -> TemplatesTab(onOpenTemplate = onOpenTemplate, onOpenWorkout = onOpenWorkout, onOpenProgram = onOpenProgram)
        }
    }
}
