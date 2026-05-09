package jp.linkserver.glyphvisualizer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
    mediaProjectionEnabled: Boolean,
    onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    glyphMeterPreviewEnabled: Boolean,
    onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    nothingStyleEnabled: Boolean,
    onNothingStyleEnabledChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val openFailedText = stringResource(R.string.about_language_open_failed)
    var localMediaProjectionEnabled by rememberSaveable { mutableStateOf(mediaProjectionEnabled) }
    var localGlyphMeterPreviewEnabled by rememberSaveable { mutableStateOf(glyphMeterPreviewEnabled) }
    var localNothingStyleEnabled by rememberSaveable { mutableStateOf(nothingStyleEnabled) }

    LaunchedEffect(mediaProjectionEnabled) {
        localMediaProjectionEnabled = mediaProjectionEnabled
    }
    LaunchedEffect(glyphMeterPreviewEnabled) {
        localGlyphMeterPreviewEnabled = glyphMeterPreviewEnabled
    }
    LaunchedEffect(nothingStyleEnabled) {
        localNothingStyleEnabled = nothingStyleEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (nothingStyleEnabled) 18.dp else 24.dp)
        ) {
            if (!nothingStyleEnabled) {
                Text(
                    text = stringResource(R.string.settings_screen_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (nothingStyleEnabled) {
                SettingsNothingGroup {
                    SettingsRadioEntry(
                        title = stringResource(R.string.settings_ui_mode_title),
                        description = stringResource(R.string.settings_ui_mode_desc),
                        options = listOf(
                            stringResource(R.string.settings_ui_mode_nothing) to localNothingStyleEnabled,
                            stringResource(R.string.settings_ui_mode_material) to !localNothingStyleEnabled
                        ),
                        onOptionSelected = { index ->
                            val checked = index == 0
                            localNothingStyleEnabled = checked
                            onNothingStyleEnabledChanged(checked)
                        },
                        nothingStyle = true,
                        position = SettingsGroupPosition.Top
                    )
                    SettingsDivider()
                    SettingsToggleEntry(
                        title = stringResource(R.string.settings_glyph_meter_preview_title),
                        description = stringResource(R.string.settings_glyph_meter_preview_desc),
                        checked = localGlyphMeterPreviewEnabled,
                        onCheckedChange = { checked ->
                            localGlyphMeterPreviewEnabled = checked
                            onGlyphMeterPreviewEnabledChanged(checked)
                        },
                        nothingStyle = true,
                        position = SettingsGroupPosition.Middle
                    )
                    SettingsDivider()
                    SettingsToggleEntry(
                        title = stringResource(R.string.settings_media_projection_title),
                        description = stringResource(R.string.settings_media_projection_desc),
                        checked = localMediaProjectionEnabled,
                        onCheckedChange = { checked ->
                            localMediaProjectionEnabled = checked
                            onMediaProjectionEnabledChanged(checked)
                        },
                        nothingStyle = true,
                        position = SettingsGroupPosition.Middle
                    )
                    SettingsDivider()
                    SettingsEntry(
                        title = stringResource(R.string.settings_language_title),
                        description = stringResource(R.string.settings_language_desc),
                        onClick = {
                            val opened = openAppLanguageSettings(context)
                            if (!opened) {
                                Toast.makeText(context, openFailedText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        nothingStyle = true,
                        position = SettingsGroupPosition.Middle
                    )
                    SettingsDivider()
                    SettingsEntry(
                        title = stringResource(R.string.settings_about_title),
                        description = stringResource(R.string.settings_about_desc),
                        onClick = onAbout,
                        nothingStyle = true,
                        position = SettingsGroupPosition.Bottom
                    )
                }
            } else {
                SettingsRadioEntry(
                    title = stringResource(R.string.settings_ui_mode_title),
                    description = stringResource(R.string.settings_ui_mode_desc),
                    options = listOf(
                        stringResource(R.string.settings_ui_mode_nothing) to localNothingStyleEnabled,
                        stringResource(R.string.settings_ui_mode_material) to !localNothingStyleEnabled
                    ),
                    onOptionSelected = { index ->
                        val checked = index == 0
                        localNothingStyleEnabled = checked
                        onNothingStyleEnabledChanged(checked)
                    },
                    nothingStyle = false,
                    position = SettingsGroupPosition.Single
                )

                SettingsToggleEntry(
                    title = stringResource(R.string.settings_glyph_meter_preview_title),
                    description = stringResource(R.string.settings_glyph_meter_preview_desc),
                    checked = localGlyphMeterPreviewEnabled,
                    onCheckedChange = { checked ->
                        localGlyphMeterPreviewEnabled = checked
                        onGlyphMeterPreviewEnabledChanged(checked)
                    },
                    nothingStyle = false,
                    position = SettingsGroupPosition.Single
                )

                SettingsToggleEntry(
                    title = stringResource(R.string.settings_media_projection_title),
                    description = stringResource(R.string.settings_media_projection_desc),
                    checked = localMediaProjectionEnabled,
                    onCheckedChange = { checked ->
                        localMediaProjectionEnabled = checked
                        onMediaProjectionEnabledChanged(checked)
                    },
                    nothingStyle = false,
                    position = SettingsGroupPosition.Single
                )

                SettingsEntry(
                    title = stringResource(R.string.settings_language_title),
                    description = stringResource(R.string.settings_language_desc),
                    onClick = {
                        val opened = openAppLanguageSettings(context)
                        if (!opened) {
                            Toast.makeText(context, openFailedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    nothingStyle = false,
                    position = SettingsGroupPosition.Single
                )

                SettingsEntry(
                    title = stringResource(R.string.settings_about_title),
                    description = stringResource(R.string.settings_about_desc),
                    onClick = onAbout,
                    nothingStyle = false,
                    position = SettingsGroupPosition.Single
                )
            }
        }
    }
}
