package com.keshtoim.forge.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.BodyMeasurement
import com.keshtoim.forge.data.series
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.profile.ProfileViewModel
import com.keshtoim.forge.ui.profile.one
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun BodyLogTab(onOpen: (epochDay: Long) -> Unit, viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val measurements = state?.measurements ?: return
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val kg = stringResource(R.string.unit_kg)
    // The change is against the previous weigh-in, which may be several entries back.
    val previousWeight = measurements.runningFold<_, Double?>(null) { last, m -> m.weightKg ?: last }
    val rows = measurements.indices.reversed().map { measurements[it] to previousWeight[it] }

    Box(Modifier.fillMaxSize()) {
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.body_log_empty),
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (measurements.isNotEmpty()) item(key = "trend") { TrendCard(measurements) }
            items(rows, key = { (entry, _) -> entry.id }) { (entry, previous) ->
                val girths = entry.values().filter { it.first != BodyParam.WEIGHT }
                ForgeCard(Modifier.fillMaxWidth(), onClick = { onOpen(entry.epochDay) }) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                LocalDate.ofEpochDay(entry.epochDay).format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEE", locale)),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            val weight = entry.weightKg
                            if (weight != null) {
                                previous?.let { weight - it }?.takeIf { abs(it) >= 0.05 }?.let { delta ->
                                    Text(
                                        (if (delta > 0) "+" else "−") + one(abs(delta)),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                                Text(one(weight) + " " + kg, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (girths.isNotEmpty()) {
                            Text(
                                girths.joinToString(" · ") { (param, value) ->
                                    context.getString(param.labelRes).lowercase(locale) + " " + one(value)
                                } + " " + context.getString(R.string.unit_cm),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onOpen(LocalDate.now().toEpochDay()) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.measure_add)) },
            shape = CircleShape,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

private enum class Period(@StringRes val labelRes: Int, val months: Long?) {
    MONTH(R.string.period_month, 1),
    QUARTER(R.string.period_quarter, 3),
    YEAR(R.string.period_year, 12),
    ALL(R.string.period_all, null),
}

@Composable
private fun TrendCard(measurements: List<BodyMeasurement>) {
    val colors = MaterialTheme.colorScheme
    val available = BodyParam.entries.filter { param -> measurements.any { param.get(it) != null } }
    var picked by rememberSaveable { mutableStateOf(BodyParam.WEIGHT) }
    var period by rememberSaveable { mutableStateOf(Period.QUARTER) }
    // The picked parameter may have lost its last value after an entry was deleted.
    val param = picked.takeIf { it in available } ?: available.first()
    val from = period.months?.let { LocalDate.now().minusMonths(it).toEpochDay() } ?: Long.MIN_VALUE
    val points = measurements.series(param.get).filter { it.epochDay >= from }
    val unit = stringResource(param.unitRes)

    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.trend_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (points.size >= 2) {
                    val change = points.last().value - points.first().value
                    Text(
                        stringResource(R.string.trend_change, (if (change > 0) "+" else if (change < 0) "−" else "") + one(abs(change)) + " " + unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (available.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    available.forEach { entry ->
                        FilterChip(
                            selected = entry == param,
                            onClick = { picked = entry },
                            label = { Text(stringResource(entry.labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == period,
                        onClick = { period = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, Period.entries.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = colors.primaryContainer, activeContentColor = colors.onPrimaryContainer),
                        icon = {},
                    ) { FitText(stringResource(entry.labelRes)) }
                }
            }
            if (points.size >= 2) {
                ReadingsChart(points)
            } else {
                Text(stringResource(R.string.trend_need_more), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}
