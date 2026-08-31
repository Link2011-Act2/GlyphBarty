package jp.linkserver.glyphvisualizer

import android.content.Context
import android.content.Intent

data class CaptureConfig(
    val sensitivity: Float,
    val noiseGate: Float,
    val dynamics: Float,
    val outputGamma: Float,
    val toneFocus: Float,
    val smoothing: Float,
    val smoothingBalance: Float,
    val reverseDirection: Boolean,
    val peakHoldEnabled: Boolean,
    val glyphMode: String,
    val fillOtherGlyphLights: Boolean,
    val phone1ClassicCSplitEnabled: Boolean,
    val binaryMode: Boolean,
    val baseIndicatorEnabled: Boolean,
    val recordingLightIncluded: Boolean,
    val levelAutoScale: Boolean,
    val spectrumAutoScale: Boolean,
    val allBrightnessAutoScale: Boolean,
    val autoScaleWindowSeconds: Float,
    val autoScaleOffset: Float,
    val latencyMs: Float,
    val mediaPlaybackOnlyEnabled: Boolean,
    val experimentalVisualizerStabilizationEnabled: Boolean,
    val experimentalVisualizerSignalWatchdogEnabled: Boolean,
    val experimentalSpectrumDecayEnabled: Boolean,
    val experimentalPerformanceOptimizationsEnabled: Boolean,
    val matrixSmoothMotionEnabled: Boolean,
    val oscilloscopeAutoTimeAxisEnabled: Boolean,
    val turnOffWhenBackDown: Boolean
)

fun CaptureUiState.toCaptureConfig(): CaptureConfig = CaptureConfig(
    sensitivity = sensitivity,
    noiseGate = noiseGate,
    dynamics = dynamics,
    outputGamma = outputGamma,
    toneFocus = toneFocus,
    smoothing = smoothing,
    smoothingBalance = smoothingBalance,
    reverseDirection = reverseDirection,
    peakHoldEnabled = peakHoldEnabled,
    glyphMode = glyphMode,
    fillOtherGlyphLights = fillOtherGlyphLights,
    phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
    binaryMode = binaryMode,
    baseIndicatorEnabled = baseIndicatorEnabled,
    recordingLightIncluded = recordingLightIncluded,
    levelAutoScale = levelAutoScale,
    spectrumAutoScale = spectrumAutoScale,
    allBrightnessAutoScale = allBrightnessAutoScale,
    autoScaleWindowSeconds = autoScaleWindowSeconds,
    autoScaleOffset = autoScaleOffset,
    latencyMs = latencyMs,
    mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
    experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
    experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
    experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
    experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
    matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
    oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
    turnOffWhenBackDown = turnOffWhenBackDown
)

internal fun CaptureConfig.applyToUiState(state: CaptureUiState): CaptureUiState = state.copy(
    sensitivity = sensitivity,
    noiseGate = noiseGate,
    dynamics = dynamics,
    outputGamma = outputGamma,
    toneFocus = toneFocus,
    smoothing = smoothing,
    smoothingBalance = smoothingBalance,
    reverseDirection = reverseDirection,
    peakHoldEnabled = peakHoldEnabled,
    glyphMode = glyphMode,
    fillOtherGlyphLights = fillOtherGlyphLights,
    phone1ClassicCSplitEnabled = phone1ClassicCSplitEnabled,
    binaryMode = binaryMode,
    baseIndicatorEnabled = baseIndicatorEnabled,
    recordingLightIncluded = recordingLightIncluded,
    levelAutoScale = levelAutoScale,
    spectrumAutoScale = spectrumAutoScale,
    allBrightnessAutoScale = allBrightnessAutoScale,
    autoScaleWindowSeconds = autoScaleWindowSeconds,
    autoScaleOffset = autoScaleOffset,
    latencyMs = latencyMs,
    mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
    experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
    experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
    experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
    experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
    matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
    oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
    turnOffWhenBackDown = turnOffWhenBackDown
)

