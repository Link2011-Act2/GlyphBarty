package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExtrasScreen(
    nothingStyleEnabled: Boolean,
    batteryGlyphEnabled: Boolean,
    batteryGlyphSupported: Boolean,
    onBatteryGlyphEnabledChanged: (Boolean) -> Unit,
    syncWithNothingOsGlyphSettingEnabled: Boolean,
    onSyncWithNothingOsGlyphSettingEnabledChanged: (Boolean) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = stringResource(R.string.menu_extras),
                        fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_menu)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            SettingsToggleEntry(
                title = stringResource(R.string.extras_battery_glyph_title),
                description = stringResource(
                    if (batteryGlyphSupported) {
                        R.string.extras_battery_glyph_description
                    } else {
                        R.string.extras_battery_glyph_unsupported
                    }
                ),
                checked = batteryGlyphEnabled && batteryGlyphSupported,
                onCheckedChange = onBatteryGlyphEnabledChanged,
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Top,
                enabled = batteryGlyphSupported
            )
            SettingsDividerGap()
            SettingsToggleEntry(
                title = stringResource(R.string.extras_sync_nothing_os_glyph_title),
                description = stringResource(R.string.extras_sync_nothing_os_glyph_description),
                checked = syncWithNothingOsGlyphSettingEnabled,
                onCheckedChange = onSyncWithNothingOsGlyphSettingEnabledChanged,
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Bottom
            )
        }
    }
}
