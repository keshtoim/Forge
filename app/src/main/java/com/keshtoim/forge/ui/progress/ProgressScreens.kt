package com.keshtoim.forge.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.SetPoint
import com.keshtoim.forge.data.secondary
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.displayName
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.relativeDay
import com.keshtoim.forge.ui.theme.ForgeTheme
import com.keshtoim.forge.ui.workout.formatNumber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun ProgressScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = { ScreenHeader(stringResource(R.string.more_progress), onBack = onBack) }) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.progress_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.first.id }) { (exercise, history) ->
                ForgeCard(Modifier.fillMaxWidth(), onClick = { onOpen(exercise.id) }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.displayName(context), style = MaterialTheme.typography.titleMedium)
                            Text(
                                pluralStringResource(R.plurals.plural_workouts, history.sessions, history.sessions) + " · " + relativeDay(context, history.lastAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}


@Composable
fun ExerciseProgressScreen(onBack: () -> Unit, viewModel: ExerciseProgressViewModel = viewModel(factory = ExerciseProgressViewModel.Factory)) {
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val current = exercise ?: return
    val metrics = Metric.forType(current.type)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val metric = metrics[selected.coerceIn(metrics.indices)]
    val series = remember(sets, metric) { metric.series(sets) }
    val workouts = remember(sets) { sets.map { it.workoutId }.distinct().size }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState())) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 6.dp, top = 6.dp).size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressHeader(current, series, metric, workouts)
            ChartPanel(metric, series)
            if (metrics.size > 1) MetricTabs(metrics, selected) { selected = it }
            Records(metrics, sets, workouts)
        }
    }
}

internal fun formatValue(value: Double) = formatNumber((value * 10).roundToLong() / 10.0)

@Composable
private fun ProgressHeader(exercise: Exercise, series: List<Point>, metric: Metric, workouts: Int) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val summary = if (series.isEmpty()) {
        stringResource(R.string.progress_empty)
    } else {
        val since = Instant.ofEpochMilli(series.first().time).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        val delta = series.last().value - series.first().value
        if (series.size > 1 && delta > 0) {
            stringResource(R.string.progress_growth, since, formatValue(delta) + " " + stringResource(metric.unitRes))
        } else {
            stringResource(R.string.progress_since, since, pluralStringResource(R.plurals.plural_workouts, workouts, workouts))
        }
    }
    Surface(shape = MaterialTheme.shapes.large, color = colors.primaryContainer) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                BasicText(
                    exercise.displayName(context),
                    style = MaterialTheme.typography.headlineLarge.copy(color = colors.onSurface),
                    maxLines = 3,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 32.sp),
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            MuscleMap(exercise.muscleGroup, exercise.secondary, Modifier.width(104.dp))
        }
    }
}

@Composable
private fun ChartPanel(metric: Metric, series: List<Point>) {
    val colors = MaterialTheme.colorScheme
    val muted = ForgeTheme.panel.onPanel.copy(alpha = 0.65f)
    val recent = series.takeLast(8)
    Surface(shape = MaterialTheme.shapes.large, color = ForgeTheme.panel.panel, contentColor = ForgeTheme.panel.onPanel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(metric.labelRes), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black), color = muted)
            Row(verticalAlignment = Alignment.Bottom) {
                // The value shrinks before it squeezes the change badge into two lines.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    FitText(
                        series.lastOrNull()?.let { formatValue(it.value) } ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp, lineHeight = 0.95.em),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        stringResource(metric.unitRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = muted,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                    )
                }
                if (series.size > 1) {
                    val delta = series.last().value - series.first().value
                    Text(
                        (if (delta >= 0) "+" else "") + formatValue(delta),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        maxLines = 1,
                        softWrap = false,
                        color = colors.onSecondary,
                        modifier = Modifier.padding(bottom = 12.dp).clip(CircleShape).background(colors.secondary).padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            if (recent.isNotEmpty()) {
                Bars(recent, Modifier.padding(top = 14.dp).fillMaxWidth().height(104.dp))
                val format = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                val zone = ZoneId.systemDefault()
                val range = listOf(recent.first(), recent.last()).distinct().joinToString(" — ") { Instant.ofEpochMilli(it.time).atZone(zone).format(format) }
                Text(range, style = MaterialTheme.typography.labelSmall, color = muted, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Bars(points: List<Point>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val idle = lerp(ForgeTheme.panel.panel, ForgeTheme.panel.onPanel, 0.12f)
    // Fixed-width bars aligned to the end keep the latest workout on the right however few there are.
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.Bottom) {
        points.forEachIndexed { i, p ->
            // Bars start at 30% so the smallest value is still visible.
            val fraction = if (max == min) 1f else 0.3f + 0.7f * ((p.value - min) / (max - min)).toFloat()
            Box(
                Modifier
                    .width(30.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (i == points.lastIndex) ForgeTheme.panel.accent else idle),
            )
        }
    }
}

@Composable
private fun MetricTabs(metrics: List<Metric>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.forEachIndexed { index, m ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.surfaceContainer)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(m.labelRes),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Records(metrics: List<Metric>, sets: List<SetPoint>, workouts: Int) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.surfaceContainer to colors.onSurfaceVariant,
    )
    val records = metrics.mapNotNull { m -> m.series(sets).maxByOrNull { it.value }?.let { m to it.value } }
    if (records.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        records.forEachIndexed { i, (m, value) ->
            val (bg, fg) = palette[i % palette.size]
            RecordChip(stringResource(m.labelRes) + " · " + formatValue(value) + " " + stringResource(m.unitRes), bg, fg)
        }
        RecordChip(pluralStringResource(R.plurals.plural_workouts, workouts, workouts), colors.surfaceContainer, colors.onSurfaceVariant)
    }
}

@Composable
private fun RecordChip(text: String, background: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier.clip(CircleShape).background(background).padding(horizontal = 14.dp, vertical = 11.dp),
    )
}
