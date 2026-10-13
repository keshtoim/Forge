package com.keshtoim.forge.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.exercises.LocalBodyType
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.labelRes

@Composable
fun BodyTypeScreen(onPick: (BodyType) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.body_type_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.body_type_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyType.entries.forEach { type ->
                    OutlinedCard(onClick = { onPick(type) }, modifier = Modifier.weight(1f)) {
                        Column(
                            Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CompositionLocalProvider(LocalBodyType provides type) {
                                MuscleMap(MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), Modifier.fillMaxWidth())
                            }
                            Text(stringResource(type.labelRes), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
