package com.keshtoim.forge.ui.plan

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(onBack: () -> Unit, viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory)) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val list = templates ?: return
    val isNew = viewModel.planId == 0L
    val locale = Locale.getDefault()
    val colors = MaterialTheme.colorScheme
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val dateLabel = viewModel.date.format(DateTimeFormatter.ofPattern("EEE, d MMMM", locale))

    LaunchedEffect(list) {
        if (isNew && viewModel.templateId == null) viewModel.templateId = list.firstOrNull()?.template?.id
    }

    Scaffold(
        topBar = {
            ScreenHeader(stringResource(if (isNew) R.string.plan_title else R.string.plan_edit), onBack = onBack) {
                if (!isNew) {
                    IconButton(onClick = { viewModel.delete(onBack) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.plan_delete))
                    }
                }
            }
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save(onBack) },
                enabled = viewModel.templateId != null,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(56.dp),
            ) {
                Text(if (isNew) stringResource(R.string.plan_save_on, dateLabel) else stringResource(R.string.save))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Section(stringResource(R.string.plan_template)) {
                    if (list.isEmpty()) {
                        Text(
                            stringResource(R.string.plan_no_templates),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    list.forEach { t ->
                        val selected = t.template.id == viewModel.templateId
                        val sets = t.items.sumOf { it.targetSets }
                        ListItem(
                            headlineContent = { Text(t.template.name) },
                            supportingContent = {
                                Text(pluralStringResource(R.plurals.exercises_count, t.items.size, t.items.size) + " · " + pluralStringResource(R.plurals.sets_total, sets, sets))
                            },
                            leadingContent = {
                                Icon(
                                    if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (selected) colors.primary else colors.outline,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { viewModel.templateId = t.template.id },
                        )
                    }
                }
            }
            item {
                Section(stringResource(R.string.plan_date), action = stringResource(R.string.plan_calendar), onAction = { pickDate = true }) {
                    val today = LocalDate.now()
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items((0L until 14L).map { today.plusDays(it) }) { day ->
                            DateChip(day, selected = day == viewModel.date, locale) { viewModel.date = day }
                        }
                    }
                    Text(
                        dateLabel.replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                    )
                    HorizontalDivider(Modifier.padding(top = 12.dp), color = colors.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.plan_time), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { pickTime = true }) {
                            Text(viewModel.time.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            item {
                val firstDay = WeekFields.of(locale).firstDayOfWeek
                val days = (0L until 7L).map { firstDay.plus(it) }
                Section(stringResource(R.string.plan_repeat)) {
                    Text(
                        if (viewModel.repeat.isEmpty()) stringResource(R.string.plan_repeat_none)
                        else days.filter { it in viewModel.repeat }.joinToString { it.shortName(locale) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEach { day -> WeekdayToggle(day, day in viewModel.repeat, locale, Modifier.weight(1f)) { viewModel.toggleRepeat(day) } }
                    }
                }
            }
        }
    }

    if (pickDate) {
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toUtcDate() >= today
                override fun isSelectableYear(year: Int) = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.date = it.toUtcDate() }
                    pickDate = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(viewModel.time.hour, viewModel.time.minute, DateFormat.is24HourFormat(LocalContext.current))
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.time = LocalTime.of(state.hour, state.minute)
                    pickTime = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

// The date picker works in UTC midnights regardless of the device zone.
private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun DayOfWeek.shortName(locale: Locale) = getDisplayName(TextStyle.SHORT, locale).lowercase(locale)

@Composable
private fun Section(title: String, action: String? = null, onAction: () -> Unit = {}, content: @Composable () -> Unit) {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (action != null) TextButton(onClick = onAction) { Text(action) }
            }
            content()
        }
    }
}

@Composable
private fun DateChip(day: LocalDate, selected: Boolean, locale: Locale, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .size(width = 48.dp, height = 64.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colors.primary else colors.background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val content = if (selected) colors.onPrimary else colors.onSurface
        Text(day.dayOfWeek.shortName(locale), style = MaterialTheme.typography.labelMedium, color = if (selected) content else colors.onSurfaceVariant)
        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = content)
    }
}

@Composable
private fun WeekdayToggle(day: DayOfWeek, on: Boolean, locale: Locale, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        day.shortName(locale),
        style = MaterialTheme.typography.labelLarge,
        color = if (on) colors.onPrimaryContainer else colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (on) colors.primaryContainer else Color.Transparent)
            .border(if (on) 2.dp else 1.dp, if (on) colors.primary else colors.surfaceVariant, CircleShape)
            .clickable(onClick = onClick)
            .wrapContentHeight(),
    )
}
