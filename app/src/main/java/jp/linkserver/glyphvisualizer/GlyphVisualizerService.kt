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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.Common
import jp.linkserver.glyphvisualizer.audio.AudioPlaybackVisualizer
import jp.linkserver.glyphvisualizer.audio.OutputMixVisualizer
import jp.linkserver.glyphvisualizer.glyph.GlyphOutputController
import jp.linkserver.glyphvisualizer.glyph.GlyphPhone2Controller
import jp.linkserver.glyphvisualizer.glyph.GlyphPhone3MatrixController

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
        private const val EXTRA_TONE_FOCUS = "extra_tone_focus"
        private const val EXTRA_SMOOTHING = "extra_smoothing"
        private const val EXTRA_SMOOTHING_BALANCE = "extra_smoothing_balance"
        private const val EXTRA_REVERSE_DIRECTION = "extra_reverse_direction"
        private const val EXTRA_PEAK_HOLD_ENABLED = "extra_peak_hold_enabled"
        private const val EXTRA_GLYPH_MODE = "extra_glyph_mode"
        private const val EXTRA_BINARY_MODE = "extra_binary_mode"
        private const val EXTRA_SPECTRUM_AUTO_SCALE = "extra_spectrum_auto_scale"
        private const val EXTRA_ALL_BRIGHTNESS_AUTO_SCALE = "extra_all_brightness_auto_scale"
        private const val EXTRA_TURN_OFF_WHEN_BACK_DOWN = "extra_turn_off_when_back_down"
        private const val BACK_DOWN_ENABLE_Z_THRESHOLD = 8.5f
        private const val BACK_DOWN_DISABLE_Z_THRESHOLD = 7.5f

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
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            turnOffWhenBackDown: Boolean
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_START_VISUALIZER
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
            }
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                AppLogger.e(TAG, "startVisualizer failed to start service", error)
                val msg = "Visualizer mode could not start: ${error.message ?: "unknown error"}"
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
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            turnOffWhenBackDown: Boolean
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_START_MEDIA_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                putExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
            }
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                AppLogger.e(TAG, "startMediaProjection failed to start service", error)
                val msg = "MediaProjection mode could not start: ${error.message ?: "unknown error"}"
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
            spectrumAutoScale: Boolean,
            allBrightnessAutoScale: Boolean,
            turnOffWhenBackDown: Boolean
        ) {
            val intent = Intent(context, GlyphVisualizerService::class.java).apply {
                action = ACTION_UPDATE_SENSITIVITY
                putExtra(EXTRA_SENSITIVITY, sensitivity)
                putExtra(EXTRA_NOISE_GATE, noiseGate)
                putExtra(EXTRA_DYNAMICS, dynamics)
                putExtra(EXTRA_TONE_FOCUS, toneFocus)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                putExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                putExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                putExtra(EXTRA_GLYPH_MODE, glyphMode)
                putExtra(EXTRA_BINARY_MODE, binaryMode)
                putExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                putExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
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
    private var toneFocus = -0.2f
    private var smoothing = 0.55f
    private var smoothingBalance = 0f
    private var reverseDirection = true
    private var peakHoldEnabled = true
    private var glyphMode = "C1_LINEAR"
    private var binaryMode = false
    private var spectrumAutoScale = false
    private var allBrightnessAutoScale = false
    private var turnOffWhenBackDown = false
    private var isBackDownSuppressed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var visualizerStartRequestId = 0
    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var gravitySensor: Sensor? = null
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
            GlyphPhone3MatrixController(this) { status ->
                CaptureUiStore.update { it.copy(statusText = status) }
            }
        } else {
            GlyphPhone2Controller(this) { status ->
                CaptureUiStore.update { it.copy(statusText = status) }
            }
        }
        audioPlaybackVisualizer = AudioPlaybackVisualizer(this)
        outputMixVisualizer = OutputMixVisualizer()
        glyphController.bind()
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VISUALIZER -> {
                try {
                    visualizerStartRequestId += 1
                    sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                    noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                    dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                    toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                    smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                    smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                    reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                    peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                    glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                    binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                    spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                    allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                    turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                    glyphController.setReverseDirection(reverseDirection)
                    glyphController.setGlyphMode(glyphMode)
                    glyphController.setBinaryMode(binaryMode)
                    glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                    glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                    updateBackDownSensorState()
                    startServiceNotification("Visualizer mode")
                    startVisualizerMode(requestId = visualizerStartRequestId, attempt = 1)
                } catch (error: SecurityException) {
                    // パーミッション不足は即座に失敗（リトライ不要）
                    val msg = "Permission denied: ${error.message ?: "unknown"}"
                    AppLogger.e(TAG, "ACTION_START_VISUALIZER permission denied", error)
                    val logMsg = "No capture start failed: $msg"
                    CaptureUiStore.update { it.copy(statusText = logMsg, logMessage = logMsg) }
                    safeStopForeground()
                    stopSelf()
                } catch (error: Throwable) {
                    AppLogger.e(TAG, "ACTION_START_VISUALIZER failed", error)
                    stopCapture("Visualizer mode crashed during startup: ${error.message ?: "unknown error"}")
                    safeStopForeground()
                    stopSelf()
                }
            }

            ACTION_START_MEDIA_PROJECTION -> {
                visualizerStartRequestId += 1
                sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                glyphController.setReverseDirection(reverseDirection)
                glyphController.setGlyphMode(glyphMode)
                glyphController.setBinaryMode(binaryMode)
                glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                updateBackDownSensorState()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtraCompat<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && data != null) {
                    startServiceNotification("MediaProjection mode", mediaProjection = true)
                    startMediaProjectionMode(resultCode, data)
                } else {
                    stopCapture("MediaProjection data was missing.")
                    stopSelf()
                }
            }

            ACTION_UPDATE_SENSITIVITY -> {
                sensitivity = intent.getFloatExtra(EXTRA_SENSITIVITY, sensitivity)
                noiseGate = intent.getFloatExtra(EXTRA_NOISE_GATE, noiseGate)
                dynamics = intent.getFloatExtra(EXTRA_DYNAMICS, dynamics)
                toneFocus = intent.getFloatExtra(EXTRA_TONE_FOCUS, toneFocus)
                smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, smoothing)
                smoothingBalance = intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, smoothingBalance)
                reverseDirection = intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, reverseDirection)
                peakHoldEnabled = intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, peakHoldEnabled)
                glyphMode = intent.getStringExtra(EXTRA_GLYPH_MODE) ?: glyphMode
                binaryMode = intent.getBooleanExtra(EXTRA_BINARY_MODE, binaryMode)
                spectrumAutoScale = intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, spectrumAutoScale)
                allBrightnessAutoScale = intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, allBrightnessAutoScale)
                turnOffWhenBackDown = intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, turnOffWhenBackDown)
                glyphController.setReverseDirection(reverseDirection)
                glyphController.setGlyphMode(glyphMode)
                glyphController.setBinaryMode(binaryMode)
                glyphController.setSpectrumAutoScaleEnabled(spectrumAutoScale)
                glyphController.setAllBrightnessAutoScaleEnabled(allBrightnessAutoScale)
                updateBackDownSensorState()
                CaptureUiStore.update {
                    it.copy(
                        sensitivity = sensitivity,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        toneFocus = toneFocus,
                        smoothing = smoothing,
                        smoothingBalance = smoothingBalance,
                        reverseDirection = reverseDirection,
                        peakHoldEnabled = peakHoldEnabled,
                        glyphMode = glyphMode,
                        binaryMode = binaryMode,
                        spectrumAutoScale = spectrumAutoScale,
                        allBrightnessAutoScale = allBrightnessAutoScale,
                        turnOffWhenBackDown = turnOffWhenBackDown
                    )
                }
            }

            ACTION_STOP -> {
                visualizerStartRequestId += 1
                try {
                    stopCapture("Capture stopped. Ready for the next session.")
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
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startVisualizerMode(requestId: Int, attempt: Int) {
        if (requestId != visualizerStartRequestId) return
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
                CaptureUiStore.update {
                    it.copy(
                        statusText = status,
                        isCapturing = true,
                        activeMode = "VISUALIZER",
                        sensitivity = sensitivity,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        toneFocus = toneFocus,
                        smoothing = smoothing,
                        reverseDirection = reverseDirection,
                            peakHoldEnabled = peakHoldEnabled,
                            glyphMode = glyphMode,
                            binaryMode = binaryMode,
                            spectrumAutoScale = spectrumAutoScale,
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
            if (requestId == visualizerStartRequestId && attempt < 3) {
                val nextAttempt = attempt + 1
                val retryMs = 60L * attempt
                CaptureUiStore.update {
                    it.copy(statusText = "Visualizer start retrying ($nextAttempt/3)...")
                }
                mainHandler.postDelayed(
                    { startVisualizerMode(requestId = requestId, attempt = nextAttempt) },
                    retryMs
                )
                return
            }
            val msg = "Visualizer mode was unavailable. Try the MediaProjection option."
            val logMsg = "No capture failed after $attempt attempt(s). Consider MediaProjection mode."
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
                        activeMode = "MEDIA PROJECTION",
                        sensitivity = sensitivity,
                        noiseGate = noiseGate,
                        dynamics = dynamics,
                        toneFocus = toneFocus,
                        smoothing = smoothing,
                        reverseDirection = reverseDirection,
                            peakHoldEnabled = peakHoldEnabled,
                            glyphMode = glyphMode,
                            binaryMode = binaryMode,
                            spectrumAutoScale = spectrumAutoScale,
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
            stopCapture("MediaProjection mode could not start.")
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
        CaptureUiStore.update {
            it.copy(
                level = level,
                peak = peak,
                meterSegments = (level * 16f).toInt().coerceIn(0, 16),
                spectrumBands = spectrumBands,
                isCapturing = true,
                activeMode = mode,
                sensitivity = sensitivity,
                noiseGate = noiseGate,
                dynamics = dynamics,
                toneFocus = toneFocus,
                smoothing = smoothing,
                smoothingBalance = smoothingBalance,
                reverseDirection = reverseDirection,
                    peakHoldEnabled = peakHoldEnabled,
                    glyphMode = glyphMode,
                    binaryMode = binaryMode,
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
        glyphController.updateAnalysis(lowEnergy, highEnergy, leftLevel, rightLevel, spectrumBands)
        glyphController.updateLevel(level)
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
        CaptureUiStore.update {
            it.copy(
                level = 0f,
                peak = 0f,
                meterSegments = 0,
                isCapturing = false,
                activeMode = "IDLE",
                statusText = if (clearStatus) "Capture stopped. Ready for the next session." else it.statusText,
                sensitivity = sensitivity,
                noiseGate = noiseGate,
                dynamics = dynamics,
                toneFocus = toneFocus,
                smoothing = smoothing,
                smoothingBalance = smoothingBalance,
                reverseDirection = reverseDirection,
                    peakHoldEnabled = peakHoldEnabled,
                    glyphMode = glyphMode,
                    binaryMode = binaryMode,
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
            .setContentTitle("Glyph Peak Meter")
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
            "Glyph Visualizer",
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
