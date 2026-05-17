package jp.linkserver.glyphvisualizer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.update.isIntDevBuild

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
    mediaProjectionEnabled: Boolean,
    onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    glyphMeterPreviewEnabled: Boolean,
    onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    automaticUpdateCheckEnabled: Boolean,
    onAutomaticUpdateCheckEnabledChanged: (Boolean) -> Unit,
    mediaPlaybackOnlyEnabled: Boolean,
    onMediaPlaybackOnlyEnabledChanged: (Boolean) -> Unit,
    experimentalVisualizerStabilizationEnabled: Boolean,
    onExperimentalVisualizerStabilizationEnabledChanged: (Boolean) -> Unit,
    showPhone1GlyphDebugControlsEverywhere: Boolean,
    onShowPhone1GlyphDebugControlsEverywhereChanged: (Boolean) -> Unit,
    showAutoEnablePhone1GlyphDebugOnStart: Boolean,
    autoEnablePhone1GlyphDebugOnStart: Boolean,
    onAutoEnablePhone1GlyphDebugOnStartChanged: (Boolean) -> Unit,
    nothingStyleEnabled: Boolean,
    onNothingStyleEnabledChanged: (Boolean) -> Unit
) {
    val settingsTitleFontFamily = FontFamily(Font(R.font.ntype82_regular))
    val context = LocalContext.current
    val openFailedText = stringResource(R.string.about_language_open_failed)
    val nothingLabel = stringResource(R.string.settings_ui_mode_nothing)
    val materialLabel = stringResource(R.string.settings_ui_mode_material)
    var localMediaProjectionEnabled by rememberSaveable { mutableStateOf(mediaProjectionEnabled) }
    var localGlyphMeterPreviewEnabled by rememberSaveable { mutableStateOf(glyphMeterPreviewEnabled) }
    var localAutomaticUpdateCheckEnabled by rememberSaveable { mutableStateOf(automaticUpdateCheckEnabled) }
    var localMediaPlaybackOnlyEnabled by rememberSaveable { mutableStateOf(mediaPlaybackOnlyEnabled) }
    var localExperimentalVisualizerStabilizationEnabled by rememberSaveable {
        mutableStateOf(experimentalVisualizerStabilizationEnabled)
    }
    var localShowPhone1GlyphDebugControlsEverywhere by rememberSaveable {
        mutableStateOf(showPhone1GlyphDebugControlsEverywhere)
    }
    var localAutoEnablePhone1GlyphDebugOnStart by rememberSaveable {
        mutableStateOf(autoEnablePhone1GlyphDebugOnStart)
    }
    var localNothingStyleEnabled by rememberSaveable { mutableStateOf(nothingStyleEnabled) }
    var showUiModeDialog by rememberSaveable { mutableStateOf(false) }
    val intDevBuild = rememberSaveable { isIntDevBuild() }

    LaunchedEffect(mediaProjectionEnabled) {
        localMediaProjectionEnabled = mediaProjectionEnabled
    }
    LaunchedEffect(glyphMeterPreviewEnabled) {
        localGlyphMeterPreviewEnabled = glyphMeterPreviewEnabled
    }
    LaunchedEffect(automaticUpdateCheckEnabled) {
        localAutomaticUpdateCheckEnabled = automaticUpdateCheckEnabled
    }
    LaunchedEffect(mediaPlaybackOnlyEnabled) {
        localMediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled
    }
    LaunchedEffect(experimentalVisualizerStabilizationEnabled) {
        localExperimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled
    }
    LaunchedEffect(showPhone1GlyphDebugControlsEverywhere) {
        localShowPhone1GlyphDebugControlsEverywhere = showPhone1GlyphDebugControlsEverywhere
    }
    LaunchedEffect(autoEnablePhone1GlyphDebugOnStart) {
        localAutoEnablePhone1GlyphDebugOnStart = autoEnablePhone1GlyphDebugOnStart
    }
    LaunchedEffect(nothingStyleEnabled) {
        localNothingStyleEnabled = nothingStyleEnabled
    }

    if (showUiModeDialog) {
        UiModeDialog(
            selectedNothingStyle = localNothingStyleEnabled,
            nothingLabel = nothingLabel,
            materialLabel = materialLabel,
            onDismiss = { showUiModeDialog = false },
            onOptionSelected = { checked ->
                localNothingStyleEnabled = checked
                onNothingStyleEnabledChanged(checked)
                showUiModeDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_screen_title),
                onBack = onBack,
                titleFontFamily = settingsTitleFontFamily
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsCategory(
                title = stringResource(R.string.settings_category_general)
            ) {
                SettingsEntry(
                    title = stringResource(R.string.settings_language_title),
                    description = stringResource(R.string.settings_language_desc),
                    onClick = {
                        val opened = openAppLanguageSettings(context)
                        if (!opened) {
                            Toast.makeText(context, openFailedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Top
                )
                SettingsDivider()
                SettingsEntry(
                    title = stringResource(R.string.settings_ui_mode_title),
                    description = if (localNothingStyleEnabled) nothingLabel else materialLabel,
                    onClick = { showUiModeDialog = true },
                    nothingStyle = localNothingStyleEnabled,
                    position = if (showAutoEnablePhone1GlyphDebugOnStart) {
                        SettingsGroupPosition.Middle
                    } else {
                        SettingsGroupPosition.Bottom
                    }
                )
                if (showAutoEnablePhone1GlyphDebugOnStart) {
                    SettingsDivider()
                    SettingsToggleEntry(
                        title = stringResource(R.string.settings_phone1_debug_auto_enable_title),
                        description = stringResource(R.string.settings_phone1_debug_auto_enable_desc),
                        checked = localAutoEnablePhone1GlyphDebugOnStart,
                        onCheckedChange = { checked ->
                            localAutoEnablePhone1GlyphDebugOnStart = checked
                            onAutoEnablePhone1GlyphDebugOnStartChanged(checked)
                        },
                        nothingStyle = localNothingStyleEnabled,
                        position = SettingsGroupPosition.Bottom
                    )
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_playback)
            ) {
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_media_playback_only_title),
                    description = stringResource(R.string.settings_media_playback_only_desc),
                    checked = localMediaPlaybackOnlyEnabled,
                    onCheckedChange = { checked ->
                        localMediaPlaybackOnlyEnabled = checked
                        onMediaPlaybackOnlyEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Top
                )
                SettingsDivider()
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_visualizer_stabilization_title),
                    description = stringResource(R.string.settings_visualizer_stabilization_desc),
                    checked = localExperimentalVisualizerStabilizationEnabled,
                    onCheckedChange = { checked ->
                        localExperimentalVisualizerStabilizationEnabled = checked
                        onExperimentalVisualizerStabilizationEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
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
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Bottom
                )
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_display)
            ) {
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_glyph_meter_preview_title),
                    description = stringResource(R.string.settings_glyph_meter_preview_desc),
                    checked = localGlyphMeterPreviewEnabled,
                    onCheckedChange = { checked ->
                        localGlyphMeterPreviewEnabled = checked
                        onGlyphMeterPreviewEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                )
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_updates)
            ) {
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_automatic_update_check_title),
                    description = stringResource(R.string.settings_automatic_update_check_desc),
                    checked = localAutomaticUpdateCheckEnabled,
                    onCheckedChange = { checked ->
                        localAutomaticUpdateCheckEnabled = checked
                        onAutomaticUpdateCheckEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = if (intDevBuild) SettingsGroupPosition.Top else SettingsGroupPosition.Single
                )
                if (intDevBuild) {
                    SettingsDivider()
                    SettingsToggleEntry(
                        title = stringResource(R.string.settings_phone1_debug_controls_title),
                        description = stringResource(R.string.settings_phone1_debug_controls_desc),
                        checked = localShowPhone1GlyphDebugControlsEverywhere,
                        onCheckedChange = { checked ->
                            localShowPhone1GlyphDebugControlsEverywhere = checked
                            onShowPhone1GlyphDebugControlsEverywhereChanged(checked)
                        },
                        nothingStyle = localNothingStyleEnabled,
                        position = SettingsGroupPosition.Bottom
                    )
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_information)
            ) {
                SettingsEntry(
                    title = stringResource(R.string.settings_about_title),
                    description = stringResource(R.string.settings_about_desc),
                    onClick = onAbout,
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
    titleFontFamily: FontFamily
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.titleLarge.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                fontWeight = FontWeight.Normal,
                fontFamily = titleFontFamily
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        }
    )
}

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.primary
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

@Composable
private fun UiModeDialog(
    selectedNothingStyle: Boolean,
    nothingLabel: String,
    materialLabel: String,
    onDismiss: () -> Unit,
    onOptionSelected: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_ui_mode_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                UiModeOption(
                    label = nothingLabel,
                    selected = selectedNothingStyle,
                    onClick = { onOptionSelected(true) }
                )
                UiModeOption(
                    label = materialLabel,
                    selected = !selectedNothingStyle,
                    onClick = { onOptionSelected(false) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun UiModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
