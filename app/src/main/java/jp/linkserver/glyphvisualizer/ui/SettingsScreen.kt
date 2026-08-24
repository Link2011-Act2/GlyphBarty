package jp.linkserver.glyphvisualizer.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isUpdateCheckIntervalIgnoredForTesting
import jp.linkserver.glyphvisualizer.update.setShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.setUpdateCheckIntervalIgnoredForTesting

private enum class MeterStyleMode {
    HIDDEN,
    LIGHTWEIGHT,
    SPECTRUM,
    CLASSIC,
    FAITHFUL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
    onExperimentalFeatures: () -> Unit,
    mediaProjectionEnabled: Boolean,
    onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    glyphMeterPreviewEnabled: Boolean,
    onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    meterVisibleEnabled: Boolean,
    onMeterVisibleEnabledChanged: (Boolean) -> Unit,
    lightweightMeterEnabled: Boolean,
    onLightweightMeterEnabledChanged: (Boolean) -> Unit,
    spectrumMeterEnabled: Boolean,
    onSpectrumMeterEnabledChanged: (Boolean) -> Unit,
    onMeterStyleChanged: (visible: Boolean, lightweight: Boolean, spectrum: Boolean, faithful: Boolean) -> Unit,
    nativeMeterViewEnabled: Boolean,
    onNativeMeterViewEnabledChanged: (Boolean) -> Unit,
    automaticUpdateCheckEnabled: Boolean,
    onAutomaticUpdateCheckEnabledChanged: (Boolean) -> Unit,
    mediaPlaybackOnlyEnabled: Boolean,
    onMediaPlaybackOnlyEnabledChanged: (Boolean) -> Unit,
    experimentalVisualizerStabilizationEnabled: Boolean,
    onExperimentalVisualizerStabilizationEnabledChanged: (Boolean) -> Unit,
    experimentalVisualizerSignalWatchdogEnabled: Boolean,
    onExperimentalVisualizerSignalWatchdogEnabledChanged: (Boolean) -> Unit,
    showAutoEnablePhone1GlyphDebugOnStart: Boolean,
    autoEnablePhone1GlyphDebugOnStart: Boolean,
    onAutoEnablePhone1GlyphDebugOnStartChanged: (Boolean) -> Unit,
    experimentalMainUiEnabled: Boolean,
    onExperimentalMainUiEnabledChanged: (Boolean) -> Unit,
    detailedHomeEnabled: Boolean,
    onDetailedHomeEnabledChanged: (Boolean) -> Unit,
    nothingStyleEnabled: Boolean,
    onNothingStyleEnabledChanged: (Boolean) -> Unit
) {
    val settingsTitleFontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
    val context = LocalContext.current
    val openFailedText = stringResource(R.string.about_language_open_failed)
    val nothingLabel = stringResource(R.string.settings_ui_mode_nothing)
    val materialLabel = stringResource(R.string.settings_ui_mode_material)
    val compactHomeLabel = stringResource(R.string.settings_home_screen_compact)
    val detailedHomeLabel = stringResource(R.string.settings_home_screen_detailed)
    var localMediaProjectionEnabled by rememberSaveable { mutableStateOf(mediaProjectionEnabled) }
    var localGlyphMeterPreviewEnabled by rememberSaveable { mutableStateOf(glyphMeterPreviewEnabled) }
    var localMeterVisibleEnabled by rememberSaveable { mutableStateOf(meterVisibleEnabled) }
    var localLightweightMeterEnabled by rememberSaveable { mutableStateOf(lightweightMeterEnabled) }
    var localSpectrumMeterEnabled by rememberSaveable { mutableStateOf(spectrumMeterEnabled) }
    var localNativeMeterViewEnabled by rememberSaveable { mutableStateOf(nativeMeterViewEnabled) }
    var localAutomaticUpdateCheckEnabled by rememberSaveable { mutableStateOf(automaticUpdateCheckEnabled) }
    var localShowLatestForTesting by rememberSaveable {
        mutableStateOf(isShowLatestReleaseForTestingEnabled(context))
    }
    var localIgnoreCheckIntervalForTesting by rememberSaveable {
        mutableStateOf(isUpdateCheckIntervalIgnoredForTesting(context))
    }
    var localMediaPlaybackOnlyEnabled by rememberSaveable { mutableStateOf(mediaPlaybackOnlyEnabled) }
    var localExperimentalVisualizerStabilizationEnabled by rememberSaveable {
        mutableStateOf(experimentalVisualizerStabilizationEnabled)
    }
    var localExperimentalVisualizerSignalWatchdogEnabled by rememberSaveable {
        mutableStateOf(experimentalVisualizerSignalWatchdogEnabled)
    }
    var localAutoEnablePhone1GlyphDebugOnStart by rememberSaveable {
        mutableStateOf(autoEnablePhone1GlyphDebugOnStart)
    }
    var localLegacyUiEnabled by rememberSaveable {
        mutableStateOf(!experimentalMainUiEnabled)
    }
    var localDetailedHomeEnabled by rememberSaveable {
        mutableStateOf(detailedHomeEnabled)
    }
    var localNothingStyleEnabled by rememberSaveable { mutableStateOf(nothingStyleEnabled) }
    var showUiModeDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeScreenDialog by rememberSaveable { mutableStateOf(false) }
    var showMeterStyleDialog by rememberSaveable { mutableStateOf(false) }
    val meterStyleMode = when {
        !localMeterVisibleEnabled -> MeterStyleMode.HIDDEN
        localLightweightMeterEnabled -> MeterStyleMode.LIGHTWEIGHT
        localSpectrumMeterEnabled -> MeterStyleMode.SPECTRUM
        localGlyphMeterPreviewEnabled -> MeterStyleMode.FAITHFUL
        else -> MeterStyleMode.CLASSIC
    }
    val meterStyleSummary = when (meterStyleMode) {
        MeterStyleMode.HIDDEN -> stringResource(R.string.settings_meter_style_hidden_title)
        MeterStyleMode.LIGHTWEIGHT -> stringResource(R.string.settings_meter_style_lightweight_title)
        MeterStyleMode.SPECTRUM -> stringResource(R.string.settings_meter_style_spectrum_title)
        MeterStyleMode.CLASSIC -> stringResource(R.string.settings_meter_style_classic_title)
        MeterStyleMode.FAITHFUL -> stringResource(R.string.settings_meter_style_faithful_title)
    }
    val intDevBuild = rememberSaveable { isIntDevBuild() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationAccessGranted by remember {
        mutableStateOf(MediaSessionPlaybackGate.hasNotificationAccess(context))
    }
    var appNotificationsEnabled by remember {
        mutableStateOf(areAppNotificationsEnabled(context))
    }
    val mediaPlaybackOnlyDescription = if (intDevBuild) {
        val permissionStatus = if (notificationAccessGranted) {
            stringResource(R.string.settings_media_playback_only_permission_status_granted)
        } else {
            stringResource(R.string.settings_media_playback_only_permission_status_not_granted)
        }
        "${stringResource(R.string.settings_media_playback_only_desc)}\n$permissionStatus"
    } else {
        stringResource(R.string.settings_media_playback_only_desc)
    }

    DisposableEffect(lifecycleOwner, context, intDevBuild) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appNotificationsEnabled = areAppNotificationsEnabled(context)
                if (intDevBuild) {
                    notificationAccessGranted = MediaSessionPlaybackGate.hasNotificationAccess(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mediaProjectionEnabled) {
        localMediaProjectionEnabled = mediaProjectionEnabled
    }
    LaunchedEffect(glyphMeterPreviewEnabled) {
        localGlyphMeterPreviewEnabled = glyphMeterPreviewEnabled
    }
    LaunchedEffect(meterVisibleEnabled) {
        localMeterVisibleEnabled = meterVisibleEnabled
    }
    LaunchedEffect(lightweightMeterEnabled) {
        localLightweightMeterEnabled = lightweightMeterEnabled
    }
    LaunchedEffect(spectrumMeterEnabled) {
        localSpectrumMeterEnabled = spectrumMeterEnabled
    }
    LaunchedEffect(nativeMeterViewEnabled) {
        localNativeMeterViewEnabled = nativeMeterViewEnabled
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
    LaunchedEffect(experimentalVisualizerSignalWatchdogEnabled) {
        localExperimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled
    }
    LaunchedEffect(autoEnablePhone1GlyphDebugOnStart) {
        localAutoEnablePhone1GlyphDebugOnStart = autoEnablePhone1GlyphDebugOnStart
    }
    LaunchedEffect(experimentalMainUiEnabled) {
        localLegacyUiEnabled = !experimentalMainUiEnabled
    }
    LaunchedEffect(detailedHomeEnabled) {
        localDetailedHomeEnabled = detailedHomeEnabled
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
    if (showHomeScreenDialog) {
        HomeScreenDialog(
            detailedHomeEnabled = localDetailedHomeEnabled,
            compactLabel = compactHomeLabel,
            detailedLabel = detailedHomeLabel,
            onDismiss = { showHomeScreenDialog = false },
            onOptionSelected = { enabled ->
                localDetailedHomeEnabled = enabled
                onDetailedHomeEnabledChanged(enabled)
                showHomeScreenDialog = false
            }
        )
    }
    if (showMeterStyleDialog) {
        MeterStyleDialog(
            selectedMode = meterStyleMode,
            onDismiss = { showMeterStyleDialog = false },
            onOptionSelected = { mode ->
                val visible = mode != MeterStyleMode.HIDDEN
                val lightweight = mode == MeterStyleMode.LIGHTWEIGHT
                val spectrum = mode == MeterStyleMode.SPECTRUM
                val faithful = mode == MeterStyleMode.FAITHFUL || spectrum
                localMeterVisibleEnabled = visible
                localLightweightMeterEnabled = lightweight
                localSpectrumMeterEnabled = spectrum
                localGlyphMeterPreviewEnabled = faithful
                onMeterStyleChanged(visible, lightweight, spectrum, faithful)
                showMeterStyleDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_screen_title),
                onBack = onBack,
                titleFontFamily = settingsTitleFontFamily,
                nothingStyleEnabled = localNothingStyleEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
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
                SettingsDividerGap()
                SettingsEntry(
                    title = stringResource(R.string.settings_notifications_title),
                    description = stringResource(
                        if (appNotificationsEnabled) {
                            R.string.settings_notifications_desc_enabled
                        } else {
                            R.string.settings_notifications_desc_disabled
                        }
                    ),
                    onClick = {
                        if (!openAppNotificationSettings(context)) {
                            Toast.makeText(
                                context,
                                R.string.settings_notifications_open_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = if (showAutoEnablePhone1GlyphDebugOnStart) {
                        SettingsGroupPosition.Middle
                    } else {
                        SettingsGroupPosition.Bottom
                    }
                )
                if (showAutoEnablePhone1GlyphDebugOnStart) {
                    SettingsDividerGap()
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
                title = stringResource(R.string.settings_category_display)
            ) {
                SettingsEntry(
                    title = stringResource(R.string.settings_ui_mode_title),
                    description = if (localNothingStyleEnabled) nothingLabel else materialLabel,
                    onClick = { showUiModeDialog = true },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Top
                )
                SettingsDividerGap()
                SettingsEntry(
                    title = stringResource(R.string.settings_home_screen_title),
                    description = if (localDetailedHomeEnabled) {
                        detailedHomeLabel
                    } else {
                        compactHomeLabel
                    },
                    onClick = { showHomeScreenDialog = true },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Middle,
                    enabled = !localLegacyUiEnabled
                )
                SettingsDividerGap()
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_experimental_main_ui_title),
                    description = stringResource(R.string.settings_experimental_main_ui_desc),
                    checked = localLegacyUiEnabled,
                    onCheckedChange = { checked ->
                        localLegacyUiEnabled = checked
                        onExperimentalMainUiEnabledChanged(!checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Middle
                )
                SettingsDividerGap()
                SettingsEntry(
                    title = stringResource(R.string.settings_meter_style_title),
                    description = meterStyleSummary,
                    onClick = { showMeterStyleDialog = true },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Middle
                )
                SettingsDividerGap()
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_native_meter_view_title),
                    description = stringResource(R.string.settings_native_meter_view_desc),
                    checked = localNativeMeterViewEnabled,
                    onCheckedChange = { checked ->
                        localNativeMeterViewEnabled = checked
                        onNativeMeterViewEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Bottom
                )
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_playback)
            ) {
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_media_playback_only_title),
                    description = mediaPlaybackOnlyDescription,
                    checked = localMediaPlaybackOnlyEnabled,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            localMediaPlaybackOnlyEnabled = false
                        }
                        onMediaPlaybackOnlyEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Top
                )
                SettingsDividerGap()
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
                SettingsDividerGap()
                SettingsToggleEntry(
                    title = stringResource(R.string.settings_visualizer_signal_watchdog_title),
                    description = stringResource(R.string.settings_visualizer_signal_watchdog_desc),
                    checked = localExperimentalVisualizerSignalWatchdogEnabled,
                    onCheckedChange = { checked ->
                        localExperimentalVisualizerSignalWatchdogEnabled = checked
                        onExperimentalVisualizerSignalWatchdogEnabledChanged(checked)
                    },
                    nothingStyle = localNothingStyleEnabled,
                    position = SettingsGroupPosition.Middle
                )
                SettingsDividerGap()
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
                    position = SettingsGroupPosition.Single
                )
            }

            if (intDevBuild) {
                SettingsCategory(
                    title = stringResource(R.string.settings_category_developer)
                ) {
                    SettingsEntry(
                        title = stringResource(R.string.settings_experimental_features_title),
                        description = stringResource(R.string.settings_experimental_features_desc),
                        onClick = onExperimentalFeatures,
                        nothingStyle = localNothingStyleEnabled,
                        position = SettingsGroupPosition.Top
                    )
                    SettingsDividerGap()
                    SettingsToggleEntry(
                        title = stringResource(R.string.about_update_test_mode_title),
                        description = stringResource(R.string.about_update_test_mode_desc),
                        checked = localShowLatestForTesting,
                        onCheckedChange = { enabled ->
                            localShowLatestForTesting = enabled
                            setShowLatestReleaseForTestingEnabled(context, enabled)
                        },
                        nothingStyle = localNothingStyleEnabled,
                        position = SettingsGroupPosition.Middle
                    )
                    SettingsDividerGap()
                    SettingsToggleEntry(
                        title = stringResource(R.string.about_update_ignore_interval_test_title),
                        description = stringResource(R.string.about_update_ignore_interval_test_desc),
                        checked = localIgnoreCheckIntervalForTesting,
                        onCheckedChange = { enabled ->
                            localIgnoreCheckIntervalForTesting = enabled
                            setUpdateCheckIntervalIgnoredForTesting(context, enabled)
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

private fun areAppNotificationsEnabled(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    return manager.areNotificationsEnabled()
}

private fun openAppNotificationSettings(context: Context): Boolean {
    return runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }.isSuccess
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
    titleFontFamily: FontFamily?,
    nothingStyleEnabled: Boolean
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
                fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold,
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
private fun HomeScreenDialog(
    detailedHomeEnabled: Boolean,
    compactLabel: String,
    detailedLabel: String,
    onDismiss: () -> Unit,
    onOptionSelected: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_home_screen_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                UiModeOption(
                    label = compactLabel,
                    selected = !detailedHomeEnabled,
                    onClick = { onOptionSelected(false) }
                )
                UiModeOption(
                    label = detailedLabel,
                    selected = detailedHomeEnabled,
                    onClick = { onOptionSelected(true) }
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
private fun MeterStyleDialog(
    selectedMode: MeterStyleMode,
    onDismiss: () -> Unit,
    onOptionSelected: (MeterStyleMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_meter_style_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MeterStyleOption(
                    title = stringResource(R.string.settings_meter_style_hidden_title),
                    description = stringResource(R.string.settings_meter_style_hidden_desc),
                    selected = selectedMode == MeterStyleMode.HIDDEN,
                    onClick = { onOptionSelected(MeterStyleMode.HIDDEN) }
                )
                MeterStyleOption(
                    title = stringResource(R.string.settings_meter_style_spectrum_title),
                    description = stringResource(R.string.settings_meter_style_spectrum_desc),
                    selected = selectedMode == MeterStyleMode.SPECTRUM,
                    onClick = { onOptionSelected(MeterStyleMode.SPECTRUM) }
                )
                MeterStyleOption(
                    title = stringResource(R.string.settings_meter_style_lightweight_title),
                    description = stringResource(R.string.settings_meter_style_lightweight_desc),
                    selected = selectedMode == MeterStyleMode.LIGHTWEIGHT,
                    onClick = { onOptionSelected(MeterStyleMode.LIGHTWEIGHT) }
                )
                MeterStyleOption(
                    title = stringResource(R.string.settings_meter_style_classic_title),
                    description = stringResource(R.string.settings_meter_style_classic_desc),
                    selected = selectedMode == MeterStyleMode.CLASSIC,
                    onClick = { onOptionSelected(MeterStyleMode.CLASSIC) }
                )
                MeterStyleOption(
                    title = stringResource(R.string.settings_meter_style_faithful_title),
                    description = stringResource(R.string.settings_meter_style_faithful_desc),
                    selected = selectedMode == MeterStyleMode.FAITHFUL,
                    onClick = { onOptionSelected(MeterStyleMode.FAITHFUL) }
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
private fun MeterStyleOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
