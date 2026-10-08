package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseCatalog(
    exercises: List<Exercise>,
    onClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    isClickable: (Exercise) -> Boolean = { true },
    selected: Set<Long>? = null,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }

    val grouped = remember(exercises, query, group, context) {
        exercises
            .map { it to it.displayName(context) }
            .filter { (e, name) -> (group == null || e.muscleGroup == group) && name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareBy({ it.first.muscleGroup.ordinal }, { it.second.lowercase() }))
            .groupBy { it.first.muscleGroup }
    }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.exercises_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) }
                }
            },
            singleLine = true,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(selected = group == null, onClick = { group = null }, label = { Text(stringResource(R.string.filter_all)) })
            }
            items(MuscleGroup.entries) { g ->
                FilterChip(
                    selected = group == g,
                    onClick = { group = if (group == g) null else g },
                    label = { Text(stringResource(g.labelRes)) },
                )
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            grouped.forEach { (muscle, items) ->
                stickyHeader(key = muscle) {
                    Text(
                        stringResource(muscle.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(items, key = { it.first.id }) { (exercise, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(stringResource(exercise.type.labelRes)) },
                        trailingContent = selected?.let { { Checkbox(checked = exercise.id in it, onCheckedChange = null) } },
                        modifier = if (isClickable(exercise)) Modifier.clickable { onClick(exercise) } else Modifier,
                    )
                }
            }
        }
    }
}
