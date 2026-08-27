package jp.linkserver.glyphvisualizer.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.GlyphControllerFamily
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphInspectorPreviewRenderer
import jp.linkserver.glyphvisualizer.glyph.GlyphLightController
import jp.linkserver.glyphvisualizer.glyph.GlyphMatrixPreviewGeometry
import jp.linkserver.glyphvisualizer.glyph.GlyphMatrixController
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRenderMode
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphPreviewFrame
import jp.linkserver.glyphvisualizer.glyph.GlyphPreviewFrameStore
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_LIGHT_BRIGHTNESS = 4095

private sealed interface GlyphChannelGroupLabel {
    data class Zone(val name: String) : GlyphChannelGroupLabel
    data class Range(val first: Int, val last: Int) : GlyphChannelGroupLabel
    data object Recording : GlyphChannelGroupLabel
}

private data class GlyphChannelGroup(
    val label: GlyphChannelGroupLabel,
    val channels: List<Int>
)

private enum class GlyphInspectorInputMode {
    LIVE,
    MANUAL,
    SWEEP
}

private enum class GlyphInspectorFrameStatus {
    LIVE,
    SIMULATED,
    FROZEN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphInterfaceInspectorScreen(
    containerBrush: Brush,
    nothingStyleEnabled: Boolean,
    initialDeviceProfile: GlyphDeviceProfile,
    onBack: () -> Unit
) {
    var liveFrame by remember { mutableStateOf<GlyphPreviewFrame?>(null) }
    var inputModeName by rememberSaveable { mutableStateOf(GlyphInspectorInputMode.LIVE.name) }
    val inputMode = GlyphInspectorInputMode.valueOf(inputModeName)
    var selectedProfileName by rememberSaveable { mutableStateOf(initialDeviceProfile.name) }
    val selectedProfile = GlyphDeviceProfile.valueOf(selectedProfileName)
    var selectedGlyphMode by rememberSaveable {
        mutableStateOf(GlyphDeviceCatalog.defaultGlyphModeForProfile(initialDeviceProfile))
    }
    var manualLevel by rememberSaveable { mutableStateOf(0.5f) }
    var virtualTimestampMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var reverseDirection by rememberSaveable { mutableStateOf(false) }
    var binaryMode by rememberSaveable { mutableStateOf(false) }
    var fillOtherGlyphLightsEnabled by rememberSaveable { mutableStateOf(false) }
    var baseIndicatorEnabled by rememberSaveable { mutableStateOf(false) }
    var recordingLightIncluded by rememberSaveable { mutableStateOf(false) }
    var centerCorrectionEnabled by rememberSaveable { mutableStateOf(true) }
    var showChannelLabels by rememberSaveable { mutableStateOf(true) }
    var frozenFrame by remember { mutableStateOf<GlyphPreviewFrame?>(null) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showPatternDialog by rememberSaveable { mutableStateOf(false) }
    val frameListener = remember {
        { frame: GlyphPreviewFrame? -> liveFrame = frame }
    }

    DisposableEffect(frameListener) {
        GlyphPreviewFrameStore.register(frameListener)
        onDispose {
            GlyphPreviewFrameStore.unregister(frameListener)
        }
    }

    LaunchedEffect(inputMode, frozenFrame) {
        if (inputMode != GlyphInspectorInputMode.LIVE && frozenFrame == null) {
            while (true) {
                virtualTimestampMs = SystemClock.elapsedRealtime()
                delay(33L)
            }
        }
    }

    LaunchedEffect(inputMode, liveFrame?.deviceProfile, liveFrame?.glyphMode, frozenFrame) {
        val frame = liveFrame ?: return@LaunchedEffect
        if (inputMode == GlyphInspectorInputMode.LIVE && frozenFrame == null) {
            selectedProfileName = frame.deviceProfile.name
            if (GlyphPatternRegistry.isSupported(frame.deviceProfile, frame.glyphMode)) {
                selectedGlyphMode = frame.glyphMode
            }
        }
    }

    LaunchedEffect(selectedProfile) {
        if (!GlyphPatternRegistry.isSupported(selectedProfile, selectedGlyphMode)) {
            selectedGlyphMode = GlyphDeviceCatalog.defaultGlyphModeForProfile(selectedProfile)
        }
    }

    val sweepLevel = triangleSweepLevel(virtualTimestampMs)
    val virtualLevel = if (inputMode == GlyphInspectorInputMode.SWEEP) sweepLevel else manualLevel
    val exactVirtualFrame = if (inputMode == GlyphInspectorInputMode.LIVE) {
        null
    } else {
        GlyphExactVirtualPreviewFrame(
            profile = selectedProfile,
            glyphMode = selectedGlyphMode,
            level = virtualLevel,
            reverseDirection = reverseDirection,
            binaryMode = binaryMode,
            fillOtherGlyphLightsEnabled = fillOtherGlyphLightsEnabled,
            baseIndicatorEnabled = baseIndicatorEnabled,
            recordingLightIncluded = recordingLightIncluded,
            centerCorrectionEnabled = centerCorrectionEnabled,
            timestampMs = virtualTimestampMs
        )
    }
    val generatedFrame = if (inputMode == GlyphInspectorInputMode.LIVE) {
        null
    } else {
        exactVirtualFrame
    }
    val currentFrame = if (inputMode == GlyphInspectorInputMode.LIVE) liveFrame else generatedFrame
    val displayedFrame = frozenFrame ?: currentFrame

    if (showProfileDialog) {
        GlyphInspectorProfileDialog(
            selectedProfile = selectedProfile,
            onSelect = { profile ->
                selectedProfileName = profile.name
                selectedGlyphMode = GlyphDeviceCatalog.defaultGlyphModeForProfile(profile)
                frozenFrame = null
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false }
        )
    }
    if (showPatternDialog) {
        GlyphInspectorPatternDialog(
            profile = selectedProfile,
            selectedGlyphMode = selectedGlyphMode,
            onSelect = { mode ->
                selectedGlyphMode = mode
                frozenFrame = null
                showPatternDialog = false
            },
            onDismiss = { showPatternDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.glyph_inspector_title),
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlyphInspectorControls(
                    inputMode = inputMode,
                    selectedProfile = selectedProfile,
                    selectedGlyphMode = selectedGlyphMode,
                    level = virtualLevel,
                    manualLevel = manualLevel,
                    reverseDirection = reverseDirection,
                    binaryMode = binaryMode,
                    fillOtherGlyphLightsEnabled = fillOtherGlyphLightsEnabled,
                    baseIndicatorEnabled = baseIndicatorEnabled,
                    recordingLightIncluded = recordingLightIncluded,
                    centerCorrectionEnabled = centerCorrectionEnabled,
                    showChannelLabels = showChannelLabels,
                    frozen = frozenFrame != null,
                    canFreeze = currentFrame != null,
                    onInputModeChanged = { mode ->
                        inputModeName = mode.name
                        frozenFrame = null
                    },
                    onOpenProfile = { showProfileDialog = true },
                    onOpenPattern = { showPatternDialog = true },
                    onManualLevelChanged = { manualLevel = it },
                    onReverseDirectionChanged = { reverseDirection = it },
                    onBinaryModeChanged = { binaryMode = it },
                    onFillOtherGlyphLightsEnabledChanged = { fillOtherGlyphLightsEnabled = it },
                    onBaseIndicatorEnabledChanged = { baseIndicatorEnabled = it },
                    onRecordingLightIncludedChanged = { recordingLightIncluded = it },
                    onCenterCorrectionEnabledChanged = { centerCorrectionEnabled = it },
                    onShowChannelLabelsChanged = { showChannelLabels = it },
                    onFrozenChanged = { frozen ->
                        frozenFrame = if (frozen) currentFrame else null
                    }
                )

                when (val frame = displayedFrame) {
                    is GlyphPreviewFrame.Lights -> GlyphLightsFrameContent(
                        frame = frame,
                        showChannelLabels = showChannelLabels,
                        status = inspectorStatus(inputMode, frozenFrame != null),
                        exactFrame = inputMode == GlyphInspectorInputMode.LIVE
                    )
                    is GlyphPreviewFrame.Matrix -> GlyphMatrixFrameContent(
                        frame = frame,
                        status = inspectorStatus(inputMode, frozenFrame != null),
                        exactFrame = inputMode == GlyphInspectorInputMode.LIVE
                    )
                    null -> GlyphInspectorWaitingState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp, horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlyphExactVirtualPreviewFrame(
    profile: GlyphDeviceProfile,
    glyphMode: String,
    level: Float,
    reverseDirection: Boolean,
    binaryMode: Boolean,
    fillOtherGlyphLightsEnabled: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    centerCorrectionEnabled: Boolean,
    timestampMs: Long
): GlyphPreviewFrame? {
    val context = LocalContext.current.applicationContext
    val outputState = remember(profile, centerCorrectionEnabled) {
        mutableStateOf<GlyphPreviewFrame?>(null)
    }
    val controller = remember(profile, centerCorrectionEnabled, context) {
        val definition = requireNotNull(GlyphDeviceCatalog.definitionForProfile(profile))
        when (definition.controllerFamily) {
            GlyphControllerFamily.LIGHTS -> GlyphLightController(
                context = context,
                onStatusChanged = {},
                previewDeviceProfile = profile,
                previewCenterCorrectionEnabled = centerCorrectionEnabled,
                previewFrameListener = { outputState.value = it }
            )
            GlyphControllerFamily.MATRIX -> GlyphMatrixController(
                context = context,
                onStatusChanged = {},
                previewDeviceProfile = profile,
                previewFrameListener = { outputState.value = it }
            )
        }
    }

    LaunchedEffect(
        controller,
        glyphMode,
        level,
        reverseDirection,
        binaryMode,
        fillOtherGlyphLightsEnabled,
        baseIndicatorEnabled,
        recordingLightIncluded,
        timestampMs
    ) {
        controller.setGlyphMode(glyphMode)
        controller.setReverseDirection(reverseDirection)
        controller.setBinaryMode(binaryMode)
        controller.setFillOtherGlyphLightsEnabled(fillOtherGlyphLightsEnabled)
        controller.setBaseIndicatorEnabled(baseIndicatorEnabled)
        controller.setRecordingLightIncluded(recordingLightIncluded)
        controller.setLevelAutoScaleEnabled(false)
        controller.setSpectrumAutoScaleEnabled(false)
        controller.setAllBrightnessAutoScaleEnabled(false)
        controller.setExperimentalPerformanceOptimizationsEnabled(false)
        controller.setMatrixSmoothMotionEnabled(true)

        val spectrum = GlyphInspectorPreviewRenderer.spectrum(timestampMs, level)
        val waveform = GlyphInspectorPreviewRenderer.waveform(
            timestampMs,
            level,
            phaseOffset = 0f
        )
        val leftWaveform = GlyphInspectorPreviewRenderer.waveform(
            timestampMs,
            level,
            phaseOffset = -0.45f
        )
        val rightWaveform = GlyphInspectorPreviewRenderer.waveform(
            timestampMs,
            level,
            phaseOffset = 0.45f
        )
        val half = (spectrum.size / 2).coerceAtLeast(1)
        val lowEnergy = spectrum.take(half).maxOrNull() ?: 0f
        val highEnergy = spectrum.drop(half).maxOrNull() ?: 0f
        controller.updateAnalysis(
            lowEnergy = lowEnergy,
            highEnergy = highEnergy,
            leftLevel = (level * 0.9f).coerceIn(0f, 1f),
            rightLevel = level.coerceIn(0f, 1f),
            spectrumBands = spectrum,
            phone4aBaseBandLevel = lowEnergy,
            waveformSamples = waveform,
            leftWaveformSamples = leftWaveform,
            rightWaveformSamples = rightWaveform
        )
        controller.updateLevel(level)
    }

    return outputState.value
}

@Composable
private fun GlyphInspectorControls(
    inputMode: GlyphInspectorInputMode,
    selectedProfile: GlyphDeviceProfile,
    selectedGlyphMode: String,
    level: Float,
    manualLevel: Float,
    reverseDirection: Boolean,
    binaryMode: Boolean,
    fillOtherGlyphLightsEnabled: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    centerCorrectionEnabled: Boolean,
    showChannelLabels: Boolean,
    frozen: Boolean,
    canFreeze: Boolean,
    onInputModeChanged: (GlyphInspectorInputMode) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPattern: () -> Unit,
    onManualLevelChanged: (Float) -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onFillOtherGlyphLightsEnabledChanged: (Boolean) -> Unit,
    onBaseIndicatorEnabledChanged: (Boolean) -> Unit,
    onRecordingLightIncludedChanged: (Boolean) -> Unit,
    onCenterCorrectionEnabledChanged: (Boolean) -> Unit,
    onShowChannelLabelsChanged: (Boolean) -> Unit,
    onFrozenChanged: (Boolean) -> Unit
) {
    val pattern = GlyphPatternRegistry.definition(selectedGlyphMode)
    val patternLabel = if (pattern != null) stringResource(pattern.labelRes) else selectedGlyphMode
    val profileLabel = GlyphDeviceCatalog.presentationForProfile(selectedProfile).deviceLabel
    val virtualInput = inputMode != GlyphInspectorInputMode.LIVE
    val isLightsProfile = GlyphDeviceCatalog.definitionForProfile(selectedProfile)
        ?.controllerFamily == GlyphControllerFamily.LIGHTS
    val isPhone4Bar = selectedProfile == GlyphDeviceProfile.PHONE4A ||
        selectedProfile == GlyphDeviceProfile.PHONE4B
    val supportsFillOtherGlyphLights = selectedProfile in setOf(
        GlyphDeviceProfile.PHONE1,
        GlyphDeviceProfile.PHONE2,
        GlyphDeviceProfile.PHONE2A
    )
    val supportsBaseIndicator = isPhone4Bar
    val fillOtherGlyphLightsEnabledForMode = supportsFillOtherGlyphLights &&
        !GlyphPatternRegistry.isAllBrightness(selectedGlyphMode) &&
        pattern?.recipe?.renderMode != GlyphPatternRenderMode.CLASSIC

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.glyph_inspector_input_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlyphInspectorInputMode.entries.forEach { mode ->
                    FilterChip(
                        selected = inputMode == mode,
                        onClick = { onInputModeChanged(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        GlyphInspectorInputMode.LIVE -> R.string.glyph_inspector_input_live
                                        GlyphInspectorInputMode.MANUAL -> R.string.glyph_inspector_input_manual
                                        GlyphInspectorInputMode.SWEEP -> R.string.glyph_inspector_input_sweep
                                    }
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
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenProfile,
                    enabled = virtualInput
                ) {
                    Text(profileLabel, maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPattern,
                    enabled = virtualInput
                ) {
                    Text(patternLabel, maxLines = 1)
                }
            }

            if (virtualInput) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.glyph_inspector_level),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.glyph_inspector_level_value,
                                (level * 100f).roundToInt()
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = if (inputMode == GlyphInspectorInputMode.MANUAL) manualLevel else level,
                        onValueChange = onManualLevelChanged,
                        enabled = inputMode == GlyphInspectorInputMode.MANUAL && !frozen,
                        valueRange = 0f..1f
                    )
                    Text(
                        text = stringResource(R.string.glyph_inspector_virtual_level_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlyphInspectorToggleRow(
                    title = stringResource(R.string.glyph_inspector_reverse),
                    checked = reverseDirection,
                    enabled = !frozen,
                    onCheckedChange = onReverseDirectionChanged
                )
                GlyphInspectorToggleRow(
                    title = stringResource(R.string.glyph_inspector_binary),
                    checked = binaryMode,
                    enabled = !frozen,
                    onCheckedChange = onBinaryModeChanged
                )
                if (fillOtherGlyphLightsEnabledForMode) {
                    GlyphInspectorToggleRow(
                        title = stringResource(R.string.glyph_inspector_fill_other_lights),
                        checked = fillOtherGlyphLightsEnabled,
                        enabled = !frozen,
                        onCheckedChange = onFillOtherGlyphLightsEnabledChanged
                    )
                }
                if (supportsBaseIndicator) {
                    GlyphInspectorToggleRow(
                        title = stringResource(R.string.glyph_inspector_base_indicator),
                        checked = baseIndicatorEnabled,
                        enabled = !frozen,
                        onCheckedChange = onBaseIndicatorEnabledChanged
                    )
                }
                if (isPhone4Bar) {
                    GlyphInspectorToggleRow(
                        title = stringResource(R.string.glyph_inspector_include_recording_light),
                        checked = recordingLightIncluded,
                        enabled = !frozen,
                        onCheckedChange = onRecordingLightIncludedChanged
                    )
                }
                if (pattern?.kind == GlyphPatternKind.CENTER) {
                    GlyphInspectorToggleRow(
                        title = stringResource(R.string.glyph_inspector_center_correction),
                        checked = centerCorrectionEnabled,
                        enabled = !frozen,
                        onCheckedChange = onCenterCorrectionEnabledChanged
                    )
                }
            }

            if (isLightsProfile) {
                GlyphInspectorToggleRow(
                    title = stringResource(R.string.glyph_inspector_channel_labels),
                    checked = showChannelLabels,
                    onCheckedChange = onShowChannelLabelsChanged
                )
            }
            GlyphInspectorToggleRow(
                title = stringResource(R.string.glyph_inspector_freeze),
                checked = frozen,
                enabled = canFreeze,
                onCheckedChange = onFrozenChanged
            )
        }
    }
}

@Composable
private fun GlyphInspectorToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            }
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun GlyphInspectorProfileDialog(
    selectedProfile: GlyphDeviceProfile,
    onSelect: (GlyphDeviceProfile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.glyph_inspector_device_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlyphDeviceProfile.entries.forEach { profile ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(profile) }
                    ) {
                        val selectedSuffix = if (profile == selectedProfile) {
                            stringResource(R.string.glyph_inspector_selected_suffix)
                        } else {
                            ""
                        }
                        Text(
                            GlyphDeviceCatalog.presentationForProfile(profile).deviceLabel +
                                selectedSuffix
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun GlyphInspectorPatternDialog(
    profile: GlyphDeviceProfile,
    selectedGlyphMode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.glyph_inspector_pattern_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlyphPatternRegistry.patternsFor(profile).forEach { pattern ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(pattern.id) }
                    ) {
                        val selectedSuffix = if (pattern.id == selectedGlyphMode) {
                            stringResource(R.string.glyph_inspector_selected_suffix)
                        } else {
                            ""
                        }
                        Text(stringResource(pattern.labelRes) + selectedSuffix)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

private fun triangleSweepLevel(timestampMs: Long): Float {
    val phase = (timestampMs % 4_000L) / 4_000f
    return if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
}

private fun inspectorStatus(
    inputMode: GlyphInspectorInputMode,
    frozen: Boolean
): GlyphInspectorFrameStatus = when {
    frozen -> GlyphInspectorFrameStatus.FROZEN
    inputMode == GlyphInspectorInputMode.LIVE -> GlyphInspectorFrameStatus.LIVE
    else -> GlyphInspectorFrameStatus.SIMULATED
}

@Composable
private fun GlyphInspectorWaitingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.outline
        ) {}
        Text(
            text = stringResource(R.string.glyph_inspector_waiting_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.glyph_inspector_waiting_desc),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlyphLightsFrameContent(
    frame: GlyphPreviewFrame.Lights,
    showChannelLabels: Boolean,
    status: GlyphInspectorFrameStatus,
    exactFrame: Boolean,
    modifier: Modifier = Modifier
) {
    val brightness = frame.brightness
    val physicalDeviceLabel = GlyphDeviceCatalog.presentationForProfile(
        frame.physicalDeviceProfile
    ).deviceLabel
    val rendererDeviceLabel = GlyphDeviceCatalog.presentationForProfile(frame.deviceProfile).deviceLabel
    val pattern = GlyphPatternRegistry.definition(frame.glyphMode)
    val patternLabel = if (pattern != null) stringResource(pattern.labelRes) else frame.glyphMode
    val litChannels = brightness.count { it > 0 }
    val maxBrightness = brightness.maxOrNull()?.coerceIn(0, MAX_LIGHT_BRIGHTNESS) ?: 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = physicalDeviceLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = patternLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (frame.physicalDeviceProfile != frame.deviceProfile) {
                            Text(
                                text = stringResource(
                                    R.string.glyph_inspector_renderer_profile,
                                    rendererDeviceLabel
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(9.dp),
                            shape = CircleShape,
                            color = inspectorStatusColor(status)
                        ) {}
                        Text(
                            text = inspectorStatusLabel(status),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.glyph_inspector_frame_summary,
                        litChannels,
                        brightness.size,
                        maxBrightness
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101010),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = stringResource(
                            if (exactFrame) {
                                R.string.glyph_inspector_final_frame_title
                            } else {
                                R.string.glyph_inspector_simulated_frame_title
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            if (exactFrame) {
                                R.string.glyph_inspector_final_frame_desc
                            } else {
                                R.string.glyph_inspector_simulated_frame_desc
                            }
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB7B7B7)
                    )
                }

                lightChannelGroups(frame.physicalDeviceProfile, brightness.size).forEach { group ->
                    GlyphChannelGroupRow(
                        group = group,
                        brightness = brightness,
                        showChannelLabels = showChannelLabels
                    )
                }
            }
        }
    }
}

@Composable
private fun GlyphMatrixFrameContent(
    frame: GlyphPreviewFrame.Matrix,
    status: GlyphInspectorFrameStatus,
    exactFrame: Boolean
) {
    val physicalDeviceLabel = GlyphDeviceCatalog.presentationForProfile(
        frame.physicalDeviceProfile
    ).deviceLabel
    val rendererDeviceLabel = GlyphDeviceCatalog.presentationForProfile(frame.deviceProfile).deviceLabel
    val pattern = GlyphPatternRegistry.definition(frame.glyphMode)
    val patternLabel = if (pattern != null) stringResource(pattern.labelRes) else frame.glyphMode
    val litPixels = frame.pixels.count { it > 0 }
    val maxBrightness = frame.pixels.maxOrNull()?.coerceIn(0, 255) ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = physicalDeviceLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = patternLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (frame.physicalDeviceProfile != frame.deviceProfile) {
                            Text(
                                text = stringResource(
                                    R.string.glyph_inspector_renderer_profile,
                                    rendererDeviceLabel
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(9.dp),
                            shape = CircleShape,
                            color = inspectorStatusColor(status)
                        ) {}
                        Text(
                            text = inspectorStatusLabel(status),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.glyph_inspector_matrix_summary,
                        litPixels,
                        frame.pixels.size,
                        frame.matrixSize,
                        maxBrightness
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101010),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(
                        if (exactFrame) {
                            R.string.glyph_inspector_final_matrix_title
                        } else {
                            R.string.glyph_inspector_simulated_matrix_title
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        if (exactFrame) {
                            R.string.glyph_inspector_final_matrix_desc
                        } else {
                            R.string.glyph_inspector_simulated_matrix_desc
                        }
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB7B7B7)
                )
                GlyphMatrixFrameCanvas(
                    frame = frame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun GlyphMatrixFrameCanvas(
    frame: GlyphPreviewFrame.Matrix,
    modifier: Modifier = Modifier
) {
    val matrixSize = frame.matrixSize
    val heights = remember(frame.physicalDeviceProfile, matrixSize) {
        GlyphMatrixPreviewGeometry.columnHeights(frame.physicalDeviceProfile, matrixSize)
    }
    Canvas(modifier = modifier) {
        if (matrixSize <= 0) return@Canvas
        val cellSize = min(size.width, size.height) / matrixSize
        val gridSize = cellSize * matrixSize
        val startX = (size.width - gridSize) / 2f
        val startY = (size.height - gridSize) / 2f
        val gap = (cellSize * 0.16f).coerceAtMost(2.2.dp.toPx())
        val dotSize = (cellSize - gap).coerceAtLeast(1f)
        for (y in 0 until matrixSize) {
            for (x in 0 until matrixSize) {
                val columnHeight = heights.getOrElse(x) { matrixSize }
                val columnTop = (matrixSize - columnHeight) / 2
                if (y !in columnTop until columnTop + columnHeight) continue
                val value = frame.pixels.getOrElse(y * matrixSize + x) { 0 }.coerceIn(0, 255)
                val ratio = value / 255f
                val color = lerp(Color(0xFF242424), Color.White, ratio)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        x = startX + x * cellSize + gap / 2f,
                        y = startY + y * cellSize + gap / 2f
                    ),
                    size = Size(dotSize, dotSize),
                    cornerRadius = CornerRadius(dotSize * 0.24f, dotSize * 0.24f)
                )
            }
        }
    }
}

@Composable
private fun inspectorStatusLabel(status: GlyphInspectorFrameStatus): String = stringResource(
    when (status) {
        GlyphInspectorFrameStatus.LIVE -> R.string.glyph_inspector_live
        GlyphInspectorFrameStatus.SIMULATED -> R.string.glyph_inspector_simulated
        GlyphInspectorFrameStatus.FROZEN -> R.string.glyph_inspector_frozen
    }
)

private fun inspectorStatusColor(status: GlyphInspectorFrameStatus): Color = when (status) {
    GlyphInspectorFrameStatus.LIVE -> Color(0xFF63D471)
    GlyphInspectorFrameStatus.SIMULATED -> Color(0xFF64B5F6)
    GlyphInspectorFrameStatus.FROZEN -> Color(0xFFFFB74D)
}

@Composable
private fun GlyphChannelGroupRow(
    group: GlyphChannelGroup,
    brightness: IntArray,
    showChannelLabels: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = groupLabel(group.label),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB7B7B7)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.channels.forEach { channel ->
                GlyphChannelTile(
                    channel = channel,
                    brightness = brightness.getOrElse(channel) { 0 },
                    showLabel = showChannelLabels
                )
            }
        }
    }
}

@Composable
private fun GlyphChannelTile(channel: Int, brightness: Int, showLabel: Boolean) {
    val clamped = brightness.coerceIn(0, MAX_LIGHT_BRIGHTNESS)
    val ratio = clamped / MAX_LIGHT_BRIGHTNESS.toFloat()
    val tileColor = lerp(Color(0xFF242424), Color.White, ratio)
    val contentColor = if (ratio >= 0.55f) Color.Black else Color.White

    Surface(
        modifier = Modifier.size(
            width = if (showLabel) 58.dp else 32.dp,
            height = if (showLabel) 68.dp else 48.dp
        ),
        shape = RoundedCornerShape(14.dp),
        color = tileColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (clamped > 0) Color.White else Color(0xFF454545)
        )
    ) {
        if (showLabel) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = channel.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = clamped.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun groupLabel(label: GlyphChannelGroupLabel): String = when (label) {
    is GlyphChannelGroupLabel.Zone -> label.name
    is GlyphChannelGroupLabel.Range -> if (label.first == label.last) {
        stringResource(R.string.glyph_inspector_channel, label.first)
    } else {
        stringResource(
            R.string.glyph_inspector_channel_range,
            label.first,
            label.last
        )
    }
    GlyphChannelGroupLabel.Recording -> stringResource(R.string.glyph_inspector_recording_light)
}

private fun lightChannelGroups(
    profile: GlyphDeviceProfile,
    channelCount: Int
): List<GlyphChannelGroup> {
    if (channelCount <= 0) return emptyList()

    val groups = when (profile) {
        GlyphDeviceProfile.PHONE1 -> listOf(
            channelGroup(GlyphChannelGroupLabel.Range(0, 1), 0..1),
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 2..5),
            channelGroup(GlyphChannelGroupLabel.Range(6, 6), 6..6),
            channelGroup(GlyphChannelGroupLabel.Zone("D1"), 7..14)
        )
        GlyphDeviceProfile.PHONE2 -> listOf(
            channelGroup(GlyphChannelGroupLabel.Range(0, 2), 0..2),
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 3..18),
            channelGroup(GlyphChannelGroupLabel.Range(19, 24), 19..24),
            channelGroup(GlyphChannelGroupLabel.Zone("D1"), 25..32)
        )
        GlyphDeviceProfile.PHONE2A -> listOf(
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 0..23),
            channelGroup(GlyphChannelGroupLabel.Zone("A"), 24..24),
            channelGroup(GlyphChannelGroupLabel.Zone("B"), 25..25)
        )
        GlyphDeviceProfile.PHONE3A -> listOf(
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 0..19),
            channelGroup(GlyphChannelGroupLabel.Zone("A"), 20..30),
            channelGroup(GlyphChannelGroupLabel.Zone("B"), 31..35)
        )
        GlyphDeviceProfile.PHONE4A -> listOf(
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 0..5),
            channelGroup(GlyphChannelGroupLabel.Recording, 6..6)
        )
        GlyphDeviceProfile.PHONE4B -> listOf(
            channelGroup(GlyphChannelGroupLabel.Zone("C"), 0..3),
            channelGroup(GlyphChannelGroupLabel.Recording, 4..4)
        )
        GlyphDeviceProfile.PHONE3_MATRIX,
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> listOf(
            channelGroup(GlyphChannelGroupLabel.Range(0, channelCount - 1), 0 until channelCount)
        )
    }

    val validGroups = groups.mapNotNull { group ->
        val channels = group.channels.filter { it in 0 until channelCount }
        if (channels.isEmpty()) null else group.copy(channels = channels)
    }
    val assignedChannels = validGroups.flatMapTo(mutableSetOf()) { it.channels }
    val unassignedChannels = (0 until channelCount).filterNot { it in assignedChannels }
    return if (unassignedChannels.isEmpty()) {
        validGroups
    } else {
        validGroups + GlyphChannelGroup(
            label = GlyphChannelGroupLabel.Range(
                unassignedChannels.first(),
                unassignedChannels.last()
            ),
            channels = unassignedChannels
        )
    }
}

private fun channelGroup(
    label: GlyphChannelGroupLabel,
    channels: IntRange
): GlyphChannelGroup = GlyphChannelGroup(label = label, channels = channels.toList())
