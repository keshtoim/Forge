package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ScreenHeader

const val PICKED_EXERCISES = "picked_exercises"

@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onDone: (LongArray) -> Unit,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(longArrayOf()) }

    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.add_exercises), onBack = onBack) },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Button(
                    onClick = { onDone(selected) },
                    shape = CircleShape,
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth().height(64.dp),
                ) {
                    Text(
                        stringResource(R.string.add_count, selected.size).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = 0.04.em),
                    )
                }
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
