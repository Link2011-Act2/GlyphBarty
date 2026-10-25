package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.pyrite.settings.SettingsGroup
import jp.linkserver.pyrite.settings.SettingsMasterSwitch
import jp.linkserver.pyrite.settings.SettingsSplitSwitchItem
import jp.linkserver.pyrite.settings.SettingsSwitchItem

@Composable
internal fun MatrixTrackTitleSettingsEntry(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    SettingsSplitSwitchItem(
        title = stringResource(R.string.settings_matrix_track_title_enabled),
        summary = stringResource(R.string.settings_matrix_track_title_enabled_description),
        checked = enabled,
        onCheckedChange = onEnabledChange,
        onClick = onClick,
        modifier = Modifier.testTag("matrix_track_title_entry")
    )
}

@Composable
internal fun MatrixTrackTitleSettingsScreen(
    enabled: Boolean,
    showArtist: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onShowArtistChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    GlyphScreenScaffold(stringResource(R.string.settings_matrix_track_title_enabled), onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(44.dp)
        ) {
            SettingsMasterSwitch(
                title = stringResource(R.string.settings_matrix_track_title_enabled),
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.testTag("matrix_track_title_master")
            )
            SettingsGroup {
                item("artist") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_matrix_track_artist_enabled),
                        summary = stringResource(R.string.settings_matrix_track_artist_enabled_description),
                        checked = showArtist,
                        onCheckedChange = onShowArtistChange,
                        enabled = enabled,
                        modifier = Modifier.testTag("matrix_track_title_artist")
                    )
                }
            }
        }
    }
}
