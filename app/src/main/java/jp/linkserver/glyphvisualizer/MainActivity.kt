package jp.linkserver.glyphvisualizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.nothing.ketchum.Common
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme

private const val MODE_C1_LINEAR = "C1_LINEAR"
private const val MODE_C1_CENTER = "C1_CENTER"
private const val MODE_D1 = "D1"
private const val MODE_D1_CENTER = "D1_CENTER"
private const val MODE_C1_SPECTRUM = "C1_SPECTRUM"
private const val MODE_D1_SPECTRUM = "D1_SPECTRUM"
private const val MODE_ALL_BRIGHTNESS = "ALL_BRIGHTNESS"

private const val MODE_P3A_C_LINEAR = "P3A_C_LINEAR"
private const val MODE_P3A_C_CENTER = "P3A_C_CENTER"
private const val MODE_P3A_C_SPECTRUM = "P3A_C_SPECTRUM"
private const val MODE_P3A_CAB_LINEAR = "P3A_CAB_LINEAR"
private const val MODE_P3A_CAB_CENTER = "P3A_CAB_CENTER"
private const val MODE_P3A_CAB_SPECTRUM = "P3A_CAB_SPECTRUM"
private const val MODE_P2A_C_LINEAR = "P2A_C_LINEAR"
private const val MODE_P2A_C_CENTER = "P2A_C_CENTER"
private const val MODE_P2A_C_SPECTRUM = "P2A_C_SPECTRUM"
private const val MODE_P2A_ALL_BRIGHTNESS = "P2A_ALL_BRIGHTNESS"
private const val MODE_P3A_ALL_BRIGHTNESS = "P3A_ALL_BRIGHTNESS"
private const val MODE_P4A_LINEAR = "P4A_LINEAR"
private const val MODE_P4A_CENTER = "P4A_CENTER"
private const val MODE_P4A_SPECTRUM = "P4A_SPECTRUM"
private const val MODE_P4A_ALL_BRIGHTNESS = "P4A_ALL_BRIGHTNESS"
private const val MODE_P3_MATRIX_BAR = "P3_MATRIX_BAR"
private const val MODE_P3_MATRIX_FIELD = "P3_MATRIX_FIELD"
private const val MODE_P3_MATRIX_CIRCLE = "P3_MATRIX_CIRCLE"
private const val MODE_P3_MATRIX_SPECTRUM = "P3_MATRIX_SPECTRUM"
private const val MODE_P3_MATRIX_SPECTRUM_CENTER = "P3_MATRIX_SPECTRUM_CENTER"
private const val MODE_P3_MATRIX_ALL_BRIGHTNESS = "P3_MATRIX_ALL_BRIGHTNESS"

private val PHONE2_MODE_KEYS = setOf(
    MODE_C1_LINEAR,
    MODE_C1_CENTER,
    MODE_D1,
    MODE_D1_CENTER,
    MODE_C1_SPECTRUM,
    MODE_D1_SPECTRUM,
    MODE_ALL_BRIGHTNESS
)

private val PHONE3A_MODE_KEYS = setOf(
    MODE_P3A_C_LINEAR,
    MODE_P3A_C_CENTER,
    MODE_P3A_C_SPECTRUM,
    MODE_P3A_CAB_LINEAR,
    MODE_P3A_CAB_CENTER,
    MODE_P3A_CAB_SPECTRUM,
    MODE_P3A_ALL_BRIGHTNESS
)

private val PHONE2A_MODE_KEYS = setOf(
    MODE_P2A_C_LINEAR,
    MODE_P2A_C_CENTER,
    MODE_P2A_C_SPECTRUM,
    MODE_P2A_ALL_BRIGHTNESS
)

private val PHONE4A_MODE_KEYS = setOf(
    MODE_P4A_LINEAR,
    MODE_P4A_CENTER,
    MODE_P4A_SPECTRUM,
    MODE_P4A_ALL_BRIGHTNESS
)

