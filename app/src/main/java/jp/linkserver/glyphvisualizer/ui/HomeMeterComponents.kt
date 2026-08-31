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
internal fun MeterInfoSection(
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
internal fun StatusMessageCard(
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
internal fun LogCard(
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
internal fun HeroCard(
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
                            toneFocus <= -0.1f -> stringResource(
                                R.string.hero_tone_focus_bass,
                                (toneFocus * -100).toInt()
                            )
                            toneFocus >= 0.1f -> stringResource(
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
internal fun IsolatedHeroCard(
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
internal fun CompactMeterOverlay(
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
internal fun IsolatedCompactMeterOverlay(
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
internal fun UpdateNotificationOverlay(
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
internal fun materialCardBorder(): BorderStroke {
    return BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun materialCardColor(prominent: Boolean = false): Color {
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
