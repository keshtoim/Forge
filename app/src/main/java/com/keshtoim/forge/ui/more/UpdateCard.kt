package com.keshtoim.forge.ui.more

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.R
import com.keshtoim.forge.data.update.AppUpdate
import com.keshtoim.forge.data.update.UpdateFailure
import com.keshtoim.forge.data.update.UpdateState
import com.keshtoim.forge.data.update.Updater
import com.keshtoim.forge.ui.ForgeCard

class UpdateViewModel(val updater: Updater) : ViewModel() {
    val state = updater.state
    val autoCheck = updater.autoCheckEnabled

    companion object {
        val Factory = viewModelFactory {
            initializer { UpdateViewModel((this[APPLICATION_KEY] as ForgeApplication).updater) }
        }
    }
}

@Composable
fun UpdateCard(modifier: Modifier = Modifier, viewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val autoCheck by viewModel.autoCheck.collectAsStateWithLifecycle()
    val updater = viewModel.updater
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val allowInstall = stringResource(R.string.update_allow_install)
    val install = { update: AppUpdate ->
        // Android asks once to let Forge install apps; the user comes back and taps "Install" again.
        if (updater.canInstall()) {
            context.startActivity(updater.installIntent(update))
        } else {
            Toast.makeText(context, allowInstall, Toast.LENGTH_LONG).show()
            context.startActivity(updater.installPermissionIntent())
        }
    }

    ForgeCard(modifier.fillMaxWidth()) {
        Column(Modifier.animateContentSize()) {
            val update = when (val s = state) {
                is UpdateState.Available -> s.update
                is UpdateState.Downloading -> s.update
                is UpdateState.Ready -> s.update
                is UpdateState.Failed -> s.update
                else -> null
            }
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = if (update != null) colors.primary else colors.onSurfaceVariant) },
                headlineContent = { Text(if (update != null) stringResource(R.string.update_available, update.version) else stringResource(R.string.update_title)) },
                supportingContent = { Text(statusText(state, updater.currentVersion)) },
                trailingContent = {
                    when (state) {
                        UpdateState.Checking -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        UpdateState.Idle, UpdateState.UpToDate, is UpdateState.Failed -> if (update == null) {
                            TextButton(onClick = { updater.check() }) { Text(stringResource(R.string.update_check)) }
                        }
                        else -> Unit
                    }
                },
            )
            if (update != null) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (update.notes.isNotBlank()) ReleaseNotes(update.notes)
                    when (val s = state) {
                        is UpdateState.Downloading -> {
                            val progress = s.progress
                            if (progress != null) {
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                        }
                        is UpdateState.Ready -> Button(onClick = { install(s.update) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text(stringResource(R.string.update_install))
                        }
                        is UpdateState.Failed -> if (s.reason != UpdateFailure.SIGNATURE) {
                            Button(onClick = { updater.download(update) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Text(stringResource(R.string.update_retry))
                            }
                        }
                        else -> Button(onClick = { updater.download(update) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            // The size is unknown when the check fell back from the rate-limited API to the release page.
                            Text(
                                if (update.size > 0) stringResource(R.string.update_download, Formatter.formatShortFileSize(context, update.size))
                                else stringResource(R.string.update_download_plain),
                            )
                        }
                    }
                    TextButton(onClick = { uriHandler.openUri(update.page) }) { Text(stringResource(R.string.update_open_page)) }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                Modifier.fillMaxWidth().clickable { updater.setAutoCheck(!autoCheck) }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.update_auto), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = autoCheck, onCheckedChange = updater::setAutoCheck)
            }
        }
    }
}

@Composable
private fun statusText(state: UpdateState, version: String): String = when (state) {
    UpdateState.Idle -> stringResource(R.string.more_version, version)
    UpdateState.Checking -> stringResource(R.string.update_checking)
    UpdateState.UpToDate -> stringResource(R.string.update_latest, version)
    is UpdateState.Available -> stringResource(R.string.update_current, version)
    is UpdateState.Downloading -> state.progress?.let { stringResource(R.string.update_downloading_percent, (it * 100).toInt()) }
        ?: stringResource(R.string.update_downloading)
    is UpdateState.Ready -> stringResource(R.string.update_ready)
    is UpdateState.Failed -> stringResource(
        when (state.reason) {
            UpdateFailure.CHECK -> R.string.update_failed_check
            UpdateFailure.DOWNLOAD -> R.string.update_failed_download
            UpdateFailure.CORRUPTED -> R.string.update_failed_corrupted
            UpdateFailure.SIGNATURE -> R.string.update_failed_signature
        },
    )
}

// Release notes are Markdown from GitHub; shown as plain text, folded to a few lines until opened.
@Composable
private fun ReleaseNotes(notes: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val text = notes.lines().joinToString("\n") { it.trimStart('#', ' ').replace("**", "") }.trim()
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.update_notes_less else R.string.update_notes_more)) }
    }
}
