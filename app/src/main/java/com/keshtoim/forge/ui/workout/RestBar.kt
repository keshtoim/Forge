package com.keshtoim.forge.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.rest.Rest

@Composable
fun RestBar(rest: Rest, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val now = rememberNow()
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            LinearProgressIndicator(
                progress = { ((rest.endsAt - now).toFloat() / (rest.totalSec * 1000)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.rest_title), style = MaterialTheme.typography.labelMedium)
                    Text(rememberRemaining(rest.endsAt), style = MaterialTheme.typography.headlineSmall)
                }
                TextButton(onClick = { onAdjust(-15) }) { Text(stringResource(R.string.rest_sub)) }
                TextButton(onClick = { onAdjust(15) }) { Text(stringResource(R.string.rest_add)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.rest_skip)) }
            }
        }
    }
}
