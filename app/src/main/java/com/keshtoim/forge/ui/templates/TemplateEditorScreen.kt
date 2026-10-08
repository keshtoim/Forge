package com.keshtoim.forge.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    viewModel: TemplateEditorViewModel = viewModel(factory = TemplateEditorViewModel.Factory),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isNew = viewModel.templateId == 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.template_new else R.string.template_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    TextButton(
                        enabled = viewModel.name.isNotBlank() && viewModel.items.isNotEmpty(),
                        onClick = { viewModel.save(onBack) },
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(viewModel.items) { index, item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                exercises[item.exerciseId]?.displayName(context).orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.remove(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.sets_count), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.setSets(index, item.targetSets - 1) }) {
                                Icon(Icons.Filled.Remove, contentDescription = null)
                            }
                            Text("${item.targetSets}", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { viewModel.setSets(index, item.targetSets + 1) }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                            }
                            Spacer(Modifier.weight(1f))
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
                OutlinedButton(onClick = onAddExercises, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_exercises))
                }
            }
        }
    }
}
