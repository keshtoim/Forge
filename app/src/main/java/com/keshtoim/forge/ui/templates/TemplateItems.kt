package com.keshtoim.forge.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.TemplateExercise
import com.keshtoim.forge.ui.ForgeCard

// Index ranges of consecutive items that go together: a superset, or a single exercise outside one.
fun List<TemplateExercise>.runs(): List<IntRange> {
    val result = mutableListOf<IntRange>()
    var start = 0
    for (i in 1..size) {
        if (i == size || this[i].superset == null || this[i].superset != this[start].superset) {
            result += start until i
            start = i
        }
    }
    return result
}

private fun List<TemplateExercise>.runOf(index: Int) = runs().first { index in it }

// Joins the item's group with the next item's one; the joined superset takes the upper item's rounds.
fun MutableList<TemplateExercise>.linkWithNext(index: Int) {
    if (index + 1 !in indices) return
    val sets = this[index].targetSets
    (runOf(index).first..runOf(index + 1).last).forEach { this[it] = this[it].copy(superset = LINKING, targetSets = sets) }
    normalizeSupersets()
}

fun MutableList<TemplateExercise>.unlink(index: Int) {
    runOf(index).forEach { this[it] = this[it].copy(superset = null) }
}

// A superset shares its rounds, so changing one member's sets changes them all.
fun MutableList<TemplateExercise>.setSets(index: Int, sets: Int) {
    val value = sets.coerceIn(1, 20)
    runOf(index).forEach { this[it] = this[it].copy(targetSets = value) }
}

// Moves within a superset, or jumps over the whole neighbouring group, leaving its own superset.
fun MutableList<TemplateExercise>.moveItem(index: Int, delta: Int) {
    val target = index + delta
    if (target !in indices) return
    val item = this[index]
    if (item.superset != null && this[target].superset == item.superset) {
        add(target, removeAt(index))
        return
    }
    val neighbour = runOf(target)
    removeAt(index)
    add(if (delta < 0) neighbour.first else neighbour.last, item.copy(superset = null))
    normalizeSupersets()
}

fun MutableList<TemplateExercise>.removeItem(index: Int) {
    removeAt(index)
    normalizeSupersets()
}

// Renumbers groups in list order and dissolves ones left with a single exercise.
private fun MutableList<TemplateExercise>.normalizeSupersets() {
    var next = 1
    runs().forEach { run ->
        val group = if (run.count() > 1) next++ else null
        run.forEach { if (this[it].superset != group) this[it] = this[it].copy(superset = group) }
    }
}

private const val LINKING = -1

fun LazyListScope.editableExercises(items: MutableList<TemplateExercise>, name: (TemplateExercise) -> String) {
    val runs = items.runs()
    runs.forEachIndexed { r, run ->
        item(key = "run-${items[run.first].id}-${run.first}") {
            if (run.count() > 1) {
                SupersetCard(items, run, name)
            } else {
                SingleCard(items, run.first, name(items[run.first]))
            }
        }
        if (r < runs.lastIndex) {
            item(key = "link-${run.last}") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = { items.linkWithNext(run.last) },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.superset_link), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleCard(items: MutableList<TemplateExercise>, index: Int, name: String) {
    val colors = MaterialTheme.colorScheme
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge("${index + 1}", colors.surfaceContainerHigh, colors.onSurfaceVariant)
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                IconButton(onClick = { items.removeItem(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = colors.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(pluralStringResource(R.plurals.sets_total, items[index].targetSets, items[index].targetSets), R.string.set_remove, R.string.set_add) {
                    items.setSets(index, items[index].targetSets + it)
                }
                Box(Modifier.weight(1f))
                MoveButtons(items, index)
            }
        }
    }
}

@Composable
private fun SupersetCard(items: MutableList<TemplateExercise>, run: IntRange, name: (TemplateExercise) -> String) {
    val colors = MaterialTheme.colorScheme
    val rounds = items[run.first].targetSets
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(2.dp, colors.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.superset),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Stepper(pluralStringResource(R.plurals.rounds, rounds, rounds), R.string.round_remove, R.string.round_add, colors.surface) {
                    items.setSets(run.first, rounds + it)
                }
            }
            run.forEach { index ->
                Surface(shape = MaterialTheme.shapes.small, color = colors.surfaceContainer) {
                    Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Badge(supersetLetter(index - run.first), colors.primary, colors.onPrimary)
                        Text(name(items[index]), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        MoveButtons(items, index)
                        IconButton(onClick = { items.removeItem(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            TextButton(onClick = { items.unlink(run.first) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.superset_unlink))
            }
        }
    }
}

fun supersetLetter(position: Int) = ('A' + position).toString()

@Composable
private fun Badge(text: String, background: Color, content: Color) {
    Box(Modifier.size(26.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun Stepper(
    label: String,
    lessRes: Int,
    moreRes: Int,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onStep: (Int) -> Unit,
) {
    Row(Modifier.clip(CircleShape).background(background), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onStep(-1) }) { Icon(Icons.Filled.Remove, contentDescription = stringResource(lessRes)) }
        Text(label, style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { onStep(1) }) { Icon(Icons.Filled.Add, contentDescription = stringResource(moreRes)) }
    }
}

@Composable
private fun MoveButtons(items: MutableList<TemplateExercise>, index: Int) {
    IconButton(onClick = { items.moveItem(index, -1) }, enabled = index > 0) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
    }
    IconButton(onClick = { items.moveItem(index, 1) }, enabled = index < items.lastIndex) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
    }
}
