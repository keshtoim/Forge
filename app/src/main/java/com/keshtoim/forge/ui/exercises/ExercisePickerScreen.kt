package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R

const val PICKED_EXERCISES = "picked_exercises"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onDone: (LongArray) -> Unit,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(longArrayOf()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_exercises)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onDone(selected) },
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_count, selected.size)) },
                )
            }
        },
    ) { padding ->
        ExerciseCatalog(
            exercises = exercises,
            onClick = { e -> selected = if (e.id in selected) selected.filter { it != e.id }.toLongArray() else selected + e.id },
            selected = selected.toSet(),
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }
}