sealed interface CaptureCommand {
    data class StartVisualizer(
        val config: CaptureConfig,
        val source: VisualizerStartSource = VisualizerStartSource.APP
    ) : CaptureCommand

    data class StartMediaProjection(
        val resultCode: Int,
        val data: Intent?,
        val config: CaptureConfig
    ) : CaptureCommand

    data class UpdateConfig(
        val config: CaptureConfig,
        val encodedOutputGamma: Float = config.outputGamma,
        val encodedRecordingLightIncluded: Boolean? = config.recordingLightIncluded
    ) : CaptureCommand

    data object Stop : CaptureCommand
}

object CaptureIntentCommandCodec {
    const val ACTION_START_VISUALIZER =
        "jp.linkserver.glyphvisualizer.action.START_VISUALIZER"
    const val ACTION_START_MEDIA_PROJECTION =
        "jp.linkserver.glyphvisualizer.action.START_MEDIA_PROJECTION"
    const val ACTION_STOP = "jp.linkserver.glyphvisualizer.action.STOP"
    const val ACTION_UPDATE_CONFIG =
        "jp.linkserver.glyphvisualizer.action.UPDATE_SENSITIVITY"

    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_SENSITIVITY = "extra_sensitivity"
    const val EXTRA_NOISE_GATE = "extra_noise_gate"
    const val EXTRA_DYNAMICS = "extra_dynamics"
    const val EXTRA_OUTPUT_GAMMA = "extra_output_gamma"
    const val EXTRA_TONE_FOCUS = "extra_tone_focus"
    const val EXTRA_SMOOTHING = "extra_smoothing"
    const val EXTRA_SMOOTHING_BALANCE = "extra_smoothing_balance"
    const val EXTRA_REVERSE_DIRECTION = "extra_reverse_direction"
    const val EXTRA_PEAK_HOLD_ENABLED = "extra_peak_hold_enabled"
    const val EXTRA_GLYPH_MODE = "extra_glyph_mode"
    const val EXTRA_FILL_OTHER_GLYPH_LIGHTS = "extra_fill_other_glyph_lights"
    const val EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED =
        "extra_phone1_classic_c_split_enabled"
    const val EXTRA_BINARY_MODE = "extra_binary_mode"
    const val EXTRA_BASE_INDICATOR_ENABLED = "extra_base_indicator_enabled"
    const val EXTRA_RECORDING_LIGHT_INCLUDED = "extra_recording_light_included"
    const val EXTRA_LEVEL_AUTO_SCALE = "extra_level_auto_scale"
    const val EXTRA_SPECTRUM_AUTO_SCALE = "extra_spectrum_auto_scale"
    const val EXTRA_ALL_BRIGHTNESS_AUTO_SCALE = "extra_all_brightness_auto_scale"
    const val EXTRA_AUTO_SCALE_WINDOW_SECONDS = "extra_auto_scale_window_seconds"
    const val EXTRA_AUTO_SCALE_OFFSET = "extra_auto_scale_offset"
    const val EXTRA_LATENCY_MS = "extra_latency_ms"
    const val EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED = "extra_media_playback_only_enabled"
    const val EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED =
        "extra_experimental_visualizer_stabilization_enabled"
    const val EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED =
        "extra_experimental_visualizer_signal_watchdog_enabled"
    const val EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED =
        "extra_experimental_spectrum_decay_enabled"
    const val EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED =
        "extra_experimental_performance_optimizations_enabled"
    const val EXTRA_MATRIX_SMOOTH_MOTION_ENABLED =
        "extra_matrix_smooth_motion_enabled"
    const val EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED =
        "extra_oscilloscope_auto_time_axis_enabled"
    const val EXTRA_TURN_OFF_WHEN_BACK_DOWN = "extra_turn_off_when_back_down"
    const val EXTRA_START_SOURCE = "extra_start_source"

