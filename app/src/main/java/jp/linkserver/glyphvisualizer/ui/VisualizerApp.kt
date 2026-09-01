package jp.linkserver.glyphvisualizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jp.linkserver.glyphvisualizer.ui.AboutScreen
import jp.linkserver.glyphvisualizer.ui.DirectNativeDetailedMeterCanvas
import jp.linkserver.glyphvisualizer.ui.DirectNativeDetailedMeterBar
import jp.linkserver.glyphvisualizer.ui.DirectNativeMeterCanvas
import jp.linkserver.glyphvisualizer.ui.DirectNativeMeterBar
import jp.linkserver.glyphvisualizer.ui.DirectNativeMeterStats
import jp.linkserver.glyphvisualizer.ui.DirectNativeSpectrumMeterCanvas
import jp.linkserver.glyphvisualizer.ui.DirectNativeSpectrumMeterBar
import jp.linkserver.glyphvisualizer.ui.ExperimentalDetailsScreenContent
import jp.linkserver.glyphvisualizer.ui.ExperimentalDetailsSummary
import jp.linkserver.glyphvisualizer.ui.ExperimentalDetailsTab
import jp.linkserver.glyphvisualizer.ui.ExperimentalMainScreenContent
import jp.linkserver.glyphvisualizer.ui.ExtrasScreen
import jp.linkserver.glyphvisualizer.ui.GlyphInterfaceInspectorScreen
import jp.linkserver.glyphvisualizer.ui.LightweightMeterCanvas
import jp.linkserver.glyphvisualizer.ui.LightweightMeterBar
import jp.linkserver.glyphvisualizer.ui.MeterCanvas
import jp.linkserver.glyphvisualizer.ui.MeterStat
import jp.linkserver.glyphvisualizer.ui.OssLicensesScreen
import jp.linkserver.glyphvisualizer.ui.Phone1GlyphDebugControls
import jp.linkserver.glyphvisualizer.ui.SettingsScreen
import jp.linkserver.glyphvisualizer.ui.SettingsEntry
import jp.linkserver.glyphvisualizer.ui.SettingsGroupPosition
import jp.linkserver.glyphvisualizer.ui.SpectrumMeterCanvas
import jp.linkserver.glyphvisualizer.ui.SpectrumMeterBar
import jp.linkserver.glyphvisualizer.ui.UiMeterModel
import jp.linkserver.glyphvisualizer.ui.UpdateOverviewScreen
import jp.linkserver.glyphvisualizer.ui.buildUiMeterModel
import jp.linkserver.glyphvisualizer.ui.normalizedSpectrumMeterBands
import jp.linkserver.glyphvisualizer.ui.symmetricPeakDistanceSteps
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import jp.linkserver.glyphvisualizer.audio.WaveformSampler
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRenderMode
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey
import jp.linkserver.glyphvisualizer.glyph.resolveGlyphVisualTuning
import jp.linkserver.glyphvisualizer.glyph.Phone4aAsPhone4bGlyphProbe
import jp.linkserver.glyphvisualizer.ui.openNotificationAccessSettings
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import jp.linkserver.glyphvisualizer.update.AppUpdateRepository
import jp.linkserver.glyphvisualizer.update.dismissUpdateNotificationUntilNextVersion
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isUpdateNotificationDismissed
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlyphVisualizerApp(
    uiState: VisualizerUiState,
    actions: VisualizerUiActions
) {
    with(uiState) {
        with(capture) {
            with(actions) {
                with(visualTuning) {
                    with(settings) {
                        with(actions.capture) {
                            with(dialogs) {
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.about_support_site_url)
    val showPhone1GlyphDebugControls = isPhone1Device
    val intDevBuild = rememberSaveable { isIntDevBuild() }
    var screen by rememberSaveable {
        mutableStateOf(if (initialSetupPending) Screen.WELCOME else Screen.MAIN)
    }
    var settingsReturnScreen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    var selectedDetailsTab by rememberSaveable { mutableStateOf(ExperimentalDetailsTab.LIVE) }
    var drawerOpen by remember { mutableStateOf(false) }
    var startPending by rememberSaveable { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateNotification by remember { mutableStateOf<AppUpdateInfo?>(null) }
        BackHandler(enabled = drawerOpen || (screen != Screen.MAIN && screen != Screen.WELCOME)) {
        when {
            drawerOpen -> drawerOpen = false
            screen == Screen.UPDATE -> screen = Screen.ABOUT
            screen == Screen.OSS -> screen = Screen.ABOUT
            screen == Screen.ABOUT -> screen = Screen.SETTINGS
            screen == Screen.GLYPH_INSPECTOR -> screen = Screen.EXPERIMENTAL
            screen == Screen.EXPERIMENTAL -> screen = Screen.SETTINGS
            screen == Screen.SETTINGS -> screen = settingsReturnScreen
            screen == Screen.LATENCY && experimentalMainUiEnabled -> {
                screen = if (detailedHomeEnabled) Screen.MAIN else Screen.DETAILS
            }
            else -> screen = Screen.MAIN
        }
    }

    val darkTheme = isSystemInDarkTheme()
    val containerBrush = Brush.verticalGradient(
        if (nothingStyleEnabled && darkTheme) {
            listOf(
                Color(0xFF000000),
                Color(0xFF000000)
            )
        } else if (!darkTheme) {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    )
    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            startPending = false
        }
    }
    LaunchedEffect(startPending) {
        if (startPending) {
            delay(4000)
            if (!isCapturing) {
                startPending = false
            }
        }
    }
    LaunchedEffect(repositoryUrl, automaticUpdateCheckEnabled) {
        val showLatestForTesting = isShowLatestReleaseForTestingEnabled(context)
        val result = withContext(Dispatchers.IO) {
            AppUpdateRepository.checkAutomatically(
                context = context,
                enabled = automaticUpdateCheckEnabled,
                    repositoryUrl = repositoryUrl,
                    showLatestForTesting = showLatestForTesting
            )
        }
        result?.onSuccess { updateInfo ->
                if (
                    updateInfo != null &&
                    (showLatestForTesting || !isUpdateNotificationDismissed(context, updateInfo.tagName))
                ) {
                    availableUpdate = updateInfo
                    updateNotification = updateInfo
                }
        }
    }
    if (showPhone1GlyphDebugPermissionDialog) {
        AlertDialog(
            onDismissRequest = onDismissPhone1GlyphDebugPermissionDialog,
            title = { Text(stringResource(R.string.phone1_glyph_debug_dialog_title)) },
            text = { Text(stringResource(R.string.phone1_glyph_debug_dialog_body)) },
            confirmButton = {
                TextButton(onClick = onDismissPhone1GlyphDebugPermissionDialog) {
                    Text(stringResource(R.string.phone1_glyph_debug_dialog_confirm))
                }
            }
        )
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 5 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 5 } + fadeOut())
                    }
                },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.WELCOME -> WelcomeScreen(
                        nothingStyleEnabled = nothingStyleEnabled,
                        onNothingStyleEnabledChanged = { enabled ->
                            val updated = CaptureUiStore.state.copy(nothingStyleEnabled = enabled)
                            CaptureUiStore.update { updated }
                            SettingsPreferences.save(context, updated)
                        },
                        onComplete = { automaticUpdateCheckEnabled ->
                            val updated = CaptureUiStore.state.copy(
                                automaticUpdateCheckEnabled = automaticUpdateCheckEnabled
                            )
                            CaptureUiStore.update { updated }
                            SettingsPreferences.save(context, updated)
                            SettingsPreferences.markInitialSetupCompleted(context)
                            screen = Screen.MAIN
                            onInitialSetupCompleted()
                        }
                    )
                    Screen.MAIN, Screen.DETAILS -> if (
                        targetScreen == Screen.MAIN &&
                        experimentalMainUiEnabled &&
                        !detailedHomeEnabled
                    ) {
                        ExperimentalMainScreenContent(
                            heroTitle = heroTitle,
                            statusText = statusText,
                            logMessage = logMessage,
                            isCapturing = isCapturing,
                            startPending = startPending,
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            fillOtherGlyphLights = fillOtherGlyphLights,
                            phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
                            baseIndicatorEnabled = baseIndicatorEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            meterVisibleEnabled = meterVisibleEnabled,
                            lightweightMeterEnabled = lightweightMeterEnabled,
                            spectrumMeterEnabled = spectrumMeterEnabled,
                            nativeMeterViewEnabled = nativeMeterViewEnabled,
                            nothingStyleEnabled = nothingStyleEnabled,
                            showPhone1GlyphDebugControls = showPhone1GlyphDebugControls,
                            onStartClick = {
                                startPending = true
                                onStartVisualizerClick()
                            },
                            onStopClick = onStopClick,
                            onGlyphModeChanged = onGlyphModeChanged,
                            onFillOtherGlyphLightsChanged = onFillOtherGlyphLightsChanged,
                            onPhone1ClassicCSplitEnabledChanged = onPhone1ClassicCSplitEnabledChanged,
                            onRecordingLightBehaviorChanged = onRecordingLightBehaviorChanged,
                            onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
                            onOpenDetails = { screen = Screen.DETAILS },
                            onOpenMenu = { drawerOpen = true },
                            onOpenSettings = {
                                settingsReturnScreen = targetScreen
                                screen = Screen.SETTINGS
                            }
                        )
                    } else {
                        MainScreenContent(
                        containerBrush = containerBrush,
                        statusText = statusText,
                        isCapturing = isCapturing,
                        heroTitle = heroTitle,
                        level = level,
                        peak = peak,
                        spectrumBands = spectrumBands,
                        latencyMs = latencyMs,
                        sensitivity = sensitivity,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        outputGamma = outputGamma,
                        toneFocus = toneFocus,
                        smoothing = smoothing,
                        smoothingBalance = smoothingBalance,
                        autoScaleWindowSeconds = autoScaleWindowSeconds,
                        autoScaleOffset = autoScaleOffset,
                        reverseDirection = reverseDirection,
                        meterSegments = meterSegments,
                        activeMode = activeMode,
                        glyphMode = glyphMode,
                        fillOtherGlyphLights = fillOtherGlyphLights,
                        phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
                        deviceProfile = deviceProfile,
                        isPhone3Device = isPhone3Device,
                        isPhone4aProDevice = isPhone4aProDevice,
                        isPhone2aDevice = isPhone2aDevice,
                        isPhone3aDevice = isPhone3aDevice,
                        isPhone4aDevice = isPhone4aDevice,
                        isPhone1Device = showPhone1GlyphDebugControls,
                        binaryMode = binaryMode,
                        matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                        oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                        baseIndicatorEnabled = baseIndicatorEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        levelAutoScale = levelAutoScale,
                        spectrumAutoScale = spectrumAutoScale,
                        allBrightnessAutoScale = allBrightnessAutoScale,
                        experimentalAdaptiveAutoScaleEnabled = experimentalAdaptiveAutoScaleEnabled,
                        visualDynamics = visualDynamics,
                        visualDynamicsOverridden = visualDynamicsOverridden,
                        onVisualDynamicsChanged = onVisualDynamicsChanged,
                        onVisualDynamicsChangeFinished = onVisualDynamicsChangeFinished,
                        onVisualDynamicsReset = onVisualDynamicsReset,
                        mediaProjectionEnabled = mediaProjectionEnabled,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        meterVisibleEnabled = meterVisibleEnabled,
                        lightweightMeterEnabled = lightweightMeterEnabled,
                        spectrumMeterEnabled = spectrumMeterEnabled,
                        nativeMeterViewEnabled = nativeMeterViewEnabled,
                        mainScreenUiIsolationEnabled = mainScreenUiIsolationEnabled,
                        nothingStyleEnabled = nothingStyleEnabled,
                        turnOffWhenBackDown = turnOffWhenBackDown,
                        onResetParametersClick = onResetParametersClick,
                        onExportParametersClick = onExportParametersClick,
                        onImportParametersClick = onImportParametersClick,
                        onSensitivityChanged = onSensitivityChanged,
                        onNoiseGateChanged = onNoiseGateChanged,
                        onDynamicsChanged = onDynamicsChanged,
                        onOutputGammaChanged = onOutputGammaChanged,
                        onSmoothingChanged = onSmoothingChanged,
                        onSmoothingBalanceChanged = onSmoothingBalanceChanged,
                        onToneFocusChanged = onToneFocusChanged,
                        onAutoScaleWindowSecondsChanged = onAutoScaleWindowSecondsChanged,
                        onAutoScaleWindowSecondsChangeFinished = onAutoScaleWindowSecondsChangeFinished,
                        onAutoScaleOffsetChanged = onAutoScaleOffsetChanged,
                        onAutoScaleOffsetChangeFinished = onAutoScaleOffsetChangeFinished,
                        onReverseDirectionChanged = onReverseDirectionChanged,
                        onGlyphModeChanged = onGlyphModeChanged,
                        onFillOtherGlyphLightsChanged = onFillOtherGlyphLightsChanged,
                        onPhone1ClassicCSplitEnabledChanged = onPhone1ClassicCSplitEnabledChanged,
                        onBinaryModeChanged = onBinaryModeChanged,
                        onMatrixSmoothMotionEnabledChanged = onMatrixSmoothMotionEnabledChanged,
                        onOscilloscopeAutoTimeAxisEnabledChanged = onOscilloscopeAutoTimeAxisEnabledChanged,
                        onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                        onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                        onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                        onExperimentalAdaptiveAutoScaleEnabledChanged =
                            onExperimentalAdaptiveAutoScaleEnabledChanged,
                        onRecordingLightBehaviorChanged = onRecordingLightBehaviorChanged,
                        onTurnOffWhenBackDownChanged = onTurnOffWhenBackDownChanged,
                        startPending = startPending,
                        onStartVisualizerClick = {
                            startPending = true
                            onStartVisualizerClick()
                        },
                        onStartProjectionClick = {
                            startPending = true
                            onStartProjectionClick()
                        },
                        onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
                        onStopClick = onStopClick,
                        logMessage = logMessage,
                        onDismissLog = onDismissLog,
                        onOpenMenu = { drawerOpen = true },
                        onOpenSettings = {
                            settingsReturnScreen = targetScreen
                            screen = Screen.SETTINGS
                        },
                        onOpenLatency = if (
                            experimentalMainUiEnabled &&
                            (targetScreen == Screen.DETAILS || detailedHomeEnabled)
                        ) {
                            { screen = Screen.LATENCY }
                        } else {
                            null
                        },
                        onBackToExperimental = if (
                            targetScreen == Screen.DETAILS && experimentalMainUiEnabled
                        ) {
                            { screen = Screen.MAIN }
                        } else {
                            null
                        },
                        experimentalDetailsAsHome = targetScreen == Screen.MAIN &&
                            experimentalMainUiEnabled &&
                            detailedHomeEnabled,
                        initialExperimentalDetailsTab = selectedDetailsTab,
                        onExperimentalDetailsTabChanged = { selectedDetailsTab = it }
                    )
                    }
                    Screen.LATENCY -> LatencyScreenContent(
                        containerBrush = containerBrush,
                        latencyMs = latencyMs,
                        defaultOutputLatencyMs = defaultOutputLatencyMs,
                        bluetoothLatencyMs = bluetoothLatencyMs,
                        latencyAutoSwitchEnabled = latencyAutoSwitchEnabled,
                        isBluetoothOutputActive = isBluetoothOutputActive,
                        nothingStyleEnabled = nothingStyleEnabled,
                        onLatencyMsChanged = onLatencyMsChanged,
                        onLatencyMsChangeFinished = onLatencyMsChangeFinished,
                        onLatencyAutoSwitchChanged = onLatencyAutoSwitchChanged,
                        onOpenMenu = { drawerOpen = true },
                        onOpenSettings = {
                            settingsReturnScreen = Screen.LATENCY
                            screen = Screen.SETTINGS
                        },
                        experimentalStyle = experimentalMainUiEnabled,
                        onBack = if (experimentalMainUiEnabled) {
                            {
                                screen = if (detailedHomeEnabled) {
                                    Screen.MAIN
                                } else {
                                    Screen.DETAILS
                                }
                            }
                        } else {
                            null
                        }
                    )
                    Screen.EXTRAS -> ExtrasScreen(
                        nothingStyleEnabled = nothingStyleEnabled,
                        batteryGlyphEnabled = batteryGlyphEnabled,
                        batteryGlyphSupported = batteryGlyphSupported,
                        onBatteryGlyphEnabledChanged = onBatteryGlyphEnabledChanged,
                        syncWithNothingOsGlyphSettingEnabled =
                            syncWithNothingOsGlyphSettingEnabled,
                        onSyncWithNothingOsGlyphSettingEnabledChanged =
                            onSyncWithNothingOsGlyphSettingEnabledChanged,
                        onOpenMenu = { drawerOpen = true },
                        onOpenSettings = {
                            settingsReturnScreen = Screen.EXTRAS
                            screen = Screen.SETTINGS
                        }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { screen = settingsReturnScreen },
                        onAbout = { screen = Screen.ABOUT },
                        onExperimentalFeatures = { screen = Screen.EXPERIMENTAL },
                        mediaProjectionEnabled = mediaProjectionEnabled,
                        onMediaProjectionEnabledChanged = onMediaProjectionEnabledChanged,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        onGlyphMeterPreviewEnabledChanged = onGlyphMeterPreviewEnabledChanged,
                        meterVisibleEnabled = meterVisibleEnabled,
                        onMeterVisibleEnabledChanged = onMeterVisibleEnabledChanged,
                        lightweightMeterEnabled = lightweightMeterEnabled,
                        onLightweightMeterEnabledChanged = onLightweightMeterEnabledChanged,
                        spectrumMeterEnabled = spectrumMeterEnabled,
                        onSpectrumMeterEnabledChanged = onSpectrumMeterEnabledChanged,
                        onMeterStyleChanged = { visible, lightweight, spectrum, faithful ->
                            val updated = CaptureUiStore.state.copy(
                                meterVisibleEnabled = visible,
                                lightweightMeterEnabled = lightweight,
                                spectrumMeterEnabled = spectrum,
                                glyphMeterPreviewEnabled = faithful
                            )
                            CaptureUiStore.update { updated }
                            SettingsPreferences.save(context, updated)
                        },
                        nativeMeterViewEnabled = nativeMeterViewEnabled,
                        onNativeMeterViewEnabledChanged = onNativeMeterViewEnabledChanged,
                        automaticUpdateCheckEnabled = automaticUpdateCheckEnabled,
                        onAutomaticUpdateCheckEnabledChanged = onAutomaticUpdateCheckEnabledChanged,
                        mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
                        onMediaPlaybackOnlyEnabledChanged = onMediaPlaybackOnlyEnabledChanged,
                        experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
                        onExperimentalVisualizerStabilizationEnabledChanged = onExperimentalVisualizerStabilizationEnabledChanged,
                        experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
                        onExperimentalVisualizerSignalWatchdogEnabledChanged = onExperimentalVisualizerSignalWatchdogEnabledChanged,
                        showAutoEnablePhone1GlyphDebugOnStart = isPhone1Device,
                        autoEnablePhone1GlyphDebugOnStart = autoEnablePhone1GlyphDebugOnStart,
                        onAutoEnablePhone1GlyphDebugOnStartChanged = onAutoEnablePhone1GlyphDebugOnStartChanged,
                        experimentalMainUiEnabled = experimentalMainUiEnabled,
                        onExperimentalMainUiEnabledChanged = onExperimentalMainUiEnabledChanged,
                        detailedHomeEnabled = detailedHomeEnabled,
                        onDetailedHomeEnabledChanged = onDetailedHomeEnabledChanged,
                        nothingStyleEnabled = nothingStyleEnabled,
                        onNothingStyleEnabledChanged = onNothingStyleEnabledChanged
                    )
                    Screen.EXPERIMENTAL -> ExperimentalScreenContent(
                        containerBrush = containerBrush,
                        actualDeviceProfile = actualDeviceProfile,
                        phone4bEmulationEnabled = phone4bEmulationEnabled,
                        debugDeviceProfileOverride = debugDeviceProfileOverride,
                        isCapturing = isCapturing,
                        nothingStyleEnabled = nothingStyleEnabled,
                        onPhone4bEmulationEnabledChanged = onPhone4bEmulationEnabledChanged,
                        onDebugDeviceProfileOverrideChanged = onDebugDeviceProfileOverrideChanged,
                        onOpenGlyphInspector = { screen = Screen.GLYPH_INSPECTOR },
                        onBack = { screen = Screen.SETTINGS }
                    )
                    Screen.GLYPH_INSPECTOR -> GlyphInterfaceInspectorScreen(
                        containerBrush = containerBrush,
                        nothingStyleEnabled = nothingStyleEnabled,
                        initialDeviceProfile = deviceProfile,
                        onBack = { screen = Screen.EXPERIMENTAL }
                    )
                    Screen.ABOUT -> AboutScreen(
                        onBack = { screen = Screen.SETTINGS },
                        onOssLicenses = { screen = Screen.OSS },
                        onUpdateAvailable = { updateInfo ->
                            availableUpdate = updateInfo
                            screen = Screen.UPDATE
                        },
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                    Screen.UPDATE -> {
                        val updateInfo = availableUpdate
                        if (updateInfo != null) {
                            UpdateOverviewScreen(
                                updateInfo = updateInfo,
                                nothingStyleEnabled = nothingStyleEnabled,
                                onBack = { screen = Screen.ABOUT }
                            )
                        } else {
                            AboutScreen(
                                onBack = { screen = Screen.SETTINGS },
                                onOssLicenses = { screen = Screen.OSS },
                                onUpdateAvailable = { foundUpdate ->
                                    availableUpdate = foundUpdate
                                    screen = Screen.UPDATE
                                },
                                nothingStyleEnabled = nothingStyleEnabled
                            )
                        }
                    }
                    Screen.OSS -> OssLicensesScreen(
                        nothingStyleEnabled = nothingStyleEnabled,
                        onBack = { screen = Screen.ABOUT }
                    )
                }
            }

            HomeDrawerOverlay(
                visible = drawerOpen,
                currentScreen = screen,
                nothingStyleEnabled = nothingStyleEnabled,
                showLatency = !experimentalMainUiEnabled,
                onDismiss = { drawerOpen = false },
                onNavigate = { destination ->
                    if (destination == Screen.SETTINGS) {
                        settingsReturnScreen = screen
                    }
                    screen = destination
                    drawerOpen = false
                }
            )

            AnimatedVisibility(
                visible = updateNotification != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 72.dp),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                updateNotification?.let { updateInfo ->
                    UpdateNotificationOverlay(
                        updateInfo = updateInfo,
                        nothingStyleEnabled = nothingStyleEnabled,
                        onOpen = {
                            availableUpdate = updateInfo
                            updateNotification = null
                            screen = Screen.UPDATE
                        },
                        onDismiss = {
                            updateNotification = null
                        },
                        onDismissUntilNextVersion = {
                            dismissUpdateNotificationUntilNextVersion(context, updateInfo.tagName)
                            updateNotification = null
                        }
                    )
                }
            }
        }
    }
}
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
internal fun InfoStrip() {
    val notes = listOf(
        stringResource(R.string.info_note_phone),
        stringResource(R.string.info_note_foreground),
        stringResource(R.string.info_note_projection)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(notes) { _, note ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                border = null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExperimentalDetailsInfoStrip(
    nothingStyleEnabled: Boolean
) {
    val notes = listOf(
        stringResource(R.string.info_note_phone),
        stringResource(R.string.info_note_foreground),
        stringResource(R.string.info_note_projection)
    )
    val dividerColor = Color(0xFF3A3A3A)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = dividerColor)
        notes.forEachIndexed { index, note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == 0) {
                                if (nothingStyleEnabled) NothingRed else MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFF666666)
                            }
                        )
                )
                Text(
                    text = note,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index != notes.lastIndex) {
                HorizontalDivider(color = dividerColor.copy(alpha = 0.7f))
            }
        }
        HorizontalDivider(color = dividerColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun GlyphVisualizerPreview() {
    GlyphBartyTheme {
        GlyphVisualizerApp(
            uiState = VisualizerUiState(
                capture = CaptureUiState(
                    statusText = stringResource(R.string.preview_status_text),
                    isCapturing = true,
                    level = 0.72f,
                    peak = 0.9f,
                    spectrumBands = FloatArray(0),
                    sensitivity = 1.35f,
                    noiseGate = 0.08f,
                    dynamics = 1.45f,
                    outputGamma = 1.8f,
                    toneFocus = 0f,
                    smoothing = 0.28f,
                    smoothingBalance = 0f,
                    autoScaleWindowSeconds = 30f,
                    autoScaleOffset = 0f,
                    latencyMs = 0f,
                    defaultOutputLatencyMs = 0f,
                    bluetoothLatencyMs = 0f,
                    latencyAutoSwitchEnabled = true,
                    isBluetoothOutputActive = false,
                    reverseDirection = false,
                    meterSegments = remember { 11 },
                    activeMode = stringResource(R.string.mode_visualizer),
                    glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice(),
                    fillOtherGlyphLights = false,
                    phone1ClassicCSplitEnabled = false,
                    binaryMode = false,
                    matrixSmoothMotionEnabled = false,
                    oscilloscopeAutoTimeAxisEnabled = false,
                    baseIndicatorEnabled = false,
                    recordingLightIncluded = false,
                    phone4bEmulationEnabled = false,
                    debugDeviceProfileOverride = null,
                    levelAutoScale = false,
                    spectrumAutoScale = false,
                    allBrightnessAutoScale = false,
                    experimentalAdaptiveAutoScaleEnabled = false,
                    mediaProjectionEnabled = false,
                    glyphMeterPreviewEnabled = true,
                    meterVisibleEnabled = true,
                    lightweightMeterEnabled = false,
                    spectrumMeterEnabled = false,
                    nativeMeterViewEnabled = true,
                    mainScreenUiIsolationEnabled = true,
                    automaticUpdateCheckEnabled = false,
                    mediaPlaybackOnlyEnabled = false,
                    experimentalVisualizerStabilizationEnabled = false,
                    experimentalVisualizerSignalWatchdogEnabled = false,
                    experimentalPerformanceOptimizationsEnabled = true,
                    autoEnablePhone1GlyphDebugOnStart = true,
                    nothingStyleEnabled = false,
                    experimentalMainUiEnabled = true,
                    detailedHomeEnabled = false,
                    turnOffWhenBackDown = false,
                    logMessage = null
                ),
                initialSetupPending = false,
                heroTitle = "Phone (2)\nGlyph Lights",
                deviceProfile = GlyphDeviceCatalog.currentProfile(),
                actualDeviceProfile = GlyphDeviceCatalog.currentProfile(),
                batteryGlyphSupported = false,
                visualDynamics = 0f,
                visualDynamicsOverridden = false,
                showPhone1GlyphDebugPermissionDialog = false
            ),
            actions = VisualizerUiActions(
                onInitialSetupCompleted = {},
                visualTuning = VisualTuningActions(
                    onVisualDynamicsChanged = {},
                    onVisualDynamicsChangeFinished = {},
                    onVisualDynamicsReset = {}
                ),
                settings = VisualizerSettingsActions(
                    onSensitivityChanged = {},
                    onNoiseGateChanged = {},
                    onDynamicsChanged = {},
                    onOutputGammaChanged = {},
                    onSmoothingChanged = {},
                    onSmoothingBalanceChanged = {},
                    onToneFocusChanged = {},
                    onAutoScaleWindowSecondsChanged = {},
                    onAutoScaleWindowSecondsChangeFinished = {},
                    onAutoScaleOffsetChanged = {},
                    onAutoScaleOffsetChangeFinished = {},
                    onLatencyMsChanged = {},
                    onLatencyMsChangeFinished = {},
                    onLatencyAutoSwitchChanged = {},
                    onGlyphMeterPreviewEnabledChanged = {},
                    onMeterVisibleEnabledChanged = {},
                    onLightweightMeterEnabledChanged = {},
                    onSpectrumMeterEnabledChanged = {},
                    onNativeMeterViewEnabledChanged = {},
                    onAutomaticUpdateCheckEnabledChanged = {},
                    onBatteryGlyphEnabledChanged = {},
                    onSyncWithNothingOsGlyphSettingEnabledChanged = {},
                    onMediaPlaybackOnlyEnabledChanged = {},
                    onExperimentalVisualizerStabilizationEnabledChanged = {},
                    onExperimentalVisualizerSignalWatchdogEnabledChanged = {},
                    onMatrixSmoothMotionEnabledChanged = {},
                    onOscilloscopeAutoTimeAxisEnabledChanged = {},
                    onAutoEnablePhone1GlyphDebugOnStartChanged = {},
                    onRecordingLightBehaviorChanged = {},
                    onPhone4bEmulationEnabledChanged = {},
                    onDebugDeviceProfileOverrideChanged = {},
                    onReverseDirectionChanged = {},
                    onGlyphModeChanged = {},
                    onFillOtherGlyphLightsChanged = {},
                    onPhone1ClassicCSplitEnabledChanged = {},
                    onBinaryModeChanged = {},
                    onLevelAutoScaleChanged = {},
                    onSpectrumAutoScaleChanged = {},
                    onAllBrightnessAutoScaleChanged = {},
                    onExperimentalAdaptiveAutoScaleEnabledChanged = {},
                    onMediaProjectionEnabledChanged = {},
                    onNothingStyleEnabledChanged = {},
                    onExperimentalMainUiEnabledChanged = {},
                    onDetailedHomeEnabledChanged = {},
                    onTurnOffWhenBackDownChanged = {},
                    onResetParametersClick = {},
                    onExportParametersClick = {},
                    onImportParametersClick = {}
                ),
                capture = VisualizerCaptureActions(
                    onStartVisualizerClick = {},
                    onStartProjectionClick = {},
                    onEnablePhone1GlyphDebugClick = {},
                    onStopClick = {}
                ),
                dialogs = VisualizerDialogActions(
                    onDismissLog = {},
                    onDismissPhone1GlyphDebugPermissionDialog = {}
                )
            )
        )
    }
}
