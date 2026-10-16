package com.keshtoim.forge.ui.more

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.ui.ForgeCard
import com.keshtoim.forge.ui.ScreenHeader

private const val REPOSITORY = "https://github.com/keshtoim/Forge"

@Composable
fun MoreScreen(onOpenProfile: () -> Unit, onOpenProgress: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val shareText = stringResource(R.string.more_share_text, REPOSITORY)
    val shareTitle = stringResource(R.string.more_share)
    // The release build takes its version from the GitHub tag, so this matches the release it came from.
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.tab_more))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ForgeCard(Modifier.fillMaxWidth()) {
                Column {
                    MenuItem(Icons.Filled.Person, stringResource(R.string.profile_title), onClick = onOpenProfile)
                    MenuItem(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.more_progress), onClick = onOpenProgress)
                    MenuItem(Icons.Filled.Settings, stringResource(R.string.settings), onClick = onOpenSettings)
                }
            }
            ForgeCard(Modifier.fillMaxWidth()) {
                Column {
                    MenuItem(
                        Icons.Filled.Star,
                        stringResource(R.string.more_star),
                        hint = stringResource(R.string.more_star_hint),
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = { uriHandler.openUri(REPOSITORY) },
                    )
                    MenuItem(Icons.Filled.Share, shareTitle, hint = stringResource(R.string.more_share_hint)) {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    }
                }
            }
            Text(
                stringResource(R.string.more_version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    hint: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = hint?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
