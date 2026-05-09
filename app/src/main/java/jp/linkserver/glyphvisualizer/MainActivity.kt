package jp.linkserver.glyphvisualizer

import android.Manifest
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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jp.linkserver.glyphvisualizer.ui.AboutScreen
import jp.linkserver.glyphvisualizer.ui.OssLicensesScreen
import jp.linkserver.glyphvisualizer.ui.SettingsScreen
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.ketchum.Common
import kotlinx.coroutines.delay
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NothingDotFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed

private const val RESPONSE_SPEED_NONE_THRESHOLD = 0.54f

private enum class Screen {
    MAIN,
    LATENCY,
    SETTINGS,
    ABOUT,
    OSS
}

class MainActivity : ComponentActivity() {
    private val parameterSyncHandler = Handler(Looper.getMainLooper())
    private val delayedParameterSyncRunnable = Runnable {
        syncCurrentParameters()
    }

    private enum class CaptureMode {
        VISUALIZER,
        MEDIA_PROJECTION
    }

    private val isPhone3Device by lazy { Common.is23112() }
    private val isPhone4aProDevice by lazy { Common.is25111p() }
    private val isPhone2aDevice by lazy { Common.is23111() || Common.is23113() }
    private val isPhone3aDevice by lazy { Common.is24111() }
    private val isPhone4aDevice by lazy { Common.is25111() }

    private var pendingStartMode: CaptureMode? = null
    private var pendingExportContent: String? = null

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

