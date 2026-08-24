package jp.linkserver.glyphvisualizer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import jp.linkserver.glyphvisualizer.audio.AudioPlaybackVisualizer
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import jp.linkserver.glyphvisualizer.audio.OutputMixVisualizer
import jp.linkserver.glyphvisualizer.audio.WaveformSampler
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRenderMode
import jp.linkserver.glyphvisualizer.glyph.GlyphOutputController
import jp.linkserver.glyphvisualizer.glyph.GlyphLightController
import jp.linkserver.glyphvisualizer.glyph.GlyphMatrixController
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class VisualizerStartSource {
    APP,
    QUICK_SETTINGS
}

class GlyphVisualizerService : Service() {
    companion object {
        private const val TAG = "GlyphVisualizerSvc"
        private const val DEBUG_UI_VISIBILITY_LOGS = false
        private const val CHANNEL_ID = "glyph_visualizer"
        private const val NOTIFICATION_ID = 42
        private const val ALERT_CHANNEL_ID = "glyph_visualizer_alerts"
        private const val ALERT_NOTIFICATION_ID = 43

        private const val ACTION_START_VISUALIZER = "jp.linkserver.glyphvisualizer.action.START_VISUALIZER"
        private const val ACTION_START_MEDIA_PROJECTION = "jp.linkserver.glyphvisualizer.action.START_MEDIA_PROJECTION"
        private const val ACTION_STOP = "jp.linkserver.glyphvisualizer.action.STOP"
        private const val ACTION_UPDATE_SENSITIVITY = "jp.linkserver.glyphvisualizer.action.UPDATE_SENSITIVITY"
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val EXTRA_SENSITIVITY = "extra_sensitivity"
        private const val EXTRA_NOISE_GATE = "extra_noise_gate"
        private const val EXTRA_DYNAMICS = "extra_dynamics"
        private const val EXTRA_OUTPUT_GAMMA = "extra_output_gamma"
        private const val EXTRA_TONE_FOCUS = "extra_tone_focus"
        private const val EXTRA_SMOOTHING = "extra_smoothing"
        private const val EXTRA_SMOOTHING_BALANCE = "extra_smoothing_balance"
        private const val EXTRA_REVERSE_DIRECTION = "extra_reverse_direction"
        private const val EXTRA_PEAK_HOLD_ENABLED = "extra_peak_hold_enabled"
        private const val EXTRA_GLYPH_MODE = "extra_glyph_mode"
        private const val EXTRA_FILL_OTHER_GLYPH_LIGHTS = "extra_fill_other_glyph_lights"
        private const val EXTRA_BINARY_MODE = "extra_binary_mode"
        private const val EXTRA_BASE_INDICATOR_ENABLED = "extra_base_indicator_enabled"
        private const val EXTRA_RECORDING_LIGHT_INCLUDED = "extra_recording_light_included"
        private const val EXTRA_LEVEL_AUTO_SCALE = "extra_level_auto_scale"
        private const val EXTRA_SPECTRUM_AUTO_SCALE = "extra_spectrum_auto_scale"
        private const val EXTRA_ALL_BRIGHTNESS_AUTO_SCALE = "extra_all_brightness_auto_scale"
        private const val EXTRA_AUTO_SCALE_WINDOW_SECONDS = "extra_auto_scale_window_seconds"
        private const val EXTRA_AUTO_SCALE_OFFSET = "extra_auto_scale_offset"
        private const val EXTRA_LATENCY_MS = "extra_latency_ms"
        private const val EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED = "extra_media_playback_only_enabled"
        private const val EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED =
            "extra_experimental_visualizer_stabilization_enabled"
        private const val EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED =
            "extra_experimental_visualizer_signal_watchdog_enabled"
        private const val EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED =
            "extra_experimental_spectrum_decay_enabled"
        private const val EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED =
            "extra_experimental_performance_optimizations_enabled"
        private const val EXTRA_MATRIX_SMOOTH_MOTION_ENABLED =
            "extra_matrix_smooth_motion_enabled"
        private const val EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED =
            "extra_oscilloscope_auto_time_axis_enabled"
        private const val EXTRA_TURN_OFF_WHEN_BACK_DOWN = "extra_turn_off_when_back_down"
        private const val EXTRA_START_SOURCE = "extra_start_source"
        private const val BACK_DOWN_ENABLE_Z_THRESHOLD = 8.5f
        private const val BACK_DOWN_DISABLE_Z_THRESHOLD = 7.5f
        private const val ACTIVE_MODE_VISUALIZER = "VISUALIZER"
        private const val ACTIVE_MODE_MEDIA_PROJECTION = "MEDIA PROJECTION"
        private const val ACTIVE_MODE_IDLE = "IDLE"
        private const val GLYPH_WARMUP_RESYNC_DELAY_MS = 900L
        private const val BACKGROUND_UI_UPDATE_INTERVAL_MS = 250L
        private const val SCREEN_OFF_UI_UPDATE_INTERVAL_MS = 1000L
        private const val MATRIX_UI_SMOOTH_UPDATE_INTERVAL_MS = 12L
        private const val MATRIX_UI_REDUCED_UPDATE_INTERVAL_MS = 33L
        private const val LIGHTWEIGHT_METER_UI_UPDATE_INTERVAL_MS = 100L
        private const val MEDIA_PLAYBACK_CHECK_INTERVAL_MS = 250L
        private const val MEDIA_PLAYBACK_RESUME_CONFIRM_MS = 1_000L
        private const val UI_LEVEL_QUANTIZATION_STEPS = 64f
        private const val UI_PEAK_QUANTIZATION_STEPS = 64f
        private const val UI_SPECTRUM_QUANTIZATION_STEPS = 32f

        fun startVisualizer(
            context: Context,
            sensitivity: Float,
            noiseGate: Float,
            dynamics: Float,
            toneFocus: Float,
            smoothing: Float,
            smoothingBalance: Float,
            reverseDirection: Boolean,
            peakHoldEnabled: Boolean,
            glyphMode: String,
            fillOtherGlyphLights: Boolean,
            binaryMode: Boolean,
            baseIndicatorEnabled: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            autoScaleOffset: Float,
            latencyMs: Float,
            mediaPlaybackOnlyEnabled: Boolean,
            experimentalVisualizerStabilizationEnabled: Boolean,
            experimentalVisualizerSignalWatchdogEnabled: Boolean,
            experimentalSpectrumDecayEnabled: Boolean,
            experimentalPerformanceOptimizationsEnabled: Boolean,
            matrixSmoothMotionEnabled: Boolean,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = 1.8f,
            oscilloscopeAutoTimeAxisEnabled: Boolean = false,
            recordingLightIncluded: Boolean = false,
            startSource: VisualizerStartSource = VisualizerStartSource.APP
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_START_VISUALIZER
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_OUTPUT_GAMMA, outputGamma)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                putExtra(EXTRA_RECORDING_LIGHT_INCLUDED, recordingLightIncluded)
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
                putExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED, experimentalVisualizerStabilizationEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED, experimentalVisualizerSignalWatchdogEnabled)
                putExtra(EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED, experimentalSpectrumDecayEnabled)
                putExtra(EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED, experimentalPerformanceOptimizationsEnabled)
                putExtra(EXTRA_MATRIX_SMOOTH_MOTION_ENABLED, matrixSmoothMotionEnabled)
                putExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, oscilloscopeAutoTimeAxisEnabled)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                putExtra(EXTRA_START_SOURCE, startSource.name)
            }
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                AppLogger.e(TAG, "startVisualizer failed to start service", error)
                val msg = context.getString(
                    R.string.status_visualizer_service_start_failed,
                    error.message ?: context.getString(R.string.status_unknown_error)
                )
                CaptureUiStore.update {
                    it.copy(statusText = msg, logMessage = msg)
                }
            }
        }

        fun startMediaProjection(
            context: Context,
            resultCode: Int,
            data: Intent,
            sensitivity: Float,
            noiseGate: Float,
            dynamics: Float,
            toneFocus: Float,
            smoothing: Float,
            smoothingBalance: Float,
            reverseDirection: Boolean,
            peakHoldEnabled: Boolean,
            glyphMode: String,
            fillOtherGlyphLights: Boolean,
            binaryMode: Boolean,
            baseIndicatorEnabled: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            autoScaleOffset: Float,
            latencyMs: Float,
            mediaPlaybackOnlyEnabled: Boolean,
            experimentalVisualizerStabilizationEnabled: Boolean,
            experimentalVisualizerSignalWatchdogEnabled: Boolean,
            experimentalSpectrumDecayEnabled: Boolean,
            experimentalPerformanceOptimizationsEnabled: Boolean,
            matrixSmoothMotionEnabled: Boolean,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = 1.8f,
            oscilloscopeAutoTimeAxisEnabled: Boolean = false,
            recordingLightIncluded: Boolean = false
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_START_MEDIA_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_OUTPUT_GAMMA, outputGamma)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                putExtra(EXTRA_RECORDING_LIGHT_INCLUDED, recordingLightIncluded)
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
                putExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED, experimentalVisualizerStabilizationEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED, experimentalVisualizerSignalWatchdogEnabled)
                putExtra(EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED, experimentalSpectrumDecayEnabled)
                putExtra(EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED, experimentalPerformanceOptimizationsEnabled)
                putExtra(EXTRA_MATRIX_SMOOTH_MOTION_ENABLED, matrixSmoothMotionEnabled)
                putExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, oscilloscopeAutoTimeAxisEnabled)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
            }
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                AppLogger.e(TAG, "startMediaProjection failed to start service", error)
                val msg = context.getString(
                    R.string.status_media_projection_service_start_failed,
                    error.message ?: context.getString(R.string.status_unknown_error)
                )
                CaptureUiStore.update {
                    it.copy(statusText = msg, logMessage = msg)
                }
            }
        }

        fun updateSensitivity(
            context: Context,
            sensitivity: Float,
            noiseGate: Float,
            dynamics: Float,
            toneFocus: Float,
            smoothing: Float,
            smoothingBalance: Float,
            reverseDirection: Boolean,
            peakHoldEnabled: Boolean,
            glyphMode: String,
            fillOtherGlyphLights: Boolean,
            binaryMode: Boolean,
            baseIndicatorEnabled: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            autoScaleOffset: Float,
            latencyMs: Float,
            mediaPlaybackOnlyEnabled: Boolean,
            experimentalVisualizerStabilizationEnabled: Boolean,
            experimentalVisualizerSignalWatchdogEnabled: Boolean,
            experimentalSpectrumDecayEnabled: Boolean,
            experimentalPerformanceOptimizationsEnabled: Boolean,
            matrixSmoothMotionEnabled: Boolean,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = Float.NaN,
            oscilloscopeAutoTimeAxisEnabled: Boolean = false,
            recordingLightIncluded: Boolean? = null
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_UPDATE_SENSITIVITY
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_OUTPUT_GAMMA, outputGamma)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                recordingLightIncluded?.let {
                    putExtra(EXTRA_RECORDING_LIGHT_INCLUDED, it)
                }
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
                putExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED, experimentalVisualizerStabilizationEnabled)
                putExtra(EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED, experimentalVisualizerSignalWatchdogEnabled)
                putExtra(EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED, experimentalSpectrumDecayEnabled)
                putExtra(EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED, experimentalPerformanceOptimizationsEnabled)
                putExtra(EXTRA_MATRIX_SMOOTH_MOTION_ENABLED, matrixSmoothMotionEnabled)
                putExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, oscilloscopeAutoTimeAxisEnabled)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
            }
            try {
                context.startService(intent)
            } catch (error: Throwable) {
                AppLogger.w(TAG, "updateSensitivity could not reach service", error)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (error: Throwable) {
                AppLogger.w(TAG, "stop could not reach service", error)
            }
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
            @Suppress("DEPRECATION")
            val services = manager.getRunningServices(Int.MAX_VALUE)
            return services.any { it.service.className == GlyphVisualizerService::class.java.name }
        }
    }

    private lateinit var glyphController: GlyphOutputController
    private lateinit var audioPlaybackVisualizer: AudioPlaybackVisualizer
    private lateinit var outputMixVisualizer: OutputMixVisualizer

    private var sensitivity = 1.75f
    private var noiseGate = 0.08f
    private var dynamics = 1.45f
    private var outputGamma = 1.8f
    private var toneFocus = -0.2f
    private var smoothing = 0.55f
    private var smoothingBalance = 0f
    private var reverseDirection = false
    private var peakHoldEnabled = true
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var fillOtherGlyphLights = false
    private var binaryMode = false
    private var baseIndicatorEnabled = false
    private var recordingLightIncluded = false
    private var levelAutoScale = true
    private var spectrumAutoScale = true
    private var allBrightnessAutoScale = true
    private var autoScaleWindowSeconds = 30f
    private var autoScaleOffset = 0f
    private var latencyMs = 0f
    private var mediaPlaybackOnlyEnabled = false
    private var experimentalVisualizerStabilizationEnabled = false
    private var experimentalVisualizerSignalWatchdogEnabled = false
    private var experimentalSpectrumDecayEnabled = false
    private var experimentalPerformanceOptimizationsEnabled = true
    private var matrixSmoothMotionEnabled = false
    private var oscilloscopeAutoTimeAxisEnabled = false
    private var turnOffWhenBackDown = false
    private var isBackDownSuppressed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private var visualizerStartRequestId = 0
    private var visualizerStartActionAtMs = 0L
    private var visualizerStartSource = VisualizerStartSource.APP
    private var audioDeviceCallbackRegistered = false
    private var lastAudioRouteSignature: String? = null
    private var suppressRouteRestartUntilMs = 0L
    private var lastUiPublishAtMs = 0L
    private var publishUiFrameCallCount = 0
    private var lastPublishUiFrameCallLogAtMs = 0L
    private var suppressedUiPublishCount = 0
    private var lastSuppressedUiPublishLogAtMs = 0L
    private var lastMediaPlaybackCheckAtMs = 0L
    private var lastMediaPlaybackActive = false
    private var mediaPlaybackResumeCandidateAtMs = 0L
    private var mediaPlaybackSuppressed = false
    private data class DelayedLevelFrame(
        val dueAtMs: Long,
        val level: Float,
        val peak: Float,
        val mode: String,
        val lowEnergy: Float,
        val highEnergy: Float,
        val leftLevel: Float,
        val rightLevel: Float,
        val spectrumBands: FloatArray,
        val phone4aBaseBandLevel: Float,
        val waveformSamples: FloatArray,
        val leftWaveformSamples: FloatArray,
        val rightWaveformSamples: FloatArray
    )
    private val pendingLevelFrames = ArrayDeque<DelayedLevelFrame>()
    private val latencyDrainRunnable = Runnable { drainPendingLevelFrames() }
    private var latestLevelFrame: DelayedLevelFrame? = null
    private val glyphWarmupResyncRunnable = Runnable {
        if (CaptureUiStore.state.activeMode == ACTIVE_MODE_IDLE) return@Runnable
        applyGlyphControllerSettings()
        latestLevelFrame?.let { renderLevelFrame(it) }
    }
    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var gravitySensor: Sensor? = null
    private val restartVisualizerForRouteChangeRunnable = Runnable {
        if (!shouldRestartVisualizerForRouteChange()) return@Runnable
        visualizerStartRequestId += 1
        val requestId = visualizerStartRequestId
        AppLogger.i(
            TAG,
            "Restarting Visualizer(0) after route change. requestId=$requestId ${AudioRouteDiagnostics.snapshot(this)}"
        )
        startVisualizerMode(requestId = requestId, attempt = 1)
    }
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            applyLatencyPresetForCurrentRoute("added")
            handleAudioRouteChanged("added", addedDevices)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            applyLatencyPresetForCurrentRoute("removed")
            handleAudioRouteChanged("removed", removedDevices)
        }
    }
    private val gravityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!turnOffWhenBackDown) return
            val z = event.values.getOrNull(2) ?: return
            val nextSuppressed = when {
                isBackDownSuppressed -> z >= BACK_DOWN_DISABLE_Z_THRESHOLD
                else -> z >= BACK_DOWN_ENABLE_Z_THRESHOLD
            }
            if (nextSuppressed == isBackDownSuppressed) return
            isBackDownSuppressed = nextSuppressed
            if (isBackDownSuppressed) {
                try {
                    glyphController.turnOff()
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "glyphController.turnOff failed while back-down suppressing", error)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        createNotificationChannel()
        val savedSettings = SettingsPreferences.load(this)
        glyphController = if (GlyphDeviceCatalog.currentOrFallback().controllerFamily == GlyphControllerFamily.MATRIX) {
            GlyphMatrixController(this) { status ->
                CaptureUiStore.update { it.copy(statusText = status) }
            }
        } else {
            GlyphLightController(
                context = this,
                onStatusChanged = { status ->
                    CaptureUiStore.update { it.copy(statusText = status) }
                },
                initialPhone4bEmulationEnabled = savedSettings.phone4bEmulationEnabled
            )
        }
        audioPlaybackVisualizer = AudioPlaybackVisualizer(this)
        outputMixVisualizer = OutputMixVisualizer(this)
        glyphController.bind()
        CaptureUiStore.update {
            it.copy(phone4bEmulationEnabled = savedSettings.phone4bEmulationEnabled)
        }
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        lastAudioRouteSignature = AudioRouteDiagnostics.outputSignature(this)
        applyLatencyPresetForCurrentRoute("service created")
        registerAudioDeviceCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VISUALIZER -> {
                try {
                    val actionReceivedAt = SystemClock.elapsedRealtime()
                    visualizerStartRequestId += 1
                    visualizerStartActionAtMs = actionReceivedAt
                    visualizerStartSource = intent.getStringExtra(EXTRA_START_SOURCE)
                        ?.let { savedName ->
                            VisualizerStartSource.entries.firstOrNull { it.name == savedName }
                        }
                        ?: VisualizerStartSource.APP
                    clearSpatialAudioWarning()
                    sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                    noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                    dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                    intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, Float.NaN).let { if (!it.isNaN()) outputGamma = it }
                    toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                    smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                    smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                    reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                    peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                    glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                    fillOtherGlyphLights = intent.getBooleanExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                    binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                    baseIndicatorEnabled = intent.getBooleanExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                    recordingLightIncluded = intent.getBooleanExtra(
                        EXTRA_RECORDING_LIGHT_INCLUDED,
                        recordingLightIncluded
                    )
                    levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                    spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                    allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                    autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                    autoScaleOffset = intent.getFloatExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                    latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                    mediaPlaybackOnlyEnabled = intent.getBooleanExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                    experimentalVisualizerStabilizationEnabled = intent.getBooleanExtra(
                        EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                        experimentalVisualizerStabilizationEnabled
                    )
                    experimentalVisualizerSignalWatchdogEnabled = intent.getBooleanExtra(
                        EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                        experimentalVisualizerSignalWatchdogEnabled
                    )
                    experimentalSpectrumDecayEnabled = intent.getBooleanExtra(
                        EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                        experimentalSpectrumDecayEnabled
                    )
                    experimentalPerformanceOptimizationsEnabled = intent.getBooleanExtra(
                        EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                        experimentalPerformanceOptimizationsEnabled
                    )
                    matrixSmoothMotionEnabled = intent.getBooleanExtra(
                        EXTRA_MATRIX_SMOOTH_MOTION_ENABLED,
                        matrixSmoothMotionEnabled
                    )
                    oscilloscopeAutoTimeAxisEnabled = intent.getBooleanExtra(
                        EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                        oscilloscopeAutoTimeAxisEnabled
                    )
                    WaveformSampler.setAutoTimeAxisEnabled(oscilloscopeAutoTimeAxisEnabled)
                    turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                    applyGlyphControllerSettings()
                    AppLogger.i(
                        TAG,
                        "ACTION_START_VISUALIZER received: requestId=$visualizerStartRequestId source=$visualizerStartSource glyphMode=$glyphMode btLikely=${
                            AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
                        } musicActive=${AudioRouteDiagnostics.isMusicActive(this)}"
                    )
                    startServiceNotification(getString(R.string.notification_mode_visualizer))
                    AppLogger.i(
                        TAG,
                        "Foreground notification posted for visualizer: requestId=$visualizerStartRequestId elapsedMs=${SystemClock.elapsedRealtime() - actionReceivedAt}"
                    )
                    scheduleGlyphWarmupResync()
                    startVisualizerMode(requestId = visualizerStartRequestId, attempt = 1)
                } catch (error: SecurityException) {
                    // パーミッション不足は即座に失敗（リトライ不要）
                    val msg = getString(
                        R.string.status_permission_denied,
                        error.message ?: getString(R.string.status_unknown_error)
                    )
                    AppLogger.e(TAG, "ACTION_START_VISUALIZER permission denied", error)
                    CaptureUiStore.update { it.copy(statusText = msg, logMessage = msg) }
                    safeStopForeground()
                    stopSelf()
                } catch (error: Throwable) {
                    AppLogger.e(TAG, "ACTION_START_VISUALIZER failed", error)
                    stopCapture(
                        getString(
                            R.string.status_visualizer_service_start_failed,
                            error.message ?: getString(R.string.status_unknown_error)
                        )
                    )
                    safeStopForeground()
                    stopSelf()
                }
            }

            ACTION_START_MEDIA_PROJECTION -> {
                visualizerStartRequestId += 1
                sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, Float.NaN).let { if (!it.isNaN()) outputGamma = it }
                toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                fillOtherGlyphLights = intent.getBooleanExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                baseIndicatorEnabled = intent.getBooleanExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                recordingLightIncluded = intent.getBooleanExtra(
                    EXTRA_RECORDING_LIGHT_INCLUDED,
                    recordingLightIncluded
                )
                levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                autoScaleOffset = intent.getFloatExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                mediaPlaybackOnlyEnabled = intent.getBooleanExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                experimentalVisualizerStabilizationEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                    experimentalVisualizerStabilizationEnabled
                )
                experimentalVisualizerSignalWatchdogEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                    experimentalVisualizerSignalWatchdogEnabled
                )
                experimentalSpectrumDecayEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                    experimentalSpectrumDecayEnabled
                )
                experimentalPerformanceOptimizationsEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                    experimentalPerformanceOptimizationsEnabled
                )
                matrixSmoothMotionEnabled = intent.getBooleanExtra(
                    EXTRA_MATRIX_SMOOTH_MOTION_ENABLED,
                    matrixSmoothMotionEnabled
                )
                oscilloscopeAutoTimeAxisEnabled = intent.getBooleanExtra(
                    EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                    oscilloscopeAutoTimeAxisEnabled
                )
                WaveformSampler.setAutoTimeAxisEnabled(oscilloscopeAutoTimeAxisEnabled)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                applyGlyphControllerSettings()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtraCompat<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && data != null) {
                    startServiceNotification(getString(R.string.notification_mode_media_projection), mediaProjection = true)
                    scheduleGlyphWarmupResync()
                    startMediaProjectionMode(resultCode, data)
                } else {
                    stopCapture(getString(R.string.status_media_projection_data_missing))
                    stopSelf()
                }
            }

            ACTION_UPDATE_SENSITIVITY -> {
                sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, Float.NaN).let { if (!it.isNaN()) outputGamma = it }
                toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                fillOtherGlyphLights = intent.getBooleanExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, fillOtherGlyphLights)
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                baseIndicatorEnabled = intent.getBooleanExtra(EXTRA_BASE_INDICATOR_ENABLED, baseIndicatorEnabled)
                recordingLightIncluded = intent.getBooleanExtra(
                    EXTRA_RECORDING_LIGHT_INCLUDED,
                    recordingLightIncluded
                )
                levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                autoScaleOffset = intent.getFloatExtra(EXTRA_AUTO_SCALE_OFFSET, autoScaleOffset)
                latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                mediaPlaybackOnlyEnabled = intent.getBooleanExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, mediaPlaybackOnlyEnabled)
                experimentalVisualizerStabilizationEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                    experimentalVisualizerStabilizationEnabled
                )
                experimentalVisualizerSignalWatchdogEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                    experimentalVisualizerSignalWatchdogEnabled
                )
                experimentalSpectrumDecayEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                    experimentalSpectrumDecayEnabled
                )
                experimentalPerformanceOptimizationsEnabled = intent.getBooleanExtra(
                    EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                    experimentalPerformanceOptimizationsEnabled
                )
                matrixSmoothMotionEnabled = intent.getBooleanExtra(
                    EXTRA_MATRIX_SMOOTH_MOTION_ENABLED,
                    matrixSmoothMotionEnabled
                )
                oscilloscopeAutoTimeAxisEnabled = intent.getBooleanExtra(
                    EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                    oscilloscopeAutoTimeAxisEnabled
                )
                WaveformSampler.setAutoTimeAxisEnabled(oscilloscopeAutoTimeAxisEnabled)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                applyGlyphControllerSettings()
                CaptureUiStore.update {
                    it.copy(
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
                        binaryMode = binaryMode,
                        baseIndicatorEnabled = baseIndicatorEnabled,
                        recordingLightIncluded = recordingLightIncluded,
                        levelAutoScale = levelAutoScale,
                        spectrumAutoScale = spectrumAutoScale,
                        autoScaleWindowSeconds = autoScaleWindowSeconds,
                        autoScaleOffset = autoScaleOffset,
                        allBrightnessAutoScale = allBrightnessAutoScale,
                        mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
                        experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
                        experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
                        experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
                        experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
                        oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                        turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
            }

            ACTION_STOP -> {
                visualizerStartRequestId += 1
                try {
                    stopCapture(getString(R.string.status_capture_stopped_ready))
                } catch (error: Throwable) {
                    AppLogger.e(TAG, "stopCapture failed while handling ACTION_STOP", error)
                }
                safeStopForeground()
                stopSelf()
            }

        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            stopCapture(CaptureUiStore.state.statusText)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "stopCapture failed in onDestroy", error)
        }
        try {
            glyphController.unbind()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "glyphController.unbind failed in onDestroy", error)
        }
        try {
            sensorManager.unregisterListener(gravityListener)
        } catch (_: Throwable) {
        }
        unregisterAudioDeviceCallback()
        mainHandler.removeCallbacks(restartVisualizerForRouteChangeRunnable)
        mainHandler.removeCallbacks(latencyDrainRunnable)
        mainHandler.removeCallbacks(glyphWarmupResyncRunnable)
        pendingLevelFrames.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startVisualizerMode(requestId: Int, attempt: Int) {
        if (requestId != visualizerStartRequestId) return
        val startAttemptAt = SystemClock.elapsedRealtime()
        suppressRouteRestartUntilMs = SystemClock.uptimeMillis() + visualizerRouteRestartSuppressionMs()
        AppLogger.i(
            TAG,
            "Visualizer start attempt begin: requestId=$requestId attempt=$attempt elapsedSinceActionMs=${
                if (visualizerStartActionAtMs > 0L) startAttemptAt - visualizerStartActionAtMs else -1L
            }"
        )
        try {
            stopRunningCapture(clearStatus = false)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "stopRunningCapture failed before visualizer start", error)
        }
        val started = outputMixVisualizer.start(
            sensitivityProvider = { sensitivity },
            noiseGateProvider = { noiseGate },
            dynamicsProvider = { dynamics },
            toneFocusProvider = { toneFocus },
            smoothingProvider = { smoothing },
            smoothingBalanceProvider = { smoothingBalance },
            experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
            experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
            experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
            onStateChanged = { status ->
                val now = SystemClock.elapsedRealtime()
                AppLogger.i(
                    TAG,
                    "Visualizer start reached active state: requestId=$requestId attempt=$attempt elapsedSinceActionMs=${
                        if (visualizerStartActionAtMs > 0L) now - visualizerStartActionAtMs else -1L
                    } attemptDurationMs=${now - startAttemptAt} status=$status"
                )
                CaptureUiStore.update {
                    it.copy(
                        statusText = status,
                        isCapturing = true,
                        activeMode = ACTIVE_MODE_VISUALIZER,
                        sensitivity = sensitivity,
                            noiseGate = noiseGate,
                            dynamics = dynamics,
                            outputGamma = outputGamma,
                            toneFocus = toneFocus,
                        smoothing = smoothing,
                        reverseDirection = reverseDirection,
                            peakHoldEnabled = peakHoldEnabled,
                            glyphMode = glyphMode,
                            fillOtherGlyphLights = fillOtherGlyphLights,
                            binaryMode = binaryMode,
                            levelAutoScale = levelAutoScale,
                            spectrumAutoScale = spectrumAutoScale,
                            allBrightnessAutoScale = allBrightnessAutoScale,
                            autoScaleWindowSeconds = autoScaleWindowSeconds,
                        autoScaleOffset = autoScaleOffset,
                        mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
                        experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
                        experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
                        experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
                        experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
                        matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                        oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                        turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
                notifyTile()
            },
            onLevelChanged = { level, peak, lowEnergy, highEnergy, leftLevel, rightLevel, spectrumBands, phone4aBaseBandLevel, waveformSamples, leftWaveformSamples, rightWaveformSamples ->
                publishLevel(
                    level,
                    peak,
                    "VISUALIZER",
                    lowEnergy,
                    highEnergy,
                    leftLevel,
                    rightLevel,
                    spectrumBands,
                    phone4aBaseBandLevel,
                    waveformSamples,
                    leftWaveformSamples,
                    rightWaveformSamples
                )
            },
            onStartFailed = { failureReason ->
                val maxAttempts = visualizerStartMaxAttempts()
                val unrecoverableSpatializerConflict =
                    failureReason == OutputMixVisualizer.StartFailureReason.UNRECOVERABLE_SPATIALIZER_CONFLICT
                if (requestId == visualizerStartRequestId && unrecoverableSpatializerConflict) {
                    AppLogger.e(
                        TAG,
                        "Visualizer async start hit unrecoverable Framework Spatializer conflict: requestId=$requestId attempt=$attempt elapsedSinceActionMs=${
                            if (visualizerStartActionAtMs > 0L) SystemClock.elapsedRealtime() - visualizerStartActionAtMs else -1L
                        }"
                    )
                    finishVisualizerStartFailure(showSpatialAudioWarning = true)
                } else if (requestId == visualizerStartRequestId && attempt < maxAttempts) {
                    val nextAttempt = attempt + 1
                    val retryMs = visualizerRetryDelayMs(attempt)
                    AppLogger.w(
                        TAG,
                        "Visualizer async start failed: requestId=$requestId attempt=$attempt retryInMs=$retryMs elapsedAttemptMs=${SystemClock.elapsedRealtime() - startAttemptAt}"
                    )
                    CaptureUiStore.update {
                        it.copy(statusText = getString(R.string.status_visualizer_retrying, nextAttempt, maxAttempts))
                    }
                    mainHandler.postDelayed(
                        { startVisualizerMode(requestId = requestId, attempt = nextAttempt) },
                        retryMs
                    )
                } else if (requestId == visualizerStartRequestId) {
                    AppLogger.e(
                        TAG,
                        "Visualizer async start exhausted retries: requestId=$requestId attempts=$attempt elapsedSinceActionMs=${
                            if (visualizerStartActionAtMs > 0L) SystemClock.elapsedRealtime() - visualizerStartActionAtMs else -1L
                        }"
                    )
                    finishVisualizerStartFailure(
                        showSpatialAudioWarning = false
                    )
                }
            },
            onSignalStalled = {
                val maxAttempts = visualizerStartMaxAttempts()
                if (requestId == visualizerStartRequestId && attempt < maxAttempts) {
                    val nextAttempt = attempt + 1
                    val retryMs = visualizerRetryDelayMs(attempt)
                    AppLogger.w(
                        TAG,
                        "Visualizer active-without-signal detected: requestId=$requestId attempt=$attempt retryInMs=$retryMs elapsedAttemptMs=${SystemClock.elapsedRealtime() - startAttemptAt}"
                    )
                    CaptureUiStore.update {
                        it.copy(statusText = getString(R.string.status_visualizer_retrying, nextAttempt, maxAttempts))
                    }
                    mainHandler.postDelayed(
                        { startVisualizerMode(requestId = requestId, attempt = nextAttempt) },
                        retryMs
                    )
                } else if (requestId == visualizerStartRequestId) {
                    AppLogger.e(
                        TAG,
                        "Visualizer active-without-signal exhausted retries: requestId=$requestId attempts=$attempt elapsedSinceActionMs=${
                            if (visualizerStartActionAtMs > 0L) SystemClock.elapsedRealtime() - visualizerStartActionAtMs else -1L
                        }"
                    )
                    val msg = getString(R.string.status_visualizer_try_media_projection)
                    CaptureUiStore.update { it.copy(statusText = msg, logMessage = msg) }
                    stopCapture(msg)
                    safeStopForeground()
                    stopSelf()
                }
            },
            onCrashed = {
                // ワーカースレッドが予期せずクラッシュした場合、自動再起動
                if (requestId == visualizerStartRequestId) {
                    AppLogger.w(TAG, "Visualizer worker crashed, auto-restarting")
                    visualizerStartRequestId += 1
                    mainHandler.postDelayed(
                        { startVisualizerMode(requestId = visualizerStartRequestId, attempt = 1) },
                        200L
                    )
                }
            }
        )
        if (!started) {
            val maxAttempts = visualizerStartMaxAttempts()
            if (requestId == visualizerStartRequestId && attempt < maxAttempts) {
                val nextAttempt = attempt + 1
                val retryMs = visualizerRetryDelayMs(attempt)
                AppLogger.w(
                    TAG,
                    "Visualizer start attempt failed: requestId=$requestId attempt=$attempt retryInMs=$retryMs elapsedAttemptMs=${SystemClock.elapsedRealtime() - startAttemptAt}"
                )
                CaptureUiStore.update {
                    it.copy(statusText = getString(R.string.status_visualizer_retrying, nextAttempt, maxAttempts))
                }
                mainHandler.postDelayed(
                    { startVisualizerMode(requestId = requestId, attempt = nextAttempt) },
                    retryMs
                )
                return
            }
            AppLogger.e(
                TAG,
                "Visualizer start exhausted retries: requestId=$requestId attempts=$attempt elapsedSinceActionMs=${
                    if (visualizerStartActionAtMs > 0L) SystemClock.elapsedRealtime() - visualizerStartActionAtMs else -1L
                }"
            )
            val msg = getString(R.string.status_visualizer_try_media_projection)
            val logMsg = getString(R.string.status_visualizer_try_media_projection)
            AppLogger.e(TAG, logMsg)
            stopCapture(msg)
            safeStopForeground()
            stopSelf()
        }
    }

    private fun startMediaProjectionMode(resultCode: Int, data: Intent) {
        stopRunningCapture(clearStatus = false)
        val started = audioPlaybackVisualizer.start(
            resultCode = resultCode,
            data = data,
            sensitivityProvider = { sensitivity },
            noiseGateProvider = { noiseGate },
            dynamicsProvider = { dynamics },
            toneFocusProvider = { toneFocus },
            smoothingProvider = { smoothing },
            smoothingBalanceProvider = { smoothingBalance },
            experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
            onStateChanged = { status ->
                CaptureUiStore.update {
                    it.copy(
                        statusText = status,
                        isCapturing = true,
                        activeMode = ACTIVE_MODE_MEDIA_PROJECTION,
                        sensitivity = sensitivity,
                            noiseGate = noiseGate,
                            dynamics = dynamics,
                            outputGamma = outputGamma,
                            toneFocus = toneFocus,
                        smoothing = smoothing,
                        reverseDirection = reverseDirection,
                            peakHoldEnabled = peakHoldEnabled,
                            glyphMode = glyphMode,
                            fillOtherGlyphLights = fillOtherGlyphLights,
                            binaryMode = binaryMode,
                            levelAutoScale = levelAutoScale,
                            spectrumAutoScale = spectrumAutoScale,
                            allBrightnessAutoScale = allBrightnessAutoScale,
                            autoScaleWindowSeconds = autoScaleWindowSeconds,
                            autoScaleOffset = autoScaleOffset,
                            mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
                            experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
                            experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
                            experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
                            experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
                            matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                            oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                            turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
                notifyTile()
            },
            onLevelChanged = { level, peak, lowEnergy, highEnergy, leftLevel, rightLevel, spectrumBands, phone4aBaseBandLevel, waveformSamples, leftWaveformSamples, rightWaveformSamples ->
                publishLevel(
                    level,
                    peak,
                    "MEDIA PROJECTION",
                    lowEnergy,
                    highEnergy,
                    leftLevel,
                    rightLevel,
                    spectrumBands,
                    phone4aBaseBandLevel,
                    waveformSamples,
                    leftWaveformSamples,
                    rightWaveformSamples
                )
            }
        )
        if (!started) {
            stopCapture(getString(R.string.status_media_projection_service_start_failed, getString(R.string.status_unknown_error)))
            safeStopForeground()
            stopSelf()
        }
    }

    private fun publishLevel(
        level: Float,
        peak: Float,
        mode: String,
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray,
        phone4aBaseBandLevel: Float,
        waveformSamples: FloatArray,
        leftWaveformSamples: FloatArray,
        rightWaveformSamples: FloatArray
    ) {
        enqueueDelayedLevelFrame(
            level = level,
            peak = peak,
            mode = mode,
            lowEnergy = lowEnergy,
            highEnergy = highEnergy,
            leftLevel = leftLevel,
            rightLevel = rightLevel,
            spectrumBands = spectrumBands,
            phone4aBaseBandLevel = phone4aBaseBandLevel,
            waveformSamples = waveformSamples,
            leftWaveformSamples = leftWaveformSamples,
            rightWaveformSamples = rightWaveformSamples
        )
    }

    private fun enqueueDelayedLevelFrame(
        level: Float,
        peak: Float,
        mode: String,
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray,
        phone4aBaseBandLevel: Float,
        waveformSamples: FloatArray,
        leftWaveformSamples: FloatArray,
        rightWaveformSamples: FloatArray
    ) {
        val dueAtMs = SystemClock.uptimeMillis() + latencyMs.coerceIn(0f, 500f).roundToLong()
        val frame = DelayedLevelFrame(
            dueAtMs = dueAtMs,
            level = level,
            peak = peak,
            mode = mode,
            lowEnergy = lowEnergy,
            highEnergy = highEnergy,
            leftLevel = leftLevel,
            rightLevel = rightLevel,
            spectrumBands = spectrumBands.copyOf(),
            phone4aBaseBandLevel = phone4aBaseBandLevel,
            waveformSamples = waveformSamples.copyOf(),
            leftWaveformSamples = leftWaveformSamples.copyOf(),
            rightWaveformSamples = rightWaveformSamples.copyOf()
        )
        latestLevelFrame = frame
        pendingLevelFrames.addLast(frame)
        drainPendingLevelFrames()
    }

    private fun drainPendingLevelFrames(forceAll: Boolean = false) {
        mainHandler.removeCallbacks(latencyDrainRunnable)
        val now = SystemClock.uptimeMillis()
        while (pendingLevelFrames.isNotEmpty()) {
            val next = pendingLevelFrames.first()
            if (!forceAll && next.dueAtMs > now) {
                mainHandler.postDelayed(latencyDrainRunnable, next.dueAtMs - now)
                return
            }
            pendingLevelFrames.removeFirst()
            renderLevelFrame(next)
        }
    }

    private fun renderLevelFrame(frame: DelayedLevelFrame) {
        val useGlyphPreviewValues = CaptureUiStore.state.glyphMeterPreviewEnabled && !isBackDownSuppressed
        val allowPausedMediaSession =
            GlyphPatternRegistry.recipeFor(frame.mode)?.renderMode == GlyphPatternRenderMode.MATRIX_OPEN_REEL
        val mediaPlaybackActive = isMediaPlaybackAllowed(allowPaused = allowPausedMediaSession)
        if (!mediaPlaybackActive) {
            val enteringMediaPlaybackSuppression = !mediaPlaybackSuppressed
            mediaPlaybackSuppressed = true
            val silentBands = FloatArray(frame.spectrumBands.size)
            if (enteringMediaPlaybackSuppression) {
                try {
                    glyphController.suspendSession()
                    AppLogger.i(TAG, "Glyph session suspended because no playing MediaSession is active")
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "glyphController.suspendSession failed during media playback suppression", error)
                }
            }
            if (shouldPublishUiFrame(frame.mode)) {
                publishUiFrame(
                    level = 0f,
                    peak = 0f,
                    spectrumBands = silentBands,
                    mode = frame.mode
                )
            }
            return
        }
        mediaPlaybackSuppressed = false
        if (!isBackDownSuppressed) {
            glyphController.updateAnalysis(
                frame.lowEnergy,
                frame.highEnergy,
                frame.leftLevel,
                frame.rightLevel,
                frame.spectrumBands,
                frame.phone4aBaseBandLevel,
                frame.waveformSamples,
                frame.leftWaveformSamples,
                frame.rightWaveformSamples
            )
            glyphController.updateLevel(frame.level)
        } else if (isBackDownSuppressed) {
            try {
                glyphController.turnOff()
            } catch (error: Throwable) {
                AppLogger.w(TAG, "glyphController.turnOff failed while back-down suppressing", error)
            }
        }
        val previewLevel = if (useGlyphPreviewValues) {
            glyphController.previewLevel().coerceIn(0f, 1f)
        } else {
            frame.level.coerceIn(0f, 1f)
        }
        val previewSpectrumBands = if (useGlyphPreviewValues) {
            glyphController.previewSpectrumBands()
        } else {
            frame.spectrumBands
        }
        if (shouldPublishUiFrame(frame.mode)) {
            publishUiFrame(
                level = previewLevel,
                peak = frame.peak,
                spectrumBands = if (previewSpectrumBands.isNotEmpty()) previewSpectrumBands else frame.spectrumBands,
                mode = frame.mode
            )
        }
    }

    private fun stopRunningCapture(clearStatus: Boolean, releaseGlyphSession: Boolean = false) {
        try {
            outputMixVisualizer.stop()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "outputMixVisualizer.stop failed", error)
        }
        try {
            audioPlaybackVisualizer.stop()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "audioPlaybackVisualizer.stop failed", error)
        }
        if (releaseGlyphSession) {
            try {
                glyphController.releaseSession()
            } catch (error: Throwable) {
                AppLogger.w(TAG, "glyphController.releaseSession failed", error)
            }
        } else {
            try {
                glyphController.turnOff()
            } catch (error: Throwable) {
                AppLogger.w(TAG, "glyphController.turnOff failed", error)
            }
        }
        mainHandler.removeCallbacks(latencyDrainRunnable)
        mainHandler.removeCallbacks(glyphWarmupResyncRunnable)
        pendingLevelFrames.clear()
        latestLevelFrame = null
        CaptureUiStore.update {
            it.copy(
                level = 0f,
                peak = 0f,
                meterSegments = 0,
                spectrumBands = FloatArray(0),
                isCapturing = false,
                activeMode = ACTIVE_MODE_IDLE,
                statusText = if (clearStatus) getString(R.string.status_capture_stopped_ready) else it.statusText,
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
                binaryMode = binaryMode,
                levelAutoScale = levelAutoScale,
                spectrumAutoScale = spectrumAutoScale,
                allBrightnessAutoScale = allBrightnessAutoScale,
                autoScaleWindowSeconds = autoScaleWindowSeconds,
                autoScaleOffset = autoScaleOffset,
                latencyMs = latencyMs,
                mediaPlaybackOnlyEnabled = mediaPlaybackOnlyEnabled,
                experimentalVisualizerStabilizationEnabled = experimentalVisualizerStabilizationEnabled,
                experimentalVisualizerSignalWatchdogEnabled = experimentalVisualizerSignalWatchdogEnabled,
                experimentalPerformanceOptimizationsEnabled = experimentalPerformanceOptimizationsEnabled,
                matrixSmoothMotionEnabled = matrixSmoothMotionEnabled,
                oscilloscopeAutoTimeAxisEnabled = oscilloscopeAutoTimeAxisEnabled,
                experimentalSpectrumDecayEnabled = experimentalSpectrumDecayEnabled,
                turnOffWhenBackDown = turnOffWhenBackDown
            )
        }
        CaptureUiStore.syncLiveFrameFromState()
        isBackDownSuppressed = false
        lastUiPublishAtMs = 0L
        publishUiFrameCallCount = 0
        lastPublishUiFrameCallLogAtMs = 0L
        suppressedUiPublishCount = 0
        lastSuppressedUiPublishLogAtMs = 0L
        lastMediaPlaybackCheckAtMs = 0L
        lastMediaPlaybackActive = false
        mediaPlaybackResumeCandidateAtMs = 0L
        mediaPlaybackSuppressed = false
    }

    private fun applyGlyphControllerSettings() {
        glyphController.setPhone4bEmulationEnabled(
            SettingsPreferences.loadPhone4bEmulationEnabled(this)
        )
        glyphController.setReverseDirection(reverseDirection)
        glyphController.setGlyphMode(glyphMode)
        glyphController.setFillOtherGlyphLightsEnabled(fillOtherGlyphLights)
        glyphController.setBinaryMode(binaryMode)
        glyphController.setBaseIndicatorEnabled(baseIndicatorEnabled)
        glyphController.setRecordingLightIncluded(recordingLightIncluded)
        glyphController.setOutputGamma(outputGamma)
        glyphController.setSmoothing(smoothing, smoothingBalance)
        glyphController.setLevelAutoScaleEnabled(levelAutoScale)
        glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
        glyphController.setExperimentalPerformanceOptimizationsEnabled(experimentalPerformanceOptimizationsEnabled)
        glyphController.setMatrixSmoothMotionEnabled(matrixSmoothMotionEnabled)
        glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
        glyphController.setAutoScaleWindowSeconds(autoScaleWindowSeconds)
        glyphController.setAutoScaleOffset(autoScaleOffset)
        updateBackDownSensorState()
    }

    private fun scheduleGlyphWarmupResync() {
        mainHandler.removeCallbacks(glyphWarmupResyncRunnable)
        mainHandler.postDelayed(glyphWarmupResyncRunnable, GLYPH_WARMUP_RESYNC_DELAY_MS)
    }

    private fun updateBackDownSensorState() {
        if (!turnOffWhenBackDown) {
            isBackDownSuppressed = false
            try {
                sensorManager.unregisterListener(gravityListener)
            } catch (_: Throwable) {
            }
            return
        }
        val sensor = gravitySensor ?: return
        try {
            sensorManager.unregisterListener(gravityListener)
        } catch (_: Throwable) {
        }
        sensorManager.registerListener(gravityListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopCapture(status: String) {
        try {
            stopRunningCapture(clearStatus = false, releaseGlyphSession = true)
        } catch (error: Throwable) {
            AppLogger.e(TAG, "stopRunningCapture failed in stopCapture", error)
        }
        CaptureUiStore.update { it.copy(statusText = status) }
        notifyTile()
    }

    private fun finishVisualizerStartFailure(showSpatialAudioWarning: Boolean) {
        val productName = if (showSpatialAudioWarning) {
            AudioRouteDiagnostics.nothingOrCmfBluetoothOutputProductName(this)
        } else {
            null
        }
        val message = if (showSpatialAudioWarning) {
            spatialAudioWarningMessage(productName)
        } else {
            getString(R.string.status_visualizer_try_media_projection)
        }

        if (showSpatialAudioWarning) {
            val showInApp =
                visualizerStartSource == VisualizerStartSource.APP && CaptureUiStore.isUiVisible()
            val notificationShown = if (showInApp) {
                false
            } else {
                showSpatialAudioWarningNotification(productName)
            }
            CaptureUiStore.update {
                it.copy(
                    logMessage = message,
                    pendingSpatialAudioWarning = if (showInApp || !notificationShown) {
                        SpatialAudioWarning(nothingOrCmfProductName = productName)
                    } else {
                        null
                    }
                )
            }
            AppLogger.w(
                TAG,
                "Framework Spatializer warning dispatched after unrecoverable Visualizer(0) creation failure: nothingOrCmfProductName=$productName source=$visualizerStartSource notificationShown=$notificationShown"
            )
        }

        AppLogger.e(TAG, message)
        stopCapture(message)
        safeStopForeground()
        stopSelf()
    }

    private fun clearSpatialAudioWarning() {
        CaptureUiStore.update { it.copy(pendingSpatialAudioWarning = null) }
        getSystemService(NotificationManager::class.java)?.cancel(ALERT_NOTIFICATION_ID)
    }

    private fun showSpatialAudioWarningNotification(productName: String?): Boolean {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: notification permission missing")
            return false
        }
        val manager = getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: notifications disabled")
            return false
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(ALERT_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: alert channel disabled")
            return false
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            ALERT_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = spatialAudioWarningMessage(productName)
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.spatial_audio_warning_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()
        manager.notify(ALERT_NOTIFICATION_ID, notification)
        return true
    }

    private fun spatialAudioWarningMessage(nothingOrCmfProductName: String?): String {
        return if (nothingOrCmfProductName != null) {
            getString(R.string.spatial_audio_warning_message, nothingOrCmfProductName)
        } else {
            getString(R.string.spatial_audio_warning_message_generic)
        }
    }

    private fun notifyTile() {
        try {
            GlyphTileService.refresh(this)
        } catch (_: Exception) {}
    }

    private fun currentUiUpdateIntervalMs(): Long {
        val useExperimentalMatrixUiInterval =
            experimentalPerformanceOptimizationsEnabled && glyphController is GlyphMatrixController
        val matrixUiIntervalMs = if (matrixSmoothMotionEnabled) {
            MATRIX_UI_SMOOTH_UPDATE_INTERVAL_MS
        } else {
            MATRIX_UI_REDUCED_UPDATE_INTERVAL_MS
        }
        val appUiVisible = CaptureUiStore.shouldPublishLiveUiFrames()
        return when {
            appUiVisible && useExperimentalMatrixUiInterval -> matrixUiIntervalMs
            appUiVisible -> 0L
            powerManager.isInteractive && useExperimentalMatrixUiInterval -> matrixUiIntervalMs
            powerManager.isInteractive -> BACKGROUND_UI_UPDATE_INTERVAL_MS
            useExperimentalMatrixUiInterval -> matrixUiIntervalMs
            else -> SCREEN_OFF_UI_UPDATE_INTERVAL_MS
        }
    }

    private fun isMediaPlaybackAllowed(allowPaused: Boolean = false): Boolean {
        if (!mediaPlaybackOnlyEnabled) return true
        val now = SystemClock.uptimeMillis()
        if (now - lastMediaPlaybackCheckAtMs >= MEDIA_PLAYBACK_CHECK_INTERVAL_MS) {
            lastMediaPlaybackCheckAtMs = now
            val playbackSnapshot = if (allowPaused) {
                MediaSessionPlaybackGate.currentPlaybackSnapshot(this)
            } else {
                null
            }
            val rawMediaPlaybackActive = if (allowPaused) {
                playbackSnapshot?.status == MediaSessionPlaybackGate.PlaybackStatus.PLAYING ||
                    playbackSnapshot?.status == MediaSessionPlaybackGate.PlaybackStatus.PAUSED
            } else {
                MediaSessionPlaybackGate.isMediaSessionPlaybackActive(this)
            }
            if (!rawMediaPlaybackActive) {
                lastMediaPlaybackActive = false
                mediaPlaybackResumeCandidateAtMs = 0L
            } else if (
                allowPaused &&
                playbackSnapshot?.status == MediaSessionPlaybackGate.PlaybackStatus.PAUSED
            ) {
                lastMediaPlaybackActive = true
                mediaPlaybackResumeCandidateAtMs = 0L
            } else if (!lastMediaPlaybackActive) {
                if (mediaPlaybackResumeCandidateAtMs <= 0L) {
                    mediaPlaybackResumeCandidateAtMs = now
                }
                if (now - mediaPlaybackResumeCandidateAtMs >= MEDIA_PLAYBACK_RESUME_CONFIRM_MS) {
                    lastMediaPlaybackActive = true
                    mediaPlaybackResumeCandidateAtMs = 0L
                    AppLogger.i(TAG, "MediaSession playback remained active; allowing Glyph session resume")
                }
            }
        }
        return lastMediaPlaybackActive
    }

    private fun publishUiFrame(
        level: Float,
        peak: Float,
        spectrumBands: FloatArray,
        mode: String,
        force: Boolean = false
    ) {
        val entryNow = SystemClock.uptimeMillis()
        publishUiFrameCallCount += 1
        if (
            DEBUG_UI_VISIBILITY_LOGS &&
                (
                    publishUiFrameCallCount == 1 ||
                        entryNow - lastPublishUiFrameCallLogAtMs >= 5_000L
                    )
        ) {
            lastPublishUiFrameCallLogAtMs = entryNow
            AppLogger.i(
                TAG,
                "publishUiFrame entered: count=$publishUiFrameCallCount mode=$mode force=$force uiVisible=${CaptureUiStore.shouldPublishLiveUiFrames()}"
            )
        }
        if (!force && !shouldPublishUiFrame(mode)) {
            suppressedUiPublishCount += 1
            if (
                DEBUG_UI_VISIBILITY_LOGS &&
                    (
                        suppressedUiPublishCount == 1 ||
                            entryNow - lastSuppressedUiPublishLogAtMs >= 5_000L
                        )
            ) {
                lastSuppressedUiPublishLogAtMs = entryNow
                AppLogger.i(
                    TAG,
                    "UI frame publish suppressed because app UI is not visible: count=$suppressedUiPublishCount mode=$mode"
                )
            }
            return
        }
        if (DEBUG_UI_VISIBILITY_LOGS && suppressedUiPublishCount > 0) {
            AppLogger.i(
                TAG,
                "UI frame publish resumed after suppression: count=$suppressedUiPublishCount mode=$mode"
            )
            suppressedUiPublishCount = 0
            lastSuppressedUiPublishLogAtMs = 0L
        }
        val uiState = CaptureUiStore.state
        val lightweightMeter = uiState.lightweightMeterEnabled && uiState.meterVisibleEnabled
        val intervalMs = if (lightweightMeter) {
            currentUiUpdateIntervalMs().coerceAtLeast(LIGHTWEIGHT_METER_UI_UPDATE_INTERVAL_MS)
        } else {
            currentUiUpdateIntervalMs()
        }
        val now = SystemClock.uptimeMillis()
        if (!force && intervalMs > 0L && now - lastUiPublishAtMs < intervalMs) return
        lastUiPublishAtMs = now
        val quantizedLevel = quantizeUiValue(level, UI_LEVEL_QUANTIZATION_STEPS)
        val quantizedPeak = if (lightweightMeter) 0f else quantizeUiValue(peak, UI_PEAK_QUANTIZATION_STEPS)
        val quantizedMeterSegments = (quantizedLevel * 16f).roundToInt().coerceIn(0, 16)
        val quantizedSpectrumBands = if (lightweightMeter) FloatArray(0) else quantizeSpectrumBandsForUi(spectrumBands)
        if (uiState.mainScreenUiIsolationEnabled) {
            val currentFrame = CaptureUiStore.liveFrame
            val sameSpectrum = currentFrame.spectrumBands.contentEquals(quantizedSpectrumBands)
            val unchanged =
                currentFrame.level == quantizedLevel &&
                    currentFrame.peak == quantizedPeak &&
                    currentFrame.meterSegments == quantizedMeterSegments &&
                    sameSpectrum
            if (!unchanged) {
                CaptureUiStore.publishLiveFrame(
                    level = quantizedLevel,
                    peak = quantizedPeak,
                    meterSegments = quantizedMeterSegments,
                    spectrumBands = if (sameSpectrum) currentFrame.spectrumBands else quantizedSpectrumBands
                )
            }
        } else {
            var shouldPublishDirectFrame = false
            var directSpectrumBands = quantizedSpectrumBands
            CaptureUiStore.update {
                val sameSpectrum = it.spectrumBands.contentEquals(quantizedSpectrumBands)
                val unchanged =
                    it.level == quantizedLevel &&
                        it.peak == quantizedPeak &&
                        it.meterSegments == quantizedMeterSegments &&
                        it.isCapturing &&
                        it.activeMode == mode &&
                        sameSpectrum
                if (unchanged) {
                    it
                } else {
                    shouldPublishDirectFrame = true
                    directSpectrumBands = if (sameSpectrum) it.spectrumBands else quantizedSpectrumBands
                    it.copy(
                        level = quantizedLevel,
                        peak = quantizedPeak,
                        meterSegments = quantizedMeterSegments,
                        spectrumBands = if (sameSpectrum) it.spectrumBands else quantizedSpectrumBands,
                        isCapturing = true,
                        activeMode = mode
                    )
                }
            }
            if (shouldPublishDirectFrame) {
                CaptureUiStore.publishDirectLiveFrame(
                    level = quantizedLevel,
                    peak = quantizedPeak,
                    meterSegments = quantizedMeterSegments,
                    spectrumBands = directSpectrumBands
                )
            }
        }
    }

    private fun shouldPublishUiFrame(mode: String): Boolean {
        return CaptureUiStore.shouldPublishLiveUiFrames()
    }

    private fun quantizeUiValue(value: Float, steps: Float): Float {
        if (steps <= 0f) return value.coerceIn(0f, 1f)
        return ((value.coerceIn(0f, 1f) * steps).roundToInt() / steps).coerceIn(0f, 1f)
    }

    private fun quantizeSpectrumBandsForUi(spectrumBands: FloatArray): FloatArray {
        if (spectrumBands.isEmpty()) return spectrumBands
        return FloatArray(spectrumBands.size) { index ->
            quantizeUiValue(spectrumBands[index], UI_SPECTRUM_QUANTIZATION_STEPS)
        }
    }

    private fun registerAudioDeviceCallback() {
        if (audioDeviceCallbackRegistered) return
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
            audioDeviceCallbackRegistered = true
            AppLogger.i(TAG, "Audio device callback registered")
        } catch (error: Throwable) {
            AppLogger.w(TAG, "registerAudioDeviceCallback failed", error)
        }
    }

    private fun unregisterAudioDeviceCallback() {
        if (!audioDeviceCallbackRegistered) return
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "unregisterAudioDeviceCallback failed", error)
        }
        audioDeviceCallbackRegistered = false
    }

    private fun handleAudioRouteChanged(reason: String, devices: Array<AudioDeviceInfo>) {
        val sinks = devices.filter { it.isSink }.toTypedArray()
        if (sinks.isEmpty()) return
        val nextSignature = AudioRouteDiagnostics.outputSignature(this)
        if (nextSignature == lastAudioRouteSignature) {
            AppLogger.i(TAG, "Audio route $reason callback matched existing signature; ignoring")
            return
        }
        lastAudioRouteSignature = nextSignature
        AppLogger.i(
            TAG,
            "Audio route $reason: ${AudioRouteDiagnostics.describeDevices(sinks)} ${AudioRouteDiagnostics.snapshot(this)}"
        )
        if (!shouldRestartVisualizerForRouteChange()) return
        if (SystemClock.uptimeMillis() < suppressRouteRestartUntilMs) {
            AppLogger.i(TAG, "Audio route restart suppressed during visualizer startup window")
            return
        }
        mainHandler.removeCallbacks(restartVisualizerForRouteChangeRunnable)
        mainHandler.postDelayed(restartVisualizerForRouteChangeRunnable, 350L)
    }

    private fun shouldRestartVisualizerForRouteChange(): Boolean {
        val state = CaptureUiStore.state
        return state.isCapturing && state.activeMode == ACTIVE_MODE_VISUALIZER
    }

    private fun applyLatencyPresetForCurrentRoute(reason: String) {
        val saved = SettingsPreferences.load(this)
        val bluetoothOutputActive = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
        val resolved = saved.withResolvedLatency(bluetoothOutputActive)
        val nextLatencyMs = resolved.latencyMs
        if (latencyMs != nextLatencyMs) {
            AppLogger.i(
                TAG,
                "Latency applied on route $reason. bluetooth=$bluetoothOutputActive latencyMs=$nextLatencyMs"
            )
            latencyMs = nextLatencyMs
            pendingLevelFrames.clear()
            mainHandler.removeCallbacks(latencyDrainRunnable)
        }
        CaptureUiStore.update {
            it.copy(
                latencyMs = latencyMs,
                defaultOutputLatencyMs = resolved.defaultOutputLatencyMs,
                bluetoothLatencyMs = resolved.bluetoothLatencyMs,
                latencyAutoSwitchEnabled = resolved.latencyAutoSwitchEnabled,
                isBluetoothOutputActive = bluetoothOutputActive
            )
        }
    }

    private fun visualizerStartMaxAttempts(): Int {
        return if (AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this) && AudioRouteDiagnostics.isMusicActive(this)) {
            6
        } else {
            4
        }
    }

    private fun visualizerRetryDelayMs(attempt: Int): Long {
        return if (AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this) && AudioRouteDiagnostics.isMusicActive(this)) {
            700L * attempt
        } else {
            160L * attempt
        }
    }

    private fun visualizerRouteRestartSuppressionMs(): Long {
        return if (AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this) && AudioRouteDiagnostics.isMusicActive(this)) {
            4000L
        } else {
            1500L
        }
    }

    private fun safeStopForeground() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "stopForeground failed (already stopped or invalid state)", error)
        }
    }

    private fun startServiceNotification(label: String, mediaProjection: Boolean = false) {
        val notification = buildNotification(label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (mediaProjection)
                ServiceInfoCompat.mediaProjectionType()
            else
                ServiceInfoCompat.mediaPlaybackType()
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(label: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.notification_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_alert_channel_description)
        }
        manager.createNotificationChannels(listOf(serviceChannel, alertChannel))
    }
}

private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

private object ServiceInfoCompat {
    fun mediaProjectionType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfoTypes.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
    }
    fun mediaPlaybackType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfoTypes.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }
}

private object ServiceInfoTypes {
    const val FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION = 32
    const val FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 2
    const val FOREGROUND_SERVICE_TYPE_MICROPHONE = 128
}
