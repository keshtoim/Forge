package com.keshtoim.forge.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BmiCategory
import com.keshtoim.forge.data.BmiFormula
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.age
import com.keshtoim.forge.data.db.BodyWeight
import com.keshtoim.forge.data.devineWeight
import com.keshtoim.forge.data.normalWeightRange
import com.keshtoim.forge.data.robinsonWeight
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.relativeDay
import com.keshtoim.forge.ui.workout.formatNumber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private enum class Editing { HEIGHT, WEIGHT, BIRTH }

internal fun one(value: Double) = formatNumber((value * 10).roundToLong() / 10.0)

@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val state = loaded ?: return
    var editing by rememberSaveable { mutableStateOf<Editing?>(null) }
    var formula by rememberSaveable { mutableStateOf(BmiFormula.QUETELET) }

    Scaffold(topBar = { ScreenHeader(stringResource(R.string.profile_title), onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ParamsCard(state, onSex = viewModel::setSex, onEdit = { editing = it }) }
            val height = state.heightCm
            val weight = state.weightKg
            if (height != null && weight != null) {
                item { BmiCard(weight, height, formula, onFormula = { formula = it }) }
                item { OtherIndicesCard(weight, height, state.sex, formula) }
            } else {
                item {
                    ForgeCard(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.profile_bmi_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item { WeightCard(state, onRecord = { editing = Editing.WEIGHT }) }
        }
    }

    when (editing) {
        Editing.HEIGHT -> NumberDialog(
            title = stringResource(R.string.profile_height),
            unit = stringResource(R.string.unit_cm),
            initial = state.heightCm?.toString().orEmpty(),
            decimal = false,
            range = 100.0..250.0,
            onDismiss = { editing = null },
            onSave = { viewModel.setHeight(it.toInt()); editing = null },
        )
        Editing.WEIGHT -> NumberDialog(
            title = stringResource(R.string.profile_record_weight),
            unit = stringResource(R.string.unit_kg),
            initial = state.weightKg?.let(::one).orEmpty(),
            decimal = true,
            range = 20.0..400.0,
            onDismiss = { editing = null },
            onSave = { viewModel.recordWeight(it); editing = null },
        )
        Editing.BIRTH -> BirthDateDialog(state.birthDate, onDismiss = { editing = null }, onSave = { viewModel.setBirthDate(it); editing = null })
        null -> Unit
    }
}

@Composable
private fun ParamsCard(state: ProfileState, onSex: (BodyType) -> Unit, onEdit: (Editing) -> Unit) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val colors = MaterialTheme.colorScheme
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_sex), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    listOf(BodyType.FEMALE to R.string.sex_female, BodyType.MALE to R.string.sex_male).forEachIndexed { index, (sex, label) ->
                        SegmentedButton(
                            selected = state.sex == sex,
                            onClick = { onSex(sex) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = colors.primaryContainer, activeContentColor = colors.onPrimaryContainer),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            val birth = state.birthDate
            ParamRow(
                label = stringResource(R.string.profile_age),
                hint = birth?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)) ?: stringResource(R.string.profile_not_set),
                value = birth?.let { age(it, LocalDate.now()) }?.let { pluralStringResource(R.plurals.age_years, it, it) } ?: "—",
                onClick = { onEdit(Editing.BIRTH) },
            )
            ParamRow(
                label = stringResource(R.string.profile_height),
                hint = stringResource(R.string.unit_cm),
                value = state.heightCm?.toString() ?: "—",
                onClick = { onEdit(Editing.HEIGHT) },
            )
            val last = state.weights.lastOrNull()
            ParamRow(
                label = stringResource(R.string.profile_weight),
                hint = last?.let { stringResource(R.string.profile_updated, relativeDay(context, LocalDate.ofEpochDay(it.epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).lowercase(locale)) }
                    ?: stringResource(R.string.profile_not_set),
                value = last?.let { one(it.weightKg) + " " + stringResource(R.string.unit_kg) } ?: "—",
                onClick = { onEdit(Editing.WEIGHT) },
            )
        }
    }
}

@Composable
private fun ParamRow(label: String, hint: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private val BmiFormula.labelRes get() = when (this) {
    BmiFormula.QUETELET -> R.string.bmi_quetelet
    BmiFormula.TREFETHEN -> R.string.bmi_trefethen
    BmiFormula.PONDERAL -> R.string.bmi_ponderal
}

private val BmiFormula.descriptionRes get() = when (this) {
    BmiFormula.QUETELET -> R.string.bmi_quetelet_about
    BmiFormula.TREFETHEN -> R.string.bmi_trefethen_about
    BmiFormula.PONDERAL -> R.string.bmi_ponderal_about
}

// Scale bounds per formula; each pair of neighbours is one category band.
private val BmiFormula.scale get() = when (this) {
    BmiFormula.PONDERAL -> listOf(8.0, 11.0, 15.0, 18.0)
    else -> listOf(15.0, 18.5, 25.0, 30.0, 35.0)
}

private val BmiCategory.labelRes get() = when (this) {
    BmiCategory.UNDERWEIGHT -> R.string.bmi_under
    BmiCategory.NORMAL -> R.string.bmi_normal
    BmiCategory.OVERWEIGHT -> R.string.bmi_over
    BmiCategory.OBESE -> R.string.bmi_obese
}

@Composable
private fun BmiCategory.color(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        BmiCategory.UNDERWEIGHT -> colors.primary.copy(alpha = 0.55f)
        BmiCategory.NORMAL -> colors.tertiary
        BmiCategory.OVERWEIGHT -> Color(0xFFF2A93B)
        BmiCategory.OBESE -> colors.secondary
    }
}

