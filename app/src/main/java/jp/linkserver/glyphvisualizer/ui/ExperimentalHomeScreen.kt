package jp.linkserver.glyphvisualizer.ui

import jp.linkserver.glyphvisualizer.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jp.linkserver.glyphvisualizer.ui.SettingsEntry
import jp.linkserver.glyphvisualizer.ui.SettingsGroupPosition
import jp.linkserver.glyphvisualizer.ui.SettingsToggleEntry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingDotFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalMainScreenContent(
    heroTitle: String,
    statusText: String,
    logMessage: String?,
    isCapturing: Boolean,
    startPending: Boolean,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    fillOtherGlyphLights: Boolean,
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    meterVisibleEnabled: Boolean,
    lightweightMeterEnabled: Boolean,
    spectrumMeterEnabled: Boolean,
    nativeMeterViewEnabled: Boolean,
    nothingStyleEnabled: Boolean,
    showPhone1GlyphDebugControls: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onGlyphModeChanged: (String) -> Unit,
    onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    onRecordingLightBehaviorChanged: (RecordingLightBehavior) -> Unit,
    onEnablePhone1GlyphDebugClick: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val displayFont = if (nothingStyleEnabled) NTypeFontFamily else FontFamily.SansSerif
    val deviceName = remember(heroTitle) {
        heroTitle.lineSequence().firstOrNull { it.isNotBlank() } ?: heroTitle
    }
    val patternDefinition = GlyphPatternRegistry.definition(glyphMode)
    val patternLabel = patternDefinition?.let { stringResource(it.labelRes) } ?: glyphMode
    var showPatternSheet by rememberSaveable { mutableStateOf(false) }
    var showDevicePatternSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showRecordingLightDialog by rememberSaveable { mutableStateOf(false) }
    val supportsFillOtherGlyphLights = deviceProfile in setOf(
        GlyphDeviceProfile.PHONE1,
        GlyphDeviceProfile.PHONE2,
        GlyphDeviceProfile.PHONE2A
    )
    val supportsRecordingLightBehavior = deviceProfile.supportsRecordingLightBehavior()
    val hasDevicePatternSettings = supportsFillOtherGlyphLights ||
        supportsRecordingLightBehavior ||
        showPhone1GlyphDebugControls
    val recordingLightBehavior = resolveRecordingLightBehavior(
        baseIndicatorEnabled = baseIndicatorEnabled,
        recordingLightIncluded = recordingLightIncluded
    )
    val recordingLightBehaviorLabel = when (recordingLightBehavior) {
        RecordingLightBehavior.NONE -> stringResource(R.string.recording_light_behavior_none)
        RecordingLightBehavior.INCLUDED_IN_METER ->
            stringResource(R.string.recording_light_behavior_meter)
        RecordingLightBehavior.BASS_INDICATOR ->
            stringResource(R.string.recording_light_behavior_bass)
    }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val darkTheme = isSystemInDarkTheme()
    val bottomSheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val bottomSheetItemColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (darkTheme) 0.10f else 0.06f
    )
    val patternSettingsSheetContainerColor = if (darkTheme) {
        bottomSheetContainerColor
    } else {
        bottomSheetItemColor.compositeOver(bottomSheetContainerColor)
    }
    val patternSettingsSheetItemColor = if (darkTheme) {
        bottomSheetItemColor
    } else {
        bottomSheetContainerColor
    }
    val dividerColor = if (darkTheme) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    }
    val startButtonContainerColor by animateColorAsState(
        targetValue = if (isCapturing && nothingStyleEnabled) {
            NothingRed
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 180),
        label = "experimental_home_start_button_container"
    )
    val startButtonContentColor by animateColorAsState(
        targetValue = if (isCapturing && nothingStyleEnabled) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 180),
        label = "experimental_home_start_button_content"
    )

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

    if (showDevicePatternSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDevicePatternSettingsSheet = false },
            containerColor = patternSettingsSheetContainerColor,
            contentColor = contentColor,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.experimental_home_pattern_settings),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = contentColor,
                    fontFamily = displayFont,
                    fontSize = 22.sp
                )
                if (supportsFillOtherGlyphLights) {
                    SettingsToggleEntry(
                        title = stringResource(R.string.fill_other_glyph_lights_title),
                        description = stringResource(R.string.fill_other_glyph_lights_desc),
                        checked = fillOtherGlyphLights,
                        onCheckedChange = onFillOtherGlyphLightsChanged,
                        nothingStyle = nothingStyleEnabled,
                        position = SettingsGroupPosition.Single,
                        containerColor = patternSettingsSheetItemColor
                    )
                }
                if (supportsRecordingLightBehavior) {
                    SettingsEntry(
                        title = stringResource(R.string.recording_light_behavior_title),
                        description = recordingLightBehaviorLabel,
                        onClick = {
                            showDevicePatternSettingsSheet = false
                            showRecordingLightDialog = true
                        },
                        nothingStyle = nothingStyleEnabled,
                        position = SettingsGroupPosition.Single,
                        containerColor = patternSettingsSheetItemColor
                    )
                }
                if (showPhone1GlyphDebugControls) {
                    Phone1GlyphDebugControls(
                        startPending = startPending,
                        onEnableClick = onEnablePhone1GlyphDebugClick,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showRecordingLightDialog) {
        ExperimentalRecordingLightDialog(
            selected = recordingLightBehavior,
            onDismiss = { showRecordingLightDialog = false },
            onSelected = { behavior ->
                showRecordingLightDialog = false
                onRecordingLightBehaviorChanged(behavior)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.align(Alignment.Center),
                    color = contentColor,
                    fontFamily = displayFont,
                    fontSize = 25.sp
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = contentColor,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(28.dp),
                color = containerColor,
                border = BorderStroke(1.dp, dividerColor)
            ) {
                Text(
                    text = "$deviceName  •  ${stringResource(
                        if (isCapturing) R.string.capture_state_live else R.string.experimental_home_state_waiting
                    )}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                    color = contentColor,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
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

            ExperimentalHomeLogPanel(
                statusText = statusText,
                logMessage = logMessage
            )

            HorizontalDivider(color = dividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .fillMaxHeight()
                        .clickable(enabled = !startPending) {
                            if (isCapturing) onStopClick() else onStartClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(startButtonContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            startPending -> CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = startButtonContentColor
                            )
                            isCapturing -> Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.button_stop),
                                tint = startButtonContentColor,
                                modifier = Modifier.size(26.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.button_no_capture),
                                tint = startButtonContentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(dividerColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { showPatternSheet = true }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = patternLabel,
                            modifier = Modifier.weight(1f),
                            color = contentColor,
                            fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    HorizontalDivider(color = dividerColor)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (hasDevicePatternSettings) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showDevicePatternSettingsSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = stringResource(
                                        R.string.experimental_home_pattern_settings
                                    ),
                                    tint = contentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(dividerColor)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(onClick = onOpenDetails)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.experimental_home_details),
                                modifier = Modifier.weight(1f),
                                color = contentColor,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                textAlign = TextAlign.End
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExperimentalMeterPreview(
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
    nothingStyleEnabled: Boolean
) {
    if (!meterVisibleEnabled) return

    val liveFrame = CaptureUiStore.liveFrame
    val meterModel = if (lightweightMeterEnabled || spectrumMeterEnabled || nativeMeterViewEnabled) {
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

    when {
        spectrumMeterEnabled && nativeMeterViewEnabled -> DirectNativeSpectrumMeterCanvas(
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            recordingLightIncluded = recordingLightIncluded,
            nothingStyleEnabled = nothingStyleEnabled
        )

        spectrumMeterEnabled -> SpectrumMeterCanvas(
            level = liveFrame.level,
            spectrumBands = liveFrame.spectrumBands,
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            recordingLightIncluded = recordingLightIncluded,
            nothingStyleEnabled = nothingStyleEnabled
        )

        lightweightMeterEnabled && nativeMeterViewEnabled -> DirectNativeMeterCanvas(
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            nothingStyleEnabled = nothingStyleEnabled
        )

        lightweightMeterEnabled -> LightweightMeterCanvas(
            level = liveFrame.level,
            nothingStyleEnabled = nothingStyleEnabled
        )

        nativeMeterViewEnabled -> DirectNativeDetailedMeterCanvas(
            glyphMode = glyphMode,
            deviceProfile = deviceProfile,
            binaryMode = binaryMode,
            glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
            recordingLightIncluded = recordingLightIncluded,
            reverseDirection = reverseDirection,
            nothingStyleEnabled = nothingStyleEnabled
        )

        else -> meterModel?.let {
            MeterCanvas(
                level = liveFrame.level,
                peak = liveFrame.peak,
                meterModel = it,
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
private fun ExperimentalHomeLogPanel(
    statusText: String,
    logMessage: String?
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayedMessage = when {
        !logMessage.isNullOrBlank() && logMessage != statusText && statusText.isNotBlank() ->
            "$statusText  •  $logMessage"
        !logMessage.isNullOrBlank() -> logMessage
        else -> statusText
    }
    val messageColor = if (!logMessage.isNullOrBlank() && logMessage != statusText) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(40.dp)
    ) {
        val arrowOffset by animateDpAsState(
            targetValue = if (expanded) {
                (maxWidth - 44.dp).coerceAtLeast(4.dp)
            } else {
                4.dp
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "experimental_home_log_arrow_offset"
        )

        AnimatedVisibility(
            visible = expanded && displayedMessage.isNotBlank(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, end = 48.dp),
            enter = fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 100))
        ) {
            Text(
                text = displayedMessage,
                color = messageColor,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = arrowOffset)
                .size(40.dp)
                .clip(CircleShape)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.ChevronLeft
                } else {
                    Icons.Default.ChevronRight
                },
                contentDescription = if (expanded) {
                    stringResource(R.string.cd_collapse)
                } else {
                    stringResource(R.string.cd_expand)
                },
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun ExperimentalDetailsSummary(
    heroTitle: String,
    isCapturing: Boolean,
    glyphMode: String,
    latencyMs: Float,
    onOpenLatency: (() -> Unit)?,
    displayFont: FontFamily,
    nothingStyleEnabled: Boolean
) {
    val deviceName = remember(heroTitle) {
        heroTitle.lineSequence().firstOrNull { it.isNotBlank() } ?: heroTitle
    }
    val deviceDetail = remember(heroTitle) {
        heroTitle.lineSequence().drop(1).firstOrNull { it.isNotBlank() }
    }
    val patternDefinition = GlyphPatternRegistry.definition(glyphMode)
    val patternLabel = patternDefinition?.let { stringResource(it.labelRes) } ?: glyphMode
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCapturing) {
                            if (nothingStyleEnabled) NothingRed else MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = deviceName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 22.sp
                )
                Text(
                    text = listOfNotNull(deviceDetail, patternLabel).joinToString("  •  "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, dividerColor)
            ) {
                Text(
                    text = stringResource(
                        if (isCapturing) R.string.capture_state_live else R.string.experimental_home_state_waiting
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp
                )
            }
        }
        HorizontalDivider(color = dividerColor)
        if (onOpenLatency != null) {
            Surface(
                onClick = onOpenLatency,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.latency_title),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.latency_value_ms, latencyMs),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = if (nothingStyleEnabled) {
                            NothingDotFontFamily
                        } else {
                            FontFamily.SansSerif
                        },
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            HorizontalDivider(color = dividerColor)
        }
    }
}
