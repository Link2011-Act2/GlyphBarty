package jp.linkserver.glyphvisualizer.ui

import jp.linkserver.glyphvisualizer.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jp.linkserver.glyphvisualizer.ui.SettingsDividerGap
import jp.linkserver.glyphvisualizer.ui.SettingsEntry
import jp.linkserver.glyphvisualizer.ui.SettingsGroupPosition
import jp.linkserver.glyphvisualizer.ui.SettingsItemSurface
import jp.linkserver.glyphvisualizer.ui.SettingsToggleEntry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import jp.linkserver.glyphvisualizer.audio.WaveformSampler
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRenderMode
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed
import jp.linkserver.glyphvisualizer.glyph.supportsGlyphVisualDynamics

internal enum class ExperimentalDetailsTab {
    LIVE,
    SYSTEM,
    TUNE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalDetailsScreenContent(
    heroTitle: String,
    statusText: String,
    logMessage: String?,
    isCapturing: Boolean,
    startPending: Boolean,
    latencyMs: Float,
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    autoScaleWindowSeconds: Float,
    autoScaleOffset: Float,
    reverseDirection: Boolean,
    glyphMode: String,
    fillOtherGlyphLights: Boolean,
    phone1ClassicCSplitEnabled: Boolean,
    deviceProfile: GlyphDeviceProfile,
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
    nothingStyleEnabled: Boolean,
    showPhone1GlyphDebugControls: Boolean,
    isHome: Boolean,
    initialTab: ExperimentalDetailsTab,
    onTabChanged: (ExperimentalDetailsTab) -> Unit,
    onResetParametersClick: () -> Unit,
    onExportParametersClick: () -> Unit,
    onImportParametersClick: () -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
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
    onStartVisualizerClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLatency: () -> Unit,
    onBack: () -> Unit
) {
    val displayFont = if (nothingStyleEnabled) NTypeFontFamily else FontFamily.SansSerif
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bottomSheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val deviceName = remember(heroTitle) {
        heroTitle.lineSequence().firstOrNull { it.isNotBlank() } ?: heroTitle
    }
    val deviceDetail = remember(heroTitle) {
        heroTitle.lineSequence().drop(1).firstOrNull { it.isNotBlank() }
    }
    val patternDefinition = GlyphPatternRegistry.definition(glyphMode)
    val patternLabel = patternDefinition?.let { stringResource(it.labelRes) } ?: glyphMode
    val glyphRenderMode = patternDefinition?.recipe?.renderMode
    val recordingLightBehavior = resolveRecordingLightBehavior(
        baseIndicatorEnabled = baseIndicatorEnabled,
        recordingLightIncluded = recordingLightIncluded
    )
    val recordingLightBehaviorLabel = when (recordingLightBehavior) {
        RecordingLightBehavior.NONE -> stringResource(R.string.recording_light_behavior_none)
        RecordingLightBehavior.INCLUDED_IN_METER -> stringResource(R.string.recording_light_behavior_meter)
        RecordingLightBehavior.BASS_INDICATOR -> stringResource(R.string.recording_light_behavior_bass)
    }
    val supportsFillOtherGlyphLights = deviceProfile in setOf(
        GlyphDeviceProfile.PHONE1,
        GlyphDeviceProfile.PHONE2,
        GlyphDeviceProfile.PHONE2A
    )
    val supportsRecordingLightBehavior = deviceProfile.supportsRecordingLightBehavior()
    val fillOtherGlyphLightsEnabledForMode = supportsFillOtherGlyphLights &&
        !GlyphPatternRegistry.isAllBrightness(glyphMode) &&
        glyphRenderMode != GlyphPatternRenderMode.CLASSIC
    val isMatrixDevice = deviceProfile in setOf(
        GlyphDeviceProfile.PHONE3_MATRIX,
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX
    )

    val detailsTabs = ExperimentalDetailsTab.entries
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal,
        pageCount = { detailsTabs.size }
    )
    val selectedTab = detailsTabs[pagerState.currentPage]
    LaunchedEffect(selectedTab) {
        onTabChanged(selectedTab)
    }
    val coroutineScope = rememberCoroutineScope()
    var showPatternSheet by rememberSaveable { mutableStateOf(false) }
    var showRecordingLightDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    if (showPatternSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPatternSheet = false },
            containerColor = bottomSheetContainerColor,
            contentColor = contentColor,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 22.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.experimental_home_select_pattern),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    color = contentColor,
                    fontFamily = displayFont,
                    fontSize = 22.sp
                )
                LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                    items(
                        items = GlyphPatternRegistry.patternsFor(deviceProfile),
                        key = { it.id }
                    ) { pattern ->
                        val patternDescription = glyphPatternDescriptionText(pattern.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onGlyphModeChanged(pattern.id)
                                    showPatternSheet = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(pattern.labelRes),
                                    color = contentColor,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 17.sp
                                )
                                patternDescription?.let { description ->
                                    Text(
                                        text = description,
                                        color = secondaryContentColor,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            if (pattern.id == glyphMode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (nothingStyleEnabled) {
                                        NothingRed
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordingLightDialog && supportsRecordingLightBehavior) {
        ExperimentalRecordingLightDialog(
            selected = recordingLightBehavior,
            onDismiss = { showRecordingLightDialog = false },
            onSelected = { behavior ->
                showRecordingLightDialog = false
                onRecordingLightBehaviorChanged(behavior)
            }
        )
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = contentColor,
                    titleContentColor = contentColor,
                    actionIconContentColor = contentColor
                ),
                navigationIcon = {
                    if (!isHome) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            if (isHome) R.string.app_name else R.string.experimental_home_details
                        ),
                        fontFamily = displayFont,
                        fontSize = 25.sp
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ExperimentalDetailsTabRow(
                selectedTab = selectedTab,
                onSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> detailsTabs[page].name }
            ) { page ->
                val tab = detailsTabs[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        when (tab) {
                            ExperimentalDetailsTab.LIVE -> ExperimentalDetailsLiveTab(
                                deviceName = deviceName,
                                deviceDetail = deviceDetail,
                                isCapturing = isCapturing,
                                startPending = startPending,
                                patternLabel = patternLabel,
                                fillOtherGlyphLights = fillOtherGlyphLights,
                                showFillOtherGlyphLights = fillOtherGlyphLightsEnabledForMode,
                                recordingLightBehaviorLabel = recordingLightBehaviorLabel,
                                supportsRecordingLightBehavior = supportsRecordingLightBehavior,
                                glyphMode = glyphMode,
                                deviceProfile = deviceProfile,
                                binaryMode = binaryMode,
                                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                                meterVisibleEnabled = meterVisibleEnabled,
                                lightweightMeterEnabled = lightweightMeterEnabled,
                                spectrumMeterEnabled = spectrumMeterEnabled,
                                nativeMeterViewEnabled = nativeMeterViewEnabled,
                                recordingLightIncluded = recordingLightIncluded,
                                reverseDirection = reverseDirection,
                                mediaProjectionEnabled = mediaProjectionEnabled,
                                showStatusPanel = isHome,
                                statusText = statusText,
                                logMessage = logMessage,
                                showPhone1GlyphDebugControls = showPhone1GlyphDebugControls,
                                onStartClick = onStartVisualizerClick,
                                onStartProjectionClick = onStartProjectionClick,
                                onEnablePhone1GlyphDebugClick = onEnablePhone1GlyphDebugClick,
                                onStopClick = onStopClick,
                                onOpenPattern = { showPatternSheet = true },
                                onFillOtherGlyphLightsChanged = onFillOtherGlyphLightsChanged,
                                onOpenRecordingLight = { showRecordingLightDialog = true },
                                nothingStyleEnabled = nothingStyleEnabled
                            )

                            ExperimentalDetailsTab.TUNE -> ExperimentalDetailsTuneTab(
                                sensitivity = sensitivity,
                                noiseGate = noiseGate,
                                dynamics = dynamics,
                                outputGamma = outputGamma,
                                toneFocus = toneFocus,
                                smoothing = smoothing,
                                autoScaleWindowSeconds = autoScaleWindowSeconds,
                                autoScaleOffset = autoScaleOffset,
                                patternLabel = patternLabel,
                                patternId = glyphMode,
                                patternKind = patternDefinition?.kind,
                                deviceProfile = deviceProfile,
                                levelAutoScale = levelAutoScale,
                                spectrumAutoScale = spectrumAutoScale,
                                allBrightnessAutoScale = allBrightnessAutoScale,
                                experimentalAdaptiveAutoScaleEnabled =
                                    experimentalAdaptiveAutoScaleEnabled,
                                visualDynamics = visualDynamics,
                                visualDynamicsOverridden = visualDynamicsOverridden,
                                onVisualDynamicsChanged = onVisualDynamicsChanged,
                                onVisualDynamicsChangeFinished = onVisualDynamicsChangeFinished,
                                onVisualDynamicsReset = onVisualDynamicsReset,
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
                                onResetClick = { showResetDialog = true },
                                onImportClick = onImportParametersClick,
                                onExportClick = onExportParametersClick,
                                nothingStyleEnabled = nothingStyleEnabled
                            )

                            ExperimentalDetailsTab.SYSTEM -> ExperimentalDetailsSystemTab(
                                latencyMs = latencyMs,
                                reverseDirection = reverseDirection,
                                binaryMode = binaryMode,
                                matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                                oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                                levelAutoScale = levelAutoScale,
                                spectrumAutoScale = spectrumAutoScale,
                                allBrightnessAutoScale = allBrightnessAutoScale,
                                experimentalAdaptiveAutoScaleEnabled =
                                    experimentalAdaptiveAutoScaleEnabled,
                                glyphMode = glyphMode,
                                isMatrixDevice = isMatrixDevice,
                                glyphRenderMode = glyphRenderMode,
                                nothingStyleEnabled = nothingStyleEnabled,
                                onOpenLatency = onOpenLatency,
                                onReverseDirectionChanged = onReverseDirectionChanged,
                                onBinaryModeChanged = onBinaryModeChanged,
                                onMatrixSmoothMotionEnabledChanged = onMatrixSmoothMotionEnabledChanged,
                                onOscilloscopeAutoTimeAxisEnabledChanged = onOscilloscopeAutoTimeAxisEnabledChanged,
                                onLevelAutoScaleChanged = onLevelAutoScaleChanged,
                                onSpectrumAutoScaleChanged = onSpectrumAutoScaleChanged,
                                onAllBrightnessAutoScaleChanged = onAllBrightnessAutoScaleChanged,
                                onExperimentalAdaptiveAutoScaleEnabledChanged =
                                    onExperimentalAdaptiveAutoScaleEnabledChanged
                            )
                        }

                }
            }
        }
    }
}

