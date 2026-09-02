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
import jp.linkserver.glyphvisualizer.battery.BatteryGlyphService
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
import jp.linkserver.glyphvisualizer.update.checkGitHubReleaseUpdate
import jp.linkserver.glyphvisualizer.update.dismissUpdateNotificationUntilNextVersion
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isUpdateNotificationDismissed
import jp.linkserver.glyphvisualizer.update.markUpdateCheckFinished
import jp.linkserver.glyphvisualizer.update.shouldCheckForUpdates
import rikka.shizuku.Shizuku

internal const val RESPONSE_SPEED_NONE_THRESHOLD = 0.54f
private const val PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE = 1401

internal enum class Screen {
    WELCOME,
    MAIN,
    DETAILS,
    LATENCY,
    EXTRAS,
    SETTINGS,
    EXPERIMENTAL,
    GLYPH_INSPECTOR,
    ABOUT,
    UPDATE,
    OSS
}

internal enum class WelcomeStep {
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
        private const val STATE_PENDING_START_MODE =
            "pending_start_mode"
        private const val STATE_PENDING_EXPORT_CONTENT =
            "pending_export_content"
    }

    private val parameterSyncHandler = Handler(Looper.getMainLooper())
    private val delayedParameterSyncRunnable = Runnable {
        syncCurrentParameters()
    }
    private var delayedMatrixSmoothMotionApplyRunnable: Runnable? = null
    private val mainCoordinator by lazy {
        MainCoordinator(
            context = this,
            scheduleParameterSync = ::scheduleParameterSync,
            syncCurrentParameters = ::syncCurrentParameters
        )
    }

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
    private var showPhone1GlyphDebugPermissionDialog by mutableStateOf(false)
    private var showOpenReelPermissionDialog by mutableStateOf(false)
    private var showNotificationPermissionExplanationDialog by mutableStateOf(false)

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE) {
                AppLogger.i(
                    "Phone1GlyphDebug",
                    "Manual permission result received requestCode=$requestCode grantResult=$grantResult"
                )
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    enablePhone1GlyphDebug()
                } else {
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
                phone1ClassicCSplitEnabled = CaptureUiStore.state.phone1ClassicCSplitEnabled,
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
        CaptureCommandGateway.startMediaProjection(
            context = this,
            resultCode = result.resultCode,
            data = Intent(data),
            config = uiState.toCaptureConfig()
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
        pendingStartMode = savedInstanceState
            ?.getString(STATE_PENDING_START_MODE)
            ?.let { savedMode ->
                runCatching { CaptureMode.valueOf(savedMode) }.getOrNull()
            }
        pendingExportContent = savedInstanceState?.getString(STATE_PENDING_EXPORT_CONTENT)
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
                    batteryGlyphEnabled = resolvedLatencySettings.batteryGlyphEnabled,
                    syncWithNothingOsGlyphSettingEnabled =
                        resolvedLatencySettings.syncWithNothingOsGlyphSettingEnabled,
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
        BatteryGlyphService.syncEnabledState(this, savedSettings.batteryGlyphEnabled)
        installContent(initialSetupPending)
    }

    private fun installContent(initialSetupPending: Boolean) {
        setContent {
            val uiState = CaptureUiStore.state
            val effectiveDeviceProfile = GlyphDeviceCatalog.effectiveUiProfile(
                actualProfile = deviceProfile,
                phone4bEmulationEnabled = uiState.phone4bEmulationEnabled,
                debugDeviceProfileOverride = uiState.debugDeviceProfileOverride
            )
            val visualTuningKey = GlyphVisualTuningKey(effectiveDeviceProfile, uiState.glyphMode)
            val visualTuningOverride = uiState.visualDynamicsOverrides[visualTuningKey]
            val visualDynamics = resolveGlyphVisualTuning(
                profile = effectiveDeviceProfile,
                patternId = uiState.glyphMode,
                localDynamicsOverrides = uiState.visualDynamicsOverrides
            ).dynamics
            val effectivePresentation = if (effectiveDeviceProfile == deviceProfile) {
                currentDevice.presentation
            } else {
                GlyphDeviceCatalog.presentationForProfile(effectiveDeviceProfile)
            }
            GlyphBartyTheme(nothingStyle = uiState.nothingStyleEnabled) {
                GlyphVisualizerApp(
                    uiState = VisualizerUiState(
                        capture = uiState,
                        initialSetupPending = initialSetupPending,
                        heroTitle = effectivePresentation.heroTitle,
                        deviceProfile = effectiveDeviceProfile,
                        actualDeviceProfile = deviceProfile,
                        batteryGlyphSupported =
                            GlyphDeviceCatalog.currentBatteryIndicatorSpecOrNull() != null,
                        visualDynamics = visualDynamics,
                        visualDynamicsOverridden = visualTuningOverride != null,
                        showPhone1GlyphDebugPermissionDialog = showPhone1GlyphDebugPermissionDialog
                    ),
                    actions = VisualizerUiActions(
                        onInitialSetupCompleted = {
                            offerNotificationPermissionIfNeeded()
                            },
                        visualTuning = VisualTuningActions(
                            onVisualDynamicsChanged = { newValue ->
                                mainCoordinator.updateDeferred { current ->
                                    current.copy(
                                        visualDynamicsOverrides = current.visualDynamicsOverrides +
                                            (visualTuningKey to newValue.coerceIn(0f, 1f))
                                    )
                                }
                            },
                            onVisualDynamicsChangeFinished = {
                                syncCurrentParameters()
                                },
                            onVisualDynamicsReset = {
                                mainCoordinator.updateAndSync { current ->
                                    current.copy(
                                        visualDynamicsOverrides =
                                            current.visualDynamicsOverrides - visualTuningKey
                                    )
                                }
                            }
                        ),
                        settings = VisualizerSettingsActions(
                            onSensitivityChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(sensitivity = newValue) }
                            },
                            onNoiseGateChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(noiseGate = newValue) }
                            },
                            onDynamicsChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(dynamics = newValue) }
                            },
                            onOutputGammaChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(outputGamma = newValue) }
                            },
                            onSmoothingChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(smoothing = newValue) }
                            },
                            onSmoothingBalanceChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(smoothingBalance = newValue) }
                            },
                            onToneFocusChanged = { newValue ->
                                mainCoordinator.updateDeferred { it.copy(toneFocus = newValue) }
                            },
                            onAutoScaleWindowSecondsChanged = { newValue ->
                                mainCoordinator.update { it.copy(autoScaleWindowSeconds = newValue) }
                            },
                            onAutoScaleWindowSecondsChangeFinished = {
                                syncCurrentParameters()
                                },
                            onAutoScaleOffsetChanged = { newValue ->
                                mainCoordinator.update { it.copy(autoScaleOffset = newValue) }
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
                                mainCoordinator.updateAndPersist {
                                    it.copy(glyphMeterPreviewEnabled = enabled)
                                }
                            },
                            onMeterVisibleEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist { it.copy(meterVisibleEnabled = enabled) }
                            },
                            onLightweightMeterEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(lightweightMeterEnabled = enabled)
                                }
                            },
                            onSpectrumMeterEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist { it.copy(spectrumMeterEnabled = enabled) }
                            },
                            onNativeMeterViewEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist { it.copy(nativeMeterViewEnabled = enabled) }
                            },
                            onAutomaticUpdateCheckEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(automaticUpdateCheckEnabled = enabled)
                                }
                            },
                            onBatteryGlyphEnabledChanged = { enabled ->
                                val safeEnabled = enabled &&
                                    GlyphDeviceCatalog.currentBatteryIndicatorSpecOrNull() != null
                                mainCoordinator.updateAndPersist {
                                    it.copy(batteryGlyphEnabled = safeEnabled)
                                }
                                BatteryGlyphService.syncEnabledState(this, safeEnabled)
                            },
                            onSyncWithNothingOsGlyphSettingEnabledChanged = { enabled ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(syncWithNothingOsGlyphSettingEnabled = enabled)
                                }
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
                                mainCoordinator.updatePushAndPersist {
                                    it.copy(experimentalVisualizerStabilizationEnabled = enabled)
                                }
                            },
                            onExperimentalVisualizerSignalWatchdogEnabledChanged = { enabled ->
                                mainCoordinator.updateAndSync {
                                    it.copy(experimentalVisualizerSignalWatchdogEnabled = enabled)
                                }
                            },
                            onMatrixSmoothMotionEnabledChanged = { enabled ->
                                val updated = CaptureUiStore.state.copy(
                                matrixSmoothMotionEnabled = enabled
                                )
                                CaptureUiStore.update { updated }
                                delayedMatrixSmoothMotionApplyRunnable?.let(parameterSyncHandler::removeCallbacks)
                                delayedMatrixSmoothMotionApplyRunnable = Runnable {
                                CaptureCommandGateway.updateConfigPreservingRecordingLight(
                                context = this,
                                config = updated.toCaptureConfig()
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
                                mainCoordinator.updateAndSync {
                                    it.copy(oscilloscopeAutoTimeAxisEnabled = enabled)
                                }
                            },
                            onAutoEnablePhone1GlyphDebugOnStartChanged = { enabled ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(autoEnablePhone1GlyphDebugOnStart = enabled)
                                }
                            },
                            onRecordingLightBehaviorChanged = { behavior ->
                                mainCoordinator.updateAndSync {
                                    it.withRecordingLightBehavior(behavior)
                                }
                            },
                            onPhone4bEmulationEnabledChanged = { enabled ->
                                if (deviceProfile == GlyphDeviceProfile.PHONE4A && !CaptureUiStore.state.isCapturing) {
                                    mainCoordinator.updateAndPersist { current ->
                                        val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                                            actualProfile = deviceProfile,
                                            phone4bEmulationEnabled = enabled,
                                            debugDeviceProfileOverride =
                                                current.debugDeviceProfileOverride
                                        )
                                        current.copy(
                                            phone4bEmulationEnabled = enabled,
                                            glyphMode = GlyphDeviceCatalog.normalizeGlyphMode(
                                                effectiveProfile,
                                                current.glyphMode
                                            )
                                        )
                                    }
                                }
                            },
                            onDebugDeviceProfileOverrideChanged = { profile ->
                                if (!CaptureUiStore.state.isCapturing) {
                                    mainCoordinator.updateAndPersist { current ->
                                        val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                                            actualProfile = deviceProfile,
                                            phone4bEmulationEnabled = current.phone4bEmulationEnabled,
                                            debugDeviceProfileOverride = profile
                                        )
                                        current.copy(
                                            debugDeviceProfileOverride = profile,
                                            glyphMode = GlyphDeviceCatalog.normalizeGlyphMode(
                                                effectiveProfile,
                                                current.glyphMode
                                            )
                                        )
                                    }
                                }
                            },
                            onReverseDirectionChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist {
                                    it.copy(reverseDirection = newValue)
                                }
                            },
                            onGlyphModeChanged = { newMode ->
                                requestGlyphModeChange(newMode)
                                },
                            onFillOtherGlyphLightsChanged = { enabled ->
                                mainCoordinator.updateAndSync { it.copy(fillOtherGlyphLights = enabled) }
                            },
                            onPhone1ClassicCSplitEnabledChanged = { enabled ->
                                mainCoordinator.updateAndSync {
                                    it.copy(phone1ClassicCSplitEnabled = enabled)
                                }
                            },
                            onBinaryModeChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist { it.copy(binaryMode = newValue) }
                            },
                            onLevelAutoScaleChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist { it.copy(levelAutoScale = newValue) }
                            },
                            onSpectrumAutoScaleChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist {
                                    it.copy(spectrumAutoScale = newValue)
                                }
                            },
                            onAllBrightnessAutoScaleChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist {
                                    it.copy(allBrightnessAutoScale = newValue)
                                }
                            },
                            onLegacyAutoScaleEnabledChanged = { enabled ->
                                mainCoordinator.updatePersistAndSync {
                                    it.copy(legacyAutoScaleEnabled = enabled)
                                }
                            },
                            onMediaProjectionEnabledChanged = { newValue ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(mediaProjectionEnabled = newValue)
                                }
                            },
                            onNothingStyleEnabledChanged = { newValue ->
                                mainCoordinator.updateAndPersist { it.copy(nothingStyleEnabled = newValue) }
                            },
                            onExperimentalMainUiEnabledChanged = { newValue ->
                                mainCoordinator.updateAndPersist {
                                    it.copy(experimentalMainUiEnabled = newValue)
                                }
                            },
                            onDetailedHomeEnabledChanged = { newValue ->
                                mainCoordinator.updateAndPersist { it.copy(detailedHomeEnabled = newValue) }
                            },
                            onTurnOffWhenBackDownChanged = { newValue ->
                                mainCoordinator.updatePushAndPersist {
                                    it.copy(turnOffWhenBackDown = newValue)
                                }
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
                                }
                        ),
                        capture = VisualizerCaptureActions(
                            onStartVisualizerClick = {
                                requestModeStart(CaptureMode.VISUALIZER)
                                },
                            onStartProjectionClick = {
                                requestModeStart(CaptureMode.MEDIA_PROJECTION)
                                },
                            onEnablePhone1GlyphDebugClick = {
                                requestPhone1GlyphDebug()
                                },
                            onStopClick = {
                                GlyphVisualizerService.stop(this)
                                }
                        ),
                        dialogs = VisualizerDialogActions(
                            onDismissLog = {
                                CaptureUiStore.update { it.copy(logMessage = null) }
                                },
                            onDismissPhone1GlyphDebugPermissionDialog = {
                                showPhone1GlyphDebugPermissionDialog = false
                                }
                        )
                    )
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
        CaptureCommandGateway.updateConfig(
            context = this,
            config = routeAware.toCaptureConfig()
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
                legacyAutoScaleEnabled = parameters.legacyAutoScaleEnabled,
                visualDynamicsOverrides = parameters.visualDynamicsOverrides,
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
        CaptureCommandGateway.updateConfig(
            context = this,
            config = updated.toCaptureConfig()
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
        pendingStartMode?.let {
            outState.putString(STATE_PENDING_START_MODE, it.name)
        }
        pendingExportContent?.let {
            outState.putString(STATE_PENDING_EXPORT_CONTENT, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        CaptureUiStore.setUiVisible(false)
        parameterSyncHandler.removeCallbacks(delayedParameterSyncRunnable)
        delayedMatrixSmoothMotionApplyRunnable?.let(parameterSyncHandler::removeCallbacks)
        delayedMatrixSmoothMotionApplyRunnable = null
        runCatching { Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener) }
        super.onDestroy()
    }

    private fun applyMediaPlaybackOnlyEnabled(enabled: Boolean) {
        val safeEnabled = enabled && MediaSessionPlaybackGate.hasNotificationAccess(this)
        val updated = CaptureUiStore.state.copy(mediaPlaybackOnlyEnabled = safeEnabled)
        CaptureUiStore.update { updated }
        CaptureCommandGateway.updateConfigPreservingRecordingLight(
            context = this,
            config = updated.toCaptureConfig()
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
        Phone1GlyphDebugHelper.autoEnableOnStartIfPossible(
            context = this,
            profile = currentEffectiveUiDeviceProfile(),
            autoEnableOnStart = CaptureUiStore.state.autoEnablePhone1GlyphDebugOnStart
        )
    }

    private fun currentEffectiveUiDeviceProfile(): GlyphDeviceProfile {
        val state = CaptureUiStore.state
        return GlyphDeviceCatalog.effectiveUiProfile(
            actualProfile = deviceProfile,
            phone4bEmulationEnabled = state.phone4bEmulationEnabled,
            debugDeviceProfileOverride = state.debugDeviceProfileOverride
        )
    }

    private fun requestPhone1GlyphDebug() {
        val effectiveUiProfile = currentEffectiveUiDeviceProfile()
        val debugAllowed = Phone1GlyphDebugHelper.supports(effectiveUiProfile)
        AppLogger.i(
            "Phone1GlyphDebug",
            "Manual debug request debugAllowed=$debugAllowed actualProfile=$deviceProfile effectiveUiProfile=$effectiveUiProfile"
        )
        if (!debugAllowed) {
            AppLogger.i("Phone1GlyphDebug", "Skipping debug request because debug controls are not allowed")
            return
        }
        val backendStatus = Phone1GlyphDebugHelper.backendStatus()
        AppLogger.i(
            "Phone1GlyphDebug",
            "Manual debug request state suiAvailable=${backendStatus.suiAvailable} apiAvailable=${backendStatus.apiAvailable} permissionGranted=${backendStatus.permissionGranted}"
        )
        when {
            backendStatus.permissionGranted -> enablePhone1GlyphDebug()
            backendStatus.apiAvailable -> {
                AppLogger.i("Phone1GlyphDebug", "Manual request will ask Shizuku API permission")
                runCatching {
                    check(
                        Phone1GlyphDebugHelper.requestPermission(
                            PHONE1_GLYPH_DEBUG_PERMISSION_REQUEST_CODE
                        )
                    ) { "Shizuku API became unavailable before requesting permission" }
                }.onFailure {
                    AppLogger.w("Phone1GlyphDebug", "Shizuku permission request failed", it)
                    showPhone1GlyphDebugPermissionDialog = true
                }
            }
            else -> {
                AppLogger.i("Phone1GlyphDebug", "No Shizuku/Sui backend available for manual request")
                showPhone1GlyphDebugPermissionDialog = true
            }
        }
    }

    private fun enablePhone1GlyphDebug() {
        val debugAllowed = Phone1GlyphDebugHelper.supports(currentEffectiveUiDeviceProfile())
        AppLogger.i(
            "Phone1GlyphDebug",
            "Manual enablePhone1GlyphDebug debugAllowed=$debugAllowed"
        )
        if (!debugAllowed) {
            AppLogger.i("Phone1GlyphDebug", "Skipping enable because debug controls are not allowed")
            return
        }
        val result = Phone1GlyphDebugHelper.enableGlyphDebug()
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
        if (result.isFailure) {
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
            CaptureCommandGateway.startVisualizer(
                context = this,
                config = uiState.toCaptureConfig(),
                source = VisualizerStartSource.APP
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

