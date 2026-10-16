package com.keshtoim.forge.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.TraitFilter
import com.keshtoim.forge.data.db.Discipline
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.Force
import com.keshtoim.forge.data.db.Mechanics
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.traits
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
    var filter by rememberSaveable { mutableStateOf(TraitFilter()) }
    var filtering by rememberSaveable { mutableStateOf(false) }

    val grouped = remember(exercises, query, group, filter, context) {
        exercises
            .map { it to it.displayName(context) }
            .filter { (e, name) -> (group == null || e.muscleGroup == group) && filter.matches(e.traits) && name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareBy({ it.first.muscleGroup.ordinal }, { it.second.lowercase() }))
            .groupBy { it.first.muscleGroup }
    }

    Column(modifier) {
        Row(Modifier.padding(start = 18.dp, end = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.exercises_search),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { filtering = true }, modifier = Modifier.padding(start = 4.dp)) {
                BadgedBox(badge = { if (filter.size > 0) Badge { Text("${filter.size}") } }) {
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.filter_title))
                }
            }
        }
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
                        stringResource(muscle.labelRes),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 14.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { it.first.id }) { (exercise, name) ->
                    val details = listOfNotNull(exercise.traits.equipment?.let { stringResource(it.labelRes) }, stringResource(exercise.type.labelRes))
                    ExerciseRow(name, details.joinToString(" · "), selected?.let { exercise.id in it }) { onClick(exercise) }
                }
            }
        }
    }
    if (filtering) {
        TraitFilterSheet(filter, matches = grouped.values.sumOf { it.size }, onChange = { filter = it }, onDismiss = { filtering = false })
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TraitFilterSheet(filter: TraitFilter, matches: Int, onChange: (TraitFilter) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (filter.size > 0) TextButton(onClick = { onChange(TraitFilter()) }) { Text(stringResource(R.string.filter_reset)) }
            }
            ChipGroup(R.string.trait_mechanics, Mechanics.entries, filter.mechanics, { it.labelRes }) { onChange(filter.copy(mechanics = it)) }
            ChipGroup(R.string.trait_discipline, Discipline.entries, filter.discipline, { it.labelRes }) { onChange(filter.copy(discipline = it)) }
            ChipGroup(R.string.trait_equipment, Equipment.entries, filter.equipment, { it.labelRes }) { onChange(filter.copy(equipment = it)) }
            ChipGroup(R.string.trait_force, Force.entries, filter.force, { it.labelRes }) { onChange(filter.copy(force = it)) }
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp)) {
                Text(pluralStringResource(R.plurals.filter_show, matches, matches))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(@StringRes title: Int, options: List<T>, picked: Set<T>, labelRes: (T) -> Int, onChange: (Set<T>) -> Unit) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            ForgeChip(selected = option in picked, label = stringResource(labelRes(option))) {
                onChange(if (option in picked) picked - option else picked + option)
            }
        }
    }
}