@Composable
private fun ExperimentalDetailsTabRow(
    selectedTab: ExperimentalDetailsTab,
    onSelected: (ExperimentalDetailsTab) -> Unit
) {
    val labels = listOf(
        ExperimentalDetailsTab.LIVE to stringResource(R.string.experimental_details_tab_live),
        ExperimentalDetailsTab.SYSTEM to stringResource(R.string.experimental_details_tab_system),
        ExperimentalDetailsTab.TUNE to stringResource(R.string.experimental_details_tab_tune)
    )
    val selectedIndex = labels.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val sidePadding = 20.dp
            val tabWidth = (maxWidth - (sidePadding * 2)) / labels.size
            val indicatorWidth = 58.dp
            val targetOffset = sidePadding +
                (tabWidth * selectedIndex) +
                ((tabWidth - indicatorWidth) / 2)
            val indicatorOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "experimental_details_tab_indicator"
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sidePadding)
            ) {
                labels.forEach { (tab, label) ->
                    val selected = tab == selectedTab
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "experimental_details_tab_color_${tab.name}"
                    )
                    val labelScale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "experimental_details_tab_scale_${tab.name}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.graphicsLayer {
                                scaleX = labelScale
                                scaleY = labelScale
                            },
                            color = labelColor,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ExperimentalDetailsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ExperimentalDetailsLiveTab(
    deviceName: String,
    deviceDetail: String?,
    isCapturing: Boolean,
    startPending: Boolean,
    patternLabel: String,
    fillOtherGlyphLights: Boolean,
    showFillOtherGlyphLights: Boolean,
    recordingLightBehaviorLabel: String,
    supportsRecordingLightBehavior: Boolean,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    mediaProjectionEnabled: Boolean,
    showStatusPanel: Boolean,
    statusText: String,
    logMessage: String?,
    showPhone1GlyphDebugControls: Boolean,
    onStartClick: () -> Unit,
    onStartProjectionClick: () -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenPattern: () -> Unit,
    onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    onOpenRecordingLight: () -> Unit,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(
            text = stringResource(R.string.experimental_details_capture_section)
        )
        Column {
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = deviceName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(
                                deviceDetail,
                                stringResource(
                                    if (isCapturing) {
                                        R.string.capture_state_live
                                    } else {
                                        R.string.experimental_home_state_waiting
                                    }
                                )
                            ).joinToString("  •  "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = if (isCapturing) onStopClick else onStartClick,
                        enabled = !startPending,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCapturing && nothingStyleEnabled) {
                                NothingRed
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (isCapturing && nothingStyleEnabled) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                    ) {
                        if (startPending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (isCapturing) R.string.button_stop else R.string.button_no_capture
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
            if (mediaProjectionEnabled) {
                SettingsDividerGap()
                SettingsEntry(
                    title = stringResource(R.string.button_media_projection),
                    description = stringResource(R.string.button_media_projection_hint),
                    onClick = onStartProjectionClick,
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Middle
                )
            }
            SettingsDividerGap()
            SettingsEntry(
                title = stringResource(R.string.glyph_pattern),
                description = patternLabel,
                onClick = onOpenPattern,
                nothingStyle = nothingStyleEnabled,
                position = if (
                    showFillOtherGlyphLights ||
                    supportsRecordingLightBehavior
                ) {
                    SettingsGroupPosition.Middle
                } else {
                    SettingsGroupPosition.Bottom
                }
            )
            if (showFillOtherGlyphLights) {
                SettingsDividerGap()
                SettingsToggleEntry(
                    title = stringResource(R.string.fill_other_glyph_lights_title),
                    description = stringResource(R.string.fill_other_glyph_lights_desc),
                    checked = fillOtherGlyphLights,
                    onCheckedChange = onFillOtherGlyphLightsChanged,
                    nothingStyle = nothingStyleEnabled,
                    position = if (supportsRecordingLightBehavior) {
                        SettingsGroupPosition.Middle
                    } else {
                        SettingsGroupPosition.Bottom
                    }
                )
            }
            if (supportsRecordingLightBehavior) {
                SettingsDividerGap()
                SettingsEntry(
                    title = stringResource(R.string.recording_light_behavior_title),
                    description = recordingLightBehaviorLabel,
                    onClick = onOpenRecordingLight,
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Bottom
                )
            }
        }
        if (showPhone1GlyphDebugControls) {
            Phone1GlyphDebugControls(
                startPending = startPending,
                onEnableClick = onEnablePhone1GlyphDebugClick,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }

    if (showStatusPanel) {
        CollapsibleVisualizerStatusPanel(
            statusText = statusText,
            logMessage = logMessage,
            nothingStyleEnabled = nothingStyleEnabled
        )
    }

    if (meterVisibleEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ExperimentalDetailsSectionTitle(
                text = stringResource(R.string.experimental_details_live_meter_section)
            )
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Single
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    ExperimentalMeterPreview(
                        glyphMode = glyphMode,
                        deviceProfile = deviceProfile,
                        binaryMode = binaryMode,
                        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                        meterVisibleEnabled = meterVisibleEnabled,
                        lightweightMeterEnabled = lightweightMeterEnabled,
                        spectrumMeterEnabled = spectrumMeterEnabled,
                        nativeMeterViewEnabled = nativeMeterViewEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        reverseDirection = reverseDirection,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperimentalDetailsTuneTab(
    sensitivity: Float,
    noiseGate: Float,
    dynamics: Float,
    outputGamma: Float,
    toneFocus: Float,
    smoothing: Float,
    autoScaleWindowSeconds: Float,
    autoScaleOffset: Float,
    patternLabel: String,
    patternId: String,
    patternKind: GlyphPatternKind?,
    deviceProfile: GlyphDeviceProfile,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    experimentalAdaptiveAutoScaleEnabled: Boolean,
    visualDynamics: Float,
    visualDynamicsOverridden: Boolean,
    onVisualDynamicsChanged: (Float) -> Unit,
    onVisualDynamicsChangeFinished: () -> Unit,
    onVisualDynamicsReset: () -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onNoiseGateChanged: (Float) -> Unit,
    onDynamicsChanged: (Float) -> Unit,
    onOutputGammaChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onToneFocusChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChanged: (Float) -> Unit,
    onAutoScaleWindowSecondsChangeFinished: () -> Unit,
    onAutoScaleOffsetChanged: (Float) -> Unit,
    onAutoScaleOffsetChangeFinished: () -> Unit,
    onResetClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    nothingStyleEnabled: Boolean
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    val visualDynamicsAvailable = experimentalAdaptiveAutoScaleEnabled &&
        GlyphPatternRegistry.isSupported(deviceProfile, patternId) &&
        supportsGlyphVisualDynamics(patternKind) &&
        when (patternKind) {
            GlyphPatternKind.SPECTRUM -> spectrumAutoScale
            GlyphPatternKind.ALL_BRIGHTNESS -> allBrightnessAutoScale
            GlyphPatternKind.LINEAR,
            GlyphPatternKind.CENTER,
            GlyphPatternKind.MATRIX_BAR,
            GlyphPatternKind.MATRIX_FIELD,
            GlyphPatternKind.MATRIX_CIRCLE -> levelAutoScale
            else -> false
        }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(text = stringResource(R.string.meter_parameters))
        Column {
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Top
            ) {
                Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                    ParameterSlider(
                        title = stringResource(R.string.param_sensitivity_title),
                        valueText = stringResource(R.string.percent_value, (sensitivity * 100).toInt()),
                        description = stringResource(R.string.param_sensitivity_desc),
                        value = sensitivity,
                        onValueChange = onSensitivityChanged,
                        valueRange = 0.6f..3.0f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                }
            }
            SettingsDividerGap()
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Middle
            ) {
                Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                    ParameterSlider(
                        title = stringResource(R.string.param_response_speed_title),
                        valueText = responseSpeedValueText(smoothing),
                        description = stringResource(R.string.param_response_speed_desc),
                        value = smoothing,
                        onValueChange = onSmoothingChanged,
                        valueRange = 0.08f..0.55f,
                        nothingStyleEnabled = nothingStyleEnabled
                    )
                }
            }
            SettingsDividerGap()
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Bottom
            ) {
                Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
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
                }
            }
        }
    }

    if (visualDynamicsAvailable) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExperimentalDetailsSectionTitle(text = stringResource(R.string.visual_dynamics_title))
            Text(
                text = stringResource(
                    R.string.visual_dynamics_profile,
                    deviceProfile.name
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.visual_dynamics_pattern,
                    patternLabel,
                    patternId
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsItemSurface(
                nothingStyle = nothingStyleEnabled,
                position = SettingsGroupPosition.Single
            ) {
                Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ParameterSlider(
                            title = stringResource(R.string.visual_dynamics_title),
                            valueText = stringResource(
                                R.string.visual_dynamics_value,
                                (visualDynamics * 100f).toInt()
                            ),
                            description = stringResource(R.string.visual_dynamics_desc),
                            value = visualDynamics,
                            onValueChange = onVisualDynamicsChanged,
                            onValueChangeFinished = onVisualDynamicsChangeFinished,
                            valueRange = 0f..1f,
                            nothingStyleEnabled = nothingStyleEnabled
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.visual_dynamics_natural),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.visual_dynamics_extreme),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onVisualDynamicsReset,
                            enabled = visualDynamicsOverridden,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.visual_dynamics_reset))
                        }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(text = stringResource(R.string.advanced_meter_title))
        SettingsItemSurface(
            nothingStyle = nothingStyleEnabled,
            position = SettingsGroupPosition.Single,
            onClick = { advancedExpanded = !advancedExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.advanced_meter_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.advanced_meter_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (advancedExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null
                )
            }
        }

        AnimatedVisibility(
            visible = advancedExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                ExperimentalParameterSettingsCard(
                    position = SettingsGroupPosition.Top,
                    nothingStyleEnabled = nothingStyleEnabled,
                    title = stringResource(R.string.param_noise_gate_title),
                    valueText = stringResource(R.string.percent_value, (noiseGate * 100).toInt()),
                    description = stringResource(R.string.param_noise_gate_desc),
                    value = noiseGate,
                    onValueChange = onNoiseGateChanged,
                    valueRange = 0f..0.35f
                )
                SettingsDividerGap()
                ExperimentalParameterSettingsCard(
                    position = SettingsGroupPosition.Middle,
                    nothingStyleEnabled = nothingStyleEnabled,
                    title = stringResource(R.string.param_dynamics_title),
                    valueText = stringResource(R.string.param_dynamics_value, dynamics),
                    description = stringResource(R.string.param_dynamics_desc),
                    value = dynamics,
                    onValueChange = onDynamicsChanged,
                    valueRange = 0.6f..2.2f
                )
                SettingsDividerGap()
                ExperimentalParameterSettingsCard(
                    position = SettingsGroupPosition.Middle,
                    nothingStyleEnabled = nothingStyleEnabled,
                    title = stringResource(R.string.param_output_gamma_title),
                    valueText = stringResource(R.string.param_dynamics_value, outputGamma),
                    description = stringResource(R.string.param_output_gamma_desc),
                    value = outputGamma,
                    onValueChange = onOutputGammaChanged,
                    valueRange = 0.6f..2.6f
                )
                SettingsDividerGap()
                ExperimentalParameterSettingsCard(
                    position = SettingsGroupPosition.Middle,
                    nothingStyleEnabled = nothingStyleEnabled,
                    title = stringResource(R.string.param_auto_scale_window_title),
                    valueText = stringResource(
                        R.string.param_auto_scale_window_value,
                        autoScaleWindowSeconds
                    ),
                    description = stringResource(R.string.param_auto_scale_window_desc),
                    value = autoScaleWindowSeconds,
                    onValueChange = onAutoScaleWindowSecondsChanged,
                    onValueChangeFinished = onAutoScaleWindowSecondsChangeFinished,
                    valueRange = 5f..60f
                )
                SettingsDividerGap()
                ExperimentalParameterSettingsCard(
                    position = SettingsGroupPosition.Bottom,
                    nothingStyleEnabled = nothingStyleEnabled,
                    title = stringResource(R.string.param_auto_scale_offset_title),
                    valueText = stringResource(
                        R.string.percent_value,
                        (autoScaleOffset * 100).toInt()
                    ),
                    description = stringResource(R.string.param_auto_scale_offset_desc),
                    value = autoScaleOffset,
                    onValueChange = onAutoScaleOffsetChanged,
                    onValueChangeFinished = onAutoScaleOffsetChangeFinished,
                    valueRange = 0f..0.4f
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(
            text = stringResource(R.string.experimental_details_data_section)
        )
        SettingsItemSurface(
            nothingStyle = nothingStyleEnabled,
            position = SettingsGroupPosition.Single
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onResetClick
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_reset_button),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onImportClick
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_import_button),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onExportClick
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_export_button),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperimentalParameterSettingsCard(
    position: SettingsGroupPosition,
    nothingStyleEnabled: Boolean,
    title: String,
    valueText: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>
) {
    SettingsItemSurface(
        nothingStyle = nothingStyleEnabled,
        position = position
    ) {
        Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            ParameterSlider(
                title = title,
                valueText = valueText,
                description = description,
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

private data class ExperimentalDetailsToggleItem(
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
private fun ExperimentalDetailsSystemTab(
    latencyMs: Float,
    reverseDirection: Boolean,
    binaryMode: Boolean,
    matrixSmoothMotionEnabled: Boolean,
    oscilloscopeAutoTimeAxisEnabled: Boolean,
    levelAutoScale: Boolean,
    spectrumAutoScale: Boolean,
    allBrightnessAutoScale: Boolean,
    experimentalAdaptiveAutoScaleEnabled: Boolean,
    glyphMode: String,
    isMatrixDevice: Boolean,
    glyphRenderMode: GlyphPatternRenderMode?,
    nothingStyleEnabled: Boolean,
    onOpenLatency: () -> Unit,
    onReverseDirectionChanged: (Boolean) -> Unit,
    onBinaryModeChanged: (Boolean) -> Unit,
    onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    onLevelAutoScaleChanged: (Boolean) -> Unit,
    onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    onExperimentalAdaptiveAutoScaleEnabledChanged: (Boolean) -> Unit
) {
    var oscilloscopeTimeAxisMultiplier by remember { mutableStateOf(1f) }
    LaunchedEffect(glyphRenderMode, oscilloscopeAutoTimeAxisEnabled) {
        if (
            glyphRenderMode != GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE ||
            !oscilloscopeAutoTimeAxisEnabled
        ) {
            oscilloscopeTimeAxisMultiplier = 1f
            return@LaunchedEffect
        }
        while (true) {
            oscilloscopeTimeAxisMultiplier = WaveformSampler.currentAutoTimeAxisMultiplier()
            delay(100L)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(
            text = stringResource(R.string.experimental_details_system_section)
        )
        SettingsEntry(
            title = stringResource(R.string.latency_title),
            description = stringResource(R.string.latency_value_ms, latencyMs),
            onClick = onOpenLatency,
            nothingStyle = nothingStyleEnabled,
            position = SettingsGroupPosition.Single
        )
    }

    val toggles = buildList {
        add(
            ExperimentalDetailsToggleItem(
                title = stringResource(R.string.glyph_direction_title),
                description = stringResource(R.string.glyph_direction_desc),
                checked = reverseDirection,
                onCheckedChange = onReverseDirectionChanged
            )
        )
        add(
            ExperimentalDetailsToggleItem(
                title = stringResource(R.string.binary_mode_title),
                description = stringResource(R.string.binary_mode_desc),
                checked = binaryMode,
                onCheckedChange = onBinaryModeChanged
            )
        )
        if (isMatrixDevice) {
            add(
                ExperimentalDetailsToggleItem(
                    title = stringResource(R.string.matrix_smooth_motion_title),
                    description = stringResource(R.string.matrix_smooth_motion_desc),
                    checked = matrixSmoothMotionEnabled,
                    onCheckedChange = onMatrixSmoothMotionEnabledChanged
                )
            )
        }
        if (glyphRenderMode == GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE) {
            add(
                ExperimentalDetailsToggleItem(
                    title = if (oscilloscopeAutoTimeAxisEnabled) {
                        stringResource(
                            R.string.oscilloscope_auto_time_axis_title_with_value,
                            oscilloscopeTimeAxisMultiplier - 1f
                        )
                    } else {
                        stringResource(R.string.oscilloscope_auto_time_axis_title)
                    },
                    description = stringResource(R.string.oscilloscope_auto_time_axis_desc),
                    checked = oscilloscopeAutoTimeAxisEnabled,
                    onCheckedChange = onOscilloscopeAutoTimeAxisEnabledChanged
                )
            )
        }
        when {
            GlyphPatternRegistry.isSpectrum(glyphMode) -> add(
                ExperimentalDetailsToggleItem(
                    title = stringResource(R.string.spectrum_auto_scale_title),
                    description = stringResource(R.string.spectrum_auto_scale_desc),
                    checked = spectrumAutoScale,
                    onCheckedChange = onSpectrumAutoScaleChanged
                )
            )
            GlyphPatternRegistry.isAllBrightness(glyphMode) -> add(
                ExperimentalDetailsToggleItem(
                    title = stringResource(R.string.all_brightness_auto_scale_title),
                    description = stringResource(R.string.all_brightness_auto_scale_desc),
                    checked = allBrightnessAutoScale,
                    onCheckedChange = onAllBrightnessAutoScaleChanged
                )
            )
            else -> add(
                ExperimentalDetailsToggleItem(
                    title = stringResource(R.string.level_auto_scale_title),
                    description = stringResource(R.string.level_auto_scale_desc),
                    checked = levelAutoScale,
                    onCheckedChange = onLevelAutoScaleChanged
                )
            )
        }
        add(
            ExperimentalDetailsToggleItem(
                title = stringResource(R.string.adaptive_auto_scale_title),
                description = stringResource(R.string.adaptive_auto_scale_desc),
                checked = experimentalAdaptiveAutoScaleEnabled,
                onCheckedChange = onExperimentalAdaptiveAutoScaleEnabledChanged
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ExperimentalDetailsSectionTitle(
            text = stringResource(R.string.experimental_details_glyph_section)
        )
        Column {
            toggles.forEachIndexed { index, item ->
                val position = when {
                    toggles.size == 1 -> SettingsGroupPosition.Single
                    index == 0 -> SettingsGroupPosition.Top
                    index == toggles.lastIndex -> SettingsGroupPosition.Bottom
                    else -> SettingsGroupPosition.Middle
                }
                SettingsToggleEntry(
                    title = item.title,
                    description = item.description,
                    checked = item.checked,
                    onCheckedChange = item.onCheckedChange,
                    nothingStyle = nothingStyleEnabled,
                    position = position
                )
                if (index != toggles.lastIndex) {
                    SettingsDividerGap()
                }
            }
        }
    }
}

@Composable
private fun ExperimentalFillOtherGlyphLightsDialog(
    selected: Boolean,
    onDismiss: () -> Unit,
    onSelected: (Boolean) -> Unit
) {
    val options = listOf(
        Triple(
            false,
            stringResource(R.string.fill_other_glyph_lights_disabled),
            stringResource(R.string.fill_other_glyph_lights_disabled_desc)
        ),
        Triple(
            true,
            stringResource(R.string.fill_other_glyph_lights_enabled),
            stringResource(R.string.fill_other_glyph_lights_desc)
        )
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fill_other_glyph_lights_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (enabled, label, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelected(enabled) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selected == enabled,
                            onClick = { onSelected(enabled) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
internal fun ExperimentalRecordingLightDialog(
    selected: RecordingLightBehavior,
    onDismiss: () -> Unit,
    onSelected: (RecordingLightBehavior) -> Unit
) {
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
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recording_light_behavior_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (behavior, label, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelected(behavior) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selected == behavior,
                            onClick = { onSelected(behavior) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
