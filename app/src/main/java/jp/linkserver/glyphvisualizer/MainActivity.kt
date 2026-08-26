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
import jp.linkserver.glyphvisualizer.glyph.Phone4aAsPhone4bGlyphProbe
import jp.linkserver.glyphvisualizer.ui.openNotificationAccessSettings
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import jp.linkserver.glyphvisualizer.update.checkGitHubReleaseUpdate
import jp.linkserver.glyphvisualizer.update.dismissUpdateNotificationUntilNextVersion
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isUpdateNotificationDismissed
import jp.linkserver.glyphvisualizer.update.markUpdateCheckFinished
import jp.linkserver.glyphvisualizer.update.shouldCheckForUpdates
import rikka.shizuku.Shizuku

private const val RESPONSE_SPEED_NONE_THRESHOLD = 0.54f
private const val PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE = 1401

private enum class Screen {
    WELCOME,
    MAIN,
    DETAILS,
    LATENCY,
    SETTINGS,
    EXPERIMENTAL,
    ABOUT,
    UPDATE,
    OSS
}

private enum class WelcomeStep {
    INTRO,
    UI_MODE,
    FEATURES,
    UPDATE_CHECK
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val STATE_PENDING_MEDIA_PLAYBACK_PERMISSION =
            "pending_media_playback_permission"
        private const val STATE_PENDING_OPEN_REEL_PERMISSION =
            "pending_open_reel_permission"
        private const val STATE_PENDING_OPEN_REEL_SETTINGS_LAUNCHED =
            "pending_open_reel_settings_launched"
        private const val STATE_PENDING_OPEN_REEL_MODE =
            "pending_open_reel_mode"
    }

    private val parameterSyncHandler = Handler(Looper.getMainLooper())
    private val delayedParameterSyncRunnable = Runnable {
        syncCurrentParameters()
    }
    private var delayedMatrixSmoothMotionApplyRunnable: Runnable? = null

    private enum class CaptureMode {
        VISUALIZER,
        MEDIA_PROJECTION
    }

    private val currentDevice by lazy { GlyphDeviceCatalog.currentOrFallback() }
    private val deviceProfile by lazy { currentDevice.profile }
    private val isPhone3Device by lazy { deviceProfile == GlyphDeviceProfile.PHONE3_MATRIX }
    private val isPhone4aProDevice by lazy { deviceProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX }
    private val isPhone2aDevice by lazy { deviceProfile == GlyphDeviceProfile.PHONE2A }
    private val isPhone3aDevice by lazy { deviceProfile == GlyphDeviceProfile.PHONE3A }
    private val isPhone4aDevice by lazy { deviceProfile == GlyphDeviceProfile.PHONE4A }

    private var pendingStartMode: CaptureMode? = null
    private var pendingExportContent: String? = null
    private var pendingMediaPlaybackOnlyPermissionRequest = false
    private var pendingOpenReelPermissionRequest = false
    private var pendingOpenReelPermissionSettingsLaunched = false
    private var pendingOpenReelGlyphMode: String? = null
    private var pendingPhone1GlyphDebugPermissionRequestFromManual by mutableStateOf(false)
    private var showPhone1GlyphDebugPermissionDialog by mutableStateOf(false)
    private var showOpenReelPermissionDialog by mutableStateOf(false)
    private var showNotificationPermissionExplanationDialog by mutableStateOf(false)

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE) {
                val fromManual = pendingPhone1GlyphDebugPermissionRequestFromManual
                AppLogger.i(
                    "Phone1GlyphDebug",
                    "Permission result received requestCode=$requestCode grantResult=$grantResult fromManual=$fromManual"
                )
                pendingPhone1GlyphDebugPermissionRequestFromManual = false
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    enablePhone1GlyphDebug(manual = fromManual)
                } else if (fromManual) {
                    showPhone1GlyphDebugPermissionDialog = true
                }
            }
        }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingStartMode) {
                CaptureMode.VISUALIZER -> startVisualizerMode()
                CaptureMode.MEDIA_PROJECTION -> launchCaptureIntent()
                null -> Unit
            }
        } else {
            CaptureUiStore.update {
                it.copy(statusText = getString(R.string.status_mic_permission_required))
            }
        }
        pendingStartMode = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.i("MainActivity", "Notification permission result: granted=$granted")
    }

    private val exportParametersLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) {
            return@registerForActivityResult
        }

        val success = runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(content)
            } ?: error("Output stream is unavailable.")
        }.isSuccess

        Toast.makeText(
            this,
            getString(if (success) R.string.settings_export_success else R.string.settings_export_failed),
            Toast.LENGTH_SHORT
        ).show()
    }

    private val importParametersLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }

        val importedState = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                SettingsPreferences.importJson(reader.readText())
            } ?: error("Input stream is unavailable.")
        }.getOrNull()

        if (importedState == null) {
            Toast.makeText(this, getString(R.string.settings_import_failed), Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        applyParameterState(
            importedState.copy(
                baseIndicatorEnabled = CaptureUiStore.state.baseIndicatorEnabled,
                recordingLightIncluded = CaptureUiStore.state.recordingLightIncluded,
                phone4bEmulationEnabled = CaptureUiStore.state.phone4bEmulationEnabled,
                debugDeviceProfileOverride = CaptureUiStore.state.debugDeviceProfileOverride
            )
        )
        Toast.makeText(this, getString(R.string.settings_import_success), Toast.LENGTH_SHORT).show()
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode != RESULT_OK || data == null) {
            CaptureUiStore.update { it.copy(statusText = getString(R.string.status_audio_capture_cancelled)) }
            return@registerForActivityResult
        }

        val uiState = CaptureUiStore.state
        GlyphVisualizerService.startMediaProjection(
            context = this,
            resultCode = result.resultCode,
            data = Intent(data),
            sensitivity = uiState.sensitivity,
            noiseGate = uiState.noiseGate,
            dynamics = uiState.dynamics,
            outputGamma = uiState.outputGamma,
            toneFocus = uiState.toneFocus,
            smoothing = uiState.smoothing,
            smoothingBalance = uiState.smoothingBalance,
            reverseDirection = uiState.reverseDirection,
            peakHoldEnabled = uiState.peakHoldEnabled,
            glyphMode = uiState.glyphMode,
            fillOtherGlyphLights = uiState.fillOtherGlyphLights,
            binaryMode = uiState.binaryMode,
            baseIndicatorEnabled = uiState.baseIndicatorEnabled,
            levelAutoScale = uiState.levelAutoScale,
            spectrumAutoScale = uiState.spectrumAutoScale,
            allBrightnessAutoScale = uiState.allBrightnessAutoScale,
            autoScaleWindowSeconds = uiState.autoScaleWindowSeconds,
            autoScaleOffset = uiState.autoScaleOffset,
            latencyMs = uiState.latencyMs,
            mediaPlaybackOnlyEnabled = uiState.mediaPlaybackOnlyEnabled,
            experimentalVisualizerStabilizationEnabled = uiState.experimentalVisualizerStabilizationEnabled,
            experimentalVisualizerSignalWatchdogEnabled = uiState.experimentalVisualizerSignalWatchdogEnabled,
            experimentalSpectrumDecayEnabled = uiState.experimentalSpectrumDecayEnabled,
            experimentalPerformanceOptimizationsEnabled = uiState.experimentalPerformanceOptimizationsEnabled,
            matrixSmoothMotionEnabled = uiState.matrixSmoothMotionEnabled,
            oscilloscopeAutoTimeAxisEnabled = uiState.oscilloscopeAutoTimeAxisEnabled,
            turnOffWhenBackDown = uiState.turnOffWhenBackDown,
            recordingLightIncluded = uiState.recordingLightIncluded
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(this)
        pendingMediaPlaybackOnlyPermissionRequest = savedInstanceState?.getBoolean(
            STATE_PENDING_MEDIA_PLAYBACK_PERMISSION,
            false
        ) ?: false
        pendingOpenReelPermissionRequest = savedInstanceState?.getBoolean(
            STATE_PENDING_OPEN_REEL_PERMISSION,
            false
        ) ?: false
        pendingOpenReelPermissionSettingsLaunched = savedInstanceState?.getBoolean(
            STATE_PENDING_OPEN_REEL_SETTINGS_LAUNCHED,
            pendingOpenReelPermissionRequest
        ) ?: false
        pendingOpenReelGlyphMode = savedInstanceState?.getString(STATE_PENDING_OPEN_REEL_MODE)
        CaptureUiStore.setUiVisible(true)
        enableEdgeToEdge()
        runCatching { Shizuku.addRequestPermissionResultListener(shizukuPermissionListener) }
        val savedSettings = SettingsPreferences.load(this)
        val initialSetupPending = !SettingsPreferences.hasCompletedInitialSetup(this)
        if (!initialSetupPending) {
            offerNotificationPermissionIfNeeded()
        }
        val savedDeviceProfile = GlyphDeviceCatalog.effectiveUiProfile(
            actualProfile = deviceProfile,
            phone4bEmulationEnabled = savedSettings.phone4bEmulationEnabled,
            debugDeviceProfileOverride = savedSettings.debugDeviceProfileOverride
        )
        val normalizedMode = GlyphDeviceCatalog.normalizeGlyphMode(savedDeviceProfile, savedSettings.glyphMode)
        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
        val resolvedLatencySettings = savedSettings.withResolvedLatency(bluetoothOutputActive)
        CaptureUiStore.update { current ->
            if (current.isCapturing) {
                // A system theme change recreates the Activity while the service keeps running.
                // Keep live capture state, but restore UI-only preferences because the service
                // may have recreated CaptureUiStore from defaults before the Activity starts.
                current.copy(
                    isBluetoothOutputActive = bluetoothOutputActive,
                    nothingStyleEnabled = resolvedLatencySettings.nothingStyleEnabled,
                    glyphMeterPreviewEnabled = resolvedLatencySettings.glyphMeterPreviewEnabled,
                    meterVisibleEnabled = resolvedLatencySettings.meterVisibleEnabled,
                    lightweightMeterEnabled = resolvedLatencySettings.lightweightMeterEnabled,
                    spectrumMeterEnabled = resolvedLatencySettings.spectrumMeterEnabled,
                    nativeMeterViewEnabled = resolvedLatencySettings.nativeMeterViewEnabled,
                    mainScreenUiIsolationEnabled = resolvedLatencySettings.mainScreenUiIsolationEnabled,
                    automaticUpdateCheckEnabled = resolvedLatencySettings.automaticUpdateCheckEnabled,
                    phone4bEmulationEnabled = resolvedLatencySettings.phone4bEmulationEnabled,
                    debugDeviceProfileOverride = resolvedLatencySettings.debugDeviceProfileOverride,
                    experimentalMainUiEnabled = resolvedLatencySettings.experimentalMainUiEnabled,
                    detailedHomeEnabled = resolvedLatencySettings.detailedHomeEnabled
                )
            } else {
                resolvedLatencySettings.copy(
                    statusText = getString(R.string.status_preparing_glyph_session),
                    glyphMode = normalizedMode,
                    logMessage = current.logMessage,
                    pendingSpatialAudioWarning = current.pendingSpatialAudioWarning
                )
            }
        }
        setContent {
            val uiState = CaptureUiStore.state
            val effectiveDeviceProfile = GlyphDeviceCatalog.effectiveUiProfile(
                actualProfile = deviceProfile,
                phone4bEmulationEnabled = uiState.phone4bEmulationEnabled,
                debugDeviceProfileOverride = uiState.debugDeviceProfileOverride
            )
            val effectivePresentation = if (effectiveDeviceProfile == deviceProfile) {
                currentDevice.presentation
            } else {
                GlyphDeviceCatalog.presentationForProfile(effectiveDeviceProfile)
            }
            GlyphBartyTheme(nothingStyle = uiState.nothingStyleEnabled) {
                GlyphVisualizerApp(
                    initialSetupPending = initialSetupPending,
                    onInitialSetupCompleted = {
                        offerNotificationPermissionIfNeeded()
                    },
                    statusText = uiState.statusText,
                    isCapturing = uiState.isCapturing,
                    heroTitle = effectivePresentation.heroTitle,
                    level = uiState.level,
                    peak = uiState.peak,
                    spectrumBands = uiState.spectrumBands,
                    sensitivity = uiState.sensitivity,
                    noiseGate = uiState.noiseGate,
                    dynamics = uiState.dynamics,
                    outputGamma = uiState.outputGamma,
                    toneFocus = uiState.toneFocus,
                    smoothing = uiState.smoothing,
                    smoothingBalance = uiState.smoothingBalance,
                    autoScaleWindowSeconds = uiState.autoScaleWindowSeconds,
                    autoScaleOffset = uiState.autoScaleOffset,
                    latencyMs = uiState.latencyMs,
                    defaultOutputLatencyMs = uiState.defaultOutputLatencyMs,
                    bluetoothLatencyMs = uiState.bluetoothLatencyMs,
                    latencyAutoSwitchEnabled = uiState.latencyAutoSwitchEnabled,
                    isBluetoothOutputActive = uiState.isBluetoothOutputActive,
                    reverseDirection = uiState.reverseDirection,
                    meterSegments = uiState.meterSegments,
                    activeMode = uiState.activeMode,
                    glyphMode = uiState.glyphMode,
                    fillOtherGlyphLights = uiState.fillOtherGlyphLights,
                    deviceProfile = effectiveDeviceProfile,
                    actualDeviceProfile = deviceProfile,
                    isPhone3Device = effectiveDeviceProfile == GlyphDeviceProfile.PHONE3_MATRIX,
                    isPhone4aProDevice = effectiveDeviceProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
                    isPhone2aDevice = effectiveDeviceProfile == GlyphDeviceProfile.PHONE2A,
                    isPhone3aDevice = effectiveDeviceProfile == GlyphDeviceProfile.PHONE3A,
                    isPhone4aDevice = effectiveDeviceProfile == GlyphDeviceProfile.PHONE4A,
                    isPhone1Device = Phone1GlyphDebugHelper.supports(effectiveDeviceProfile),
                    binaryMode = uiState.binaryMode,
                    baseIndicatorEnabled = uiState.baseIndicatorEnabled,
                    recordingLightIncluded = uiState.recordingLightIncluded,
                    phone4bEmulationEnabled = uiState.phone4bEmulationEnabled,
                    debugDeviceProfileOverride = uiState.debugDeviceProfileOverride,
                    levelAutoScale = uiState.levelAutoScale,
                    spectrumAutoScale = uiState.spectrumAutoScale,
                    allBrightnessAutoScale = uiState.allBrightnessAutoScale,
                    mediaProjectionEnabled = uiState.mediaProjectionEnabled,
                    glyphMeterPreviewEnabled = uiState.glyphMeterPreviewEnabled,
                    meterVisibleEnabled = uiState.meterVisibleEnabled,
                    lightweightMeterEnabled = uiState.lightweightMeterEnabled,
                    spectrumMeterEnabled = uiState.spectrumMeterEnabled,
                    nativeMeterViewEnabled = uiState.nativeMeterViewEnabled,
                    mainScreenUiIsolationEnabled = uiState.mainScreenUiIsolationEnabled,
                    automaticUpdateCheckEnabled = uiState.automaticUpdateCheckEnabled,
                    mediaPlaybackOnlyEnabled = uiState.mediaPlaybackOnlyEnabled,
                    experimentalVisualizerStabilizationEnabled = uiState.experimentalVisualizerStabilizationEnabled,
                    experimentalVisualizerSignalWatchdogEnabled = uiState.experimentalVisualizerSignalWatchdogEnabled,
                    experimentalPerformanceOptimizationsEnabled = uiState.experimentalPerformanceOptimizationsEnabled,
                    matrixSmoothMotionEnabled = uiState.matrixSmoothMotionEnabled,
                    oscilloscopeAutoTimeAxisEnabled = uiState.oscilloscopeAutoTimeAxisEnabled,
                    autoEnablePhone1GlyphDebugOnStart = uiState.autoEnablePhone1GlyphDebugOnStart,
                    nothingStyleEnabled = uiState.nothingStyleEnabled,
                    experimentalMainUiEnabled = uiState.experimentalMainUiEnabled,
                    detailedHomeEnabled = uiState.detailedHomeEnabled,
                    turnOffWhenBackDown = uiState.turnOffWhenBackDown,
                    onSensitivityChanged = { newValue ->
                        CaptureUiStore.update { it.copy(sensitivity = newValue) }
                        scheduleParameterSync()
                    },
                    onNoiseGateChanged = { newValue ->
                        CaptureUiStore.update { it.copy(noiseGate = newValue) }
                        scheduleParameterSync()
                    },
                    onDynamicsChanged = { newValue ->
                        CaptureUiStore.update { it.copy(dynamics = newValue) }
                        scheduleParameterSync()
                    },
                    onOutputGammaChanged = { newValue ->
                        CaptureUiStore.update { it.copy(outputGamma = newValue) }
                        scheduleParameterSync()
                    },
                    onSmoothingChanged = { newValue ->
                        CaptureUiStore.update { it.copy(smoothing = newValue) }
                        scheduleParameterSync()
                    },
                    onSmoothingBalanceChanged = { newValue ->
                        CaptureUiStore.update { it.copy(smoothingBalance = newValue) }
                        scheduleParameterSync()
                    },
                    onToneFocusChanged = { newValue ->
                        CaptureUiStore.update { it.copy(toneFocus = newValue) }
                        scheduleParameterSync()
                    },
                    onAutoScaleWindowSecondsChanged = { newValue ->
                        CaptureUiStore.update { it.copy(autoScaleWindowSeconds = newValue) }
                    },
                    onAutoScaleWindowSecondsChangeFinished = {
                        syncCurrentParameters()
                    },
                    onAutoScaleOffsetChanged = { newValue ->
                        CaptureUiStore.update { it.copy(autoScaleOffset = newValue) }
                    },
                    onAutoScaleOffsetChangeFinished = {
                        syncCurrentParameters()
                    },
                    onLatencyMsChanged = { newValue ->
                        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
                        CaptureUiStore.update {
                            it.withLatencyEditedForCurrentRoute(newValue, bluetoothOutputActive)
                        }
                    },
                    onLatencyMsChangeFinished = {
                        syncCurrentParameters()
                    },
                    onLatencyAutoSwitchChanged = { enabled ->
                        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
                        val current = CaptureUiStore.state
                        val updated = if (enabled) {
                            current.copy(latencyAutoSwitchEnabled = true)
                                .withLatencyEditedForCurrentRoute(current.latencyMs, bluetoothOutputActive)
                        } else {
                            current.copy(
                                latencyAutoSwitchEnabled = false,
                                latencyMs = current.resolvedLatencyMs(bluetoothOutputActive)
                            ).withResolvedLatency(bluetoothOutputActive)
                        }
                        CaptureUiStore.update { updated }
                        syncCurrentParameters(updated)
                    },
                    onGlyphMeterPreviewEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(glyphMeterPreviewEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onMeterVisibleEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(meterVisibleEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onLightweightMeterEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(lightweightMeterEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onSpectrumMeterEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(spectrumMeterEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onNativeMeterViewEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(nativeMeterViewEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onAutomaticUpdateCheckEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(automaticUpdateCheckEnabled = enabled)
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onMediaPlaybackOnlyEnabledChanged = { enabled ->
                        if (enabled && !MediaSessionPlaybackGate.hasNotificationAccess(this)) {
                            pendingMediaPlaybackOnlyPermissionRequest = true
                            if (CaptureUiStore.state.mediaPlaybackOnlyEnabled) {
                                applyMediaPlaybackOnlyEnabled(false)
                            }
                            val opened = openNotificationAccessSettings(this)
                            if (!opened) {
                                pendingMediaPlaybackOnlyPermissionRequest = false
                                Toast.makeText(
                                    this,
                                    getString(R.string.settings_media_playback_only_open_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    getString(R.string.settings_media_playback_only_permission_required),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            pendingMediaPlaybackOnlyPermissionRequest = false
                            applyMediaPlaybackOnlyEnabled(enabled)
                        }
                    },
                    onExperimentalVisualizerStabilizationEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(
                            experimentalVisualizerStabilizationEnabled = enabled
                        )
                        CaptureUiStore.update { updated }
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onExperimentalVisualizerSignalWatchdogEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(
                            experimentalVisualizerSignalWatchdogEnabled = enabled
                        )
                        CaptureUiStore.update { updated }
                        syncCurrentParameters(updated)
                    },
                    onMatrixSmoothMotionEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(
                            matrixSmoothMotionEnabled = enabled
                        )
                        CaptureUiStore.update { updated }
                        delayedMatrixSmoothMotionApplyRunnable?.let(parameterSyncHandler::removeCallbacks)
                        delayedMatrixSmoothMotionApplyRunnable = Runnable {
                            GlyphVisualizerService.updateSensitivity(
                                this,
                                updated.sensitivity,
                                updated.noiseGate,
                                updated.dynamics,
                                updated.toneFocus,
                                updated.smoothing,
                                updated.smoothingBalance,
                                updated.reverseDirection,
                                updated.peakHoldEnabled,
                                updated.glyphMode,
                                updated.fillOtherGlyphLights,
                                updated.binaryMode,
                                updated.baseIndicatorEnabled,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.autoScaleOffset,
                                updated.latencyMs,
                                updated.mediaPlaybackOnlyEnabled,
                                updated.experimentalVisualizerStabilizationEnabled,
                                updated.experimentalVisualizerSignalWatchdogEnabled,
                                updated.experimentalSpectrumDecayEnabled,
                                updated.experimentalPerformanceOptimizationsEnabled,
                                updated.matrixSmoothMotionEnabled,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma,
                                updated.oscilloscopeAutoTimeAxisEnabled
                            )
                            SettingsPreferences.save(this, updated)
                            delayedMatrixSmoothMotionApplyRunnable = null
                        }
                        parameterSyncHandler.postDelayed(
                            delayedMatrixSmoothMotionApplyRunnable!!,
                            140L
                        )
                    },
                    onOscilloscopeAutoTimeAxisEnabledChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(
                            oscilloscopeAutoTimeAxisEnabled = enabled
                        )
                        CaptureUiStore.update { updated }
                        syncCurrentParameters(updated)
                    },
                    onAutoEnablePhone1GlyphDebugOnStartChanged = { enabled ->
                        val updated = CaptureUiStore.state.copy(
                            autoEnablePhone1GlyphDebugOnStart = enabled
                        )
                        CaptureUiStore.update { updated }
                        SettingsPreferences.save(this, updated)
                    },
                    onRecordingLightBehaviorChanged = { behavior ->
                        val updated = CaptureUiStore.state.withRecordingLightBehavior(behavior)
                        CaptureUiStore.update { updated }
                        syncCurrentParameters(updated)
                    },
                    onPhone4bEmulationEnabledChanged = { enabled ->
                        if (deviceProfile == GlyphDeviceProfile.PHONE4A && !CaptureUiStore.state.isCapturing) {
                            val current = CaptureUiStore.state
                            val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                                actualProfile = deviceProfile,
                                phone4bEmulationEnabled = enabled,
                                debugDeviceProfileOverride = current.debugDeviceProfileOverride
                            )
                            val updated = current.copy(
                                phone4bEmulationEnabled = enabled,
                                glyphMode = GlyphDeviceCatalog.normalizeGlyphMode(
                                    effectiveProfile,
                                    current.glyphMode
                                )
                            )
                            CaptureUiStore.update { updated }
                            SettingsPreferences.save(this, updated)
                        }
                    },
                    onDebugDeviceProfileOverrideChanged = { profile ->
                        if (!CaptureUiStore.state.isCapturing) {
                            val current = CaptureUiStore.state
                            val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                                actualProfile = deviceProfile,
                                phone4bEmulationEnabled = current.phone4bEmulationEnabled,
                                debugDeviceProfileOverride = profile
                            )
                            val updated = current.copy(
                                debugDeviceProfileOverride = profile,
                                glyphMode = GlyphDeviceCatalog.normalizeGlyphMode(
                                    effectiveProfile,
                                    current.glyphMode
                                )
                            )
                            CaptureUiStore.update { updated }
                            SettingsPreferences.save(this, updated)
                        }
                    },
                    onReverseDirectionChanged = { newValue ->
                        CaptureUiStore.update { it.copy(reverseDirection = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onGlyphModeChanged = { newMode ->
                        requestGlyphModeChange(newMode)
                    },
                    onFillOtherGlyphLightsChanged = { enabled ->
                        CaptureUiStore.update { it.copy(fillOtherGlyphLights = enabled) }
                        syncCurrentParameters()
                    },
                    onBinaryModeChanged = { newValue ->
                        CaptureUiStore.update { it.copy(binaryMode = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onLevelAutoScaleChanged = { newValue ->
                        CaptureUiStore.update { it.copy(levelAutoScale = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onSpectrumAutoScaleChanged = { newValue ->
                        CaptureUiStore.update { it.copy(spectrumAutoScale = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onAllBrightnessAutoScaleChanged = { newValue ->
                        CaptureUiStore.update { it.copy(allBrightnessAutoScale = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onTurnOffWhenBackDownChanged = { newValue ->
                        CaptureUiStore.update { it.copy(turnOffWhenBackDown = newValue) }
                        val updated = CaptureUiStore.state
                        GlyphVisualizerService.updateSensitivity(
                            this,
                            updated.sensitivity,
                            updated.noiseGate,
                            updated.dynamics,
                            updated.toneFocus,
                            updated.smoothing,
                            updated.smoothingBalance,
                            updated.reverseDirection,
                            updated.peakHoldEnabled,
                            updated.glyphMode,
                            updated.fillOtherGlyphLights,
                            updated.binaryMode,
                            updated.baseIndicatorEnabled,
                            updated.levelAutoScale,
                            updated.spectrumAutoScale,
                            updated.allBrightnessAutoScale,
                            updated.autoScaleWindowSeconds,
                            updated.autoScaleOffset,
                            updated.latencyMs,
                            updated.mediaPlaybackOnlyEnabled,
                            updated.experimentalVisualizerStabilizationEnabled,
                            updated.experimentalVisualizerSignalWatchdogEnabled,
                            updated.experimentalSpectrumDecayEnabled,
                            updated.experimentalPerformanceOptimizationsEnabled,
                            updated.matrixSmoothMotionEnabled,
                            updated.turnOffWhenBackDown,
                            updated.outputGamma,
                            updated.oscilloscopeAutoTimeAxisEnabled
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onMediaProjectionEnabledChanged = { newValue ->
                        CaptureUiStore.update { it.copy(mediaProjectionEnabled = newValue) }
                        SettingsPreferences.save(this, CaptureUiStore.state)
                    },
                    onNothingStyleEnabledChanged = { newValue ->
                        CaptureUiStore.update { it.copy(nothingStyleEnabled = newValue) }
                        SettingsPreferences.save(this, CaptureUiStore.state)
                    },
                    onExperimentalMainUiEnabledChanged = { newValue ->
                        CaptureUiStore.update { it.copy(experimentalMainUiEnabled = newValue) }
                        SettingsPreferences.save(this, CaptureUiStore.state)
                    },
                    onDetailedHomeEnabledChanged = { newValue ->
                        CaptureUiStore.update { it.copy(detailedHomeEnabled = newValue) }
                        SettingsPreferences.save(this, CaptureUiStore.state)
                    },
                    onResetParametersClick = {
                        applyParameterState(defaultParameterState())
                        Toast.makeText(this, getString(R.string.settings_reset_done), Toast.LENGTH_SHORT).show()
                    },
                    onExportParametersClick = {
                        exportParameters()
                    },
                    onImportParametersClick = {
                        importParameters()
                    },
                    onStartVisualizerClick = {
                        requestModeStart(CaptureMode.VISUALIZER)
                    },
                    onStartProjectionClick = {
                        requestModeStart(CaptureMode.MEDIA_PROJECTION)
                    },
                    onEnablePhone1GlyphDebugClick = {
                        requestPhone1GlyphDebug(manual = true)
                    },
                    onStopClick = {
                        GlyphVisualizerService.stop(this)
                    },
                    logMessage = uiState.logMessage,
                    onDismissLog = {
                        CaptureUiStore.update { it.copy(logMessage = null) }
                    },
                    showPhone1GlyphDebugPermissionDialog = showPhone1GlyphDebugPermissionDialog,
                    onDismissPhone1GlyphDebugPermissionDialog = {
                        showPhone1GlyphDebugPermissionDialog = false
                    }
                )
                if (
                    showNotificationPermissionExplanationDialog &&
                    uiState.pendingSpatialAudioWarning == null
                ) {
                    AlertDialog(
                        onDismissRequest = {
                            dismissNotificationPermissionExplanation()
                        },
                        title = {
                            Text(stringResource(R.string.notification_permission_dialog_title))
                        },
                        text = {
                            Text(stringResource(R.string.notification_permission_dialog_message))
                        },
                        confirmButton = {
                            TextButton(onClick = { requestNotificationPermission() }) {
                                Text(stringResource(R.string.notification_permission_dialog_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismissNotificationPermissionExplanation() }) {
                                Text(stringResource(R.string.notification_permission_dialog_dismiss))
                            }
                        }
                    )
                }
                uiState.pendingSpatialAudioWarning?.let { warning ->
                    AlertDialog(
                        onDismissRequest = { dismissSpatialAudioWarning() },
                        title = {
                            Text(stringResource(R.string.spatial_audio_warning_title))
                        },
                        text = {
                            val message = warning.nothingOrCmfProductName?.let { productName ->
                                stringResource(R.string.spatial_audio_warning_message, productName)
                            } ?: stringResource(R.string.spatial_audio_warning_message_generic)
                            Text(message)
                        },
                        confirmButton = {
                            TextButton(onClick = { dismissSpatialAudioWarning() }) {
                                Text(stringResource(R.string.dialog_ok))
                            }
                        }
                    )
                }
                if (showOpenReelPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showOpenReelPermissionDialog = false
                            pendingOpenReelGlyphMode = null
                        },
                        title = {
                            Text(stringResource(R.string.open_reel_permission_title))
                        },
                        text = {
                            Text(stringResource(R.string.open_reel_permission_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val requestedMode = pendingOpenReelGlyphMode
                                    if (
                                        requestedMode != null &&
                                        MediaSessionPlaybackGate.hasNotificationAccess(this@MainActivity)
                                    ) {
                                        showOpenReelPermissionDialog = false
                                        pendingOpenReelGlyphMode = null
                                        applyGlyphMode(requestedMode)
                                    } else {
                                        pendingOpenReelPermissionRequest = true
                                        pendingOpenReelPermissionSettingsLaunched = false
                                        showOpenReelPermissionDialog = false
                                        val opened = openNotificationAccessSettings(this@MainActivity)
                                        if (!opened) {
                                            pendingOpenReelPermissionRequest = false
                                            pendingOpenReelPermissionSettingsLaunched = false
                                            pendingOpenReelGlyphMode = null
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(R.string.settings_media_playback_only_open_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.open_reel_permission_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showOpenReelPermissionDialog = false
                                    pendingOpenReelGlyphMode = null
                                }
                            ) {
                                Text(stringResource(R.string.open_reel_permission_cancel))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun offerNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            SettingsPreferences.markNotificationPermissionPromptShown(this)
            return
        }
        if (!SettingsPreferences.hasShownNotificationPermissionPrompt(this)) {
            showNotificationPermissionExplanationDialog = true
        }
    }

    private fun requestNotificationPermission() {
        showNotificationPermissionExplanationDialog = false
        SettingsPreferences.markNotificationPermissionPromptShown(this)
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun dismissNotificationPermissionExplanation() {
        showNotificationPermissionExplanationDialog = false
        SettingsPreferences.markNotificationPermissionPromptShown(this)
    }

    private fun dismissSpatialAudioWarning() {
        CaptureUiStore.update { it.copy(pendingSpatialAudioWarning = null) }
    }

    private fun defaultParameterState(): CaptureUiState {
        return applyRouteAwareLatency(
            SettingsPreferences.defaultParameters().copy(
                glyphMode = GlyphDeviceCatalog.defaultGlyphModeForProfile(
                    GlyphDeviceCatalog.effectiveUiProfile(
                        actualProfile = deviceProfile,
                        phone4bEmulationEnabled = CaptureUiStore.state.phone4bEmulationEnabled,
                        debugDeviceProfileOverride = CaptureUiStore.state.debugDeviceProfileOverride
                    )
                ),
                phone4bEmulationEnabled = CaptureUiStore.state.phone4bEmulationEnabled,
                debugDeviceProfileOverride = CaptureUiStore.state.debugDeviceProfileOverride
            )
        )
    }

    private fun applyRouteAwareLatency(state: CaptureUiState): CaptureUiState {
        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
        return state.withResolvedLatency(bluetoothOutputActive)
    }

    private fun sanitizeParameterState(state: CaptureUiState): CaptureUiState {
        val parameters = SettingsPreferences.parameterStateOf(state)
        return applyRouteAwareLatency(parameters.copy(
            sensitivity = parameters.sensitivity.coerceIn(0.6f, 3.0f),
            noiseGate = parameters.noiseGate.coerceIn(0f, 0.35f),
            dynamics = parameters.dynamics.coerceIn(0.6f, 2.2f),
            outputGamma = parameters.outputGamma.coerceIn(0.6f, 2.6f),
            toneFocus = parameters.toneFocus.coerceIn(-1f, 1f),
            smoothing = parameters.smoothing.coerceIn(0.08f, 0.55f),
            autoScaleWindowSeconds = parameters.autoScaleWindowSeconds.coerceIn(5f, 60f),
            autoScaleOffset = parameters.autoScaleOffset.coerceIn(0f, 0.4f),
            latencyMs = parameters.latencyMs.coerceIn(0f, 500f),
            defaultOutputLatencyMs = parameters.defaultOutputLatencyMs.coerceIn(0f, 500f),
            bluetoothLatencyMs = parameters.bluetoothLatencyMs.coerceIn(0f, 500f),
            glyphMode = GlyphDeviceCatalog.normalizeGlyphMode(
                GlyphDeviceCatalog.effectiveUiProfile(
                    actualProfile = deviceProfile,
                    phone4bEmulationEnabled = parameters.phone4bEmulationEnabled,
                    debugDeviceProfileOverride = parameters.debugDeviceProfileOverride
                ),
                parameters.glyphMode
            )
        ))
    }

    private fun syncCurrentParameters(updated: CaptureUiState = CaptureUiStore.state) {
        parameterSyncHandler.removeCallbacks(delayedParameterSyncRunnable)
        val routeAware = applyRouteAwareLatency(updated)
        if (routeAware != CaptureUiStore.state) {
            CaptureUiStore.update { routeAware }
        }
        GlyphVisualizerService.updateSensitivity(
            this,
            routeAware.sensitivity,
            routeAware.noiseGate,
            routeAware.dynamics,
            routeAware.toneFocus,
            routeAware.smoothing,
            routeAware.smoothingBalance,
            routeAware.reverseDirection,
            routeAware.peakHoldEnabled,
            routeAware.glyphMode,
            routeAware.fillOtherGlyphLights,
            routeAware.binaryMode,
            routeAware.baseIndicatorEnabled,
            routeAware.levelAutoScale,
            routeAware.spectrumAutoScale,
            routeAware.allBrightnessAutoScale,
            routeAware.autoScaleWindowSeconds,
            routeAware.autoScaleOffset,
            routeAware.latencyMs,
            routeAware.mediaPlaybackOnlyEnabled,
            routeAware.experimentalVisualizerStabilizationEnabled,
            routeAware.experimentalVisualizerSignalWatchdogEnabled,
            routeAware.experimentalSpectrumDecayEnabled,
            routeAware.experimentalPerformanceOptimizationsEnabled,
            routeAware.matrixSmoothMotionEnabled,
            routeAware.turnOffWhenBackDown,
            routeAware.outputGamma,
            routeAware.oscilloscopeAutoTimeAxisEnabled,
            recordingLightIncluded = routeAware.recordingLightIncluded
        )
        SettingsPreferences.save(this, routeAware)
    }

    private fun scheduleParameterSync(delayMs: Long = 72L) {
        parameterSyncHandler.removeCallbacks(delayedParameterSyncRunnable)
        parameterSyncHandler.postDelayed(delayedParameterSyncRunnable, delayMs)
    }

    private fun applyParameterState(state: CaptureUiState) {
        val parameters = sanitizeParameterState(state)
        CaptureUiStore.update { current ->
            current.copy(
                sensitivity = parameters.sensitivity,
                noiseGate = parameters.noiseGate,
                dynamics = parameters.dynamics,
                outputGamma = parameters.outputGamma,
                toneFocus = parameters.toneFocus,
                smoothing = parameters.smoothing,
                smoothingBalance = parameters.smoothingBalance,
                autoScaleWindowSeconds = parameters.autoScaleWindowSeconds,
                autoScaleOffset = parameters.autoScaleOffset,
                reverseDirection = parameters.reverseDirection,
                peakHoldEnabled = parameters.peakHoldEnabled,
                glyphMode = parameters.glyphMode,
                fillOtherGlyphLights = parameters.fillOtherGlyphLights,
                binaryMode = parameters.binaryMode,
                baseIndicatorEnabled = parameters.baseIndicatorEnabled,
                recordingLightIncluded = parameters.recordingLightIncluded,
                levelAutoScale = parameters.levelAutoScale,
                spectrumAutoScale = parameters.spectrumAutoScale,
                allBrightnessAutoScale = parameters.allBrightnessAutoScale,
                experimentalVisualizerStabilizationEnabled = parameters.experimentalVisualizerStabilizationEnabled,
                experimentalVisualizerSignalWatchdogEnabled = parameters.experimentalVisualizerSignalWatchdogEnabled,
                experimentalSpectrumDecayEnabled = parameters.experimentalSpectrumDecayEnabled,
                experimentalPerformanceOptimizationsEnabled = parameters.experimentalPerformanceOptimizationsEnabled,
                matrixSmoothMotionEnabled = parameters.matrixSmoothMotionEnabled,
                oscilloscopeAutoTimeAxisEnabled = parameters.oscilloscopeAutoTimeAxisEnabled,
                turnOffWhenBackDown = parameters.turnOffWhenBackDown
            )
        }
        val updated = CaptureUiStore.state
        GlyphVisualizerService.updateSensitivity(
            this,
            updated.sensitivity,
            updated.noiseGate,
            updated.dynamics,
            updated.toneFocus,
            updated.smoothing,
            updated.smoothingBalance,
            updated.reverseDirection,
            updated.peakHoldEnabled,
            updated.glyphMode,
            updated.fillOtherGlyphLights,
            updated.binaryMode,
            updated.baseIndicatorEnabled,
            updated.levelAutoScale,
            updated.spectrumAutoScale,
            updated.allBrightnessAutoScale,
            updated.autoScaleWindowSeconds,
            updated.autoScaleOffset,
            updated.latencyMs,
            updated.mediaPlaybackOnlyEnabled,
            updated.experimentalVisualizerStabilizationEnabled,
            updated.experimentalVisualizerSignalWatchdogEnabled,
            updated.experimentalSpectrumDecayEnabled,
            updated.experimentalPerformanceOptimizationsEnabled,
            updated.matrixSmoothMotionEnabled,
            updated.turnOffWhenBackDown,
            updated.outputGamma,
            updated.oscilloscopeAutoTimeAxisEnabled,
            recordingLightIncluded = updated.recordingLightIncluded
        )
        SettingsPreferences.save(this, updated)
    }

    private fun exportParameters() {
        pendingExportContent = SettingsPreferences.exportJson(CaptureUiStore.state)
        exportParametersLauncher.launch("glyph-barty-parameters.json")
    }

    private fun importParameters() {
        importParametersLauncher.launch(arrayOf("application/json", "text/plain"))
    }

    private fun requestModeStart(mode: CaptureMode) {
        val requestStartedAt = SystemClock.elapsedRealtime()
        pendingStartMode = mode
        AppLogger.i(
            "MainActivity",
            "Mode start requested: mode=$mode permissionGranted=${
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            } btLikely=${AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)} musicActive=${AudioRouteDiagnostics.isMusicActive(this)}"
        )
        silentlyEnablePhone1GlyphDebugIfPossible()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            when (mode) {
                CaptureMode.VISUALIZER -> startVisualizerMode()
                CaptureMode.MEDIA_PROJECTION -> launchCaptureIntent()
            }
            AppLogger.i(
                "MainActivity",
                "Mode start dispatch finished: mode=$mode elapsedMs=${SystemClock.elapsedRealtime() - requestStartedAt}"
            )
            pendingStartMode = null
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onStart() {
        super.onStart()
        CaptureUiStore.setUiVisible(true)
        if (!CaptureUiStore.state.isCapturing) {
            CaptureUiStore.syncLiveFrameFromState()
        }
        if (pendingMediaPlaybackOnlyPermissionRequest) {
            pendingMediaPlaybackOnlyPermissionRequest = false
            if (MediaSessionPlaybackGate.hasNotificationAccess(this)) {
                applyMediaPlaybackOnlyEnabled(true)
            } else if (CaptureUiStore.state.mediaPlaybackOnlyEnabled) {
                applyMediaPlaybackOnlyEnabled(false)
            }
        }
        if (pendingOpenReelPermissionRequest) {
            finishPendingOpenReelPermissionRequest()
        }
        if (
            CaptureUiStore.state.mediaPlaybackOnlyEnabled &&
            !MediaSessionPlaybackGate.hasNotificationAccess(this)
        ) {
            applyMediaPlaybackOnlyEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingOpenReelPermissionRequest) {
            finishPendingOpenReelPermissionRequest()
        }
    }

    override fun onPause() {
        if (pendingOpenReelPermissionRequest) {
            pendingOpenReelPermissionSettingsLaunched = true
        }
        super.onPause()
    }

    override fun onStop() {
        CaptureUiStore.setUiVisible(false)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            STATE_PENDING_MEDIA_PLAYBACK_PERMISSION,
            pendingMediaPlaybackOnlyPermissionRequest
        )
        outState.putBoolean(
            STATE_PENDING_OPEN_REEL_PERMISSION,
            pendingOpenReelPermissionRequest
        )
        outState.putBoolean(
            STATE_PENDING_OPEN_REEL_SETTINGS_LAUNCHED,
            pendingOpenReelPermissionSettingsLaunched
        )
        pendingOpenReelGlyphMode?.let {
            outState.putString(STATE_PENDING_OPEN_REEL_MODE, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        CaptureUiStore.setUiVisible(false)
        parameterSyncHandler.removeCallbacks(delayedParameterSyncRunnable)
        runCatching { Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener) }
        super.onDestroy()
    }

    private fun applyMediaPlaybackOnlyEnabled(enabled: Boolean) {
        val safeEnabled = enabled && MediaSessionPlaybackGate.hasNotificationAccess(this)
        val updated = CaptureUiStore.state.copy(mediaPlaybackOnlyEnabled = safeEnabled)
        CaptureUiStore.update { updated }
        GlyphVisualizerService.updateSensitivity(
            this,
            updated.sensitivity,
            updated.noiseGate,
            updated.dynamics,
            updated.toneFocus,
            updated.smoothing,
            updated.smoothingBalance,
            updated.reverseDirection,
            updated.peakHoldEnabled,
            updated.glyphMode,
            updated.fillOtherGlyphLights,
            updated.binaryMode,
            updated.baseIndicatorEnabled,
            updated.levelAutoScale,
            updated.spectrumAutoScale,
            updated.allBrightnessAutoScale,
            updated.autoScaleWindowSeconds,
            updated.autoScaleOffset,
            updated.latencyMs,
            updated.mediaPlaybackOnlyEnabled,
            updated.experimentalVisualizerStabilizationEnabled,
            updated.experimentalVisualizerSignalWatchdogEnabled,
            updated.experimentalSpectrumDecayEnabled,
            updated.experimentalPerformanceOptimizationsEnabled,
            updated.matrixSmoothMotionEnabled,
            updated.turnOffWhenBackDown,
            updated.outputGamma,
            updated.oscilloscopeAutoTimeAxisEnabled
        )
        SettingsPreferences.save(this, updated)
    }

    private fun requestGlyphModeChange(newMode: String) {
        if (newMode == CaptureUiStore.state.glyphMode) return
        if (GlyphPatternRegistry.requiresNotificationAccess(newMode)) {
            if (MediaSessionPlaybackGate.hasNotificationAccess(this)) {
                applyGlyphMode(newMode)
                return
            }
            pendingOpenReelGlyphMode = newMode
            showOpenReelPermissionDialog = true
            return
        }
        applyGlyphMode(newMode)
    }

    private fun applyGlyphMode(newMode: String) {
        CaptureUiStore.update { it.copy(glyphMode = newMode) }
        syncCurrentParameters()
    }

    private fun finishPendingOpenReelPermissionRequest() {
        if (!pendingOpenReelPermissionSettingsLaunched) return
        pendingOpenReelPermissionRequest = false
        pendingOpenReelPermissionSettingsLaunched = false
        val requestedMode = pendingOpenReelGlyphMode
        if (requestedMode != null && MediaSessionPlaybackGate.hasNotificationAccess(this)) {
            applyGlyphMode(requestedMode)
        } else {
            Toast.makeText(
                this,
                getString(R.string.open_reel_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingOpenReelGlyphMode = null
    }

    private fun silentlyEnablePhone1GlyphDebugIfPossible() {
        if (
            Phone1GlyphDebugHelper.supports(currentEffectiveUiDeviceProfile()) &&
            CaptureUiStore.state.autoEnablePhone1GlyphDebugOnStart
        ) {
            AppLogger.i("Phone1GlyphDebug", "Attempting silent Phone (1) glyph debug enable on mode start")
            requestPhone1GlyphDebug(manual = false)
        }
    }

    private fun currentEffectiveUiDeviceProfile(): GlyphDeviceProfile {
        val state = CaptureUiStore.state
        return GlyphDeviceCatalog.effectiveUiProfile(
            actualProfile = deviceProfile,
            phone4bEmulationEnabled = state.phone4bEmulationEnabled,
            debugDeviceProfileOverride = state.debugDeviceProfileOverride
        )
    }

    private fun requestPhone1GlyphDebug(manual: Boolean) {
        val effectiveUiProfile = currentEffectiveUiDeviceProfile()
        val debugAllowed = Phone1GlyphDebugHelper.supports(effectiveUiProfile)
        AppLogger.i(
            "Phone1GlyphDebug",
            "requestPhone1GlyphDebug manual=$manual debugAllowed=$debugAllowed actualProfile=$deviceProfile effectiveUiProfile=$effectiveUiProfile"
        )
        if (!debugAllowed) {
            AppLogger.i("Phone1GlyphDebug", "Skipping debug request because debug controls are not allowed")
            return
        }
        val hasPermission = Phone1GlyphDebugHelper.hasPermission(this)
        val shizukuAvailable = Phone1GlyphDebugHelper.isShizukuAvailable(this)
        val backendAvailable = Phone1GlyphDebugHelper.isBackendAvailable(this)
        AppLogger.i(
            "Phone1GlyphDebug",
            "Debug request state hasPermission=$hasPermission shizukuAvailable=$shizukuAvailable backendAvailable=$backendAvailable"
        )
        when {
            hasPermission -> enablePhone1GlyphDebug(manual)
            shizukuAvailable -> {
                AppLogger.i(
                    "Phone1GlyphDebug",
                    if (manual) {
                        "Manual request will ask Shizuku permission"
                    } else {
                        "Automatic request will ask Shizuku permission"
                    }
                )
                pendingPhone1GlyphDebugPermissionRequestFromManual = manual
                runCatching {
                    Phone1GlyphDebugHelper.requestPermission(this, PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE)
                }.onFailure {
                    AppLogger.w("Phone1GlyphDebug", "Shizuku permission request failed", it)
                    pendingPhone1GlyphDebugPermissionRequestFromManual = false
                    if (manual) {
                        showPhone1GlyphDebugPermissionDialog = true
                    }
                }
            }
            !backendAvailable -> {
                AppLogger.i("Phone1GlyphDebug", "No Shizuku/Sui backend available; showing dialog only for manual flow")
                if (manual) {
                    showPhone1GlyphDebugPermissionDialog = true
                }
            }
            else -> Unit
        }
    }

    private fun enablePhone1GlyphDebug(manual: Boolean) {
        val debugAllowed = Phone1GlyphDebugHelper.supports(currentEffectiveUiDeviceProfile())
        AppLogger.i(
            "Phone1GlyphDebug",
            "enablePhone1GlyphDebug manual=$manual debugAllowed=$debugAllowed"
        )
        if (!debugAllowed) {
            AppLogger.i("Phone1GlyphDebug", "Skipping enable because debug controls are not allowed")
            return
        }
        val result = Phone1GlyphDebugHelper.enableGlyphDebug(this)
        if (result.isSuccess) {
            AppLogger.i("Phone1GlyphDebug", "enablePhone1GlyphDebug completed successfully")
            Toast.makeText(
                this,
                getString(R.string.phone1_glyph_debug_success),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            AppLogger.w(
                "Phone1GlyphDebug",
                "enablePhone1GlyphDebug failed",
                result.exceptionOrNull()
            )
        }
        if (manual && result.isFailure) {
            showPhone1GlyphDebugPermissionDialog = true
        }
    }

    private fun startVisualizerMode() {
        val uiState = CaptureUiStore.state
        val dispatchStartedAt = SystemClock.elapsedRealtime()
        try {
            AppLogger.i(
                "MainActivity",
                "Dispatching Visualizer start to service: glyphMode=${uiState.glyphMode} latencyMs=${uiState.latencyMs} btLikely=${AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)}"
            )
            GlyphVisualizerService.startVisualizer(
                this,
                uiState.sensitivity,
                uiState.noiseGate,
                uiState.dynamics,
                uiState.toneFocus,
                uiState.smoothing,
                uiState.smoothingBalance,
                uiState.reverseDirection,
                uiState.peakHoldEnabled,
                uiState.glyphMode,
                uiState.fillOtherGlyphLights,
                uiState.binaryMode,
                uiState.baseIndicatorEnabled,
                uiState.levelAutoScale,
                uiState.spectrumAutoScale,
                uiState.allBrightnessAutoScale,
                uiState.autoScaleWindowSeconds,
                uiState.autoScaleOffset,
                uiState.latencyMs,
                uiState.mediaPlaybackOnlyEnabled,
                uiState.experimentalVisualizerStabilizationEnabled,
                uiState.experimentalVisualizerSignalWatchdogEnabled,
                uiState.experimentalSpectrumDecayEnabled,
                uiState.experimentalPerformanceOptimizationsEnabled,
                uiState.matrixSmoothMotionEnabled,
                uiState.turnOffWhenBackDown,
                uiState.outputGamma,
                uiState.oscilloscopeAutoTimeAxisEnabled,
                recordingLightIncluded = uiState.recordingLightIncluded,
                startSource = VisualizerStartSource.APP
            )
            AppLogger.i(
                "MainActivity",
                "Visualizer start dispatched to service in ${SystemClock.elapsedRealtime() - dispatchStartedAt}ms"
            )
        } catch (error: Throwable) {
            val msg = getString(
                R.string.status_no_capture_start_failed,
                error.message ?: getString(R.string.status_unknown_error)
            )
            CaptureUiStore.update {
                it.copy(statusText = msg, logMessage = msg)
            }
        }
    }

    private fun launchCaptureIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            CaptureUiStore.update {
                it.copy(statusText = getString(R.string.status_audio_capture_requires_android10))
            }
            return
        }
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlyphVisualizerApp(
    statusText: String,
    initialSetupPending: Boolean,
    onInitialSetupCompleted: () -> Unit,
    isCapturing: Boolean,
    heroTitle: String,
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    smoothingBalance: Float,
    autoScaleWindowSeconds: Float,
    autoScaleOffset: Float,
    latencyMs: Float,
    defaultOutputLatencyMs: Float,
    bluetoothLatencyMs: Float,
    latencyAutoSwitchEnabled: Boolean,
    isBluetoothOutputActive: Boolean,
    reverseDirection: Boolean,
    meterSegments: Int,
    activeMode: String,
    glyphMode: String,
    fillOtherGlyphLights: Boolean,
    deviceProfile: GlyphDeviceProfile,
    actualDeviceProfile: GlyphDeviceProfile,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    isPhone1Device: Boolean,
    binaryMode: Boolean,
    matrixSmoothMotionEnabled: Boolean,
    oscilloscopeAutoTimeAxisEnabled: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    phone4bEmulationEnabled: Boolean,
    debugDeviceProfileOverride: GlyphDeviceProfile?,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    mediaProjectionEnabled: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    mainScreenUiIsolationEnabled: Boolean,
    automaticUpdateCheckEnabled: Boolean,
    mediaPlaybackOnlyEnabled: Boolean,
    experimentalVisualizerStabilizationEnabled: Boolean,
    experimentalVisualizerSignalWatchdogEnabled: Boolean,
    experimentalPerformanceOptimizationsEnabled: Boolean,
    autoEnablePhone1GlyphDebugOnStart: Boolean,
    nothingStyleEnabled: Boolean,
    experimentalMainUiEnabled: Boolean,
    detailedHomeEnabled: Boolean,
    turnOffWhenBackDown: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onSmoothingBalanceChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChangeFinished: () -> Unit,
    onAutoScaleOffsetChanged: (Float) -> Unit,
    onAutoScaleOffsetChangeFinished: () -> Unit,
    onLatencyMsChanged: (Float) -> Unit,
    onLatencyMsChangeFinished: () -> Unit,
    onLatencyAutoSwitchChanged: (Boolean) -> Unit,
    onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    onMeterVisibleEnabledChanged: (Boolean) -> Unit,
    onLightweightMeterEnabledChanged: (Boolean) -> Unit,
    onSpectrumMeterEnabledChanged: (Boolean) -> Unit,
    onNativeMeterViewEnabledChanged: (Boolean) -> Unit,
    onAutomaticUpdateCheckEnabledChanged: (Boolean) -> Unit,
    onMediaPlaybackOnlyEnabledChanged: (Boolean) -> Unit,
    onExperimentalVisualizerStabilizationEnabledChanged: (Boolean) -> Unit,
    onExperimentalVisualizerSignalWatchdogEnabledChanged: (Boolean) -> Unit,
    onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    onAutoEnablePhone1GlyphDebugOnStartChanged: (Boolean) -> Unit,
    onRecordingLightBehaviorChanged: (RecordingLightBehavior) -> Unit,
    onPhone4bEmulationEnabledChanged: (Boolean) -> Unit,
    onDebugDeviceProfileOverrideChanged: (GlyphDeviceProfile?) -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    onNothingStyleEnabledChanged: (Boolean) -> Unit,
    onExperimentalMainUiEnabledChanged: (Boolean) -> Unit,
    onDetailedHomeEnabledChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onStopClick: () -> Unit,
    logMessage: String?,
    onDismissLog: () -> Unit,
    showPhone1GlyphDebugPermissionDialog: Boolean,
    onDismissPhone1GlyphDebugPermissionDialog: () -> Unit
) {
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
        if (automaticUpdateCheckEnabled && shouldCheckForUpdates(context)) {
            val result = withContext(Dispatchers.IO) {
                checkGitHubReleaseUpdate(
                    repositoryUrl = repositoryUrl,
                    showLatestForTesting = showLatestForTesting
                )
            }
            markUpdateCheckFinished(context)
            result.onSuccess { updateInfo ->
                if (
                    updateInfo != null &&
                    (showLatestForTesting || !isUpdateNotificationDismissed(context, updateInfo.tagName))
                ) {
                    availableUpdate = updateInfo
                    updateNotification = updateInfo
                }
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
                            onRecordingLightBehaviorChanged = onRecordingLightBehaviorChanged,
                            onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
                            onOpenDetails = { screen = Screen.DETAILS },
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
                        onBinaryModeChanged = onBinaryModeChanged,
                        onMatrixSmoothMotionEnabledChanged = onMatrixSmoothMotionEnabledChanged,
                        onOscilloscopeAutoTimeAxisEnabledChanged = onOscilloscopeAutoTimeAxisEnabledChanged,
                        onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                        onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                        onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
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
                        onBack = { screen = Screen.SETTINGS }
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

@Composable
private fun WelcomeScreen(
    nothingStyleEnabled: Boolean,
    onNothingStyleEnabledChanged: (Boolean) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(WelcomeStep.INTRO) }
    val welcomeHeadingFontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Crossfade(
            targetState = nothingStyleEnabled,
            animationSpec = tween(durationMillis = 280),
            label = "welcome_theme_crossfade"
        ) { themedNothingStyleEnabled ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (isSystemInDarkTheme()) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = when (step) {
                                WelcomeStep.INTRO -> stringResource(R.string.welcome_intro_title)
                                WelcomeStep.UI_MODE -> stringResource(R.string.welcome_ui_mode_title)
                                WelcomeStep.FEATURES -> stringResource(R.string.welcome_features_title)
                                WelcomeStep.UPDATE_CHECK -> stringResource(R.string.welcome_update_check_title)
                            },
                            style = if (themedNothingStyleEnabled) {
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = welcomeHeadingFontFamily
                                )
                            } else {
                                MaterialTheme.typography.headlineMedium
                            }
                        )
                        Text(
                            text = stringResource(
                                R.string.welcome_step_counter,
                                step.ordinal + 1,
                                WelcomeStep.entries.size
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                if (forward) {
                                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { -it / 4 } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it / 4 } + fadeOut())
                                }
                            },
                            label = "welcome_step_transition"
                        ) { currentStep ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                when (currentStep) {
                                    WelcomeStep.INTRO -> {
                                        Text(
                                            text = stringResource(R.string.welcome_intro_body),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = { step = WelcomeStep.UI_MODE },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.welcome_next))
                                        }
                                    }

                                    WelcomeStep.UI_MODE -> {
                                        Text(
                                            text = stringResource(R.string.welcome_ui_mode_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                WelcomeRadioOption(
                                                    label = stringResource(R.string.settings_ui_mode_nothing),
                                                    selected = themedNothingStyleEnabled,
                                                    onClick = { onNothingStyleEnabledChanged(true) }
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                                WelcomeRadioOption(
                                                    label = stringResource(R.string.settings_ui_mode_material),
                                                    selected = !themedNothingStyleEnabled,
                                                    onClick = { onNothingStyleEnabledChanged(false) }
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { step = WelcomeStep.INTRO },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.welcome_back))
                                            }
                                            Button(
                                                onClick = { step = WelcomeStep.FEATURES },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.welcome_next))
                                            }
                                        }
                                    }

                                    WelcomeStep.FEATURES -> {
                                        Text(
                                            text = stringResource(R.string.welcome_features_body),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.welcome_feature_live_meter),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = stringResource(R.string.welcome_feature_latency),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = stringResource(R.string.welcome_feature_media_only),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { step = WelcomeStep.UI_MODE },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.welcome_back))
                                            }
                                            Button(
                                                onClick = { step = WelcomeStep.UPDATE_CHECK },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(stringResource(R.string.welcome_next))
                                            }
                                        }
                                    }

                                    WelcomeStep.UPDATE_CHECK -> {
                                        Text(
                                            text = stringResource(R.string.welcome_update_check_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = { onComplete(true) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.welcome_update_check_enable))
                                        }
                                        OutlinedButton(
                                            onClick = { onComplete(false) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.welcome_update_check_skip))
                                        }
                                        TextButton(
                                            onClick = { step = WelcomeStep.FEATURES },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(stringResource(R.string.welcome_back))
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
private fun WelcomeRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    containerBrush: Brush,
    statusText: String,
    isCapturing: Boolean,
    heroTitle: String,
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    latencyMs: Float,
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    smoothingBalance: Float,
    autoScaleWindowSeconds: Float,
    autoScaleOffset: Float,
    reverseDirection: Boolean,
    meterSegments: Int,
    activeMode: String,
    glyphMode: String,
    fillOtherGlyphLights: Boolean,
    deviceProfile: GlyphDeviceProfile,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    isPhone1Device: Boolean,
    binaryMode: Boolean,
    matrixSmoothMotionEnabled: Boolean,
    oscilloscopeAutoTimeAxisEnabled: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    mediaProjectionEnabled: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    mainScreenUiIsolationEnabled: Boolean,
    nothingStyleEnabled: Boolean,
    turnOffWhenBackDown: Boolean,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onSmoothingBalanceChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChangeFinished: () -> Unit,
    onAutoScaleOffsetChanged: (Float) -> Unit,
    onAutoScaleOffsetChangeFinished: () -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onRecordingLightBehaviorChanged: (RecordingLightBehavior) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    startPending: Boolean,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onStopClick: () -> Unit,
    logMessage: String?,
    onDismissLog: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLatency: (() -> Unit)? = null,
    onBackToExperimental: (() -> Unit)? = null,
    experimentalDetailsAsHome: Boolean = false,
    initialExperimentalDetailsTab: ExperimentalDetailsTab,
    onExperimentalDetailsTabChanged: (ExperimentalDetailsTab) -> Unit
) {
    val scrollState = rememberScrollState()
    val experimentalDetailsStyle = experimentalDetailsAsHome || onBackToExperimental != null
    if (experimentalDetailsStyle) {
        ExperimentalDetailsScreenContent(
            heroTitle = heroTitle,
            statusText = statusText,
            logMessage = logMessage,
            isCapturing = isCapturing,
            startPending = startPending,
            latencyMs = latencyMs,
            sensitivity = sensitivity,
            noiseGate = noiseGate,
            dynamics = dynamics,
            outputGamma = outputGamma,
            toneFocus = toneFocus,
            smoothing = smoothing,
            autoScaleWindowSeconds = autoScaleWindowSeconds,
            autoScaleOffset = autoScaleOffset,
            reverseDirection = reverseDirection,
            glyphMode = glyphMode,
            fillOtherGlyphLights = fillOtherGlyphLights,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
            oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
            baseIndicatorEnabled = baseIndicatorEnabled,
            recordingLightIncluded = recordingLightIncluded,
            levelAutoScale = levelAutoScale,
            spectrumAutoScale = spectrumAutoScale,
            allBrightnessAutoScale = allBrightnessAutoScale,
            mediaProjectionEnabled = mediaProjectionEnabled,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            meterVisibleEnabled = meterVisibleEnabled,
            lightweightMeterEnabled = lightweightMeterEnabled,
            spectrumMeterEnabled = spectrumMeterEnabled,
            nativeMeterViewEnabled = nativeMeterViewEnabled,
            nothingStyleEnabled = nothingStyleEnabled,
            showPhone1GlyphDebugControls = isPhone1Device,
            isHome = experimentalDetailsAsHome,
            initialTab = initialExperimentalDetailsTab,
            onTabChanged = onExperimentalDetailsTabChanged,
            onResetParametersClick = onResetParametersClick,
            onExportParametersClick = onExportParametersClick,
            onImportParametersClick = onImportParametersClick,
            onSensitivityChanged = onSensitivityChanged,
            onNoiseGateChanged = onNoiseGateChanged,
            onDynamicsChanged = onDynamicsChanged,
            onOutputGammaChanged = onOutputGammaChanged,
            onSmoothingChanged = onSmoothingChanged,
            onToneFocusChanged = onToneFocusChanged,
            onAutoScaleWindowSecondsChanged = onAutoScaleWindowSecondsChanged,
            onAutoScaleWindowSecondsChangeFinished = onAutoScaleWindowSecondsChangeFinished,
            onAutoScaleOffsetChanged = onAutoScaleOffsetChanged,
            onAutoScaleOffsetChangeFinished = onAutoScaleOffsetChangeFinished,
            onReverseDirectionChanged = onReverseDirectionChanged,
            onGlyphModeChanged = onGlyphModeChanged,
            onFillOtherGlyphLightsChanged = onFillOtherGlyphLightsChanged,
            onBinaryModeChanged = onBinaryModeChanged,
            onMatrixSmoothMotionEnabledChanged = onMatrixSmoothMotionEnabledChanged,
            onOscilloscopeAutoTimeAxisEnabledChanged = onOscilloscopeAutoTimeAxisEnabledChanged,
            onLevelAutoScaleChanged = onLevelAutoScaleChanged,
            onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
            onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
            onRecordingLightBehaviorChanged = onRecordingLightBehaviorChanged,
            onStartVisualizerClick = onStartVisualizerClick,
            onStartProjectionClick = onStartProjectionClick,
            onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
            onStopClick = onStopClick,
            onOpenSettings = onOpenSettings,
            onOpenLatency = onOpenLatency ?: {},
            onBack = onBackToExperimental ?: {}
        )
        return
    }
    var heroBottomInRoot by remember { mutableStateOf(Float.POSITIVE_INFINITY) }
    var compactMeterDismissed by rememberSaveable { mutableStateOf(false) }
    val meterModel = if (!meterVisibleEnabled || lightweightMeterEnabled || spectrumMeterEnabled || nativeMeterViewEnabled) {
        null
    } else {
        remember(
            level,
            glyphMode,
            deviceProfile,
            binaryMode,
            glyphMeterPreviewEnabled,
            recordingLightIncluded,
            reverseDirection
        ) {
            buildUiMeterModel(
                level = level,
                meterSegments = meterSegments,
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection
            )
        }
    }
    val collapsedMeterVisible = heroBottomInRoot <= 0f
    LaunchedEffect(collapsedMeterVisible) {
        if (!collapsedMeterVisible) {
            compactMeterDismissed = false
        }
    }

    Scaffold(
        containerColor = if (experimentalDetailsStyle) Color.Black else Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (experimentalDetailsStyle) Color.Black else Color.Transparent,
                    navigationIconContentColor = if (experimentalDetailsStyle) Color.White else Color.Unspecified,
                    titleContentColor = if (experimentalDetailsStyle) Color.White else Color.Unspecified,
                    actionIconContentColor = if (experimentalDetailsStyle) Color.White else Color.Unspecified
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_menu)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            if (experimentalDetailsStyle) {
                                R.string.experimental_home_details
                            } else {
                                R.string.app_name
                            }
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (experimentalDetailsStyle) {
                        Modifier.background(Color.Black)
                    } else {
                        Modifier.background(containerBrush)
                    }
                )
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = if (experimentalDetailsStyle) 24.dp else 20.dp,
                            vertical = if (experimentalDetailsStyle) 18.dp else 12.dp
                        )
                        .padding(bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        if (experimentalDetailsStyle) 24.dp else 18.dp
                    )
                ) {
                    if (experimentalDetailsStyle) {
                        ExperimentalDetailsSummary(
                            heroTitle = heroTitle,
                            isCapturing = isCapturing,
                            glyphMode = glyphMode,
                            latencyMs = latencyMs,
                            onOpenLatency = onOpenLatency,
                            displayFont = FontFamily.SansSerif,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    } else if (mainScreenUiIsolationEnabled && meterVisibleEnabled) {
                        IsolatedHeroCard(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                heroBottomInRoot = coordinates.boundsInRoot().bottom
                            },
                            isCapturing = isCapturing,
                            statusText = statusText,
                            heroTitle = heroTitle,
                            sensitivity = sensitivity,
                            toneFocus = toneFocus,
                            smoothing = smoothing,
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            meterVisibleEnabled = meterVisibleEnabled,
                            lightweightMeterEnabled = lightweightMeterEnabled,
                            spectrumMeterEnabled = spectrumMeterEnabled,
                            nativeMeterViewEnabled = nativeMeterViewEnabled,
                            activeMode = activeMode,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    } else {
                        HeroCard(
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                heroBottomInRoot = coordinates.boundsInRoot().bottom
                            },
                            isCapturing = isCapturing,
                            statusText = statusText,
                            heroTitle = heroTitle,
                            level = level,
                            peak = peak,
                            spectrumBands = spectrumBands,
                            sensitivity = sensitivity,
                            toneFocus = toneFocus,
                            smoothing = smoothing,
                            meterModel = meterModel,
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            meterVisibleEnabled = meterVisibleEnabled,
                            lightweightMeterEnabled = lightweightMeterEnabled,
                            spectrumMeterEnabled = spectrumMeterEnabled,
                            nativeMeterViewEnabled = nativeMeterViewEnabled,
                            activeMode = activeMode,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    }

                    ControlCard(
                        isCapturing = isCapturing,
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
                        activeMode = activeMode,
                        glyphMode = glyphMode,
                        fillOtherGlyphLights = fillOtherGlyphLights,
                        deviceProfile = deviceProfile,
                        isPhone3Device = isPhone3Device,
                        isPhone4aProDevice = isPhone4aProDevice,
                        isPhone2aDevice = isPhone2aDevice,
                        isPhone3aDevice = isPhone3aDevice,
                        isPhone4aDevice = isPhone4aDevice,
                        isPhone1Device = isPhone1Device,
                        binaryMode = binaryMode,
                        matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                        oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                        baseIndicatorEnabled = baseIndicatorEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        levelAutoScale = levelAutoScale,
                        spectrumAutoScale = spectrumAutoScale,
                        allBrightnessAutoScale = allBrightnessAutoScale,
                        mediaProjectionEnabled = mediaProjectionEnabled,
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
                        onBinaryModeChanged = onBinaryModeChanged,
                        onMatrixSmoothMotionEnabledChanged = onMatrixSmoothMotionEnabledChanged,
                        onOscilloscopeAutoTimeAxisEnabledChanged = onOscilloscopeAutoTimeAxisEnabledChanged,
                        onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                        onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                        onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                        onRecordingLightBehaviorChanged = onRecordingLightBehaviorChanged,
                        onTurnOffWhenBackDownChanged = onTurnOffWhenBackDownChanged,
                        startPending = startPending,
                        onStartVisualizerClick = onStartVisualizerClick,
                        onStartProjectionClick = onStartProjectionClick,
                        onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
                        onStopClick = onStopClick,
                        experimentalDetailsStyle = experimentalDetailsStyle
                    )

                    if (experimentalDetailsStyle) {
                        ExperimentalDetailsInfoStrip(
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    } else {
                        InfoStrip()
                    }

                    MeterInfoSection(
                        statusText = statusText,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        logMessage = logMessage,
                        onDismissLog = onDismissLog,
                        experimentalDetailsStyle = experimentalDetailsStyle
                    )
                }

                AnimatedVisibility(
                    visible = meterVisibleEnabled && collapsedMeterVisible && !compactMeterDismissed,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (mainScreenUiIsolationEnabled) {
                        IsolatedCompactMeterOverlay(
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            lightweightMeterEnabled = lightweightMeterEnabled,
                            spectrumMeterEnabled = spectrumMeterEnabled,
                            nativeMeterViewEnabled = nativeMeterViewEnabled,
                            nothingStyleEnabled = nothingStyleEnabled,
                            onDismissUpward = { compactMeterDismissed = true }
                        )
                    } else {
                        CompactMeterOverlay(
                            level = level,
                            peak = peak,
                            spectrumBands = spectrumBands,
                            meterModel = meterModel,
                            lightweightMeterEnabled = lightweightMeterEnabled,
                            spectrumMeterEnabled = spectrumMeterEnabled,
                            nativeMeterViewEnabled = nativeMeterViewEnabled,
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            nothingStyleEnabled = nothingStyleEnabled,
                            onDismissUpward = { compactMeterDismissed = true }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LatencyScreenContent(
    containerBrush: Brush,
    latencyMs: Float,
    defaultOutputLatencyMs: Float,
    bluetoothLatencyMs: Float,
    latencyAutoSwitchEnabled: Boolean,
    isBluetoothOutputActive: Boolean,
    nothingStyleEnabled: Boolean,
    onLatencyMsChanged: (Float) -> Unit,
    onLatencyMsChangeFinished: () -> Unit,
    onLatencyAutoSwitchChanged: (Boolean) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    experimentalStyle: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val displayFont = if (nothingStyleEnabled) NTypeFontFamily else FontFamily.SansSerif
    var latencySliderValue by rememberSaveable { mutableStateOf(latencyMs) }

    LaunchedEffect(latencyMs) {
        latencySliderValue = latencyMs
    }

    Scaffold(
        containerColor = if (experimentalStyle) Color.Black else Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (experimentalStyle) Color.Black else Color.Transparent,
                    navigationIconContentColor = if (experimentalStyle) Color.White else Color.Unspecified,
                    titleContentColor = if (experimentalStyle) Color.White else Color.Unspecified,
                    actionIconContentColor = if (experimentalStyle) Color.White else Color.Unspecified
                ),
                navigationIcon = {
                    IconButton(onClick = onBack ?: onOpenMenu) {
                        Icon(
                            if (onBack == null) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                if (onBack == null) R.string.cd_menu else R.string.cd_back
                            )
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.latency_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = if (nothingStyleEnabled) displayFont else null
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (experimentalStyle) {
                        Modifier.background(Color.Black)
                    } else {
                        Modifier.background(containerBrush)
                    }
                )
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = if (experimentalStyle) 24.dp else 20.dp,
                        vertical = if (experimentalStyle) 18.dp else 12.dp
                    )
                    .padding(bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                androidx.compose.material3.Card(
                    shape = if (experimentalStyle) {
                        RoundedCornerShape(0.dp)
                    } else {
                        RoundedCornerShape(28.dp)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (experimentalStyle) {
                            Color.Transparent
                        } else {
                            materialCardColor(prominent = true)
                        }
                    ),
                    border = if (experimentalStyle) null else materialCardBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (experimentalStyle) 0.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            if (experimentalStyle) 22.dp else 16.dp
                        )
                    ) {
                        if (experimentalStyle) {
                            HorizontalDivider(color = Color(0xFF3A3A3A))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.latency_auto_switch_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.latency_auto_switch_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StableSwitch(
                                checked = latencyAutoSwitchEnabled,
                                onCheckedChange = onLatencyAutoSwitchChanged
                            )
                        }

                        if (latencyAutoSwitchEnabled) {
                            Text(
                                text = if (isBluetoothOutputActive) {
                                    stringResource(
                                        R.string.latency_active_route_with_value,
                                        stringResource(R.string.latency_route_name_bluetooth),
                                        bluetoothLatencyMs
                                    )
                                } else {
                                    stringResource(
                                        R.string.latency_active_route_with_value,
                                        stringResource(R.string.latency_route_name_default),
                                        defaultOutputLatencyMs
                                    )
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ParameterSlider(
                            title = stringResource(R.string.latency_slider_title),
                            valueText = stringResource(R.string.latency_value_ms, latencySliderValue),
                            description = if (latencyAutoSwitchEnabled) {
                                if (isBluetoothOutputActive) {
                                    stringResource(R.string.latency_slider_desc_bluetooth)
                                } else {
                                    stringResource(R.string.latency_slider_desc_default)
                                }
                            } else {
                                stringResource(R.string.latency_slider_desc)
                            },
                            value = latencySliderValue,
                            onValueChange = { newValue ->
                                latencySliderValue = newValue
                            },
                            onValueChangeFinished = {
                                onLatencyMsChanged(latencySliderValue)
                                onLatencyMsChangeFinished()
                            },
                            valueRange = 0f..500f,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                        if (experimentalStyle) {
                            HorizontalDivider(color = Color(0xFF3A3A3A))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentalScreenContent(
    containerBrush: Brush,
    actualDeviceProfile: GlyphDeviceProfile,
    phone4bEmulationEnabled: Boolean,
    debugDeviceProfileOverride: GlyphDeviceProfile?,
    isCapturing: Boolean,
    nothingStyleEnabled: Boolean,
    onPhone4bEmulationEnabledChanged: (Boolean) -> Unit,
    onDebugDeviceProfileOverrideChanged: (GlyphDeviceProfile?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusText by rememberSaveable { mutableStateOf("") }
    var showDeviceProfileDialog by rememberSaveable { mutableStateOf(false) }
    val isActualPhone4a = actualDeviceProfile == GlyphDeviceProfile.PHONE4A
    val isActualPhone4b = actualDeviceProfile == GlyphDeviceProfile.PHONE4B
    val emulatedOnPhone4a = isActualPhone4a &&
        (phone4bEmulationEnabled || debugDeviceProfileOverride == GlyphDeviceProfile.PHONE4B)
    val automaticDeviceProfile = GlyphDeviceCatalog.effectiveProfile(
        actualProfile = actualDeviceProfile,
        phone4bEmulationEnabled = phone4bEmulationEnabled
    )
    val selectedDeviceProfile = debugDeviceProfileOverride ?: automaticDeviceProfile
    val showProbeControls = isActualPhone4b || emulatedOnPhone4a
    val probe = remember(actualDeviceProfile, emulatedOnPhone4a) {
        Phone4aAsPhone4bGlyphProbe(
            context = context,
            emulatedOnPhone4a = emulatedOnPhone4a
        ) { message ->
            statusText = message
        }
    }

    LaunchedEffect(probe, showProbeControls, isCapturing) {
        if (showProbeControls && !isCapturing) {
            if (GlyphVisualizerService.isRunning(context)) {
                GlyphVisualizerService.stop(context)
                delay(200L)
            }
            probe.bind()
        }
    }
    DisposableEffect(probe, showProbeControls, isCapturing) {
        onDispose {
            probe.release()
        }
    }

    if (showDeviceProfileDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceProfileDialog = false },
            title = { Text(stringResource(R.string.experimental_device_profile_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExperimentalDeviceProfileOption(
                        title = stringResource(
                            R.string.experimental_device_profile_automatic,
                            debugDeviceProfileLabel(automaticDeviceProfile)
                        ),
                        selected = debugDeviceProfileOverride == null,
                        onClick = {
                            onDebugDeviceProfileOverrideChanged(null)
                            showDeviceProfileDialog = false
                        }
                    )
                    GlyphDeviceProfile.entries.forEach { profile ->
                        ExperimentalDeviceProfileOption(
                            title = debugDeviceProfileLabel(profile),
                            selected = debugDeviceProfileOverride == profile,
                            onClick = {
                                onDebugDeviceProfileOverrideChanged(profile)
                                showDeviceProfileDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeviceProfileDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.experimental_screen_title),
                        fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerBrush)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val profileSummary = if (debugDeviceProfileOverride == null) {
                    stringResource(
                        R.string.experimental_device_profile_automatic,
                        debugDeviceProfileLabel(automaticDeviceProfile)
                    )
                } else {
                    debugDeviceProfileLabel(selectedDeviceProfile)
                }
                val profileDescription = buildString {
                    append(stringResource(R.string.experimental_device_profile_desc))
                    append('\n')
                    append(
                        stringResource(
                            R.string.experimental_device_profile_current,
                            profileSummary
                        )
                    )
                    if (isCapturing) {
                        append('\n')
                        append(stringResource(R.string.experimental_device_profile_stop_first))
                    }
                }
                SettingsEntry(
                    title = stringResource(R.string.experimental_device_profile_title),
                    description = profileDescription,
                    onClick = {
                        if (!isCapturing) showDeviceProfileDialog = true
                    },
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                )

                if (!isActualPhone4a && !isActualPhone4b) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.experimental_unsupported_device),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    return@Column
                }

                if (!showProbeControls) {
                    return@Column
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = if (nothingStyleEnabled) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.experimental_phone4b_probe_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                if (emulatedOnPhone4a) {
                                    R.string.experimental_p4a_as_p4b_probe_desc
                                } else {
                                    R.string.experimental_phone4b_native_probe_desc
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.experimental_phone4b_probe_how_to),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.experimental_phone4b_probe_steps),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCapturing) {
                            Text(
                                text = stringResource(R.string.experimental_p4a_as_p4b_probe_running_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = if (statusText.isBlank()) {
                                stringResource(R.string.experimental_p4a_as_p4b_probe_status_waiting)
                            } else {
                                statusText
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.experimental_p4a_as_p4b_probe_channels),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed((0..3).toList()) { _, channel ->
                                FilterChip(
                                    selected = false,
                                    onClick = { probe.probe(channel) },
                                    enabled = !isCapturing,
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.experimental_p4a_as_p4b_glyph_channel,
                                                channel + 1,
                                                if (emulatedOnPhone4a) channel + 2 else channel
                                            )
                                        )
                                    }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { probe.probe(4) },
                                    enabled = !isCapturing,
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.experimental_p4a_as_p4b_recording_light,
                                                if (emulatedOnPhone4a) 6 else 4
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { probe.probeAll(includeRecordingLight = false) },
                                enabled = !isCapturing
                            ) {
                                Text(stringResource(R.string.experimental_p4a_as_p4b_all_glyphs))
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { probe.probeAll(includeRecordingLight = true) },
                                enabled = !isCapturing
                            ) {
                                Text(stringResource(R.string.experimental_p4a_as_p4b_all_with_recording))
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { probe.turnOff() },
                            enabled = !isCapturing
                        ) {
                            Text(stringResource(R.string.experimental_p4a_as_p4b_turn_off))
                        }
                    }
                }
            }
        }
    }
}

private fun debugDeviceProfileLabel(profile: GlyphDeviceProfile): String = when (profile) {
    GlyphDeviceProfile.PHONE1 -> "Phone (1)"
    GlyphDeviceProfile.PHONE2 -> "Phone (2)"
    GlyphDeviceProfile.PHONE2A -> "Phone (2a) Series"
    GlyphDeviceProfile.PHONE3A -> "Phone (3a) Series"
    GlyphDeviceProfile.PHONE4A -> "Phone (4a)"
    GlyphDeviceProfile.PHONE4B -> "Phone (4b)"
    GlyphDeviceProfile.PHONE3_MATRIX -> "Phone (3)"
    GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> "Phone (4a) Pro"
}

@Composable
private fun ExperimentalDeviceProfileOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun HomeDrawerOverlay(
    visible: Boolean,
    currentScreen: Screen,
    nothingStyleEnabled: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val drawerColor = when {
        nothingStyleEnabled && darkTheme -> Color(0xFF050505)
        nothingStyleEnabled -> Color(0xFFF5F5F5)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val selectedColor = when {
        nothingStyleEnabled && darkTheme -> Color(0xFF2A2A2A)
        nothingStyleEnabled -> Color(0xFFE7E7E7)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally { -it / 4 } + fadeIn(),
            exit = slideOutHorizontally { -it / 4 } + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f),
                shape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp),
                color = drawerColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HomeDrawerItem(
                    title = stringResource(R.string.menu_main),
                    icon = Icons.Default.PlayArrow,
                    selected = currentScreen == Screen.MAIN,
                    nothingStyleEnabled = nothingStyleEnabled,
                    selectedColor = selectedColor,
                    onClick = { onNavigate(Screen.MAIN) }
                )
                HomeDrawerItem(
                    title = stringResource(R.string.menu_latency),
                    icon = Icons.Default.Equalizer,
                    selected = currentScreen == Screen.LATENCY,
                    nothingStyleEnabled = nothingStyleEnabled,
                    selectedColor = selectedColor,
                    onClick = { onNavigate(Screen.LATENCY) }
                )
            }
            }
        }
    }
}

@Composable
private fun HomeDrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    nothingStyleEnabled: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val iconAndTextColor = when {
        nothingStyleEnabled && selected && isSystemInDarkTheme() -> Color(0xFFF2F2F2)
        nothingStyleEnabled && selected -> Color(0xFF151515)
        selected -> MaterialTheme.colorScheme.onSurface
        nothingStyleEnabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (selected) selectedColor else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconAndTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = iconAndTextColor
            )
        }
    }
}

@Composable
private fun MeterInfoSection(
    statusText: String,
    noiseGate: Float,
    dynamics: Float,
    logMessage: String?,
    onDismissLog: () -> Unit,
    experimentalDetailsStyle: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(if (experimentalDetailsStyle) 0.dp else 16.dp),
            color = if (experimentalDetailsStyle) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = BorderStroke(
                1.dp,
                if (experimentalDetailsStyle) Color(0xFF3A3A3A) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.log_section_hide)
                        } else {
                            stringResource(R.string.log_section_show)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (expanded) {
                            stringResource(R.string.cd_collapse)
                        } else {
                            stringResource(R.string.cd_expand)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(
                            R.string.hero_gate_dynamics,
                            (noiseGate * 100).toInt(),
                            dynamics
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (statusText.isNotBlank()) {
                    StatusMessageCard(message = statusText)
                }

                if (!logMessage.isNullOrBlank() && logMessage != statusText) {
                    LogCard(
                        message = logMessage,
                        onDismiss = onDismissLog
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMessageCard(
    message: String
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LogCard(
    message: String,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(message) { expanded = true }

    androidx.compose.material3.Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.log_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) {
                            stringResource(R.string.cd_collapse)
                        } else {
                            stringResource(R.string.cd_expand)
                        },
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_dismiss),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                    )
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun responseSpeedValueText(smoothing: Float): String {
    return if (smoothing >= RESPONSE_SPEED_NONE_THRESHOLD) {
        stringResource(R.string.response_speed_none)
    } else {
        stringResource(R.string.percent_value, (smoothing * 100).toInt())
    }
}

@Composable
private fun HeroCard(
    modifier: Modifier = Modifier,
    isCapturing: Boolean,
    statusText: String,
    heroTitle: String,
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    sensitivity: Float,
    toneFocus: Float,
    smoothing: Float,
    meterModel: UiMeterModel?,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    activeMode: String,
    nothingStyleEnabled: Boolean
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = materialCardColor(prominent = true)
        ),
        border = materialCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = heroTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.hero_sensitivity,
                            (sensitivity * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.hero_response_speed,
                            responseSpeedValueText(smoothing)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when {
                            toneFocus < -0.1f -> stringResource(
                                R.string.hero_tone_focus_bass,
                                (toneFocus * -100).toInt()
                            )
                            toneFocus > 0.1f -> stringResource(
                                R.string.hero_tone_focus_treble,
                                (toneFocus * 100).toInt()
                            )
                            else -> stringResource(R.string.hero_tone_focus_balanced)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val statusChipBackground = if (isCapturing) {
                        if (nothingStyleEnabled) NothingRed else MaterialTheme.colorScheme.primary
                    } else if (nothingStyleEnabled) {
                        if (isSystemInDarkTheme()) Color(0xFF2A2A2A) else Color(0xFFF2F2F2)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val statusChipLabelColor = if (isCapturing) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    FilterChip(
                        selected = isCapturing,
                        onClick = {},
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = statusChipBackground,
                            selectedContainerColor = statusChipBackground,
                            labelColor = statusChipLabelColor,
                            selectedLabelColor = statusChipLabelColor
                        ),
                        label = {
                            Text(
                                if (isCapturing) {
                                    stringResource(R.string.capture_state_live)
                                } else {
                                    stringResource(R.string.capture_state_idle)
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeModeLabel(activeMode),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Spectrum表示は現在無効 (コードは保持)
            if (meterVisibleEnabled) {
                if (spectrumMeterEnabled && nativeMeterViewEnabled) {
                    DirectNativeSpectrumMeterCanvas(
                        glyphMode = glyphMode,
                        deviceProfile = deviceProfile,
                        recordingLightIncluded = recordingLightIncluded,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                } else if (spectrumMeterEnabled) {
                    SpectrumMeterCanvas(
                        level = level,
                        spectrumBands = spectrumBands,
                        glyphMode = glyphMode,
                        deviceProfile = deviceProfile,
                        recordingLightIncluded = recordingLightIncluded,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                } else if (nativeMeterViewEnabled && lightweightMeterEnabled) {
                    DirectNativeMeterCanvas(
                        glyphMode = glyphMode,
                        deviceProfile = deviceProfile,
                        binaryMode = binaryMode,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        reverseDirection = reverseDirection,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                } else if (lightweightMeterEnabled) {
                    LightweightMeterCanvas(
                        level = level,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                } else {
                    if (nativeMeterViewEnabled) {
                        DirectNativeDetailedMeterCanvas(
                            glyphMode = glyphMode,
                            deviceProfile = deviceProfile,
                            binaryMode = binaryMode,
                            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                            recordingLightIncluded = recordingLightIncluded,
                            reverseDirection = reverseDirection,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    } else {
                        meterModel?.let {
                            MeterCanvas(
                                level = level,
                                peak = peak,
                                meterModel = it,
                                nothingStyleEnabled = nothingStyleEnabled
                            )
                        }
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IsolatedHeroCard(
    modifier: Modifier = Modifier,
    isCapturing: Boolean,
    statusText: String,
    heroTitle: String,
    sensitivity: Float,
    toneFocus: Float,
    smoothing: Float,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    activeMode: String,
    nothingStyleEnabled: Boolean
) {
    if (nativeMeterViewEnabled) {
        HeroCard(
            modifier = modifier,
            isCapturing = isCapturing,
            statusText = statusText,
            heroTitle = heroTitle,
            level = 0f,
            peak = 0f,
            spectrumBands = FloatArray(0),
            sensitivity = sensitivity,
            toneFocus = toneFocus,
            smoothing = smoothing,
            meterModel = null,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            meterVisibleEnabled = meterVisibleEnabled,
            lightweightMeterEnabled = lightweightMeterEnabled,
            spectrumMeterEnabled = spectrumMeterEnabled,
            nativeMeterViewEnabled = nativeMeterViewEnabled,
            activeMode = activeMode,
            nothingStyleEnabled = nothingStyleEnabled
        )
    } else {
        val liveFrame = CaptureUiStore.liveFrame
        val meterModel = if (lightweightMeterEnabled || spectrumMeterEnabled) {
            null
        } else {
            remember(
                liveFrame.level,
                liveFrame.meterSegments,
                glyphMode,
                deviceProfile,
                binaryMode,
                glyphMeterPreviewEnabled,
                recordingLightIncluded,
                reverseDirection
            ) {
                buildUiMeterModel(
                    level = liveFrame.level,
                    meterSegments = liveFrame.meterSegments,
                    glyphMode = glyphMode,
                    deviceProfile = deviceProfile,
                    binaryMode = binaryMode,
                    glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                    recordingLightIncluded = recordingLightIncluded,
                    reverseDirection = reverseDirection
                )
            }
        }
        HeroCard(
            modifier = modifier,
            isCapturing = isCapturing,
            statusText = statusText,
            heroTitle = heroTitle,
            level = liveFrame.level,
            peak = liveFrame.peak,
            spectrumBands = liveFrame.spectrumBands,
            sensitivity = sensitivity,
            toneFocus = toneFocus,
            smoothing = smoothing,
            meterModel = meterModel,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            meterVisibleEnabled = meterVisibleEnabled,
            lightweightMeterEnabled = lightweightMeterEnabled,
            spectrumMeterEnabled = spectrumMeterEnabled,
            nativeMeterViewEnabled = nativeMeterViewEnabled,
            activeMode = activeMode,
            nothingStyleEnabled = nothingStyleEnabled
        )
    }
}

@Composable
private fun CompactMeterOverlay(
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    meterModel: UiMeterModel?,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    nothingStyleEnabled: Boolean,
    onDismissUpward: () -> Unit
) {
    val density = LocalDensity.current
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val activeColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val animatedPeak by animateFloatAsState(
        targetValue = peak,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "compact-meter-peak"
    )

    androidx.compose.material3.Card(
        modifier = Modifier.pointerInput(Unit) {
            val dismissThresholdPx = with(density) { 28.dp.toPx() }
            var totalDrag = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalDrag < -dismissThresholdPx) {
                        onDismissUpward()
                    }
                    totalDrag = 0f
                },
                onDragCancel = {
                    totalDrag = 0f
                }
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (spectrumMeterEnabled && nativeMeterViewEnabled) {
                DirectNativeSpectrumMeterBar(
                    glyphMode = glyphMode,
                    deviceProfile = deviceProfile,
                    recordingLightIncluded = recordingLightIncluded,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
            } else if (spectrumMeterEnabled) {
                SpectrumMeterBar(
                    level = level,
                    spectrumBands = spectrumBands,
                    glyphMode = glyphMode,
                    deviceProfile = deviceProfile,
                    recordingLightIncluded = recordingLightIncluded,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
            } else if (nativeMeterViewEnabled && lightweightMeterEnabled) {
                DirectNativeMeterBar(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
            } else if (lightweightMeterEnabled) {
                LightweightMeterBar(
                    level = level,
                    nothingStyleEnabled = nothingStyleEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
            } else if (nativeMeterViewEnabled) {
                DirectNativeDetailedMeterBar(
                    glyphMode = glyphMode,
                    deviceProfile = deviceProfile,
                    binaryMode = binaryMode,
                    glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                    recordingLightIncluded = recordingLightIncluded,
                    reverseDirection = reverseDirection,
                    compact = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                val safeMeterModel = meterModel ?: return@Canvas
                val segmentCount = safeMeterModel.segmentCount.coerceAtLeast(1)
                val centerIndex = segmentCount / 2
                val gap = 4.dp.toPx()
                val totalGap = gap * (segmentCount - 1)
                val widthPerSegment = (size.width - totalGap) / segmentCount.toFloat()
                val top = 0f
                val height = size.height

                for (segment in 0 until segmentCount) {
                    val left = segment * (widthPerSegment + gap)
                    val intensity = safeMeterModel.segmentLevels.getOrElse(segment) { 0f }.coerceIn(0f, 1f)
                    val color = if (intensity > 0.001f) {
                        activeColor.copy(alpha = 0.18f + intensity * 0.82f)
                    } else {
                        inactiveColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(widthPerSegment, height),
                        cornerRadius = CornerRadius(widthPerSegment / 2f, widthPerSegment / 2f)
                    )
                }

                if (safeMeterModel.usesSymmetricCenterLayout) {
                    val centerPeakX = centerIndex * (widthPerSegment + gap) + (widthPerSegment / 2f)
                    val leftCenterPeakX = if (safeMeterModel.symmetricSeedCount == 2) {
                        (centerIndex - 1).coerceAtLeast(0) * (widthPerSegment + gap) + (widthPerSegment / 2f)
                    } else {
                        centerPeakX
                    }
                    val betweenCentersPeakX = (leftCenterPeakX + centerPeakX) / 2f
                    val maxPairDistance = symmetricPeakDistanceSteps(segmentCount, safeMeterModel.symmetricSeedCount)
                    val peakHalfWidth = if (safeMeterModel.centerDirectionReversed) widthPerSegment / 2f else 0f
                    val peakProgress = if (safeMeterModel.centerDirectionReversed) {
                        1f - animatedPeak.coerceIn(0f, 1f)
                    } else {
                        animatedPeak.coerceIn(0f, 1f)
                    }
                    val peakDistance = peakProgress * maxPairDistance
                    if (peakDistance <= 0.001f) {
                        if (safeMeterModel.centerDirectionReversed) {
                            val leftRestingPeakX = if (safeMeterModel.symmetricSeedCount == 2) leftCenterPeakX else centerPeakX
                            val rightRestingPeakX = centerPeakX
                            drawLine(
                                color = peakColor,
                                start = Offset((leftRestingPeakX - peakHalfWidth).coerceIn(0f, size.width), 0f),
                                end = Offset((leftRestingPeakX - peakHalfWidth).coerceIn(0f, size.width), size.height),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = peakColor,
                                start = Offset((rightRestingPeakX + peakHalfWidth).coerceIn(0f, size.width), 0f),
                                end = Offset((rightRestingPeakX + peakHalfWidth).coerceIn(0f, size.width), size.height),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        } else {
                            val restingPeakX = if (safeMeterModel.symmetricSeedCount == 2) betweenCentersPeakX else centerPeakX
                            drawLine(
                                color = peakColor,
                                start = Offset(restingPeakX, 0f),
                                end = Offset(restingPeakX, size.height),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    } else {
                        val leftPeakX: Float
                        val rightPeakX: Float
                        if (safeMeterModel.symmetricSeedCount == 2) {
                            val unitSpan = widthPerSegment + gap
                            if (peakDistance <= 1f) {
                                val halfGap = (centerPeakX - leftCenterPeakX) / 2f
                                leftPeakX = betweenCentersPeakX - (halfGap * peakDistance)
                                rightPeakX = betweenCentersPeakX + (halfGap * peakDistance)
                            } else {
                                val extraTravel = unitSpan * (peakDistance - 1f)
                                leftPeakX = leftCenterPeakX - extraTravel
                                rightPeakX = centerPeakX + extraTravel
                            }
                        } else {
                            val travelPerSide = (widthPerSegment + gap) * peakDistance
                            leftPeakX = centerPeakX - travelPerSide
                            rightPeakX = centerPeakX + travelPerSide
                        }
                        drawLine(
                            color = peakColor,
                            start = Offset((leftPeakX - peakHalfWidth).coerceIn(0f, size.width), 0f),
                            end = Offset((leftPeakX - peakHalfWidth).coerceIn(0f, size.width), size.height),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = peakColor,
                            start = Offset((rightPeakX + peakHalfWidth).coerceIn(0f, size.width), 0f),
                            end = Offset((rightPeakX + peakHalfWidth).coerceIn(0f, size.width), size.height),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    val peakX = ((animatedPeak.coerceIn(0f, 1f) * (segmentCount - 1).coerceAtLeast(0).toFloat()) * (widthPerSegment + gap)) +
                        (widthPerSegment / 2f)
                    drawLine(
                        color = peakColor,
                        start = Offset(peakX, 0f),
                        end = Offset(peakX, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            }

            Column(
                modifier = Modifier.width(88.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (nativeMeterViewEnabled) {
                    DirectNativeMeterStats(
                        glyphMode = glyphMode,
                        deviceProfile = deviceProfile,
                        binaryMode = binaryMode,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        reverseDirection = reverseDirection,
                        lightweightMode = lightweightMeterEnabled,
                        spectrumMode = spectrumMeterEnabled,
                        nothingStyleEnabled = nothingStyleEnabled,
                        compact = true,
                        modifier = Modifier.width(88.dp)
                    )
                } else {
                    val spectrumBandsForStats = if (spectrumMeterEnabled) {
                        normalizedSpectrumMeterBands(
                            spectrumBands,
                            glyphMode,
                            deviceProfile,
                            recordingLightIncluded
                        )
                    } else {
                        FloatArray(0)
                    }
                    MeterStat(
                        label = stringResource(R.string.meter_label_level),
                        value = stringResource(R.string.percent_value, (level * 100).toInt()),
                        nothingStyleEnabled = nothingStyleEnabled,
                        compact = true
                    )
                    MeterStat(
                        label = stringResource(R.string.meter_label_segments),
                        value = stringResource(
                            R.string.meter_segments_value,
                            if (spectrumMeterEnabled) {
                                spectrumBandsForStats.count { it * level.coerceIn(0f, 1f) > 0.001f }
                            } else {
                                meterModel?.activeSegments ?: (level.coerceIn(0f, 1f) * 16f).toInt().coerceIn(0, 16)
                            },
                            if (spectrumMeterEnabled) spectrumBandsForStats.size else meterModel?.segmentCount ?: 16
                        ),
                        nothingStyleEnabled = nothingStyleEnabled,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun IsolatedCompactMeterOverlay(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    nothingStyleEnabled: Boolean,
    onDismissUpward: () -> Unit
) {
    if (nativeMeterViewEnabled) {
        CompactMeterOverlay(
            level = 0f,
            peak = 0f,
            spectrumBands = FloatArray(0),
            meterModel = null,
            lightweightMeterEnabled = lightweightMeterEnabled,
            spectrumMeterEnabled = spectrumMeterEnabled,
            nativeMeterViewEnabled = nativeMeterViewEnabled,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            nothingStyleEnabled = nothingStyleEnabled,
            onDismissUpward = onDismissUpward
        )
    } else {
        val liveFrame = CaptureUiStore.liveFrame
        val meterModel = if (lightweightMeterEnabled || spectrumMeterEnabled) {
            null
        } else {
            remember(
                liveFrame.level,
                liveFrame.meterSegments,
                glyphMode,
                deviceProfile,
                binaryMode,
                glyphMeterPreviewEnabled,
                recordingLightIncluded,
                reverseDirection
            ) {
                buildUiMeterModel(
                    level = liveFrame.level,
                    meterSegments = liveFrame.meterSegments,
                    glyphMode = glyphMode,
                    deviceProfile = deviceProfile,
                    binaryMode = binaryMode,
                    glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                    recordingLightIncluded = recordingLightIncluded,
                    reverseDirection = reverseDirection
                )
            }
        }
        CompactMeterOverlay(
            level = liveFrame.level,
            peak = liveFrame.peak,
            spectrumBands = liveFrame.spectrumBands,
            meterModel = meterModel,
            lightweightMeterEnabled = lightweightMeterEnabled,
            spectrumMeterEnabled = spectrumMeterEnabled,
            nativeMeterViewEnabled = nativeMeterViewEnabled,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            nothingStyleEnabled = nothingStyleEnabled,
            onDismissUpward = onDismissUpward
        )
    }
}

@Composable
private fun UpdateNotificationOverlay(
    updateInfo: AppUpdateInfo,
    nothingStyleEnabled: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onDismissUntilNextVersion: () -> Unit
) {
    val density = LocalDensity.current
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var dragOffsetPx by remember(updateInfo.tagName) { mutableStateOf(0f) }
    var dismissing by remember(updateInfo.tagName) { mutableStateOf(false) }
    val exitDistancePx = with(density) { 96.dp.toPx() }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (dismissing) -exitDistancePx else dragOffsetPx,
        animationSpec = tween(durationMillis = 180),
        label = "update-notification-dismiss",
        finishedListener = {
            if (dismissing) {
                onDismiss()
            }
        }
    )
    val animatedAlpha = ((exitDistancePx + animatedOffsetPx) / exitDistancePx).coerceIn(0f, 1f)

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = animatedOffsetPx
                alpha = animatedAlpha
            }
            .pointerInput(updateInfo.tagName) {
                val dismissThresholdPx = with(density) { 28.dp.toPx() }
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtMost(0f)
                    },
                    onDragEnd = {
                        if (dragOffsetPx < -dismissThresholdPx) {
                            dismissing = true
                        } else {
                            dragOffsetPx = 0f
                        }
                    },
                    onDragCancel = {
                        dragOffsetPx = 0f
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_notification_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = updateInfo.tagName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onDismissUntilNextVersion) {
                Text(stringResource(R.string.update_notification_dont_show))
            }
        }
    }
}

@Composable
private fun activeModeLabel(activeMode: String): String {
    return when (activeMode) {
        "VISUALIZER" -> stringResource(R.string.mode_visualizer)
        "MEDIA PROJECTION" -> stringResource(R.string.mode_media_projection)
        "IDLE" -> stringResource(R.string.mode_idle)
        else -> activeMode
    }
}

@Composable
private fun materialCardBorder(): BorderStroke {
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun materialCardColor(prominent: Boolean = false): Color {
    return if (prominent) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

@Composable
private fun SpectrumCanvas(spectrumBands: FloatArray) {
    val bandCount = spectrumBands.size
    val spectrumColors = listOf(
        Color(0xFF7B2FFF),
        Color(0xFF3E7BFF),
        Color(0xFF00BFFF),
        Color(0xFF00E0A0),
        Color(0xFFFFD700),
        Color(0xFFFF7A00),
        Color(0xFFFF3A3A)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF7B2FFF).copy(alpha = 0.15f),
                            Color(0xFF3E7BFF).copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceDim
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (bandCount > 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barGap = 3.dp.toPx()
                    val barWidth = (size.width - barGap * (bandCount - 1)) / bandCount
                    val maxHeight = size.height

                    for (i in 0 until bandCount) {
                        val value = spectrumBands[i].coerceIn(0f, 1f)
                        val barHeight = maxHeight * value
                        val left = i * (barWidth + barGap)
                        val top = maxHeight - barHeight

                        val colorRatio = i.toFloat() / (bandCount - 1).coerceAtLeast(1)
                        val colorIndex = (colorRatio * (spectrumColors.size - 1))
                        val lo = colorIndex.toInt().coerceIn(0, spectrumColors.size - 2)
                        val hi = lo + 1
                        val frac = colorIndex - lo
                        val barColor = androidx.compose.ui.graphics.lerp(spectrumColors[lo], spectrumColors[hi], frac)

                        // inactive track
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.06f),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, maxHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.spectrum_label_bass),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.spectrum_label_bands, bandCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.spectrum_label_treble),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ParameterSlider(
    title: String,
    valueText: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    nothingStyleEnabled: Boolean
) {
    val inactiveTrackColor = when {
        nothingStyleEnabled && isSystemInDarkTheme() -> Color(0xFF2A2A2A)
        nothingStyleEnabled -> Color(0xFFF2F2F2)
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = inactiveTrackColor,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
internal fun glyphPatternDescriptionText(glyphMode: String): String? {
    val renderMode = GlyphPatternRegistry.definition(glyphMode)?.recipe?.renderMode ?: return null
    val resId = when (renderMode) {
        GlyphPatternRenderMode.LINEAR -> R.string.glyph_pattern_desc_linear
        GlyphPatternRenderMode.LINEAR_PEAK -> R.string.glyph_pattern_desc_linear_peak
        GlyphPatternRenderMode.CENTER -> R.string.glyph_pattern_desc_center
        GlyphPatternRenderMode.SPECTRUM -> R.string.glyph_pattern_desc_spectrum
        GlyphPatternRenderMode.SPECTRUM_MARKER -> R.string.glyph_pattern_desc_spectrum_marker
        GlyphPatternRenderMode.CLASSIC -> R.string.glyph_pattern_desc_classic
        GlyphPatternRenderMode.ALL_BRIGHTNESS -> R.string.glyph_pattern_desc_all_brightness
        GlyphPatternRenderMode.MATRIX_BAR -> R.string.glyph_pattern_desc_matrix_bar
        GlyphPatternRenderMode.MATRIX_FIELD -> R.string.glyph_pattern_desc_matrix_field
        GlyphPatternRenderMode.MATRIX_CIRCLE -> R.string.glyph_pattern_desc_matrix_circle
        GlyphPatternRenderMode.MATRIX_RIPPLE -> R.string.glyph_pattern_desc_matrix_ripple
        GlyphPatternRenderMode.MATRIX_SPECTRUM -> R.string.glyph_pattern_desc_matrix_spectrum
        GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER -> R.string.glyph_pattern_desc_matrix_spectrum_center
        GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM -> R.string.glyph_pattern_desc_matrix_spectrum_bottom
        GlyphPatternRenderMode.MATRIX_SPECTROGRAM -> R.string.glyph_pattern_desc_matrix_spectrogram
        GlyphPatternRenderMode.MATRIX_SPECTRUM_ANALYZER -> R.string.glyph_pattern_desc_matrix_spectrum_analyzer
        GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE -> R.string.glyph_pattern_desc_matrix_oscilloscope
        GlyphPatternRenderMode.MATRIX_RADIAL_SPECTRUM -> R.string.glyph_pattern_desc_matrix_radial_spectrum
        GlyphPatternRenderMode.MATRIX_OPEN_REEL -> R.string.glyph_pattern_desc_matrix_open_reel
        GlyphPatternRenderMode.MATRIX_RAIN -> R.string.glyph_pattern_desc_matrix_rain
        GlyphPatternRenderMode.MATRIX_WAVE_FIELD -> R.string.glyph_pattern_desc_matrix_wave_field
        GlyphPatternRenderMode.MATRIX_SKYLINE -> R.string.glyph_pattern_desc_matrix_skyline
        GlyphPatternRenderMode.MATRIX_PULSE_GRID -> R.string.glyph_pattern_desc_matrix_pulse_grid
        GlyphPatternRenderMode.PULSE_TRAIN -> R.string.glyph_pattern_desc_linear_peak
    }
    return stringResource(resId)
}

@Composable
private fun ControlCard(
    isCapturing: Boolean,
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    smoothingBalance: Float,
    autoScaleWindowSeconds: Float,
    autoScaleOffset: Float,
    reverseDirection: Boolean,
    activeMode: String,
    glyphMode: String,
    fillOtherGlyphLights: Boolean,
    deviceProfile: GlyphDeviceProfile,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    isPhone1Device: Boolean,
    binaryMode: Boolean,
    matrixSmoothMotionEnabled: Boolean,
    oscilloscopeAutoTimeAxisEnabled: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    mediaProjectionEnabled: Boolean,
    nothingStyleEnabled: Boolean,
    turnOffWhenBackDown: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onSmoothingBalanceChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChangeFinished: () -> Unit,
    onAutoScaleOffsetChanged: (Float) -> Unit,
    onAutoScaleOffsetChangeFinished: () -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onRecordingLightBehaviorChanged: (RecordingLightBehavior) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    startPending: Boolean,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onStopClick: () -> Unit,
    experimentalDetailsStyle: Boolean = false
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showImportExportDialog by rememberSaveable { mutableStateOf(false) }
    var showRecordingLightBehaviorDialog by rememberSaveable { mutableStateOf(false) }
    val stopButtonColor = if (nothingStyleEnabled) NothingRed else MaterialTheme.colorScheme.primary
    val stopButtonContentColor = if (nothingStyleEnabled) Color.White else MaterialTheme.colorScheme.onPrimary
    val supportsFillOtherGlyphLights = deviceProfile in setOf(
        GlyphDeviceProfile.PHONE1,
        GlyphDeviceProfile.PHONE2,
        GlyphDeviceProfile.PHONE2A
    )
    val isMatrixDevice = deviceProfile in setOf(
        GlyphDeviceProfile.PHONE3_MATRIX,
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX
    )
    val isClassicGlyphMode = GlyphPatternRegistry.definition(glyphMode)?.recipe?.renderMode ==
        GlyphPatternRenderMode.CLASSIC
    val glyphRenderMode = GlyphPatternRegistry.definition(glyphMode)?.recipe?.renderMode
    var oscilloscopeTimeAxisMultiplier by remember { mutableStateOf(1f) }
    val fillOtherGlyphLightsEnabledForMode = supportsFillOtherGlyphLights &&
        !GlyphPatternRegistry.isAllBrightness(glyphMode) &&
        !isClassicGlyphMode
    val glyphPatternDescription = glyphPatternDescriptionText(glyphMode)
    val recordingLightBehavior = resolveRecordingLightBehavior(
        baseIndicatorEnabled = baseIndicatorEnabled,
        recordingLightIncluded = recordingLightIncluded
    )
    val recordingLightBehaviorLabel = when (recordingLightBehavior) {
        RecordingLightBehavior.NONE -> stringResource(R.string.recording_light_behavior_none)
        RecordingLightBehavior.INCLUDED_IN_METER -> stringResource(R.string.recording_light_behavior_meter)
        RecordingLightBehavior.BASS_INDICATOR -> stringResource(R.string.recording_light_behavior_bass)
    }
    LaunchedEffect(glyphRenderMode, oscilloscopeAutoTimeAxisEnabled) {
        if (glyphRenderMode != GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE || !oscilloscopeAutoTimeAxisEnabled) {
            oscilloscopeTimeAxisMultiplier = 1f
            return@LaunchedEffect
        }
        while (true) {
            oscilloscopeTimeAxisMultiplier = WaveformSampler.currentAutoTimeAxisMultiplier()
            delay(100L)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_parameters_dialog_title)) },
            text = { Text(stringResource(R.string.reset_parameters_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetParametersClick()
                    }
                ) {
                    Text(stringResource(R.string.settings_reset_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showImportExportDialog) {
        AlertDialog(
            onDismissRequest = { showImportExportDialog = false },
            title = { Text(stringResource(R.string.import_export_dialog_title)) },
            text = { Text(stringResource(R.string.import_export_dialog_message)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        onClick = {
                            showImportExportDialog = false
                            onImportParametersClick()
                        }
                    ) {
                        Text(stringResource(R.string.settings_import_button))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = {
                            showImportExportDialog = false
                            onExportParametersClick()
                        }
                    ) {
                        Text(stringResource(R.string.settings_export_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportExportDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }


    if (showRecordingLightBehaviorDialog) {
        val options = listOf(
            Triple(
                RecordingLightBehavior.NONE,
                stringResource(R.string.recording_light_behavior_none),
                stringResource(R.string.recording_light_behavior_none_desc)
            ),
            Triple(
                RecordingLightBehavior.INCLUDED_IN_METER,
                stringResource(R.string.recording_light_behavior_meter),
                stringResource(R.string.recording_light_behavior_meter_desc)
            ),
            Triple(
                RecordingLightBehavior.BASS_INDICATOR,
                stringResource(R.string.recording_light_behavior_bass),
                stringResource(R.string.recording_light_behavior_bass_desc)
            )
        )
        AlertDialog(
            onDismissRequest = { showRecordingLightBehaviorDialog = false },
            title = { Text(stringResource(R.string.recording_light_behavior_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (behavior, label, description) ->
                        val selectBehavior = {
                            showRecordingLightBehaviorDialog = false
                            onRecordingLightBehaviorChanged(behavior)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = selectBehavior)
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = recordingLightBehavior == behavior,
                                onClick = selectBehavior
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = label,
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
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecordingLightBehaviorDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    androidx.compose.material3.Card(
        shape = if (experimentalDetailsStyle) {
            RoundedCornerShape(0.dp)
        } else {
            RoundedCornerShape(28.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (experimentalDetailsStyle) {
                Color.Transparent
            } else {
                materialCardColor(prominent = true)
            }
        ),
        border = if (experimentalDetailsStyle) null else materialCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (experimentalDetailsStyle) 0.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (experimentalDetailsStyle) 20.dp else 16.dp
            )
        ) {
            Text(
                text = stringResource(R.string.capture_control_title),
                style = if (experimentalDetailsStyle) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontFamily = FontFamily.Default
            )

            if (isCapturing) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStopClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = stopButtonColor,
                        contentColor = stopButtonContentColor
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(stopButtonContentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_stop))
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartVisualizerClick,
                    enabled = !startPending,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (startPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_no_capture))
                }
                if (isPhone1Device) {
                    Phone1GlyphDebugControls(
                        startPending = startPending,
                        onEnableClick = onEnablePhone1GlyphDebugClick
                    )
                }
                if (mediaProjectionEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onStartProjectionClick,
                            enabled = !startPending
                        ) {
                            Text(stringResource(R.string.button_media_projection))
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.glyph_pattern),
                style = if (experimentalDetailsStyle) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontFamily = FontFamily.Default
            )
            val modes = GlyphPatternRegistry.patternsFor(deviceProfile)
                .map { it.id to stringResource(it.labelRes) }

            if (modes.size >= 4) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.chunked(2).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { (key, label) ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = glyphMode == key,
                                    onClick = { onGlyphModeChanged(key) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = glyphPatternChipColors()
                                )
                            }
                            if (rowModes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { (key, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = glyphMode == key,
                            onClick = { onGlyphModeChanged(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = glyphPatternChipColors()
                        )
                    }
                }
            }

            glyphPatternDescription?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (supportsFillOtherGlyphLights) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fill_other_glyph_lights_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (fillOtherGlyphLightsEnabledForMode) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                disabledContentColor
                            }
                        )
                        Text(
                            text = stringResource(R.string.fill_other_glyph_lights_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (fillOtherGlyphLightsEnabledForMode) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                disabledContentColor
                            }
                        )
                    }
                    Box(
                        modifier = Modifier.width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = fillOtherGlyphLights,
                            onCheckedChange = if (fillOtherGlyphLightsEnabledForMode) {
                                onFillOtherGlyphLightsChanged
                            } else {
                                null
                            },
                            enabled = fillOtherGlyphLightsEnabledForMode
                        )
                    }
                }
            }

            if (deviceProfile.supportsRecordingLightBehavior()) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showRecordingLightBehaviorDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recording_light_behavior_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = recordingLightBehaviorLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.meter_parameters),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showResetDialog = true }) {
                    Text(stringResource(R.string.settings_reset_button))
                }
                IconButton(onClick = { showImportExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.cd_import_export)
                    )
                }
            }

            ParameterSlider(
                title = stringResource(R.string.param_sensitivity_title),
                valueText = stringResource(R.string.percent_value, (sensitivity * 100).toInt()),
                description = stringResource(R.string.param_sensitivity_desc),
                value = sensitivity,
                onValueChange = onSensitivityChanged,
                valueRange = 0.6f..3.0f,
                nothingStyleEnabled = nothingStyleEnabled
            )

            ParameterSlider(
                title = stringResource(R.string.param_response_speed_title),
                valueText = responseSpeedValueText(smoothing),
                description = stringResource(R.string.param_response_speed_desc),
                value = smoothing,
                onValueChange = onSmoothingChanged,
                valueRange = 0.08f..0.55f,
                nothingStyleEnabled = nothingStyleEnabled
            )

            ParameterSlider(
                title = stringResource(R.string.param_tone_focus_title),
                valueText = when {
                    toneFocus < -0.1f -> stringResource(
                        R.string.param_tone_focus_bass,
                        (toneFocus * -100).toInt()
                    )
                    toneFocus > 0.1f -> stringResource(
                        R.string.param_tone_focus_treble,
                        (toneFocus * 100).toInt()
                    )
                    else -> stringResource(R.string.param_tone_focus_balanced)
                },
                description = stringResource(R.string.param_tone_focus_desc),
                value = toneFocus,
                onValueChange = onToneFocusChanged,
                valueRange = -1f..1f,
                nothingStyleEnabled = nothingStyleEnabled
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { advancedExpanded = !advancedExpanded },
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.advanced_meter_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.advanced_meter_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (advancedExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (advancedExpanded) {
                        stringResource(R.string.cd_collapse)
                    } else {
                        stringResource(R.string.cd_expand)
                    }
                )
            }

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ParameterSlider(
                        title = stringResource(R.string.param_noise_gate_title),
                        valueText = stringResource(R.string.percent_value, (noiseGate * 100).toInt()),
                        description = stringResource(R.string.param_noise_gate_desc),
                        value = noiseGate,
                        onValueChange = onNoiseGateChanged,
                        valueRange = 0f..0.35f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )

                    ParameterSlider(
                        title = stringResource(R.string.param_dynamics_title),
                        valueText = stringResource(R.string.param_dynamics_value, dynamics),
                        description = stringResource(R.string.param_dynamics_desc),
                        value = dynamics,
                        onValueChange = onDynamicsChanged,
                        valueRange = 0.6f..2.2f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )

                    if (
                        GlyphPatternRegistry.isAllBrightness(glyphMode) ||
                            (GlyphPatternRegistry.isSpectrum(glyphMode) && !isPhone3Device && !isPhone4aProDevice) ||
                            glyphRenderMode == GlyphPatternRenderMode.MATRIX_SPECTROGRAM ||
                            glyphRenderMode == GlyphPatternRenderMode.MATRIX_SPECTRUM_ANALYZER ||
                            glyphRenderMode == GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE ||
                            glyphRenderMode == GlyphPatternRenderMode.MATRIX_WAVE_FIELD ||
                            glyphRenderMode == GlyphPatternRenderMode.MATRIX_PULSE_GRID
                    ) {
                        ParameterSlider(
                            title = stringResource(R.string.param_output_gamma_title),
                            valueText = stringResource(R.string.param_dynamics_value, outputGamma),
                            description = stringResource(R.string.param_output_gamma_desc),
                            value = outputGamma,
                            onValueChange = onOutputGammaChanged,
                            valueRange = 0.6f..2.6f,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                    }

                    ParameterSlider(
                        title = stringResource(R.string.param_auto_scale_window_title),
                        valueText = stringResource(R.string.param_auto_scale_window_value, autoScaleWindowSeconds),
                        description = stringResource(R.string.param_auto_scale_window_desc),
                        value = autoScaleWindowSeconds,
                        onValueChange = onAutoScaleWindowSecondsChanged,
                        onValueChangeFinished = onAutoScaleWindowSecondsChangeFinished,
                        valueRange = 5f..60f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )

                    ParameterSlider(
                        title = stringResource(R.string.param_auto_scale_offset_title),
                        valueText = stringResource(R.string.percent_value, (autoScaleOffset * 100).toInt()),
                        description = stringResource(R.string.param_auto_scale_offset_desc),
                        value = autoScaleOffset,
                        onValueChange = onAutoScaleOffsetChanged,
                        onValueChangeFinished = onAutoScaleOffsetChangeFinished,
                        valueRange = 0f..0.4f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.glyph_direction_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.glyph_direction_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StableSwitch(
                    checked = reverseDirection,
                    onCheckedChange = onReverseDirectionChanged
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.binary_mode_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.binary_mode_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StableSwitch(
                    checked = binaryMode,
                    onCheckedChange = onBinaryModeChanged
                )
            }

            if (isMatrixDevice) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.matrix_smooth_motion_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.matrix_smooth_motion_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StableSwitch(
                        checked = matrixSmoothMotionEnabled,
                        onCheckedChange = onMatrixSmoothMotionEnabledChanged
                    )
                }
            }

            if (glyphRenderMode == GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (oscilloscopeAutoTimeAxisEnabled) {
                                stringResource(
                                    R.string.oscilloscope_auto_time_axis_title_with_value,
                                    oscilloscopeTimeAxisMultiplier - 1f
                                )
                            } else {
                                stringResource(R.string.oscilloscope_auto_time_axis_title)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.oscilloscope_auto_time_axis_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StableSwitch(
                        checked = oscilloscopeAutoTimeAxisEnabled,
                        onCheckedChange = onOscilloscopeAutoTimeAxisEnabledChanged
                    )
                }
            }

            if (!GlyphPatternRegistry.isSpectrum(glyphMode) && !GlyphPatternRegistry.isAllBrightness(glyphMode)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.level_auto_scale_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.level_auto_scale_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StableSwitch(
                        checked = levelAutoScale,
                        onCheckedChange = onLevelAutoScaleChanged
                    )
                }
            }

            if (GlyphPatternRegistry.isSpectrum(glyphMode)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.spectrum_auto_scale_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.spectrum_auto_scale_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StableSwitch(
                        checked = spectrumAutoScale,
                        onCheckedChange = onSpectrumAutoScaleChanged
                    )
                }
            }

            if (GlyphPatternRegistry.isAllBrightness(glyphMode)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.all_brightness_auto_scale_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.all_brightness_auto_scale_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StableSwitch(
                        checked = allBrightnessAutoScale,
                        onCheckedChange = onAllBrightnessAutoScaleChanged
                    )
                }
            }

            // Kept in state/service for possible future revival, but hidden in UI because
            // some devices already force this behavior at the OS level.

        }
    }
}

@Composable
private fun StableSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var interactionResetKey by remember { mutableStateOf(0) }
    val interactionSource = remember(interactionResetKey) { MutableInteractionSource() }
    LaunchedEffect(checked) {
        delay(220L)
        interactionResetKey += 1
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource
    )
}

@Composable
private fun glyphPatternChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun InfoStrip() {
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
private fun ExperimentalDetailsInfoStrip(
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
            statusText = stringResource(R.string.preview_status_text),
            isCapturing = true,
            heroTitle = "Phone (2)\nGlyph Lights",
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
            deviceProfile = GlyphDeviceCatalog.currentProfile(),
            actualDeviceProfile = GlyphDeviceCatalog.currentProfile(),
            isPhone3Device = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE3_MATRIX,
            isPhone4aProDevice = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
            isPhone2aDevice = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE2A,
            isPhone3aDevice = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE3A,
            isPhone4aDevice = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE4A,
            isPhone1Device = GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE1,
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
            onBinaryModeChanged = {},
            onLevelAutoScaleChanged = {},
            onSpectrumAutoScaleChanged = {},
            onAllBrightnessAutoScaleChanged = {},
            onMediaProjectionEnabledChanged = {},
            onNothingStyleEnabledChanged = {},
            onExperimentalMainUiEnabledChanged = {},
            onDetailedHomeEnabledChanged = {},
            onTurnOffWhenBackDownChanged = {},
            onResetParametersClick = {},
            onExportParametersClick = {},
            onImportParametersClick = {},
            onStartVisualizerClick = {},
            onStartProjectionClick = {},
            onEnablePhone1GlyphDebugClick = {},
            onStopClick = {},
            logMessage = null,
            initialSetupPending = false,
            onInitialSetupCompleted = {},
            onDismissLog = {},
            showPhone1GlyphDebugPermissionDialog = false,
            onDismissPhone1GlyphDebugPermissionDialog = {}
        )
    }
}