private val PHONE3_MODE_KEYS = setOf(
    MODE_P3_MATRIX_BAR,
    MODE_P3_MATRIX_FIELD,
    MODE_P3_MATRIX_CIRCLE,
    MODE_P3_MATRIX_SPECTRUM,
    MODE_P3_MATRIX_SPECTRUM_CENTER,
    MODE_P3_MATRIX_ALL_BRIGHTNESS
)

private val PHONE4A_PRO_MODE_KEYS = setOf(
    MODE_P3_MATRIX_BAR,
    MODE_P3_MATRIX_FIELD,
    MODE_P3_MATRIX_CIRCLE,
    MODE_P3_MATRIX_SPECTRUM,
    MODE_P3_MATRIX_SPECTRUM_CENTER,
    MODE_P3_MATRIX_ALL_BRIGHTNESS
)

private val SPECTRUM_MODE_KEYS = setOf(
    MODE_C1_SPECTRUM,
    MODE_D1_SPECTRUM,
    MODE_P2A_C_SPECTRUM,
    MODE_P3A_C_SPECTRUM,
    MODE_P3A_CAB_SPECTRUM,
    MODE_P4A_SPECTRUM,
    MODE_P3_MATRIX_SPECTRUM,
    MODE_P3_MATRIX_SPECTRUM_CENTER
)

private val ALL_BRIGHTNESS_MODE_KEYS = setOf(
    MODE_ALL_BRIGHTNESS,
    MODE_P2A_ALL_BRIGHTNESS,
    MODE_P3A_ALL_BRIGHTNESS,
    MODE_P4A_ALL_BRIGHTNESS,
    MODE_P3_MATRIX_ALL_BRIGHTNESS
)

private const val RESPONSE_SPEED_NONE_THRESHOLD = 0.54f

private enum class Screen {
    MAIN,
    SETTINGS,
    ABOUT,
    OSS
}