    fun encode(context: Context, command: CaptureCommand): Intent {
        return Intent(context, GlyphVisualizerService::class.java).apply {
            when (command) {
                is CaptureCommand.StartVisualizer -> {
                    action = ACTION_START_VISUALIZER
                    putConfig(command.config)
                    putExtra(EXTRA_START_SOURCE, command.source.name)
                }

                is CaptureCommand.StartMediaProjection -> {
                    action = ACTION_START_MEDIA_PROJECTION
                    putExtra(EXTRA_RESULT_CODE, command.resultCode)
                    putExtra(EXTRA_RESULT_DATA, command.data)
                    putConfig(command.config)
                }

                is CaptureCommand.UpdateConfig -> {
                    action = ACTION_UPDATE_CONFIG
                    putConfig(
                        config = command.config,
                        outputGamma = command.encodedOutputGamma,
                        recordingLightIncluded = command.encodedRecordingLightIncluded
                    )
                }

                CaptureCommand.Stop -> action = ACTION_STOP
            }
        }
    }

    fun decode(intent: Intent?, fallbackConfig: CaptureConfig): CaptureCommand? {
        intent ?: return null
        return when (intent.action) {
            ACTION_START_VISUALIZER -> CaptureCommand.StartVisualizer(
                config = intent.readConfig(fallbackConfig),
                source = intent.getStringExtra(EXTRA_START_SOURCE)
                    ?.let { savedName ->
                        VisualizerStartSource.entries.firstOrNull { it.name == savedName }
                    }
                    ?: VisualizerStartSource.APP
            )

            ACTION_START_MEDIA_PROJECTION -> CaptureCommand.StartMediaProjection(
                resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0),
                data = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java),
                config = intent.readConfig(fallbackConfig)
            )

