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
import androidx.compose.material.icons.filled.Star
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
internal fun WelcomeScreen(
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
internal fun MainScreenContent(
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
    experimentalAdaptiveAutoScaleEnabled: Boolean,
    visualDynamics: Float,
    visualDynamicsOverridden: Boolean,
    onVisualDynamicsChanged: (Float) -> Unit,
    onVisualDynamicsChangeFinished: () -> Unit,
    onVisualDynamicsReset: () -> Unit,
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
    onPhone1ClassicCSplitEnabledChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onExperimentalAdaptiveAutoScaleEnabledChanged: (Boolean) -> Unit,
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
            phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
            deviceProfile = deviceProfile,
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
            onStartVisualizerClick = onStartVisualizerClick,
            onStartProjectionClick = onStartProjectionClick,
            onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
            onStopClick = onStopClick,
            onOpenMenu = onOpenMenu,
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
                        phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
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
                        onPhone1ClassicCSplitEnabledChanged = onPhone1ClassicCSplitEnabledChanged,
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
internal fun LatencyScreenContent(
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
    val experimentalDarkStyle = experimentalStyle && isSystemInDarkTheme()
    val experimentalDividerColor = if (experimentalDarkStyle) {
        Color(0xFF3A3A3A)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    var latencySliderValue by rememberSaveable { mutableStateOf(latencyMs) }

    LaunchedEffect(latencyMs) {
        latencySliderValue = latencyMs
    }

    Scaffold(
        containerColor = if (experimentalDarkStyle) Color.Black else Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (experimentalDarkStyle) Color.Black else Color.Transparent,
                    navigationIconContentColor = if (experimentalDarkStyle) {
                        Color.White
                    } else {
                        Color.Unspecified
                    },
                    titleContentColor = if (experimentalDarkStyle) Color.White else Color.Unspecified,
                    actionIconContentColor = if (experimentalDarkStyle) {
                        Color.White
                    } else {
                        Color.Unspecified
                    }
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
                    if (experimentalDarkStyle) {
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
                            HorizontalDivider(color = experimentalDividerColor)
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
                            HorizontalDivider(color = experimentalDividerColor)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalScreenContent(
    containerBrush: Brush,
    actualDeviceProfile: GlyphDeviceProfile,
    phone4bEmulationEnabled: Boolean,
    debugDeviceProfileOverride: GlyphDeviceProfile?,
    isCapturing: Boolean,
    nothingStyleEnabled: Boolean,
    onPhone4bEmulationEnabledChanged: (Boolean) -> Unit,
    onDebugDeviceProfileOverrideChanged: (GlyphDeviceProfile?) -> Unit,
    onOpenGlyphInspector: () -> Unit,
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
                SettingsEntry(
                    title = stringResource(R.string.glyph_inspector_title),
                    description = stringResource(R.string.glyph_inspector_entry_desc),
                    onClick = onOpenGlyphInspector,
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                )

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
internal fun HomeDrawerOverlay(
    visible: Boolean,
    currentScreen: Screen,
    nothingStyleEnabled: Boolean,
    showLatency: Boolean,
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
                    fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null,
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
                if (showLatency) {
                    HomeDrawerItem(
                        title = stringResource(R.string.menu_latency),
                        icon = Icons.Default.Equalizer,
                        selected = currentScreen == Screen.LATENCY,
                        nothingStyleEnabled = nothingStyleEnabled,
                        selectedColor = selectedColor,
                        onClick = { onNavigate(Screen.LATENCY) }
                    )
                }
                HomeDrawerItem(
                    title = stringResource(R.string.menu_extras),
                    icon = Icons.Default.Star,
                    selected = currentScreen == Screen.EXTRAS,
                    nothingStyleEnabled = nothingStyleEnabled,
                    selectedColor = selectedColor,
                    onClick = { onNavigate(Screen.EXTRAS) }
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
