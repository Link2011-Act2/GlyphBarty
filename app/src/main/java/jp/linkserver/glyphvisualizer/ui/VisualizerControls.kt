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
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRenderMode
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey
import jp.linkserver.glyphvisualizer.glyph.resolveGlyphVisualTuning
import jp.linkserver.glyphvisualizer.glyph.Phone4aAsPhone4bGlyphProbe
import jp.linkserver.glyphvisualizer.glyph.supportsFillOtherGlyphLights
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
internal fun ControlCard(
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
    phone1ClassicCSplitEnabled: Boolean,
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
    onPhone1ClassicCSplitEnabledChanged: (Boolean) -> Unit,
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
    val supportsFillOtherGlyphLights = deviceProfile.supportsFillOtherGlyphLights()
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
                    toneFocus <= -0.1f -> stringResource(
                        R.string.param_tone_focus_bass,
                        (toneFocus * -100).toInt()
                    )
                    toneFocus >= 0.1f -> stringResource(
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
internal fun StableSwitch(
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
