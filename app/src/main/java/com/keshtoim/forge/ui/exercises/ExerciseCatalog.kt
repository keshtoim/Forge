package com.keshtoim.forge.ui.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ForgeChip
import com.keshtoim.forge.ui.SearchField
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseCatalog(
    exercises: List<Exercise>,
    onClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
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
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.exercises_search),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ForgeChip(selected = group == null, label = stringResource(R.string.filter_all)) { group = null } }
            items(MuscleGroup.entries) { g ->
                ForgeChip(selected = group == g, label = stringResource(g.labelRes)) { group = if (group == g) null else g }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (muscle, items) ->
                stickyHeader(key = muscle) {
                    Text(
                        stringResource(muscle.labelRes).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 14.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { it.first.id }) { (exercise, name) ->
                    ExerciseRow(name, stringResource(exercise.type.labelRes), selected?.let { exercise.id in it }) { onClick(exercise) }
                }
            }
        }
    }
}

// [checked] is null when the catalog is browsed rather than used to pick exercises.
@Composable
private fun ExerciseRow(name: String, type: String, checked: Boolean?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ForgeCard(
        Modifier
            .fillMaxWidth()
            .then(if (checked == true) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.medium) else Modifier)
            .then(if (checked != null) Modifier.semantics { selected = checked } else Modifier),
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(type, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (checked != null) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (checked) colors.primary else colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
