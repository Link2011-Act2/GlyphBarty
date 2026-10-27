package jp.linkserver.glyphvisualizer.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.linkserver.pyrite.settings.SettingsGroup
import jp.linkserver.pyrite.settings.SettingsGroupScope
import jp.linkserver.pyrite.settings.SettingsNavigationItem
import jp.linkserver.pyrite.settings.SettingsSwitchItem
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jp.linkserver.glyphvisualizer.BLUETOOTH_STARTUP_STABILIZATION_ENABLED
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import jp.linkserver.glyphvisualizer.glyph.MatrixRecoveryTestMode
import jp.linkserver.glyphvisualizer.glyph.MatrixRecoveryTestSettings
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isUpdateCheckIntervalIgnoredForTesting
import jp.linkserver.glyphvisualizer.update.setShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.setUpdateCheckIntervalIgnoredForTesting

private fun matrixRecoveryTestTitle(mode: MatrixRecoveryTestMode): Int = when (mode) {
    MatrixRecoveryTestMode.STANDARD -> R.string.settings_matrix_recovery_test_a_title
    MatrixRecoveryTestMode.KEEP_SESSION -> R.string.settings_matrix_recovery_test_b_title
    MatrixRecoveryTestMode.TURN_OFF_ONLY -> R.string.settings_matrix_recovery_test_c_title
    MatrixRecoveryTestMode.CLOSE_ONLY -> R.string.settings_matrix_recovery_test_d_title
}

private fun matrixRecoveryTestDescription(mode: MatrixRecoveryTestMode): Int = when (mode) {
    MatrixRecoveryTestMode.STANDARD -> R.string.settings_matrix_recovery_test_a_desc
    MatrixRecoveryTestMode.KEEP_SESSION -> R.string.settings_matrix_recovery_test_b_desc
    MatrixRecoveryTestMode.TURN_OFF_ONLY -> R.string.settings_matrix_recovery_test_c_desc
    MatrixRecoveryTestMode.CLOSE_ONLY -> R.string.settings_matrix_recovery_test_d_desc
}

private enum class MeterStyleMode {
    HIDDEN,
    LIGHTWEIGHT,
    SPECTRUM,
    CLASSIC,
    FAITHFUL
}

