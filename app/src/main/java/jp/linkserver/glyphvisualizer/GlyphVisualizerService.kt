package jp.linkserver.glyphvisualizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.Common
import jp.linkserver.glyphvisualizer.audio.AudioPlaybackVisualizer
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
import jp.linkserver.glyphvisualizer.audio.OutputMixVisualizer
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphOutputController
import jp.linkserver.glyphvisualizer.glyph.GlyphLightController
import jp.linkserver.glyphvisualizer.glyph.GlyphMatrixController
import kotlin.math.roundToLong

class GlyphVisualizerService : Service() {
    companion object {
        private const val TAG = "GlyphVisualizerSvc"
        private const val CHANNEL_ID = "glyph_visualizer"
        private const val NOTIFICATION_ID = 42

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
        private const val EXTRA_BINARY_MODE = "extra_binary_mode"
        private const val EXTRA_LEVEL_AUTO_SCALE = "extra_level_auto_scale"
        private const val EXTRA_SPECTRUM_AUTO_SCALE = "extra_spectrum_auto_scale"
        private const val EXTRA_ALL_BRIGHTNESS_AUTO_SCALE = "extra_all_brightness_auto_scale"
        private const val EXTRA_AUTO_SCALE_WINDOW_SECONDS = "extra_auto_scale_window_seconds"
        private const val EXTRA_LATENCY_MS = "extra_latency_ms"
        private const val EXTRA_TURN_OFF_WHEN_BACK_DOWN = "extra_turn_off_when_back_down"
        private const val BACK_DOWN_ENABLE_Z_THRESHOLD = 8.5f
        private const val BACK_DOWN_DISABLE_Z_THRESHOLD = 7.5f
        private const val ACTIVE_MODE_VISUALIZER = "VISUALIZER"
        private const val ACTIVE_MODE_MEDIA_PROJECTION = "MEDIA PROJECTION"
        private const val ACTIVE_MODE_IDLE = "IDLE"

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
            binaryMode: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            latencyMs: Float,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = 1.8f
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
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
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
            binaryMode: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            latencyMs: Float,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = 1.8f
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
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
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
            binaryMode: Boolean,
            levelAutoScale: Boolean,
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            autoScaleWindowSeconds: Float,
            latencyMs: Float,
            turnOffWhenBackDown: Boolean,
            outputGamma: Float = Float.NaN
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
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                putExtra(EXTRA_LATENCY_MS, latencyMs)
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
    private var reverseDirection = true
    private var peakHoldEnabled = true
    private var glyphMode = GlyphPatternRegistry.P2_C1_LINEAR
    private var binaryMode = false
    private var levelAutoScale = false
    private var spectrumAutoScale = false
    private var allBrightnessAutoScale = false
    private var autoScaleWindowSeconds = 30f
    private var latencyMs = 0f
    private var turnOffWhenBackDown = false
    private var isBackDownSuppressed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var visualizerStartRequestId = 0
    private var visualizerStartActionAtMs = 0L
    private var audioDeviceCallbackRegistered = false
    private var lastAudioRouteSignature: String? = null
    private var suppressRouteRestartUntilMs = 0L
    private data class DelayedLevelFrame(
        val dueAtMs: Long,
        val level: Float,
        val peak: Float,
        val mode: String,
        val lowEnergy: Float,
        val highEnergy: Float,
        val leftLevel: Float,
        val rightLevel: Float,
        val spectrumBands: FloatArray
    )
    private val pendingLevelFrames = ArrayDeque<DelayedLevelFrame>()
    private val latencyDrainRunnable = Runnable { drainPendingLevelFrames() }
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
        glyphController = if (Common.is23112() || Common.is25111p()) {
            GlyphMatrixController(this) { status ->
                CaptureUiStore.update { it.copy(statusText = status) }
            }
        } else {
            GlyphLightController(this) { status ->
                CaptureUiStore.update { it.copy(statusText = status) }
            }
        }
        audioPlaybackVisualizer = AudioPlaybackVisualizer(this)
        outputMixVisualizer = OutputMixVisualizer(this)
        glyphController.bind()
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
                    binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                    levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                    spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                    allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                    autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                    latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                    turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                    glyphController.setReverseDirection(reverseDirection)
                    glyphController.setGlyphMode(glyphMode)
                    glyphController.setBinaryMode(binaryMode)
                    glyphController.setOutputGamma(outputGamma)
                    glyphController.setLevelAutoScaleEnabled(levelAutoScale)
                    glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                    glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                    glyphController.setAutoScaleWindowSeconds(autoScaleWindowSeconds)
                    updateBackDownSensorState()
                    AppLogger.i(
                        TAG,
                        "ACTION_START_VISUALIZER received: requestId=$visualizerStartRequestId glyphMode=$glyphMode btLikely=${
                            AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this)
                        } musicActive=${AudioRouteDiagnostics.isMusicActive(this)}"
                    )
                    startServiceNotification(getString(R.string.notification_mode_visualizer))
                    AppLogger.i(
                        TAG,
                        "Foreground notification posted for visualizer: requestId=$visualizerStartRequestId elapsedMs=${SystemClock.elapsedRealtime() - actionReceivedAt}"
                    )
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
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                glyphController.setReverseDirection(reverseDirection)
                glyphController.setGlyphMode(glyphMode)
                glyphController.setBinaryMode(binaryMode)
                glyphController.setOutputGamma(outputGamma)
                glyphController.setLevelAutoScaleEnabled(levelAutoScale)
                glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                glyphController.setAutoScaleWindowSeconds(autoScaleWindowSeconds)
                updateBackDownSensorState()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtraCompat<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && data != null) {
                    startServiceNotification(getString(R.string.notification_mode_media_projection), mediaProjection = true)
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
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                levelAutoScale = intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, levelAutoScale)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                autoScaleWindowSeconds = intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, autoScaleWindowSeconds)
                latencyMs = intent.getFloatExtra(EXTRA_LATENCY_MS, latencyMs)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                glyphController.setReverseDirection(reverseDirection)
                glyphController.setGlyphMode(glyphMode)
                glyphController.setBinaryMode(binaryMode)
                glyphController.setOutputGamma(outputGamma)
                glyphController.setLevelAutoScaleEnabled(levelAutoScale)
                glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                glyphController.setAutoScaleWindowSeconds(autoScaleWindowSeconds)
                updateBackDownSensorState()
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
                        binaryMode = binaryMode,
                        levelAutoScale = levelAutoScale,
                        spectrumAutoScale = spectrumAutoScale,
                        autoScaleWindowSeconds = autoScaleWindowSeconds,
                        allBrightnessAutoScale = allBrightnessAutoScale,
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
                            binaryMode = binaryMode,
                            levelAutoScale = levelAutoScale,
                            spectrumAutoScale = spectrumAutoScale,
                            allBrightnessAutoScale = allBrightnessAutoScale,
                            autoScaleWindowSeconds = autoScaleWindowSeconds,
                            turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
                notifyTile()
            },
            onLevelChanged = { level, peak, lowEnergy, highEnergy, leftLevel, rightLevel, spectrumBands ->
                publishLevel(
                    level,
                    peak,
                    "VISUALIZER",
                    lowEnergy,
                    highEnergy,
                    leftLevel,
                    rightLevel,
                    spectrumBands
                )
            },
            onStartFailed = {
                val maxAttempts = visualizerStartMaxAttempts()
                if (requestId == visualizerStartRequestId && attempt < maxAttempts) {
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
                } else {
                    AppLogger.e(
                        TAG,
                        "Visualizer async start exhausted retries: requestId=$requestId attempts=$attempt elapsedSinceActionMs=${
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
                            binaryMode = binaryMode,
                            levelAutoScale = levelAutoScale,
                            spectrumAutoScale = spectrumAutoScale,
                            allBrightnessAutoScale = allBrightnessAutoScale,
                            autoScaleWindowSeconds = autoScaleWindowSeconds,
                            turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
                notifyTile()
            },
            onLevelChanged = { level, peak, lowEnergy, highEnergy, leftLevel, rightLevel, spectrumBands ->
                publishLevel(
                    level,
                    peak,
                    "MEDIA PROJECTION",
                    lowEnergy,
                    highEnergy,
                    leftLevel,
                    rightLevel,
                    spectrumBands
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
        spectrumBands: FloatArray
    ) {
        enqueueDelayedLevelFrame(
            level = level,
            peak = peak,
            mode = mode,
            lowEnergy = lowEnergy,
            highEnergy = highEnergy,
            leftLevel = leftLevel,
            rightLevel = rightLevel,
            spectrumBands = spectrumBands
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
        spectrumBands: FloatArray
    ) {
        val dueAtMs = SystemClock.uptimeMillis() + latencyMs.coerceIn(0f, 500f).roundToLong()
        pendingLevelFrames.addLast(
            DelayedLevelFrame(
                dueAtMs = dueAtMs,
                level = level,
                peak = peak,
                mode = mode,
                lowEnergy = lowEnergy,
                highEnergy = highEnergy,
                leftLevel = leftLevel,
                rightLevel = rightLevel,
                spectrumBands = spectrumBands.copyOf()
            )
        )
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
        CaptureUiStore.update {
            it.copy(
                level = frame.level,
                peak = frame.peak,
                meterSegments = (frame.level * 16f).toInt().coerceIn(0, 16),
                spectrumBands = frame.spectrumBands,
                isCapturing = true,
                activeMode = frame.mode,
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
                    binaryMode = binaryMode,
                levelAutoScale = levelAutoScale,
                    spectrumAutoScale = spectrumAutoScale,
                    allBrightnessAutoScale = allBrightnessAutoScale,
                    autoScaleWindowSeconds = autoScaleWindowSeconds,
                    latencyMs = latencyMs,
                    turnOffWhenBackDown = turnOffWhenBackDown
            )
        }
        if (isBackDownSuppressed) {
            try {
                glyphController.turnOff()
            } catch (error: Throwable) {
                AppLogger.w(TAG, "glyphController.turnOff failed while back-down suppressing", error)
            }
            return
        }
        glyphController.updateAnalysis(
            frame.lowEnergy,
            frame.highEnergy,
            frame.leftLevel,
            frame.rightLevel,
            frame.spectrumBands
        )
        glyphController.updateLevel(frame.level)
    }

    private fun stopRunningCapture(clearStatus: Boolean) {
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
        try {
            glyphController.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "glyphController.turnOff failed", error)
        }
        mainHandler.removeCallbacks(latencyDrainRunnable)
        pendingLevelFrames.clear()
        CaptureUiStore.update {
            it.copy(
                level = 0f,
                peak = 0f,
                meterSegments = 0,
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
                binaryMode = binaryMode,
                levelAutoScale = levelAutoScale,
                spectrumAutoScale = spectrumAutoScale,
                allBrightnessAutoScale = allBrightnessAutoScale,
                autoScaleWindowSeconds = autoScaleWindowSeconds,
                latencyMs = latencyMs,
                turnOffWhenBackDown = turnOffWhenBackDown
            )
        }
        isBackDownSuppressed = false
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
            stopRunningCapture(clearStatus = false)
        } catch (error: Throwable) {
            AppLogger.e(TAG, "stopRunningCapture failed in stopCapture", error)
        }
        CaptureUiStore.update { it.copy(statusText = status) }
        notifyTile()
    }

    private fun notifyTile() {
        try {
            GlyphTileService.refresh(this)
        } catch (_: Exception) {}
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
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