@Composable
private fun BmiCard(weightKg: Double, heightCm: Int, formula: BmiFormula, onFormula: (BmiFormula) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val value = formula.of(weightKg, heightCm)
    val category = formula.category(value)
    val range = normalWeightRange(heightCm)
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_bmi), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(stringResource(formula.labelRes), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(one(value), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    stringResource(category.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp).clip(CircleShape).background(category.color()).padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
            BmiScale(formula, value, Modifier.padding(top = 14.dp))
            Text(
                stringResource(R.string.profile_normal_range, heightCm, one(range.start), one(range.endInclusive)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BmiFormula.entries.forEach { entry ->
                    FilterChip(
                        selected = entry == formula,
                        onClick = { onFormula(entry) },
                        label = { Text(stringResource(entry.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryContainer,
                            selectedLabelColor = colors.onPrimaryContainer,
                        ),
                    )
                }
            }
            Text(
                stringResource(formula.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun BmiScale(formula: BmiFormula, value: Double, modifier: Modifier) {
    val bounds = formula.scale
    val bands = bounds.zipWithNext()
    val fraction = ((value - bounds.first()) / (bounds.last() - bounds.first())).coerceIn(0.0, 1.0).toFloat()
    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            Row(Modifier.fillMaxWidth().height(10.dp).align(Alignment.Center).clip(CircleShape)) {
                bands.forEach { (from, to) ->
                    Box(Modifier.weight((to - from).toFloat()).fillMaxSize().background(formula.category((from + to) / 2).color()))
                }
            }
            Box(
                Modifier
                    .offset(x = maxWidth * fraction - 9.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            bands.forEach { (from, to) ->
                Text(
                    formatNumber(from),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight((to - from).toFloat()),
                )
            }
        }
    }
}

@Composable
private fun OtherIndicesCard(weightKg: Double, heightCm: Int, sex: BodyType, selected: BmiFormula) {
    val kg = stringResource(R.string.unit_kg)
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            BmiFormula.entries.filter { it != selected }.forEach { formula ->
                IndexRow(stringResource(formula.labelRes), stringResource(formula.descriptionRes), one(formula.of(weightKg, heightCm)))
            }
            IndexRow(stringResource(R.string.ideal_devine), stringResource(R.string.ideal_hint), one(devineWeight(heightCm, sex)) + " " + kg)
            IndexRow(stringResource(R.string.ideal_robinson), stringResource(R.string.ideal_hint), one(robinsonWeight(heightCm, sex)) + " " + kg)
            Text(
                stringResource(R.string.profile_indices_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun IndexRow(name: String, hint: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WeightCard(state: ProfileState, onRecord: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val recent = state.recent(today)
    val change = state.recentChange(today)
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_weight), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (change != null) {
                    Text(
                        stringResource(R.string.profile_weight_change, (if (change > 0) "+" else if (change < 0) "−" else "") + one(abs(change))),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (recent.size >= 2) WeightChart(recent, Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = onRecord, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.profile_record_weight), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun WeightChart(points: List<BodyWeight>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val min = points.minOf { it.weightKg }
    val max = points.maxOf { it.weightKg }
    val firstDay = points.first().epochDay
    val span = (points.last().epochDay - firstDay).coerceAtLeast(1)
    val months = points.map { LocalDate.ofEpochDay(it.epochDay).month }.distinct()
    Column(modifier) {
        Row {
            Text(one(points.first().weightKg), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(one(points.last().weightKg), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 8.dp)) {
            val inset = 6.dp.toPx()
            fun at(p: BodyWeight) = Offset(
                inset + (size.width - 2 * inset) * (p.epochDay - firstDay) / span,
                inset + (size.height - 2 * inset) * (if (max > min) ((max - p.weightKg) / (max - min)).toFloat() else 0.5f),
            )
            drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val path = Path().apply {
                points.forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
            }
            drawPath(path, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val last = at(points.last())
            drawCircle(colors.surfaceContainer, 7.dp.toPx(), last)
            drawCircle(colors.primary, 5.dp.toPx(), last)
        }
        Row {
            months.forEach { month ->
                Text(
                    month.getDisplayName(TextStyle.FULL_STANDALONE, locale).lowercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun NumberDialog(
    title: String,
    unit: String,
    initial: String,
    decimal: Boolean,
    range: ClosedFloatingPointRange<Double>,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val parsed = text.replace(',', '.').toDoubleOrNull()?.takeIf { it in range }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                suffix = { Text(unit) },
                singleLine = true,
                isError = text.isNotEmpty() && parsed == null,
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = { TextButton(onClick = { parsed?.let(onSave) }, enabled = parsed != null) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateDialog(initial: LocalDate?, onDismiss: () -> Unit, onSave: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today.minusYears(25)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() < today
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}
