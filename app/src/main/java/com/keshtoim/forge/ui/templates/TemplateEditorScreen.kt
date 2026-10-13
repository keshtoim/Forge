package com.keshtoim.forge.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.exercises.muscleHeat

@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    viewModel: TemplateEditorViewModel = viewModel(factory = TemplateEditorViewModel.Factory),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isNew = viewModel.templateId == 0L
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            ScreenHeader(stringResource(if (isNew) R.string.template_new else R.string.template_edit), onBack = onBack) {
                if (!isNew) {
                    IconButton(onClick = { viewModel.delete(onBack) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save(onBack) },
                enabled = viewModel.name.isNotBlank() && viewModel.items.isNotEmpty(),
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth().height(64.dp),
            ) {
                Text(stringResource(R.string.save).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = 0.04.em))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainer,
                        unfocusedContainerColor = colors.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (viewModel.items.isNotEmpty()) {
                item {
                    val chosen = viewModel.items.mapNotNull { exercises[it.exerciseId] }
                    val sets = viewModel.items.sumOf { it.targetSets }
                    ForgeCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            MuscleMap(muscleHeat(chosen), Modifier.width(110.dp))
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(pluralStringResource(R.plurals.exercises_count, viewModel.items.size, viewModel.items.size), style = MaterialTheme.typography.titleMedium)
                                Text(pluralStringResource(R.plurals.sets_total, sets, sets), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            itemsIndexed(viewModel.items) { index, item ->
                ForgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            }
                            Text(
                                exercises[item.exerciseId]?.displayName(context).orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            IconButton(onClick = { viewModel.remove(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = colors.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.clip(CircleShape).background(colors.surfaceContainerHigh), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.setSets(index, item.targetSets - 1) }) {
                                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.set_remove))
                                }
                                Text(pluralStringResource(R.plurals.sets_total, item.targetSets, item.targetSets), style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { viewModel.setSets(index, item.targetSets + 1) }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.set_add))
                                }
                            }
                            Box(Modifier.weight(1f))
                            IconButton(onClick = { viewModel.move(index, -1) }, enabled = index > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                            }
                            IconButton(onClick = { viewModel.move(index, 1) }, enabled = index < viewModel.items.lastIndex) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onAddExercises,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, colors.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_exercises), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
