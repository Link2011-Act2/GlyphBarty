package jp.linkserver.glyphvisualizer.battery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.GlyphBatteryIndicatorSpec
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.MainActivity
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.SettingsPreferences
import jp.linkserver.glyphvisualizer.glyph.GlyphSdkSessionCoordinator
import jp.linkserver.glyphvisualizer.glyph.NothingOsGlyphSettings
import jp.linkserver.glyphvisualizer.glyph.NothingOsGlyphSettingsMonitor
import jp.linkserver.glyphvisualizer.glyph.NothingOsGlyphSettingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

class BatteryGlyphService : Service(), SensorEventListener {
    companion object {
        private const val TAG = "BatteryGlyphService"
        private const val CHANNEL_ID = "battery_glyph_monitor"
        private const val NOTIFICATION_ID = 47
        private const val SESSION_READY_TIMEOUT_MS = 4_000L
        private const val BATTERY_CONFIRM_RETRY_MS = 500L
        private const val SENSOR_ALPHA = 0.8f

        fun syncEnabledState(context: Context, enabled: Boolean) {
            val supported = GlyphDeviceCatalog.currentBatteryIndicatorSpecOrNull() != null
            if (enabled && supported) {
                runCatching {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, BatteryGlyphService::class.java)
                    )
                }.onFailure { error ->
                    AppLogger.w(TAG, "Could not start Battery Glyph monitoring", error)
                }
            } else {
                if (enabled && !supported) {
                    AppLogger.w(TAG, "Battery Glyph is enabled but this device is unsupported")
                }
                context.stopService(Intent(context, BatteryGlyphService::class.java))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(android.os.Looper.getMainLooper())
    private val faceDownDetector = BatteryFaceDownDetector()
    private val shakeDetector = BatteryShakeDetector()
    private val gravity = FloatArray(3)
    private var hasGravitySample = false
    private var charging = false
    private var sensorsRegistered = false
    private var glyphOutputAllowed = true
    private var animationJob: Job? = null
    private var activeSession: BatteryGlyphSdkSession? = null
    private var activeOwnerToken: Any? = null
    private var batteryIndicatorSpec: GlyphBatteryIndicatorSpec? = null
    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    private var linearAccelerationSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private lateinit var nothingOsGlyphSettingsMonitor: NothingOsGlyphSettingsMonitor

    private val confirmChargingRunnable = Runnable { refreshChargingState(allowDisplay = true) }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            AppLogger.i(TAG, "Battery event received: action=$action")
            refreshChargingState(allowDisplay = true)
            if (action == Intent.ACTION_POWER_CONNECTED && !charging) {
                mainHandler.postDelayed(
                    confirmChargingRunnable,
                    BATTERY_CONFIRM_RETRY_MS
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        createNotificationChannel()
        startMonitoringForeground(charging = false)
        glyphOutputAllowed = NothingOsGlyphSettings.currentState(this).outputAllowed

        batteryIndicatorSpec = GlyphDeviceCatalog.currentBatteryIndicatorSpecOrNull()
        if (batteryIndicatorSpec == null) {
            AppLogger.w(TAG, "Battery Glyph service stopped on unsupported device: ${Build.MODEL}")
            stopSelf()
            return
        }

        sensorManager = getSystemService(SensorManager::class.java)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Establish the baseline before registering: enabling the feature while already charging
        // must not masquerade as a new charging transition.
        charging = currentBatterySnapshot().charging
        registerBatteryReceiver()
        updateSensorMonitoring()
        updateNotification()
        nothingOsGlyphSettingsMonitor = NothingOsGlyphSettingsMonitor(
            context = this,
            onStateChanged = ::handleNothingOsGlyphSettingChanged
        )
        nothingOsGlyphSettingsMonitor.start()
        AppLogger.i(TAG, "Monitoring started: charging=$charging model=${Build.MODEL}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val enabled = SettingsPreferences.load(this).batteryGlyphEnabled
        if (!enabled || batteryIndicatorSpec == null) {
            AppLogger.i(TAG, "Monitoring stopped because the setting is off or unsupported")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(confirmChargingRunnable)
        if (::nothingOsGlyphSettingsMonitor.isInitialized) {
            nothingOsGlyphSettingsMonitor.stop()
        }
        runCatching { unregisterReceiver(batteryReceiver) }
        stopSensorMonitoring()
        cancelActiveDisplay("service destroyed")
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        AppLogger.i(TAG, "Monitoring stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (!charging) return
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                copyVector(event.values, gravity)
                hasGravitySample = true
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                evaluateShake(vectorMagnitude(event.values), event.timestamp / 1_000_000L)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                faceDownDetector.onSample(
                    xAcceleration = event.values[0],
                    yAcceleration = event.values[1],
                    zAcceleration = event.values[2],
                    timestampMs = event.timestamp / 1_000_000L
                )
                if (gravitySensor == null) {
                    for (index in 0..2) {
                        gravity[index] = SENSOR_ALPHA * gravity[index] +
                            (1f - SENSOR_ALPHA) * event.values[index]
                    }
                    hasGravitySample = true
                }
                if (linearAccelerationSensor == null && hasGravitySample) {
                    val x = event.values[0] - gravity[0]
                    val y = event.values[1] - gravity[1]
                    val z = event.values[2] - gravity[2]
                    evaluateShake(sqrt(x * x + y * y + z * z), event.timestamp / 1_000_000L)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun evaluateShake(linearMagnitude: Float, timestampMs: Long) {
        if (!hasGravitySample) return
        if (
            shakeDetector.onSample(
                charging = charging,
                faceDown = faceDownDetector.isFaceDown,
                linearAccelerationMagnitude = linearMagnitude,
                timestampMs = timestampMs
            )
        ) {
            AppLogger.i(TAG, "Face-down shake detected while charging")
            requestBatteryDisplay("shake")
        }
    }

    private fun refreshChargingState(allowDisplay: Boolean) {
        val wasCharging = charging
        charging = currentBatterySnapshot().charging
        if (wasCharging != charging) {
            AppLogger.i(TAG, "Charging state changed: $wasCharging -> $charging")
            updateSensorMonitoring()
            updateNotification()
            if (!charging) cancelActiveDisplay("charging ended")
        }
        if (allowDisplay && !wasCharging && charging) {
            requestBatteryDisplay("charging started")
        }
    }

    private fun requestBatteryDisplay(source: String) {
        if (!charging || !glyphOutputAllowed || animationJob?.isActive == true) return
        val latest = currentBatterySnapshot()
        if (!latest.charging) {
            refreshChargingState(allowDisplay = false)
            return
        }
        val spec = batteryIndicatorSpec ?: return
        val percent = latest.percent
        val ownerToken = Any()
        val claimed = GlyphSdkSessionCoordinator.tryClaimBattery(ownerToken)
        if (!claimed) {
            AppLogger.i(TAG, "Battery display skipped because another battery display owns the Glyph SDK")
            return
        }

        val sdkSession = BatteryGlyphSdkSession(applicationContext, spec)
        activeOwnerToken = ownerToken
        activeSession = sdkSession
        animationJob = serviceScope.launch {
            try {
                val ready = withTimeoutOrNull(SESSION_READY_TIMEOUT_MS) {
                    sdkSession.open()
                } == true
                if (!ready) {
                    AppLogger.w(TAG, "Glyph session was not ready for battery display")
                    return@launch
                }
                AppLogger.i(TAG, "Battery display started: source=$source percent=$percent")
                playBatteryAnimation(sdkSession, spec, percent)
                AppLogger.i(TAG, "Battery display finished")
            } catch (error: Throwable) {
                if (animationJob?.isActive == true) {
                    AppLogger.w(TAG, "Battery display failed", error)
                }
            } finally {
                sdkSession.close()
                GlyphSdkSessionCoordinator.releaseBattery(ownerToken)
                if (activeOwnerToken === ownerToken) {
                    activeOwnerToken = null
                    activeSession = null
                    animationJob = null
                }
            }
        }
    }

    private suspend fun playBatteryAnimation(
        sdkSession: BatteryGlyphSdkSession,
        spec: GlyphBatteryIndicatorSpec,
        percent: Int
    ) {
        val targetCount = BatteryGlyphLogic.litChannelCount(
            batteryPercent = percent,
            channelCount = spec.orderedChannels.size
        )
        val durationMs = BatteryGlyphLogic.animationDurationMs(percent)
        animateCount(sdkSession, spec, from = 0, to = targetCount, durationMs = durationMs)
        delay(BatteryGlyphLogic.HOLD_DURATION_MS)
        animateCount(sdkSession, spec, from = targetCount, to = 0, durationMs = durationMs)
        sdkSession.send(IntArray(spec.frameChannelCount))
    }

    private suspend fun animateCount(
        sdkSession: BatteryGlyphSdkSession,
        spec: GlyphBatteryIndicatorSpec,
        from: Int,
        to: Int,
        durationMs: Long
    ) {
        if (durationMs <= 0L || from == to) {
            sdkSession.send(BatteryGlyphLogic.frameForLitCount(spec, to))
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
        var lastCount = Int.MIN_VALUE
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            val rawCount = from + (to - from) * fraction
            val count = if (to >= from) floor(rawCount).toInt() else ceil(rawCount).toInt()
            if (count != lastCount) {
                sdkSession.send(BatteryGlyphLogic.frameForLitCount(spec, count))
                lastCount = count
            }
            if (fraction >= 1f) break
            delay(BatteryGlyphLogic.FRAME_INTERVAL_MS.coerceAtMost(durationMs - elapsed))
        }
        if (lastCount != to) {
            sdkSession.send(BatteryGlyphLogic.frameForLitCount(spec, to))
        }
    }

    private fun cancelActiveDisplay(reason: String) {
        val job = animationJob
        if (job == null && activeSession == null) return
        AppLogger.i(TAG, "Battery display cancelled: $reason")
        job?.cancel()
        activeSession?.close()
        activeOwnerToken?.let(GlyphSdkSessionCoordinator::releaseBattery)
        animationJob = null
        activeSession = null
        activeOwnerToken = null
    }

    private fun updateSensorMonitoring() {
        if (charging && glyphOutputAllowed) startSensorMonitoring() else stopSensorMonitoring()
    }

    private fun handleNothingOsGlyphSettingChanged(state: NothingOsGlyphSettingState) {
        if (glyphOutputAllowed == state.outputAllowed) return
        glyphOutputAllowed = state.outputAllowed
        AppLogger.i(
            TAG,
            "Nothing OS Glyph sync changed: system=${state.systemEnabled} allowed=${state.outputAllowed}"
        )
        if (!glyphOutputAllowed) cancelActiveDisplay("Nothing OS Glyph setting is off")
        updateSensorMonitoring()
        updateNotification()
    }

    private fun startSensorMonitoring() {
        if (sensorsRegistered) return
        hasGravitySample = false
        gravity.fill(0f)
        faceDownDetector.reset()
        shakeDetector.reset()
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearAccelerationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorsRegistered = true
        AppLogger.i(
            TAG,
            "Shake sensors started: accelerometer=${accelerometerSensor != null} " +
                "gravity=${gravitySensor != null} linear=${linearAccelerationSensor != null}"
        )
    }

    private fun stopSensorMonitoring() {
        if (!sensorsRegistered || !::sensorManager.isInitialized) return
        sensorManager.unregisterListener(this)
        sensorsRegistered = false
        hasGravitySample = false
        faceDownDetector.reset()
        shakeDetector.reset()
        AppLogger.i(TAG, "Shake sensors stopped")
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BatteryManager.ACTION_CHARGING)
            addAction(BatteryManager.ACTION_DISCHARGING)
        }
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun currentBatterySnapshot(): BatterySnapshot {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val managerCharging = getSystemService(BatteryManager::class.java)?.isCharging == true
        val chargingStatus = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val fallbackPercent = getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?: 0
        val percent = if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else {
            fallbackPercent.coerceIn(0, 100)
        }
        return BatterySnapshot(
            charging = plugged != 0 && (managerCharging || chargingStatus),
            percent = percent
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.battery_glyph_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startMonitoringForeground(charging: Boolean) {
        val notification = buildNotification(charging)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(charging))
    }

    private fun buildNotification(charging: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.battery_glyph_notification_title))
            .setContentText(
                getString(
                    when {
                        !glyphOutputAllowed ->
                            R.string.battery_glyph_notification_system_glyph_disabled
                        charging -> R.string.battery_glyph_notification_charging
                        else -> R.string.battery_glyph_notification_waiting
                    }
                )
            )
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun copyVector(source: FloatArray, target: FloatArray) {
        for (index in 0..2) target[index] = source[index]
    }

    private fun vectorMagnitude(values: FloatArray): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }
}

private data class BatterySnapshot(
    val charging: Boolean,
    val percent: Int
)

private class BatteryGlyphSdkSession(
    context: Context,
    private val spec: GlyphBatteryIndicatorSpec
) {
    private val glyphManager = GlyphManager.getInstance(context.applicationContext)
    private var initialized = false
    private var sessionOpen = false
    private var active = false
    private var ready = CompletableDeferred<Boolean>()

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            if (!active) return
            try {
                if (!glyphManager.register(spec.sdkDeviceId)) {
                    ready.complete(false)
                    return
                }
                glyphManager.openSession()
                sessionOpen = true
                ready.complete(true)
            } catch (error: GlyphException) {
                AppLogger.w("BatteryGlyphSession", "Could not open Glyph session", error)
                ready.complete(false)
            } catch (error: Throwable) {
                AppLogger.w("BatteryGlyphSession", "Could not initialize Glyph session", error)
                ready.complete(false)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            sessionOpen = false
            ready.complete(false)
        }
    }

    suspend fun open(): Boolean {
        active = true
        initialized = true
        glyphManager.init(callback)
        return ready.await()
    }

    fun send(colors: IntArray) {
        if (!active || !sessionOpen) return
        glyphManager.setFrameColors(colors)
    }

    fun close() {
        if (!active && !initialized) return
        active = false
        if (!ready.isCompleted) ready.complete(false)
        if (sessionOpen) {
            runCatching { glyphManager.setFrameColors(IntArray(spec.frameChannelCount)) }
            runCatching { glyphManager.closeSession() }
            sessionOpen = false
        }
        if (initialized) {
            runCatching { glyphManager.unInit() }
            initialized = false
        }
    }
}
