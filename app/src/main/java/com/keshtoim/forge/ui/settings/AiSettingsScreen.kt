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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
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
import com.keshtoim.forge.data.ai.AiFailure
import com.keshtoim.forge.data.ai.AiProvider
import com.keshtoim.forge.data.ai.AiSettings
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.ui.FitText
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.labelRes
import kotlinx.coroutines.launch

sealed interface AiCheck {
    data object Running : AiCheck
    data object Passed : AiCheck
    data class Failed(val failure: AiFailure) : AiCheck
}

class AiSettingsViewModel(private val settings: AiSettings, private val connection: AiConnection) : ViewModel() {
    val state = settings.state

    var check by mutableStateOf<AiCheck?>(null)
        private set

    fun setEnabled(enabled: Boolean) = settings.setEnabled(enabled)

    fun setProvider(provider: AiProvider) {
        settings.setProvider(provider)
        check = null
    }

    fun setKey(key: String) {
        settings.setKey(state.value.provider, key)
        check = null
    }

    fun setModel(model: String) {
        settings.setModel(state.value.provider, model)
        check = null
    }

    fun check() {
        if (state.value.config == null) return
        check = AiCheck.Running
        viewModelScope.launch {
            check = try {
                connection.ask("Reply with the single word OK.", listOf(AiTurn(fromUser = true, text = "ping")))
                AiCheck.Passed
            } catch (e: AiException) {
                AiCheck.Failed(e.failure)
            }
        }
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
                    OutlinedTextField(
                        value = state.models[provider].orEmpty(),
                        onValueChange = viewModel::setModel,
                        label = { Text(stringResource(R.string.ai_model)) },
                        placeholder = { Text(provider.defaultModel) },
                        supportingText = { Text(stringResource(R.string.ai_model_hint, provider.defaultModel)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = viewModel::check,
                        enabled = state.config != null && viewModel.check != AiCheck.Running,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.ai_check)) }
                    viewModel.check?.let { CheckResult(it) }
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
private fun CheckResult(check: AiCheck) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (check) {
            AiCheck.Running -> {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.ai_checking), style = MaterialTheme.typography.bodyMedium)
            }
            AiCheck.Passed -> Text(stringResource(R.string.ai_check_ok), style = MaterialTheme.typography.bodyMedium, color = colors.primary)
            is AiCheck.Failed -> Text(stringResource(check.failure.labelRes), style = MaterialTheme.typography.bodyMedium, color = colors.error)
        }
    }
}
