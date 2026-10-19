package com.keshtoim.forge.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.R
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiException
import com.keshtoim.forge.data.ai.AiModel
import com.keshtoim.forge.data.ai.AiProvider
import com.keshtoim.forge.data.ai.AiSettings
import com.keshtoim.forge.data.ai.AiStatus
import com.keshtoim.forge.ui.AiErrorText
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AiSettingsViewModel(private val settings: AiSettings, private val connection: AiConnection) : ViewModel() {
    val state = settings.state

    // The same status the workout indicator shows, with the last error and its details.
    val status = connection.status

    private var pending: Job? = null

    fun setEnabled(enabled: Boolean) {
        settings.setEnabled(enabled)
        if (enabled) loadIfMissing()
    }

    fun setProvider(provider: AiProvider) {
        settings.setProvider(provider)
        loadIfMissing()
    }

    // Checks start by themselves once typing pauses, so pasting a key or picking a model is enough.
    fun setKey(key: String) {
        val before = state.value.keys[state.value.provider].orEmpty()
        settings.setKey(state.value.provider, key)
        if (state.value.keys[state.value.provider].orEmpty() != before) check(delayMs = 800)
    }

    fun setModel(model: String) {
        settings.setModel(state.value.provider, model)
        pending?.cancel()
        pending = viewModelScope.launch {
            delay(800)
            connection.verify()
        }
    }

    fun check(delayMs: Long = 0) {
        pending?.cancel()
        if (state.value.config == null) return
        pending = viewModelScope.launch {
            delay(delayMs)
            // A failure lands in [status]; there is nothing more to do with it here.
            try {
                connection.check()
            } catch (_: AiException) {
            }
        }
    }

    private fun loadIfMissing() {
        if (state.value.catalogs[state.value.provider].isNullOrEmpty()) check() else connection.verify()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ForgeApplication
                AiSettingsViewModel(app.aiSettings, app.aiConnection)
            }
        }
    }
}

@Composable
fun AiSettingsScreen(onBack: () -> Unit, viewModel: AiSettingsViewModel = viewModel(factory = AiSettingsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    var showKey by rememberSaveable { mutableStateOf(false) }
    val provider = state.provider

    Scaffold(topBar = { ScreenHeader(stringResource(R.string.ai_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ForgeCard(Modifier.fillMaxWidth()) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.ai_enable)) },
                    supportingContent = { Text(stringResource(R.string.ai_enable_hint)) },
                    trailingContent = { Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled) },
                )
            }
            if (!state.enabled) return@Column
            ForgeCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ai_provider), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        AiProvider.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = option == provider,
                                onClick = { viewModel.setProvider(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, AiProvider.entries.size),
                                icon = {},
                            ) { FitText(option.title) }
                        }
                    }
                    OutlinedTextField(
                        value = state.keys[provider].orEmpty(),
                        onValueChange = viewModel::setKey,
                        label = { Text(stringResource(R.string.ai_key, provider.title)) },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = stringResource(if (showKey) R.string.ai_key_hide else R.string.ai_key_show),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { uriHandler.openUri(provider.keyUrl) }) { Text(stringResource(R.string.ai_key_get, provider.title)) }
                    ModelPicker(
                        typed = state.models[provider].orEmpty(),
                        default = provider.defaultModel,
                        choices = state.choices(provider),
                        loaded = !state.catalogs[provider].isNullOrEmpty(),
                        onChange = viewModel::setModel,
                    )
                    TextButton(onClick = { uriHandler.openUri(provider.modelsUrl) }) { Text(stringResource(R.string.ai_models_docs, provider.title)) }
                    Button(
                        onClick = { viewModel.check() },
                        enabled = state.config != null && status != AiStatus.Checking,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.ai_check)) }
                    StatusRow(status, state.model(provider))
                }
            }
            Text(
                stringResource(R.string.ai_privacy, provider.title),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun StatusRow(status: AiStatus, model: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (status) {
            AiStatus.Off -> Text(stringResource(R.string.ai_status_no_key), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AiStatus.Offline -> Text(stringResource(R.string.ai_error_connection), style = MaterialTheme.typography.bodyMedium, color = colors.error)
            AiStatus.Checking -> {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.ai_checking), style = MaterialTheme.typography.bodyMedium)
            }
            AiStatus.Ready -> Text(stringResource(R.string.ai_check_ok, model), style = MaterialTheme.typography.bodyMedium, color = colors.primary)
            is AiStatus.Failed -> AiErrorText(status.error)
        }
    }
}

// An editable dropdown: pick one of the listed models or type any id the provider accepts.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(typed: String, default: String, choices: List<AiModel>, loaded: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Typing narrows the list; after a pick the whole list shows again on the next open.
    var filtering by remember { mutableStateOf(false) }
    val query = typed.trim().lowercase()
    val shown = if (filtering && query.isNotEmpty()) choices.filter { query in it.id.lowercase() || query in it.name.lowercase() } else choices
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = typed,
            onValueChange = {
                onChange(it)
                filtering = true
                expanded = true
            },
            label = { Text(stringResource(R.string.ai_model)) },
            placeholder = { Text(default) },
            supportingText = {
                Text(if (loaded) pluralStringResource(R.plurals.ai_models_found, choices.size, choices.size) else stringResource(R.string.ai_model_hint, default))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        if (shown.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                shown.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.name, style = MaterialTheme.typography.bodyLarge)
                                if (model.name != model.id) {
                                    Text(model.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        onClick = {
                            onChange(model.id)
                            filtering = false
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