// Keep the saved preference and callback intact so the previous behavior can be restored quickly.
internal const val NATIVE_METER_VIEW_ALWAYS_ENABLED = true
private const val NATIVE_METER_VIEW_SETTING_VISIBLE = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiDeviceProfile: GlyphDeviceProfile,
    matrixSupported: Boolean,
    matrixTrackTitleEnabled: Boolean,
    onMatrixTrackTitleEnabledChanged: (Boolean) -> Unit,
    matrixTrackArtistEnabled: Boolean,
    onMatrixTrackArtistEnabledChanged: (Boolean) -> Unit,
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
    syncWithNothingOsGlyphSettingEnabled: Boolean,
    onSyncWithNothingOsGlyphSettingEnabledChanged: (Boolean) -> Unit,
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
    var showTrackTitleSettings by rememberSaveable { mutableStateOf(false) }
    if (matrixSupported && showTrackTitleSettings) {
        BackHandler { showTrackTitleSettings = false }
        MatrixTrackTitleSettingsScreen(
            enabled = matrixTrackTitleEnabled,
            showArtist = matrixTrackArtistEnabled,
            onEnabledChange = onMatrixTrackTitleEnabledChanged,
            onShowArtistChange = onMatrixTrackArtistEnabledChanged,
            onBack = { showTrackTitleSettings = false }
        )
        return
    }
    val context = LocalContext.current
    val openFailedText = stringResource(R.string.about_language_open_failed)
    val nothingLabel = stringResource(R.string.settings_ui_mode_nothing)
    val materialLabel = stringResource(R.string.settings_ui_mode_material)
    val compactHomeLabel = stringResource(R.string.settings_home_screen_compact)
    val detailedHomeLabel = stringResource(R.string.settings_home_screen_detailed)
    var localShowLatestForTesting by rememberSaveable {
        mutableStateOf(isShowLatestReleaseForTestingEnabled(context))
    }
    var localIgnoreCheckIntervalForTesting by rememberSaveable {
        mutableStateOf(isUpdateCheckIntervalIgnoredForTesting(context))
    }
    var showUiModeDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeScreenDialog by rememberSaveable { mutableStateOf(false) }
    var showMeterStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showMatrixRecoveryTestDialog by rememberSaveable { mutableStateOf(false) }
    var matrixRecoveryTestMode by remember { mutableStateOf(MatrixRecoveryTestSettings.load(context)) }
    val matrixRecoveryTestAvailable = MatrixRecoveryTestSettings.isVisible(uiDeviceProfile)
    val meterStyleMode = when {
        !meterVisibleEnabled -> MeterStyleMode.HIDDEN
        lightweightMeterEnabled -> MeterStyleMode.LIGHTWEIGHT
        spectrumMeterEnabled -> MeterStyleMode.SPECTRUM
        glyphMeterPreviewEnabled -> MeterStyleMode.FAITHFUL
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
                matrixRecoveryTestMode = MatrixRecoveryTestSettings.load(context)
                appNotificationsEnabled = areAppNotificationsEnabled(context)
                if (intDevBuild) {
                    notificationAccessGranted = MediaSessionPlaybackGate.hasNotificationAccess(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showUiModeDialog) {
        UiModeDialog(
            selectedNothingStyle = nothingStyleEnabled,
            nothingLabel = nothingLabel,
            materialLabel = materialLabel,
            onDismiss = { showUiModeDialog = false },
            onOptionSelected = { checked ->
                onNothingStyleEnabledChanged(checked)
                showUiModeDialog = false
            }
        )
    }
    if (showHomeScreenDialog) {
        HomeScreenDialog(
            detailedHomeEnabled = detailedHomeEnabled,
            compactLabel = compactHomeLabel,
            detailedLabel = detailedHomeLabel,
            onDismiss = { showHomeScreenDialog = false },
            onOptionSelected = { enabled ->
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
                onMeterStyleChanged(visible, lightweight, spectrum, faithful)
                showMeterStyleDialog = false
            }
        )
    }

    if (matrixRecoveryTestAvailable && showMatrixRecoveryTestDialog) {
        AlertDialog(
            onDismissRequest = { showMatrixRecoveryTestDialog = false },
            title = { Text(stringResource(R.string.settings_matrix_recovery_test_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatrixRecoveryTestMode.entries.forEach { mode ->
                        MeterStyleOption(
                            title = stringResource(matrixRecoveryTestTitle(mode)),
                            description = stringResource(matrixRecoveryTestDescription(mode)),
                            selected = matrixRecoveryTestMode == mode,
                            selectableRow = true,
                            onClick = {
                                MatrixRecoveryTestSettings.save(context, mode)
                                matrixRecoveryTestMode = mode
                                showMatrixRecoveryTestDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMatrixRecoveryTestDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    GlyphScreenScaffold(stringResource(R.string.settings_screen_title), onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (matrixRecoveryTestAvailable) {
                SettingsCategory(title = stringResource(R.string.settings_matrix_recovery_test_group)) {
                    item("matrix_recovery_test_mode") {
                        SettingsNavigationItem(
                            title = stringResource(R.string.settings_matrix_recovery_test_title),
                            summary = stringResource(matrixRecoveryTestTitle(matrixRecoveryTestMode)),
                            onClick = { showMatrixRecoveryTestDialog = true }
                        )
                    }
                }
            }
            SettingsCategory(
                title = stringResource(R.string.settings_category_general)
            ) {
                item("settings_language_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_language_title),
                        summary = stringResource(R.string.settings_language_desc),
                        onClick = {
                            val opened = openAppLanguageSettings(context)
                            if (!opened) {
                                Toast.makeText(context, openFailedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                item("settings_notifications_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_notifications_title),
                        summary = stringResource(
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
                        }
                    )
                }
                if (showAutoEnablePhone1GlyphDebugOnStart) {
                    item("settings_phone1_debug_auto_enable_title") {
                        SettingsSwitchItem(
                            title = stringResource(R.string.settings_phone1_debug_auto_enable_title),
                            summary = stringResource(R.string.settings_phone1_debug_auto_enable_desc),
                            checked = autoEnablePhone1GlyphDebugOnStart,
                            onCheckedChange = onAutoEnablePhone1GlyphDebugOnStartChanged
                        )
                    }
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_display)
            ) {
                item("settings_ui_mode_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_ui_mode_title),
                        summary = if (nothingStyleEnabled) nothingLabel else materialLabel,
                        onClick = { showUiModeDialog = true }
                    )
                }
                item("settings_home_screen_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_home_screen_title),
                        summary = if (detailedHomeEnabled) {
                            detailedHomeLabel
                        } else {
                            compactHomeLabel
                        },
                        onClick = { showHomeScreenDialog = true },
                        enabled = experimentalMainUiEnabled
                    )
                }
                item("settings_experimental_main_ui_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_experimental_main_ui_title),
                        summary = stringResource(R.string.settings_experimental_main_ui_desc),
                        checked = !experimentalMainUiEnabled,
                        onCheckedChange = { checked -> onExperimentalMainUiEnabledChanged(!checked) }
                    )
                }
                item("settings_meter_style_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_meter_style_title),
                        summary = meterStyleSummary,
                        onClick = { showMeterStyleDialog = true }
                    )
                }
                if (NATIVE_METER_VIEW_SETTING_VISIBLE) {
                    item("settings_native_meter_view_title") {
                        SettingsSwitchItem(
                            title = stringResource(R.string.settings_native_meter_view_title),
                            summary = stringResource(R.string.settings_native_meter_view_desc),
                            checked = nativeMeterViewEnabled,
                            onCheckedChange = onNativeMeterViewEnabledChanged
                        )
                    }
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_playback)
            ) {
                item("extras_sync_nothing_os_glyph_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.extras_sync_nothing_os_glyph_title),
                        summary = stringResource(R.string.extras_sync_nothing_os_glyph_description),
                        checked = syncWithNothingOsGlyphSettingEnabled,
                        onCheckedChange = onSyncWithNothingOsGlyphSettingEnabledChanged
                    )
                }
                if (matrixSupported) {
                    item("settings_matrix_track_title_enabled") {
                        MatrixTrackTitleSettingsEntry(
                            enabled = matrixTrackTitleEnabled,
                            onEnabledChange = onMatrixTrackTitleEnabledChanged,
                            onClick = { showTrackTitleSettings = true }
                        )
                    }
                }
                item("settings_media_playback_only_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_media_playback_only_title),
                        summary = mediaPlaybackOnlyDescription,
                        checked = mediaPlaybackOnlyEnabled,
                        onCheckedChange = onMediaPlaybackOnlyEnabledChanged
                    )
                }
                if (BLUETOOTH_STARTUP_STABILIZATION_ENABLED) {
                    item("settings_visualizer_stabilization_title") {
                        SettingsSwitchItem(
                            title = stringResource(R.string.settings_visualizer_stabilization_title),
                            summary = stringResource(R.string.settings_visualizer_stabilization_desc),
                            checked = experimentalVisualizerStabilizationEnabled,
                            onCheckedChange = onExperimentalVisualizerStabilizationEnabledChanged
                        )
                    }
                }
                item("settings_visualizer_signal_watchdog_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_visualizer_signal_watchdog_title),
                        summary = stringResource(R.string.settings_visualizer_signal_watchdog_desc),
                        checked = experimentalVisualizerSignalWatchdogEnabled,
                        onCheckedChange = onExperimentalVisualizerSignalWatchdogEnabledChanged
                    )
                }
                item("settings_media_projection_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_media_projection_title),
                        summary = stringResource(R.string.settings_media_projection_desc),
                        checked = mediaProjectionEnabled,
                        onCheckedChange = onMediaProjectionEnabledChanged
                    )
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_updates)
            ) {
                item("settings_automatic_update_check_title") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_automatic_update_check_title),
                        summary = stringResource(R.string.settings_automatic_update_check_desc),
                        checked = automaticUpdateCheckEnabled,
                        onCheckedChange = onAutomaticUpdateCheckEnabledChanged
                    )
                }
            }

            if (intDevBuild) {
                SettingsCategory(
                    title = stringResource(R.string.settings_category_developer)
                ) {
                    item("settings_experimental_features_title") {
                        SettingsNavigationItem(
                            title = stringResource(R.string.settings_experimental_features_title),
                            summary = stringResource(R.string.settings_experimental_features_desc),
                            onClick = onExperimentalFeatures
                        )
                    }
                    item("about_update_test_mode_title") {
                        SettingsSwitchItem(
                            title = stringResource(R.string.about_update_test_mode_title),
                            summary = stringResource(R.string.about_update_test_mode_desc),
                            checked = localShowLatestForTesting,
                            onCheckedChange = { enabled ->
                                localShowLatestForTesting = enabled
                                setShowLatestReleaseForTestingEnabled(context, enabled)
                            }
                        )
                    }
                    item("about_update_ignore_interval_test_title") {
                        SettingsSwitchItem(
                            title = stringResource(R.string.about_update_ignore_interval_test_title),
                            summary = stringResource(R.string.about_update_ignore_interval_test_desc),
                            checked = localIgnoreCheckIntervalForTesting,
                            onCheckedChange = { enabled ->
                                localIgnoreCheckIntervalForTesting = enabled
                                setUpdateCheckIntervalIgnoredForTesting(context, enabled)
                            }
                        )
                    }
                }
            }

            SettingsCategory(
                title = stringResource(R.string.settings_category_information)
            ) {
                item("settings_about_title") {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_about_title),
                        summary = stringResource(R.string.settings_about_desc),
                        onClick = onAbout
                    )
                }
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
private fun SettingsCategory(
    title: String,
    content: SettingsGroupScope.() -> Unit
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
        SettingsGroup(content = content)
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
    selectableRow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(
            if (selectableRow) Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            else Modifier
        ),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = if (selectableRow) null else onClick
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