        applyParameterState(importedState)
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
                            binaryMode = uiState.binaryMode,
                            levelAutoScale = uiState.levelAutoScale,
                                                        spectrumAutoScale = uiState.spectrumAutoScale,
                                                                                                                allBrightnessAutoScale = uiState.allBrightnessAutoScale,
                            autoScaleWindowSeconds = uiState.autoScaleWindowSeconds,
                            latencyMs = uiState.latencyMs,
                                                        turnOffWhenBackDown = uiState.turnOffWhenBackDown
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val savedSettings = SettingsPreferences.load(this)
        val normalizedMode = normalizeGlyphModeForCurrentDevice(savedSettings.glyphMode)
        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
        val resolvedLatencySettings = savedSettings.withResolvedLatency(bluetoothOutputActive)
        CaptureUiStore.update {
            it.copy(
                statusText = getString(R.string.status_preparing_glyph_session),
                sensitivity = resolvedLatencySettings.sensitivity,
                noiseGate = resolvedLatencySettings.noiseGate,
                dynamics = resolvedLatencySettings.dynamics,
                outputGamma = resolvedLatencySettings.outputGamma,
                toneFocus = resolvedLatencySettings.toneFocus,
                smoothing = resolvedLatencySettings.smoothing,
                smoothingBalance = resolvedLatencySettings.smoothingBalance,
                autoScaleWindowSeconds = resolvedLatencySettings.autoScaleWindowSeconds,
                latencyMs = resolvedLatencySettings.latencyMs,
                defaultOutputLatencyMs = resolvedLatencySettings.defaultOutputLatencyMs,
                bluetoothLatencyMs = resolvedLatencySettings.bluetoothLatencyMs,
                latencyAutoSwitchEnabled = resolvedLatencySettings.latencyAutoSwitchEnabled,
                isBluetoothOutputActive = resolvedLatencySettings.isBluetoothOutputActive,
                reverseDirection = resolvedLatencySettings.reverseDirection,
                    peakHoldEnabled = resolvedLatencySettings.peakHoldEnabled,
                    glyphMode = normalizedMode,
                    binaryMode = resolvedLatencySettings.binaryMode,
                    levelAutoScale = resolvedLatencySettings.levelAutoScale,
                    spectrumAutoScale = resolvedLatencySettings.spectrumAutoScale,
                    mediaProjectionEnabled = resolvedLatencySettings.mediaProjectionEnabled,
                    glyphMeterPreviewEnabled = resolvedLatencySettings.glyphMeterPreviewEnabled,
                    nothingStyleEnabled = resolvedLatencySettings.nothingStyleEnabled,
                    turnOffWhenBackDown = resolvedLatencySettings.turnOffWhenBackDown,
                allBrightnessAutoScale = resolvedLatencySettings.allBrightnessAutoScale,
            )
        }
        setContent {
            val uiState = CaptureUiStore.state
            GlyphBartyTheme(nothingStyle = uiState.nothingStyleEnabled) {
                GlyphVisualizerApp(
                    statusText = uiState.statusText,
                    isCapturing = uiState.isCapturing,
                    heroTitle = GlyphDevicePresentationRegistry.current().heroTitle,
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
                    latencyMs = uiState.latencyMs,
                    defaultOutputLatencyMs = uiState.defaultOutputLatencyMs,
                    bluetoothLatencyMs = uiState.bluetoothLatencyMs,
                    latencyAutoSwitchEnabled = uiState.latencyAutoSwitchEnabled,
                    isBluetoothOutputActive = uiState.isBluetoothOutputActive,
                    reverseDirection = uiState.reverseDirection,
                    meterSegments = uiState.meterSegments,
                    activeMode = uiState.activeMode,
                        glyphMode = uiState.glyphMode,
                        isPhone3Device = isPhone3Device,
                        isPhone4aProDevice = isPhone4aProDevice,
                        isPhone2aDevice = isPhone2aDevice,
                        isPhone3aDevice = isPhone3aDevice,
                        isPhone4aDevice = isPhone4aDevice,
                        binaryMode = uiState.binaryMode,
                        levelAutoScale = uiState.levelAutoScale,
                        spectrumAutoScale = uiState.spectrumAutoScale,
                        allBrightnessAutoScale = uiState.allBrightnessAutoScale,
                        mediaProjectionEnabled = uiState.mediaProjectionEnabled,
                        glyphMeterPreviewEnabled = uiState.glyphMeterPreviewEnabled,
                        nothingStyleEnabled = uiState.nothingStyleEnabled,
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
                        )
                        SettingsPreferences.save(this, updated)
                    },
                        onGlyphModeChanged = { newMode ->
                            CaptureUiStore.update { it.copy(glyphMode = newMode) }
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
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
                                updated.binaryMode,
                                updated.levelAutoScale,
                                updated.spectrumAutoScale,
                                updated.allBrightnessAutoScale,
                                updated.autoScaleWindowSeconds,
                                updated.latencyMs,
                                updated.turnOffWhenBackDown,
                                updated.outputGamma
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
                    onStopClick = {
                        GlyphVisualizerService.stop(this)
                    },
                    logMessage = uiState.logMessage,
                    onDismissLog = {
                        CaptureUiStore.update { it.copy(logMessage = null) }
                    }
                )
            }
        }
    }

    private fun defaultGlyphModeForCurrentDevice(): String = when {
        isPhone3Device -> GlyphPatternRegistry.P3_MATRIX_SPECTRUM
        isPhone4aProDevice -> GlyphPatternRegistry.P3_MATRIX_SPECTRUM
        isPhone2aDevice -> GlyphPatternRegistry.P2A_C_LINEAR
        isPhone3aDevice -> GlyphPatternRegistry.P3A_C_LINEAR
        isPhone4aDevice -> GlyphPatternRegistry.P4A_LINEAR
        else -> GlyphPatternRegistry.P2_C1_LINEAR
    }

    private fun currentGlyphDeviceProfile(): GlyphDeviceProfile = when {
        isPhone3Device -> GlyphDeviceProfile.PHONE3_MATRIX
        isPhone4aProDevice -> GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        isPhone2aDevice -> GlyphDeviceProfile.PHONE2A
        isPhone3aDevice -> GlyphDeviceProfile.PHONE3A
        isPhone4aDevice -> GlyphDeviceProfile.PHONE4A
        else -> GlyphDeviceProfile.PHONE2
    }

    private fun normalizeGlyphModeForCurrentDevice(glyphMode: String): String {
        val profile = currentGlyphDeviceProfile()
        return if (GlyphPatternRegistry.isSupported(profile, glyphMode)) {
            glyphMode
        } else {
            defaultGlyphModeForCurrentDevice()
        }
    }

    private fun defaultParameterState(): CaptureUiState {
        return applyRouteAwareLatency(
            SettingsPreferences.defaultParameters().copy(
                glyphMode = defaultGlyphModeForCurrentDevice()
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
            autoScaleWindowSeconds = parameters.autoScaleWindowSeconds.coerceIn(10f, 60f),
            latencyMs = parameters.latencyMs.coerceIn(0f, 500f),
            defaultOutputLatencyMs = parameters.defaultOutputLatencyMs.coerceIn(0f, 500f),
            bluetoothLatencyMs = parameters.bluetoothLatencyMs.coerceIn(0f, 500f),
            glyphMode = normalizeGlyphModeForCurrentDevice(parameters.glyphMode)
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
            routeAware.binaryMode,
            routeAware.levelAutoScale,
            routeAware.spectrumAutoScale,
            routeAware.allBrightnessAutoScale,
            routeAware.autoScaleWindowSeconds,
            routeAware.latencyMs,
            routeAware.turnOffWhenBackDown,
            routeAware.outputGamma
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
                reverseDirection = parameters.reverseDirection,
                peakHoldEnabled = parameters.peakHoldEnabled,
                glyphMode = parameters.glyphMode,
                binaryMode = parameters.binaryMode,
                levelAutoScale = parameters.levelAutoScale,
                spectrumAutoScale = parameters.spectrumAutoScale,
                allBrightnessAutoScale = parameters.allBrightnessAutoScale,
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
            updated.binaryMode,
            updated.levelAutoScale,
            updated.spectrumAutoScale,
            updated.allBrightnessAutoScale,
            updated.autoScaleWindowSeconds,
            updated.latencyMs,
            updated.turnOffWhenBackDown,
            updated.outputGamma
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

    override fun onDestroy() {
        parameterSyncHandler.removeCallbacks(delayedParameterSyncRunnable)
        super.onDestroy()
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
                uiState.binaryMode,
                uiState.levelAutoScale,
                uiState.spectrumAutoScale,
                uiState.allBrightnessAutoScale,
                uiState.autoScaleWindowSeconds,
                uiState.latencyMs,
                uiState.turnOffWhenBackDown,
                uiState.outputGamma
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
    latencyMs: Float,
    defaultOutputLatencyMs: Float,
    bluetoothLatencyMs: Float,
    latencyAutoSwitchEnabled: Boolean,
    isBluetoothOutputActive: Boolean,
    reverseDirection: Boolean,
    meterSegments: Int,
    activeMode: String,
    glyphMode: String,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    binaryMode: Boolean,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    mediaProjectionEnabled: Boolean,
    glyphMeterPreviewEnabled: Boolean,
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
    onLatencyMsChanged: (Float) -> Unit,
    onLatencyMsChangeFinished: () -> Unit,
    onLatencyAutoSwitchChanged: (Boolean) -> Unit,
    onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    onNothingStyleEnabledChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onStopClick: () -> Unit,
    logMessage: String?,
    onDismissLog: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var drawerOpen by remember { mutableStateOf(false) }
    var startPending by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = drawerOpen || screen != Screen.MAIN) {
        when {
            drawerOpen -> drawerOpen = false
            screen == Screen.OSS -> screen = Screen.ABOUT
            screen == Screen.ABOUT -> screen = Screen.SETTINGS
            else -> screen = Screen.MAIN
        }
    }

    val containerBrush = Brush.verticalGradient(
        if (nothingStyleEnabled && isSystemInDarkTheme()) {
            listOf(
                Color(0xFF000000),
                Color(0xFF000000)
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
                    Screen.MAIN -> MainScreenContent(
                        containerBrush = containerBrush,
                        statusText = statusText,
                        isCapturing = isCapturing,
                        heroTitle = heroTitle,
                        level = level,
                        peak = peak,
                        spectrumBands = spectrumBands,
                        sensitivity = sensitivity,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        outputGamma = outputGamma,
                        toneFocus = toneFocus,
                        smoothing = smoothing,
                        smoothingBalance = smoothingBalance,
                        autoScaleWindowSeconds = autoScaleWindowSeconds,
                        reverseDirection = reverseDirection,
                        meterSegments = meterSegments,
                        activeMode = activeMode,
                        glyphMode = glyphMode,
                        isPhone3Device = isPhone3Device,
                        isPhone4aProDevice = isPhone4aProDevice,
                        isPhone2aDevice = isPhone2aDevice,
                        isPhone3aDevice = isPhone3aDevice,
                        isPhone4aDevice = isPhone4aDevice,
                        binaryMode = binaryMode,
                        levelAutoScale = levelAutoScale,
                        spectrumAutoScale = spectrumAutoScale,
                        allBrightnessAutoScale = allBrightnessAutoScale,
                        mediaProjectionEnabled = mediaProjectionEnabled,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
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
                        onReverseDirectionChanged = onReverseDirectionChanged,
                        onGlyphModeChanged = onGlyphModeChanged,
                        onBinaryModeChanged = onBinaryModeChanged,
                        onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                        onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                        onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
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
                        onStopClick = onStopClick,
                        logMessage = logMessage,
                        onDismissLog = onDismissLog,
                        onOpenMenu = { drawerOpen = true },
                        onOpenSettings = { screen = Screen.SETTINGS }
                    )
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
                        onOpenSettings = { screen = Screen.SETTINGS }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        onBack = { screen = Screen.MAIN },
                        onAbout = { screen = Screen.ABOUT },
                        mediaProjectionEnabled = mediaProjectionEnabled,
                        onMediaProjectionEnabledChanged = onMediaProjectionEnabledChanged,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        onGlyphMeterPreviewEnabledChanged = onGlyphMeterPreviewEnabledChanged,
                        nothingStyleEnabled = nothingStyleEnabled,
                        onNothingStyleEnabledChanged = onNothingStyleEnabledChanged
                    )
                    Screen.ABOUT -> AboutScreen(
                        onBack = { screen = Screen.SETTINGS },
                        onOssLicenses = { screen = Screen.OSS },
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                    Screen.OSS -> OssLicensesScreen(onBack = { screen = Screen.ABOUT })
                }
            }

            HomeDrawerOverlay(
                visible = drawerOpen,
                currentScreen = screen,
                nothingStyleEnabled = nothingStyleEnabled,
                onDismiss = { drawerOpen = false },
                onNavigate = { destination ->
                    screen = destination
                    drawerOpen = false
                }
            )
        }
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
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    smoothingBalance: Float,
    autoScaleWindowSeconds: Float,
    reverseDirection: Boolean,
    meterSegments: Int,
    activeMode: String,
    glyphMode: String,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    binaryMode: Boolean,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    mediaProjectionEnabled: Boolean,
    glyphMeterPreviewEnabled: Boolean,
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
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    startPending: Boolean,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onStopClick: () -> Unit,
    logMessage: String?,
    onDismissLog: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall
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
                .background(containerBrush)
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HeroCard(
                    isCapturing = isCapturing,
                    statusText = statusText,
                    heroTitle = heroTitle,
                    level = level,
                    peak = peak,
                    spectrumBands = spectrumBands,
                    sensitivity = sensitivity,
                    glyphMode = glyphMode,
                    toneFocus = toneFocus,
                    smoothing = smoothing,
                    meterSegments = meterSegments,
                    activeMode = activeMode,
                    isPhone3Device = isPhone3Device,
                    isPhone4aProDevice = isPhone4aProDevice,
                    isPhone2aDevice = isPhone2aDevice,
                    isPhone3aDevice = isPhone3aDevice,
                    isPhone4aDevice = isPhone4aDevice,
                    binaryMode = binaryMode,
                    glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                    nothingStyleEnabled = nothingStyleEnabled
                )

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
                    reverseDirection = reverseDirection,
                    activeMode = activeMode,
                    glyphMode = glyphMode,
                    isPhone3Device = isPhone3Device,
                    isPhone4aProDevice = isPhone4aProDevice,
                    isPhone2aDevice = isPhone2aDevice,
                    isPhone3aDevice = isPhone3aDevice,
                    isPhone4aDevice = isPhone4aDevice,
                    binaryMode = binaryMode,
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
                    onReverseDirectionChanged = onReverseDirectionChanged,
                    onGlyphModeChanged = onGlyphModeChanged,
                    onBinaryModeChanged = onBinaryModeChanged,
                    onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                    onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                    onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                        onTurnOffWhenBackDownChanged = onTurnOffWhenBackDownChanged,
                        startPending = startPending,
                        onStartVisualizerClick = onStartVisualizerClick,
                        onStartProjectionClick = onStartProjectionClick,
                        onStopClick = onStopClick
                )

                InfoStrip()

                MeterInfoSection(
                    statusText = statusText,
                    noiseGate = noiseGate,
                    dynamics = dynamics,
                    logMessage = logMessage,
                    onDismissLog = onDismissLog
                )
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
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    var latencySliderValue by rememberSaveable { mutableStateOf(latencyMs) }

    LaunchedEffect(latencyMs) {
        latencySliderValue = latencyMs
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
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
                        text = stringResource(R.string.latency_title),
                        style = MaterialTheme.typography.headlineSmall
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
                .background(containerBrush)
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                androidx.compose.material3.Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                    text = if (latencyAutoSwitchEnabled) {
                                        stringResource(R.string.latency_auto_switch_on)
                                    } else {
                                        stringResource(R.string.latency_auto_switch_off)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
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
                    }
                }
            }
        }
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
    val drawerColor = if (nothingStyleEnabled && isSystemInDarkTheme()) {
        Color(0xFF1C2028)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val selectedColor = if (nothingStyleEnabled && isSystemInDarkTheme()) {
        Color(0xFF4A566E)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
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
                    selectedColor = selectedColor,
                    onClick = { onNavigate(Screen.MAIN) }
                )
                HomeDrawerItem(
                    title = stringResource(R.string.menu_latency),
                    icon = Icons.Default.Equalizer,
                    selected = currentScreen == Screen.LATENCY,
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
    selectedColor: Color,
    onClick: () -> Unit
) {
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
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
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
    onDismissLog: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
private fun responseSpeedValueText(smoothing: Float): String {
    return if (smoothing >= RESPONSE_SPEED_NONE_THRESHOLD) {
        stringResource(R.string.response_speed_none)
    } else {
        stringResource(R.string.percent_value, (smoothing * 100).toInt())
    }
}

@Composable
private fun HeroCard(
    isCapturing: Boolean,
    statusText: String,
    heroTitle: String,
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    sensitivity: Float,
    glyphMode: String,
    toneFocus: Float,
    smoothing: Float,
    meterSegments: Int,
    activeMode: String,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    nothingStyleEnabled: Boolean
) {
    val deviceProfile = rememberGlyphDeviceProfile(
        isPhone3Device = isPhone3Device,
        isPhone4aProDevice = isPhone4aProDevice,
        isPhone2aDevice = isPhone2aDevice,
        isPhone3aDevice = isPhone3aDevice,
        isPhone4aDevice = isPhone4aDevice
    )
    val meterModel = remember(level, glyphMode, deviceProfile, binaryMode, glyphMeterPreviewEnabled) {
        buildUiMeterModel(
            level = level,
            meterSegments = meterSegments,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled
        )
    }
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    val statusChipBackground = if (nothingStyleEnabled && isCapturing) {
                        NothingRed
                    } else if (nothingStyleEnabled) {
                        if (isSystemInDarkTheme()) Color(0xFF2A2A2A) else Color(0xFFF2F2F2)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val statusChipLabelColor = if (nothingStyleEnabled && isCapturing) {
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
            MeterCanvas(
                level = level,
                peak = peak,
                meterModel = meterModel,
                nothingStyleEnabled = nothingStyleEnabled
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

private data class UiMeterModel(
    val segmentCount: Int,
    val activeSegments: Int,
    val segmentLevels: List<Float>,
    val usesGlyphBrightnessPreview: Boolean,
    val usesSymmetricCenterLayout: Boolean = false,
    val symmetricSeedCount: Int = 1
)

private fun rememberGlyphDeviceProfile(
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean
): GlyphDeviceProfile {
    return when {
        isPhone3Device -> GlyphDeviceProfile.PHONE3_MATRIX
        isPhone4aProDevice -> GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        isPhone2aDevice -> GlyphDeviceProfile.PHONE2A
        isPhone3aDevice -> GlyphDeviceProfile.PHONE3A
        isPhone4aDevice -> GlyphDeviceProfile.PHONE4A
        else -> GlyphDeviceProfile.PHONE2
    }
}

private fun buildUiMeterModel(
    level: Float,
    meterSegments: Int,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean
): UiMeterModel {
    if (!glyphMeterPreviewEnabled) {
        val legacySegments = meterSegments.coerceIn(0, 16)
        return UiMeterModel(
            segmentCount = 16,
            activeSegments = legacySegments,
            segmentLevels = List(16) { index -> if (index < legacySegments) 1f else 0f },
            usesGlyphBrightnessPreview = false
        )
    }

    val segmentCount = GlyphPatternRegistry.uiMeterSegmentCount(deviceProfile, glyphMode).coerceAtLeast(1)
    val patternKind = GlyphPatternRegistry.kindOf(glyphMode)
    val normalizedLevel = level.coerceIn(0f, 1f)
    val matrixOnOffOnly = when (deviceProfile) {
        GlyphDeviceProfile.PHONE3_MATRIX,
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> true
        else -> false
    }
    val usesGlyphBrightnessPreview = glyphMeterPreviewEnabled && !binaryMode && !matrixOnOffOnly
    val symmetricSeedCount = if (segmentCount % 2 == 0) 2 else 1
    if (patternKind == jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind.MATRIX_CIRCLE && matrixOnOffOnly) {
        return buildSymmetricMeterModel(
            normalizedLevel = normalizedLevel,
            segmentCount = segmentCount,
            seedCount = symmetricSeedCount,
            usesBrightnessPreview = false
        )
    }
    if (patternKind == jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind.CENTER) {
        return buildSymmetricMeterModel(
            normalizedLevel = normalizedLevel,
            segmentCount = segmentCount,
            seedCount = symmetricSeedCount,
            usesBrightnessPreview = usesGlyphBrightnessPreview
        )
    }

    val virtualLit = normalizedLevel * segmentCount
    val fullLit = virtualLit.toInt().coerceIn(0, segmentCount)
    val edgeFraction = (virtualLit - fullLit).coerceIn(0f, 1f)
    val segmentLevels = List(segmentCount) { index ->
        when {
            index < fullLit -> 1f
            index == fullLit && fullLit < segmentCount -> {
                if (usesGlyphBrightnessPreview) edgeFraction else 0f
            }
            else -> 0f
        }
    }
    val activeSegments = if (usesGlyphBrightnessPreview) {
        segmentLevels.count { it > 0.001f }
    } else {
        fullLit
    }
    return UiMeterModel(
        segmentCount = segmentCount,
        activeSegments = activeSegments,
        segmentLevels = segmentLevels,
        usesGlyphBrightnessPreview = usesGlyphBrightnessPreview
    )
}

private fun buildSymmetricMeterModel(
    normalizedLevel: Float,
    segmentCount: Int,
    seedCount: Int,
    usesBrightnessPreview: Boolean
): UiMeterModel {
    val safeSeedCount = seedCount.coerceIn(1, segmentCount.coerceAtLeast(1))
    val logicalStepCount = (1 + ((segmentCount - safeSeedCount).coerceAtLeast(0) / 2)).coerceAtLeast(1)
    val virtualSteps = normalizedLevel.coerceIn(0f, 1f) * logicalStepCount
    val fullSteps = virtualSteps.toInt().coerceIn(0, logicalStepCount)
    val edgeFraction = (virtualSteps - fullSteps).coerceIn(0f, 1f)
    val segmentLevels = buildSymmetricSegmentLevels(
        segmentCount = segmentCount,
        seedCount = safeSeedCount,
        fullSteps = fullSteps,
        edgeFraction = edgeFraction,
        usesBrightnessPreview = usesBrightnessPreview
    )
    val activeSegments = if (usesBrightnessPreview) {
        segmentLevels.count { it > 0.001f }
    } else {
        when {
            fullSteps <= 0 -> 0
            else -> (safeSeedCount + ((fullSteps - 1) * 2)).coerceAtMost(segmentCount)
        }
    }
    return UiMeterModel(
        segmentCount = segmentCount,
        activeSegments = activeSegments,
        segmentLevels = segmentLevels,
        usesGlyphBrightnessPreview = usesBrightnessPreview,
        usesSymmetricCenterLayout = true,
        symmetricSeedCount = safeSeedCount
    )
}

private fun buildSymmetricSegmentLevels(
    segmentCount: Int,
    seedCount: Int,
    fullSteps: Int,
    edgeFraction: Float,
    usesBrightnessPreview: Boolean
): List<Float> {
    if (segmentCount <= 0) return emptyList()
    if (fullSteps <= 0 && (!usesBrightnessPreview || edgeFraction <= 0.001f)) {
        return List(segmentCount) { 0f }
    }

    val levels = MutableList(segmentCount) { 0f }
    val rightCenter = segmentCount / 2
    val leftCenter = if (seedCount == 2) (rightCenter - 1).coerceAtLeast(0) else rightCenter

    val centerIntensity = when {
        fullSteps > 0 -> 1f
        usesBrightnessPreview -> edgeFraction
        else -> 0f
    }
    levels[leftCenter] = centerIntensity
    if (seedCount == 2) {
        levels[rightCenter] = centerIntensity
    }

    val litPairCount = (fullSteps - 1).coerceAtLeast(0)
    for (pairIndex in 1..litPairCount) {
        val left = leftCenter - pairIndex
        val right = rightCenter + pairIndex
        if (left >= 0) levels[left] = 1f
        if (right < segmentCount) levels[right] = 1f
    }

    if (usesBrightnessPreview && fullSteps in 1 until logicalStepCountFor(segmentCount, seedCount) && edgeFraction > 0.001f) {
        val pairIndex = fullSteps
        val left = leftCenter - pairIndex
        val right = rightCenter + pairIndex
        if (left >= 0) levels[left] = edgeFraction
        if (right < segmentCount) levels[right] = edgeFraction
    }
    return levels
}

private fun logicalStepCountFor(segmentCount: Int, seedCount: Int): Int {
    val safeSeedCount = seedCount.coerceIn(1, segmentCount.coerceAtLeast(1))
    return (1 + ((segmentCount - safeSeedCount).coerceAtLeast(0) / 2)).coerceAtLeast(1)
}

@Composable
private fun MeterCanvas(
    level: Float,
    peak: Float,
    meterModel: UiMeterModel,
    nothingStyleEnabled: Boolean
) {
    val meterVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val meterOnSurfaceColor = MaterialTheme.colorScheme.onSurface
    val meterPrimaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val peakColor = MaterialTheme.colorScheme.tertiary
    val sweepColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "meter-level"
    )
    val animatedPeak by animateFloatAsState(
        targetValue = peak,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "meter-peak"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segmentCount = meterModel.segmentCount.coerceAtLeast(1)
                val segmentGap = 10.dp.toPx()
                val totalGap = segmentGap * (segmentCount - 1)
                val segmentWidth = (size.width - totalGap) / segmentCount.toFloat()
                val maxHeight = size.height
                val centerIndex = segmentCount / 2

                for (segment in 0 until segmentCount) {
                    val left = segment * (segmentWidth + segmentGap)
                    val segmentRatio = if (meterModel.usesSymmetricCenterLayout) {
                        val leftCenterIndex = if (meterModel.symmetricSeedCount == 2) {
                            (centerIndex - 1).coerceAtLeast(0)
                        } else {
                            centerIndex
                        }
                        val nearestCenterDistance = minOf(
                            kotlin.math.abs(segment - leftCenterIndex),
                            kotlin.math.abs(segment - centerIndex)
                        ).toFloat()
                        val maxDistance = maxOf(leftCenterIndex, segmentCount - 1 - centerIndex).toFloat().coerceAtLeast(1f)
                        (0.2f + (nearestCenterDistance / maxDistance) * 0.8f).coerceIn(0.2f, 1f)
                    } else {
                        (segment + 1) / segmentCount.toFloat()
                    }
                    val barHeight = maxHeight * segmentRatio
                    val top = maxHeight - barHeight
                    val intensity = meterModel.segmentLevels.getOrElse(segment) { 0f }.coerceIn(0f, 1f)

                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(left, top),
                        size = Size(segmentWidth, barHeight),
                        cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                    )

                    if (intensity > 0.001f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    meterVariantColor.copy(alpha = 0.32f + (0.40f * intensity)),
                                    meterOnSurfaceColor.copy(alpha = 0.44f + (0.42f * intensity)),
                                    meterPrimaryColor.copy(alpha = 0.22f + (0.78f * intensity))
                                )
                            ),
                            topLeft = Offset(left, top),
                            size = Size(segmentWidth, barHeight),
                            cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                        )
                    }
                }

                if (meterModel.usesSymmetricCenterLayout) {
                    val centerPeakX = centerIndex * (segmentWidth + segmentGap) + (segmentWidth / 2f)
                    val leftCenterPeakX = if (meterModel.symmetricSeedCount == 2) {
                        (centerIndex - 1).coerceAtLeast(0) * (segmentWidth + segmentGap) + (segmentWidth / 2f)
                    } else {
                        centerPeakX
                    }
                    val betweenCentersPeakX = (leftCenterPeakX + centerPeakX) / 2f
                    val maxPairDistance = ((segmentCount - meterModel.symmetricSeedCount).coerceAtLeast(0) / 2).coerceAtLeast(1).toFloat()
                    val peakDistance = animatedPeak.coerceIn(0f, 1f) * maxPairDistance
                    if (peakDistance <= 0.001f) {
                        if (meterModel.symmetricSeedCount == 2 && meterModel.activeSegments > 0) {
                            drawLine(
                                color = peakColor,
                                start = Offset(betweenCentersPeakX, 0f),
                                end = Offset(betweenCentersPeakX, size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        } else {
                            val restingPeakX = if (meterModel.symmetricSeedCount == 2) betweenCentersPeakX else centerPeakX
                            drawLine(
                                color = peakColor,
                                start = Offset(restingPeakX, 0f),
                                end = Offset(restingPeakX, size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    } else {
                        val leftPeakX: Float
                        val rightPeakX: Float
                        if (meterModel.symmetricSeedCount == 2) {
                            val unitSpan = segmentWidth + segmentGap
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
                            val travelPerSide = (segmentWidth + segmentGap) * peakDistance
                            leftPeakX = centerPeakX - travelPerSide
                            rightPeakX = centerPeakX + travelPerSide
                        }
                        val clampedLeftPeakX = leftPeakX.coerceAtLeast(segmentWidth / 2f)
                        val clampedRightPeakX = rightPeakX.coerceAtMost(size.width - (segmentWidth / 2f))
                        drawLine(
                            color = peakColor,
                            start = Offset(clampedLeftPeakX, 0f),
                            end = Offset(clampedLeftPeakX, size.height),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = peakColor,
                            start = Offset(clampedRightPeakX, 0f),
                            end = Offset(clampedRightPeakX, size.height),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    val peakX = ((animatedPeak.coerceIn(0f, 1f) * (segmentCount - 1).coerceAtLeast(0).toFloat()) * (segmentWidth + segmentGap)) +
                        (segmentWidth / 2f)
                    drawLine(
                        color = peakColor,
                        start = Offset(peakX, 0f),
                        end = Offset(peakX, size.height),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                if (!meterModel.usesGlyphBrightnessPreview) {
                    val sweepHeight = maxHeight * animatedLevel.coerceIn(0f, 1f)
                    drawRoundRect(
                        color = sweepColor,
                        topLeft = Offset(0f, maxHeight - sweepHeight),
                        size = Size(size.width, sweepHeight),
                        cornerRadius = CornerRadius(40f, 40f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (animatedLevel * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(
                    R.string.meter_segments_value,
                    meterModel.activeSegments,
                    meterModel.segmentCount
                ),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
private fun meterBackgroundBrush(nothingStyleEnabled: Boolean): Brush {
    return if (nothingStyleEnabled) {
        val nothingMeterSurface = if (isSystemInDarkTheme()) {
            Color(0xFF2A2A2A)
        } else {
            Color(0xFFF2F2F2)
        }
        Brush.linearGradient(
            listOf(
                nothingMeterSurface,
                nothingMeterSurface
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.surfaceDim
            )
        )
    }
}

@Composable
private fun MeterStat(
    label: String,
    value: String,
    nothingStyleEnabled: Boolean
) {
    val valueStyle = if (nothingStyleEnabled) {
        MaterialTheme.typography.titleLarge.copy(
            fontFamily = NothingDotFontFamily,
            fontWeight = FontWeight.Normal
        )
    } else {
        MaterialTheme.typography.titleLarge
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.SemiBold
        )
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
private fun ParameterSlider(
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
    reverseDirection: Boolean,
    activeMode: String,
    glyphMode: String,
    isPhone3Device: Boolean,
    isPhone4aProDevice: Boolean,
    isPhone2aDevice: Boolean,
    isPhone3aDevice: Boolean,
    isPhone4aDevice: Boolean,
    binaryMode: Boolean,
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
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    startPending: Boolean,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onStopClick: () -> Unit
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showImportExportDialog by rememberSaveable { mutableStateOf(false) }
    val stopButtonColor = if (nothingStyleEnabled) NothingRed else MaterialTheme.colorScheme.error
    val stopButtonContentColor = if (nothingStyleEnabled) Color.White else MaterialTheme.colorScheme.onError

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

    androidx.compose.material3.Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_control_title),
                style = MaterialTheme.typography.titleMedium
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
                style = MaterialTheme.typography.titleMedium
            )
            val deviceProfile = when {
                isPhone3Device -> GlyphDeviceProfile.PHONE3_MATRIX
                isPhone4aProDevice -> GlyphDeviceProfile.PHONE4A_PRO_MATRIX
                isPhone2aDevice -> GlyphDeviceProfile.PHONE2A
                isPhone3aDevice -> GlyphDeviceProfile.PHONE3A
                isPhone4aDevice -> GlyphDeviceProfile.PHONE4A
                else -> GlyphDeviceProfile.PHONE2
            }
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

                    if (GlyphPatternRegistry.isAllBrightness(glyphMode) ||
                        (GlyphPatternRegistry.isSpectrum(glyphMode) && !isPhone3Device && !isPhone4aProDevice)
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
                        valueRange = 10f..60f,
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
                        text = if (reverseDirection) {
                            stringResource(R.string.glyph_direction_top_to_bottom)
                        } else {
                            stringResource(R.string.glyph_direction_bottom_to_top)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reverseDirection,
                    onCheckedChange = onReverseDirectionChanged
                )
            }

            if (!isPhone3Device && !isPhone4aProDevice) {
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
                            text = if (binaryMode) {
                                stringResource(R.string.binary_mode_on)
                            } else {
                                stringResource(R.string.binary_mode_off)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = binaryMode,
                        onCheckedChange = onBinaryModeChanged
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
                            text = if (levelAutoScale) {
                                stringResource(R.string.level_auto_scale_on)
                            } else {
                                stringResource(R.string.level_auto_scale_off)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
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
                            text = if (spectrumAutoScale) {
                                stringResource(R.string.spectrum_auto_scale_on)
                            } else {
                                stringResource(R.string.spectrum_auto_scale_off)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
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
                            text = if (allBrightnessAutoScale) {
                                stringResource(R.string.all_brightness_auto_scale_on)
                            } else {
                                stringResource(R.string.all_brightness_auto_scale_off)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allBrightnessAutoScale,
                        onCheckedChange = onAllBrightnessAutoScaleChanged
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
                        text = stringResource(R.string.turn_off_when_back_down_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (turnOffWhenBackDown) {
                            stringResource(R.string.turn_off_when_back_down_on)
                        } else {
                            stringResource(R.string.turn_off_when_back_down_off)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = turnOffWhenBackDown,
                    onCheckedChange = onTurnOffWhenBackDownChanged
                )
            }

        }
    }
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
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
            latencyMs = 0f,
            defaultOutputLatencyMs = 0f,
            bluetoothLatencyMs = 0f,
            latencyAutoSwitchEnabled = true,
            isBluetoothOutputActive = false,
            reverseDirection = true,
            meterSegments = remember { 11 },
            activeMode = stringResource(R.string.mode_visualizer),
            glyphMode = GlyphPatternRegistry.P2_C1_LINEAR,
            isPhone3Device = false,
            isPhone4aProDevice = false,
            isPhone2aDevice = false,
            isPhone3aDevice = false,
            isPhone4aDevice = false,
            binaryMode = false,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            mediaProjectionEnabled = false,
            glyphMeterPreviewEnabled = true,
            nothingStyleEnabled = false,
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
            onLatencyMsChanged = {},
            onLatencyMsChangeFinished = {},
            onLatencyAutoSwitchChanged = {},
            onGlyphMeterPreviewEnabledChanged = {},
            onReverseDirectionChanged = {},
            onGlyphModeChanged = {},
            onBinaryModeChanged = {},
            onLevelAutoScaleChanged = {},
            onSpectrumAutoScaleChanged = {},
            onAllBrightnessAutoScaleChanged = {},
            onMediaProjectionEnabledChanged = {},
            onNothingStyleEnabledChanged = {},
            onTurnOffWhenBackDownChanged = {},
            onResetParametersClick = {},
            onExportParametersClick = {},
            onImportParametersClick = {},
            onStartVisualizerClick = {},
            onStartProjectionClick = {},
            onStopClick = {},
            logMessage = null,
            onDismissLog = {}
        )
    }
}