            ACTION_UPDATE_CONFIG -> CaptureCommand.UpdateConfig(
                config = intent.readConfig(fallbackConfig),
                encodedOutputGamma = intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, Float.NaN),
                encodedRecordingLightIncluded = if (intent.hasExtra(EXTRA_RECORDING_LIGHT_INCLUDED)) {
                    intent.getBooleanExtra(
                        EXTRA_RECORDING_LIGHT_INCLUDED,
                        fallbackConfig.recordingLightIncluded
                    )
                } else {
                    null
                }
            )

            ACTION_STOP -> CaptureCommand.Stop
            else -> null
        }
    }

    private fun Intent.putConfig(
        config: CaptureConfig,
        outputGamma: Float = config.outputGamma,
        recordingLightIncluded: Boolean? = config.recordingLightIncluded
    ) {
        putExtra(EXTRA_SENSITIVITY, config.sensitivity)
        putExtra(EXTRA_NOISE_GATE, config.noiseGate)
        putExtra(EXTRA_DYNAMICS, config.dynamics)
        putExtra(EXTRA_OUTPUT_GAMMA, outputGamma)
        putExtra(EXTRA_TONE_FOCUS, config.toneFocus)
        putExtra(EXTRA_SMOOTHING, config.smoothing)
        putExtra(EXTRA_SMOOTHING_BALANCE, config.smoothingBalance)
        putExtra(EXTRA_REVERSE_DIRECTION, config.reverseDirection)
        putExtra(EXTRA_PEAK_HOLD_ENABLED, config.peakHoldEnabled)
        putExtra(EXTRA_GLYPH_MODE, config.glyphMode)
        putExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, config.fillOtherGlyphLights)
        putExtra(EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED, config.phone1ClassicCSplitEnabled)
        putExtra(EXTRA_BINARY_MODE, config.binaryMode)
        putExtra(EXTRA_BASE_INDICATOR_ENABLED, config.baseIndicatorEnabled)
        recordingLightIncluded?.let { putExtra(EXTRA_RECORDING_LIGHT_INCLUDED, it) }
        putExtra(EXTRA_LEVEL_AUTO_SCALE, config.levelAutoScale)
        putExtra(EXTRA_SPECTRUM_AUTO_SCALE, config.spectrumAutoScale)
        putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, config.allBrightnessAutoScale)
        putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, config.autoScaleWindowSeconds)
        putExtra(EXTRA_AUTO_SCALE_OFFSET, config.autoScaleOffset)
        putExtra(EXTRA_LATENCY_MS, config.latencyMs)
        putExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, config.mediaPlaybackOnlyEnabled)
        putExtra(
            EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
            config.experimentalVisualizerStabilizationEnabled
        )
        putExtra(
            EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
            config.experimentalVisualizerSignalWatchdogEnabled
        )
        putExtra(
            EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
            config.experimentalSpectrumDecayEnabled
        )
        putExtra(
            EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
            config.experimentalPerformanceOptimizationsEnabled
        )
        putExtra(EXTRA_MATRIX_SMOOTH_MOTION_ENABLED, config.matrixSmoothMotionEnabled)
        putExtra(
            EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
            config.oscilloscopeAutoTimeAxisEnabled
        )
        putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, config.turnOffWhenBackDown)
    }

    private fun Intent.readConfig(fallback: CaptureConfig): CaptureConfig {
        val decodedOutputGamma = getFloatExtra(EXTRA_OUTPUT_GAMMA, Float.NaN)
        return CaptureConfig(
            sensitivity = getFloatExtra(EXTRA_SENSITIVITY, fallback.sensitivity),
            noiseGate = getFloatExtra(EXTRA_NOISE_GATE, fallback.noiseGate),
            dynamics = getFloatExtra(EXTRA_DYNAMICS, fallback.dynamics),
            outputGamma = decodedOutputGamma.takeUnless(Float::isNaN) ?: fallback.outputGamma,
            toneFocus = getFloatExtra(EXTRA_TONE_FOCUS, fallback.toneFocus),
            smoothing = getFloatExtra(EXTRA_SMOOTHING, fallback.smoothing),
            smoothingBalance = getFloatExtra(
                EXTRA_SMOOTHING_BALANCE,
                fallback.smoothingBalance
            ),
            reverseDirection = getBooleanExtra(
                EXTRA_REVERSE_DIRECTION,
                fallback.reverseDirection
            ),
            peakHoldEnabled = getBooleanExtra(
                EXTRA_PEAK_HOLD_ENABLED,
                fallback.peakHoldEnabled
            ),
            glyphMode = getStringExtra(EXTRA_GLYPH_MODE) ?: fallback.glyphMode,
            fillOtherGlyphLights = getBooleanExtra(
                EXTRA_FILL_OTHER_GLYPH_LIGHTS,
                fallback.fillOtherGlyphLights
            ),
            phone1ClassicCSplitEnabled = getBooleanExtra(
                EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED,
                fallback.phone1ClassicCSplitEnabled
            ),
            binaryMode = getBooleanExtra(EXTRA_BINARY_MODE, fallback.binaryMode),
            baseIndicatorEnabled = getBooleanExtra(
                EXTRA_BASE_INDICATOR_ENABLED,
                fallback.baseIndicatorEnabled
            ),
            recordingLightIncluded = getBooleanExtra(
                EXTRA_RECORDING_LIGHT_INCLUDED,
                fallback.recordingLightIncluded
            ),
            levelAutoScale = getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, fallback.levelAutoScale),
            spectrumAutoScale = getBooleanExtra(
                EXTRA_SPECTRUM_AUTO_SCALE,
                fallback.spectrumAutoScale
            ),
            allBrightnessAutoScale = getBooleanExtra(
                EXTRA_ALL_BRIGHTNESS_AUTO_SCALE,
                fallback.allBrightnessAutoScale
            ),
            autoScaleWindowSeconds = getFloatExtra(
                EXTRA_AUTO_SCALE_WINDOW_SECONDS,
                fallback.autoScaleWindowSeconds
            ),
            autoScaleOffset = getFloatExtra(EXTRA_AUTO_SCALE_OFFSET, fallback.autoScaleOffset),
            latencyMs = getFloatExtra(EXTRA_LATENCY_MS, fallback.latencyMs),
            mediaPlaybackOnlyEnabled = getBooleanExtra(
                EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED,
                fallback.mediaPlaybackOnlyEnabled
            ),
            experimentalVisualizerStabilizationEnabled = getBooleanExtra(
                EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                fallback.experimentalVisualizerStabilizationEnabled
            ),
            experimentalVisualizerSignalWatchdogEnabled = getBooleanExtra(
                EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                fallback.experimentalVisualizerSignalWatchdogEnabled
            ),
            experimentalSpectrumDecayEnabled = getBooleanExtra(
                EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                fallback.experimentalSpectrumDecayEnabled
            ),
            experimentalPerformanceOptimizationsEnabled = getBooleanExtra(
                EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                fallback.experimentalPerformanceOptimizationsEnabled
            ),
            matrixSmoothMotionEnabled = getBooleanExtra(
                EXTRA_MATRIX_SMOOTH_MOTION_ENABLED,
                fallback.matrixSmoothMotionEnabled
            ),
            oscilloscopeAutoTimeAxisEnabled = getBooleanExtra(
                EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                fallback.oscilloscopeAutoTimeAxisEnabled
            ),
            turnOffWhenBackDown = getBooleanExtra(
                EXTRA_TURN_OFF_WHEN_BACK_DOWN,
                fallback.turnOffWhenBackDown
            )
        )
    }
}

