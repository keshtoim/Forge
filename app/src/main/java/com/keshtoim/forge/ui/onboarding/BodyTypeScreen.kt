package com.keshtoim.forge.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.ui.exercises.LocalBodyType
import com.keshtoim.forge.ui.exercises.MuscleMap
import com.keshtoim.forge.ui.labelRes

private val PREVIEW = mapOf(MuscleGroup.CHEST to 3, MuscleGroup.SHOULDERS to 2, MuscleGroup.GLUTES to 2, MuscleGroup.QUADS to 1)

@Composable
fun BodyTypeScreen(onPick: (BodyType) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Nothing is preselected so the choice is never nudged.
    var selected by rememberSaveable { mutableStateOf<BodyType?>(null) }

    // Shown before the app's Scaffold exists, so this Surface is what gives the text its theme colour.
    Surface(color = colors.background, contentColor = colors.onBackground) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.body_type_step),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onSecondaryContainer,
                modifier = Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                stringResource(R.string.body_type_title),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                stringResource(R.string.body_type_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(Modifier.padding(top = 28.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyType.entries.forEach { type ->
                    FigureCard(type, selected == type, Modifier.weight(1f)) { selected = type }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { selected?.let(onPick) },
                enabled = selected != null,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(stringResource(R.string.home_go), style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.04.em))
            }
        }
    }
}

@Composable
private fun FigureCard(type: BodyType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Surface(
        shape = shape,
        color = colors.surfaceContainer,
        border = BorderStroke(2.dp, if (selected) colors.primary else Color.Transparent),
        shadowElevation = if (selected) 6.dp else 1.dp,
        modifier = modifier.clip(shape).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompositionLocalProvider(LocalBodyType provides type) {
                    MuscleMap(PREVIEW, Modifier.fillMaxWidth())
                }
                Text(
                    stringResource(type.labelRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