class MainActivity : ComponentActivity() {
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
                                                        turnOffWhenBackDown = uiState.turnOffWhenBackDown
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val savedSettings = SettingsPreferences.load(this)
        val normalizedMode = normalizeGlyphModeForCurrentDevice(savedSettings.glyphMode)
        CaptureUiStore.update {
            it.copy(
                statusText = getString(R.string.status_preparing_glyph_session),
                sensitivity = savedSettings.sensitivity,
                noiseGate = savedSettings.noiseGate,
                dynamics = savedSettings.dynamics,
                outputGamma = savedSettings.outputGamma,
                toneFocus = savedSettings.toneFocus,
                smoothing = savedSettings.smoothing,
                smoothingBalance = savedSettings.smoothingBalance,
                reverseDirection = savedSettings.reverseDirection,
                    peakHoldEnabled = savedSettings.peakHoldEnabled,
                    glyphMode = normalizedMode,
                    binaryMode = savedSettings.binaryMode,
                    levelAutoScale = savedSettings.levelAutoScale,
                    spectrumAutoScale = savedSettings.spectrumAutoScale,
                    turnOffWhenBackDown = savedSettings.turnOffWhenBackDown,
                allBrightnessAutoScale = savedSettings.allBrightnessAutoScale,
            )
        }
        setContent {
            GlyphBartyTheme {
                val uiState = CaptureUiStore.state
                GlyphVisualizerApp(
                    statusText = uiState.statusText,
                    isCapturing = uiState.isCapturing,
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
                        turnOffWhenBackDown = uiState.turnOffWhenBackDown,
                    onSensitivityChanged = { newValue ->
                        CaptureUiStore.update { it.copy(sensitivity = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onNoiseGateChanged = { newValue ->
                        CaptureUiStore.update { it.copy(noiseGate = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onDynamicsChanged = { newValue ->
                        CaptureUiStore.update { it.copy(dynamics = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onOutputGammaChanged = { newValue ->
                        CaptureUiStore.update { it.copy(outputGamma = newValue) }
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
                            updated.turnOffWhenBackDown,
                            updated.outputGamma
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onSmoothingChanged = { newValue ->
                        CaptureUiStore.update { it.copy(smoothing = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onSmoothingBalanceChanged = { newValue ->
                        CaptureUiStore.update { it.copy(smoothingBalance = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                    onToneFocusChanged = { newValue ->
                        CaptureUiStore.update { it.copy(toneFocus = newValue) }
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
                                updated.turnOffWhenBackDown
                        )
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
                                updated.turnOffWhenBackDown
                        )
                        SettingsPreferences.save(this, updated)
                    },
                        onGlyphModeChanged = { newMode ->
                            CaptureUiStore.update { it.copy(glyphMode = newMode) }
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
                                updated.turnOffWhenBackDown
                            )
                            SettingsPreferences.save(this, updated)
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
                                updated.turnOffWhenBackDown
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
                                updated.turnOffWhenBackDown
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
                                updated.turnOffWhenBackDown
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
                                updated.turnOffWhenBackDown
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
                                updated.turnOffWhenBackDown
                            )
                            SettingsPreferences.save(this, updated)
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
        isPhone3Device -> MODE_P3_MATRIX_SPECTRUM
        isPhone4aProDevice -> MODE_P3_MATRIX_SPECTRUM
        isPhone2aDevice -> MODE_P2A_C_LINEAR
        isPhone3aDevice -> MODE_P3A_C_LINEAR
        isPhone4aDevice -> MODE_P4A_LINEAR
        else -> MODE_C1_LINEAR
    }

    private fun normalizeGlyphModeForCurrentDevice(glyphMode: String): String = when {
        isPhone3Device && glyphMode in PHONE3_MODE_KEYS -> glyphMode
        isPhone3Device -> defaultGlyphModeForCurrentDevice()
        isPhone4aProDevice && glyphMode in PHONE4A_PRO_MODE_KEYS -> glyphMode
        isPhone4aProDevice -> defaultGlyphModeForCurrentDevice()
        isPhone2aDevice && glyphMode in PHONE2A_MODE_KEYS -> glyphMode
        isPhone2aDevice -> defaultGlyphModeForCurrentDevice()
        isPhone3aDevice && glyphMode in PHONE3A_MODE_KEYS -> glyphMode
        isPhone3aDevice -> defaultGlyphModeForCurrentDevice()
        isPhone4aDevice && glyphMode in PHONE4A_MODE_KEYS -> glyphMode
        isPhone4aDevice -> defaultGlyphModeForCurrentDevice()
        glyphMode in PHONE2_MODE_KEYS -> glyphMode
        else -> defaultGlyphModeForCurrentDevice()
    }

    private fun defaultParameterState(): CaptureUiState {
        return SettingsPreferences.defaultParameters().copy(
            glyphMode = defaultGlyphModeForCurrentDevice()
        )
    }

    private fun sanitizeParameterState(state: CaptureUiState): CaptureUiState {
        val parameters = SettingsPreferences.parameterStateOf(state)
        return parameters.copy(
            sensitivity = parameters.sensitivity.coerceIn(0.6f, 3.0f),
            noiseGate = parameters.noiseGate.coerceIn(0f, 0.35f),
            dynamics = parameters.dynamics.coerceIn(0.6f, 2.2f),
            outputGamma = parameters.outputGamma.coerceIn(0.6f, 2.6f),
            toneFocus = parameters.toneFocus.coerceIn(-1f, 1f),
            smoothing = parameters.smoothing.coerceIn(0.08f, 0.55f),
            glyphMode = normalizeGlyphModeForCurrentDevice(parameters.glyphMode)
        )
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
        pendingStartMode = mode
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            when (mode) {
                CaptureMode.VISUALIZER -> startVisualizerMode()
                CaptureMode.MEDIA_PROJECTION -> launchCaptureIntent()
            }
            pendingStartMode = null
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVisualizerMode() {
        val uiState = CaptureUiStore.state
        try {
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
                uiState.turnOffWhenBackDown,
                uiState.outputGamma
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
    turnOffWhenBackDown: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onSmoothingBalanceChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
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
    BackHandler(enabled = screen != Screen.MAIN) {
        when (screen) {
            Screen.OSS -> screen = Screen.ABOUT
            Screen.ABOUT -> screen = Screen.SETTINGS
            else -> screen = Screen.MAIN
        }
    }

    val containerBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
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
                    onReverseDirectionChanged = onReverseDirectionChanged,
                    onGlyphModeChanged = onGlyphModeChanged,
                    onBinaryModeChanged = onBinaryModeChanged,
                    onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                    onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                    onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                    onTurnOffWhenBackDownChanged = onTurnOffWhenBackDownChanged,
                    onStartVisualizerClick = onStartVisualizerClick,
                    onStartProjectionClick = onStartProjectionClick,
                    onStopClick = onStopClick,
                    logMessage = logMessage,
                    onDismissLog = onDismissLog,
                    onOpenSettings = { screen = Screen.SETTINGS }
                )
                Screen.SETTINGS -> SettingsScreen(
                    onBack = { screen = Screen.MAIN },
                    onAbout = { screen = Screen.ABOUT }
                )
                Screen.ABOUT -> AboutScreen(
                    onBack = { screen = Screen.SETTINGS },
                    onOssLicenses = { screen = Screen.OSS }
                )
                Screen.OSS -> OssLicensesScreen(onBack = { screen = Screen.ABOUT })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    containerBrush: Brush,
    statusText: String,
    isCapturing: Boolean,
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
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onStopClick: () -> Unit,
    logMessage: String?,
    onDismissLog: () -> Unit,
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
                    level = level,
                    peak = peak,
                    spectrumBands = spectrumBands,
                    glyphMode = glyphMode,
                    noiseGate = noiseGate,
                    dynamics = dynamics,
                    toneFocus = toneFocus,
                    smoothing = smoothing,
                    meterSegments = meterSegments,
                    activeMode = activeMode
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
                    onReverseDirectionChanged = onReverseDirectionChanged,
                    onGlyphModeChanged = onGlyphModeChanged,
                    onBinaryModeChanged = onBinaryModeChanged,
                    onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                    onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                    onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                    onTurnOffWhenBackDownChanged = onTurnOffWhenBackDownChanged,
                    onStartVisualizerClick = onStartVisualizerClick,
                    onStartProjectionClick = onStartProjectionClick,
                    onStopClick = onStopClick
                )

                InfoStrip()

                if (logMessage != null) {
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
private fun LogCard(
    message: String,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(message) { expanded = true }

    androidx.compose.material3.ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
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
    level: Float,
    peak: Float,
    spectrumBands: FloatArray,
    glyphMode: String,
    noiseGate: Float,
    dynamics: Float,
    toneFocus: Float,
    smoothing: Float,
    meterSegments: Int,
    activeMode: String
) {
    androidx.compose.material3.ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
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
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.hero_gate_dynamics,
                            (noiseGate * 100).toInt(),
                            dynamics
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
                    FilterChip(
                        selected = isCapturing,
                        onClick = {},
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
                meterSegments = meterSegments
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

@Composable
private fun MeterCanvas(
    level: Float,
    peak: Float,
    meterSegments: Int
) {
    val litBrush = Brush.verticalGradient(
        listOf(
            Color(0xFFFFB000),
            Color(0xFFFF7A00),
            MaterialTheme.colorScheme.primary
        )
    )
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
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceDim
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segmentGap = 10.dp.toPx()
                val segmentWidth = (size.width - (segmentGap * 15f)) / 16f
                val maxHeight = size.height

                for (segment in 0 until 16) {
                    val left = segment * (segmentWidth + segmentGap)
                    val segmentRatio = (segment + 1) / 16f
                    val barHeight = maxHeight * segmentRatio
                    val top = maxHeight - barHeight
                    val isLit = segment < meterSegments

                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(left, top),
                        size = Size(segmentWidth, barHeight),
                        cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                    )

                    if (isLit) {
                        drawRoundRect(
                            brush = litBrush,
                            topLeft = Offset(left, top),
                            size = Size(segmentWidth, barHeight),
                            cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                        )
                    }
                }

                val peakX = ((animatedPeak.coerceIn(0f, 1f) * 15f) * (segmentWidth + segmentGap)) +
                    (segmentWidth / 2f)
                drawLine(
                    color = peakColor,
                    start = Offset(peakX, 0f),
                    end = Offset(peakX, size.height),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val sweepHeight = maxHeight * animatedLevel.coerceIn(0f, 1f)
                drawRoundRect(
                    color = sweepColor,
                    topLeft = Offset(0f, maxHeight - sweepHeight),
                    size = Size(size.width, sweepHeight),
                    cornerRadius = CornerRadius(40f, 40f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (animatedLevel * 100).toInt())
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(R.string.meter_segments_value, meterSegments)
            )
        }
    }
}

@Composable
private fun MeterStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
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
    valueRange: ClosedFloatingPointRange<Float>
) {
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
            valueRange = valueRange
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
    turnOffWhenBackDown: Boolean,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onSmoothingBalanceChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
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

    androidx.compose.material3.ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_control_title),
                style = MaterialTheme.typography.titleLarge
            )

            if (isCapturing) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStopClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_stop))
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartVisualizerClick,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_no_capture))
                }
                // MediaProjection start button removed from UI by request.
                // The service and supporting code remain; re-add button if needed.
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.glyph_pattern),
                style = MaterialTheme.typography.titleMedium
            )
            val modes = if (isPhone3Device) {
                listOf(
                    MODE_P3_MATRIX_SPECTRUM to stringResource(R.string.mode_matrix_spectrum),
                    MODE_P3_MATRIX_SPECTRUM_CENTER to stringResource(R.string.mode_matrix_spectrum_center),
                    MODE_P3_MATRIX_BAR to stringResource(R.string.mode_matrix_bar),
                    MODE_P3_MATRIX_FIELD to stringResource(R.string.mode_matrix_field),
                    MODE_P3_MATRIX_CIRCLE to stringResource(R.string.mode_matrix_circle),
                    MODE_P3_MATRIX_ALL_BRIGHTNESS to stringResource(R.string.mode_all_brightness)
                )
            } else if (isPhone4aProDevice) {
                listOf(
                    MODE_P3_MATRIX_SPECTRUM to stringResource(R.string.mode_matrix_spectrum),
                    MODE_P3_MATRIX_SPECTRUM_CENTER to stringResource(R.string.mode_matrix_spectrum_center),
                    MODE_P3_MATRIX_BAR to stringResource(R.string.mode_matrix_bar),
                    MODE_P3_MATRIX_FIELD to stringResource(R.string.mode_matrix_field),
                    MODE_P3_MATRIX_CIRCLE to stringResource(R.string.mode_matrix_circle),
                    MODE_P3_MATRIX_ALL_BRIGHTNESS to stringResource(R.string.mode_all_brightness)
                )
            } else if (isPhone2aDevice) {
                listOf(
                    MODE_P2A_C_LINEAR to stringResource(R.string.mode_c1_linear),
                    MODE_P2A_C_CENTER to stringResource(R.string.mode_c1_center),
                    MODE_P2A_C_SPECTRUM to stringResource(R.string.mode_c1_spectrum),
                    MODE_P2A_ALL_BRIGHTNESS to stringResource(R.string.mode_all_brightness)
                )
            } else if (isPhone3aDevice) {
                listOf(
                    MODE_P3A_C_LINEAR to stringResource(R.string.mode_p3a_c_linear),
                    MODE_P3A_CAB_LINEAR to stringResource(R.string.mode_p3a_cab_linear),
                    MODE_P3A_C_CENTER to stringResource(R.string.mode_p3a_c_center),
                    MODE_P3A_CAB_CENTER to stringResource(R.string.mode_p3a_cab_center),
                    MODE_P3A_C_SPECTRUM to stringResource(R.string.mode_p3a_c_spectrum),
                    MODE_P3A_CAB_SPECTRUM to stringResource(R.string.mode_p3a_cab_spectrum),
                    MODE_P3A_ALL_BRIGHTNESS to stringResource(R.string.mode_all_brightness)
                )
            } else if (isPhone4aDevice) {
                listOf(
                    MODE_P4A_LINEAR to stringResource(R.string.mode_p4a_linear),
                    MODE_P4A_CENTER to stringResource(R.string.mode_p4a_center),
                    MODE_P4A_SPECTRUM to stringResource(R.string.mode_p4a_spectrum),
                    MODE_P4A_ALL_BRIGHTNESS to stringResource(R.string.mode_p4a_all_brightness)
                )
            } else {
                listOf(
                    MODE_C1_LINEAR to stringResource(R.string.mode_c1_linear),
                    MODE_D1 to stringResource(R.string.mode_d1),
                    MODE_C1_CENTER to stringResource(R.string.mode_c1_center),
                    MODE_D1_CENTER to stringResource(R.string.mode_d1_center),
                    MODE_C1_SPECTRUM to stringResource(R.string.mode_c1_spectrum),
                    MODE_D1_SPECTRUM to stringResource(R.string.mode_d1_spectrum),
                    MODE_ALL_BRIGHTNESS to stringResource(R.string.mode_all_brightness)
                )
            }

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
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
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
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
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
                valueRange = 0.6f..3.0f
            )

            ParameterSlider(
                title = stringResource(R.string.param_response_speed_title),
                valueText = responseSpeedValueText(smoothing),
                description = stringResource(R.string.param_response_speed_desc),
                value = smoothing,
                onValueChange = onSmoothingChanged,
                valueRange = 0.08f..0.55f
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
                valueRange = -1f..1f
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
                        valueRange = 0f..0.35f
                    )

                    ParameterSlider(
                        title = stringResource(R.string.param_dynamics_title),
                        valueText = stringResource(R.string.param_dynamics_value, dynamics),
                        description = stringResource(R.string.param_dynamics_desc),
                        value = dynamics,
                        onValueChange = onDynamicsChanged,
                        valueRange = 0.6f..2.2f
                    )

                    if (glyphMode in ALL_BRIGHTNESS_MODE_KEYS || ((glyphMode in SPECTRUM_MODE_KEYS) && !isPhone3Device && !isPhone4aProDevice)) {
                        ParameterSlider(
                            title = stringResource(R.string.param_output_gamma_title),
                            valueText = stringResource(R.string.param_dynamics_value, outputGamma),
                            description = stringResource(R.string.param_output_gamma_desc),
                            value = outputGamma,
                            onValueChange = onOutputGammaChanged,
                            valueRange = 0.6f..2.6f
                        )
                    }
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

            if (glyphMode !in SPECTRUM_MODE_KEYS && glyphMode !in ALL_BRIGHTNESS_MODE_KEYS) {
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

            if (glyphMode in SPECTRUM_MODE_KEYS) {
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

            if (glyphMode in ALL_BRIGHTNESS_MODE_KEYS) {
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
                color = MaterialTheme.colorScheme.secondaryContainer
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
            reverseDirection = true,
            meterSegments = remember { 11 },
            activeMode = stringResource(R.string.mode_visualizer),
            glyphMode = MODE_C1_LINEAR,
            isPhone3Device = false,
            isPhone4aProDevice = false,
            isPhone2aDevice = false,
            isPhone3aDevice = false,
            isPhone4aDevice = false,
            binaryMode = false,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            turnOffWhenBackDown = false,
            onSensitivityChanged = {},
            onNoiseGateChanged = {},
            onDynamicsChanged = {},
            onOutputGammaChanged = {},
            onSmoothingChanged = {},
            onSmoothingBalanceChanged = {},
            onToneFocusChanged = {},
            onReverseDirectionChanged = {},
            onGlyphModeChanged = {},
            onBinaryModeChanged = {},
            onLevelAutoScaleChanged = {},
            onSpectrumAutoScaleChanged = {},
            onAllBrightnessAutoScaleChanged = {},
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