object CaptureCommandGateway {
    private const val TAG = "GlyphVisualizerSvc"

    fun startVisualizer(
        context: Context,
        config: CaptureConfig,
        source: VisualizerStartSource = VisualizerStartSource.APP
    ) {
        val command = CaptureCommand.StartVisualizer(config, source)
        try {
            context.startForegroundService(CaptureIntentCommandCodec.encode(context, command))
        } catch (error: Throwable) {
            AppLogger.e(TAG, "startVisualizer failed to start service", error)
            val msg = context.getString(
                R.string.status_visualizer_service_start_failed,
                error.message ?: context.getString(R.string.status_unknown_error)
            )
            CaptureUiStore.updateRuntime { it.copy(statusText = msg, logMessage = msg) }
        }
    }

    fun startMediaProjection(
        context: Context,
        resultCode: Int,
        data: Intent,
        config: CaptureConfig
    ) {
        val command = CaptureCommand.StartMediaProjection(resultCode, data, config)
        try {
            context.startForegroundService(CaptureIntentCommandCodec.encode(context, command))
        } catch (error: Throwable) {
            AppLogger.e(TAG, "startMediaProjection failed to start service", error)
            val msg = context.getString(
                R.string.status_media_projection_service_start_failed,
                error.message ?: context.getString(R.string.status_unknown_error)
            )
            CaptureUiStore.updateRuntime { it.copy(statusText = msg, logMessage = msg) }
        }
    }

    fun updateConfig(context: Context, config: CaptureConfig) {
        dispatchUpdate(context, CaptureCommand.UpdateConfig(config))
    }

    fun updateConfigPreservingRecordingLight(context: Context, config: CaptureConfig) {
        dispatchUpdate(
            context,
            CaptureCommand.UpdateConfig(
                config = config,
                encodedRecordingLightIncluded = null
            )
        )
    }

    fun updateLegacyConfig(
        context: Context,
        config: CaptureConfig,
        encodedOutputGamma: Float,
        encodedRecordingLightIncluded: Boolean?
    ) {
        dispatchUpdate(
            context,
            CaptureCommand.UpdateConfig(
                config = config,
                encodedOutputGamma = encodedOutputGamma,
                encodedRecordingLightIncluded = encodedRecordingLightIncluded
            )
        )
    }

    private fun dispatchUpdate(context: Context, command: CaptureCommand.UpdateConfig) {
        if (!CaptureUiStore.runtimeState.isCapturing) return
        try {
            context.startService(CaptureIntentCommandCodec.encode(context, command))
        } catch (error: Throwable) {
            AppLogger.w(TAG, "updateSensitivity could not reach service", error)
        }
    }

    fun stop(context: Context) {
        try {
            context.startService(CaptureIntentCommandCodec.encode(context, CaptureCommand.Stop))
        } catch (error: Throwable) {
            AppLogger.w(TAG, "stop could not reach service", error)
        }
    }
}
