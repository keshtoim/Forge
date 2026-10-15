package com.keshtoim.forge.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshtoim.forge.R
import com.keshtoim.forge.data.BodyType
import com.keshtoim.forge.data.ThemeMode
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader
import com.keshtoim.forge.ui.labelRes
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmImport by remember { mutableStateOf(false) }
    val bodyType by viewModel.bodyType.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    fun report(ok: Boolean, @StringRes success: Int) {
        scope.launch { snackbar.showSnackbar(context.getString(if (ok) success else R.string.backup_failed)) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.export(it) { ok -> report(ok, R.string.backup_exported) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import(it) { ok -> report(ok, R.string.backup_imported) } }
    }

    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.settings), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
            val segmentColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                activeBorderColor = MaterialTheme.colorScheme.primary,
            )
            ForgeCard(Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        colors = itemColors,
                        headlineContent = { Text(stringResource(R.string.backup_export)) },
                        supportingContent = { Text(stringResource(R.string.backup_export_hint)) },
                        leadingContent = { Icon(Icons.Filled.Upload, contentDescription = null) },
                        modifier = Modifier.clickable { exportLauncher.launch("forge-backup-${LocalDate.now()}.json") },
                    )
                    ListItem(
                        colors = itemColors,
                        headlineContent = { Text(stringResource(R.string.backup_import)) },
                        supportingContent = { Text(stringResource(R.string.backup_import_hint)) },
                        leadingContent = { Icon(Icons.Filled.Download, contentDescription = null) },
                        modifier = Modifier.clickable { confirmImport = true },
                    )
                }
            }
            ForgeCard(Modifier.fillMaxWidth()) {
                ListItem(
                    colors = itemColors,
                    headlineContent = { Text(stringResource(R.string.settings_theme)) },
                    leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = mode == themeMode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                                    colors = segmentColors,
                                ) { Text(stringResource(mode.labelRes), maxLines = 1) }
                            }
                        }
                    },
                )
            }
            ForgeCard(Modifier.fillMaxWidth()) {
                ListItem(
                    colors = itemColors,
                    headlineContent = { Text(stringResource(R.string.settings_body_type)) },
                    leadingContent = { Icon(Icons.Filled.Accessibility, contentDescription = null) },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            BodyType.entries.forEachIndexed { index, type ->
                                SegmentedButton(
                                    selected = type == bodyType,
                                    onClick = { viewModel.setBodyType(type) },
                                    shape = SegmentedButtonDefaults.itemShape(index, BodyType.entries.size),
                                    colors = segmentColors,
                                ) { Text(stringResource(type.labelRes)) }
                            }
                        }
                    },
                )
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.backup_import)) },
            text = { Text(stringResource(R.string.backup_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    // Some file managers label JSON as text/plain or octet-stream.
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text(stringResource(R.string.backup_import_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
