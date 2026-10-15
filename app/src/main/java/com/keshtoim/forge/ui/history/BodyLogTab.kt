package com.keshtoim.forge.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.profile.NumberDialog
import com.keshtoim.forge.ui.profile.ProfileViewModel
import com.keshtoim.forge.ui.profile.one
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun BodyLogTab(viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weights = state?.weights ?: return
    var recording by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val kg = stringResource(R.string.unit_kg)
    // Newest first, each paired with the weigh-in before it for the change.
    val rows = weights.zip(listOf(null) + weights.dropLast(1)).reversed()

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
            itemsIndexed(rows, key = { _, (entry, _) -> entry.id }) { _, (entry, previous) ->
                ForgeCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(one(entry.weightKg) + " " + kg, style = MaterialTheme.typography.titleMedium)
                            Text(
                                LocalDate.ofEpochDay(entry.epochDay).format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEE", locale)),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        previous?.let { entry.weightKg - it.weightKg }?.takeIf { abs(it) >= 0.05 }?.let { delta ->
                            Text(
                                (if (delta > 0) "+" else "−") + one(abs(delta)),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteWeight(entry) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete), tint = colors.outline)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { recording = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.profile_record_weight)) },
            shape = CircleShape,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (recording) {
        NumberDialog(
            title = stringResource(R.string.profile_record_weight),
            unit = kg,
            initial = weights.lastOrNull()?.let { one(it.weightKg) }.orEmpty(),
            decimal = true,
            range = 20.0..400.0,
            onDismiss = { recording = false },
            onSave = { viewModel.recordWeight(it); recording = false },
        )
    }
}
