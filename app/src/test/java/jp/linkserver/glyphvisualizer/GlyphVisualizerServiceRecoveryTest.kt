package jp.linkserver.glyphvisualizer

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Spatializer
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.google.common.collect.ImmutableList
import jp.linkserver.glyphvisualizer.audio.AudioPlaybackVisualizer
import jp.linkserver.glyphvisualizer.audio.AudioCaptureGeneration
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
import jp.linkserver.glyphvisualizer.audio.OutputMixVisualizer
import jp.linkserver.glyphvisualizer.audio.VisualizerCaptureHealth
import jp.linkserver.glyphvisualizer.glyph.GlyphOutputController
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.AudioDeviceInfoBuilder
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Collections

/** Exercises actual service timers and the polling worker with a controllable platform capture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [RecoveryVisualizerShadow::class, RecoverySpatializerShadow::class])
@LooperMode(LooperMode.Mode.PAUSED)
class GlyphVisualizerServiceRecoveryTest {
    private lateinit var service: GlyphVisualizerService
    private lateinit var originalState: CaptureUiState
    private var originalUiVisible = false
    private val controllerCalls = Collections.synchronizedList(mutableListOf<String>())
    private val mainLooper get() = shadowOf(Looper.getMainLooper())
    private fun field(name: String) = GlyphVisualizerService::class.java.getDeclaredField(name).apply { isAccessible = true }
    private val coordinator get() = field("captureSessionCoordinator").get(service) as CaptureSessionCoordinator
    private val pending get() = field("pendingVisualizerRecoveryRetryRunnable").get(service) as Runnable?
    private val output get() = field("outputMixVisualizer").get(service) as OutputMixVisualizer
    private val matrixHandler get() = field("matrixOutputHandler").get(service) as Handler?
    private fun captureSession(): Any = OutputMixVisualizer::class.java.getDeclaredField("activeSession")
        .apply { isAccessible = true }.get(output)!!
    private fun generation(session: Any) = session.javaClass.getDeclaredField("generation")
        .apply { isAccessible = true }.get(session) as AudioCaptureGeneration
    private fun workerHealth(): VisualizerCaptureHealth? {
        val session = captureSession()
        var health: VisualizerCaptureHealth? = null
        generation(session).runIfCurrent {
            health = session.javaClass.getDeclaredField("captureHealth").apply { isAccessible = true }
                .get(session) as VisualizerCaptureHealth
        }
        return health
    }
    private fun awaitWorkerHealthWithoutDispatch(expected: VisualizerCaptureHealth) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (workerHealth() != expected && System.nanoTime() < deadline) Thread.sleep(5)
        assertEquals(expected, workerHealth())
    }
    private fun startSilentWorkerAndAwaitRetry() {
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        mainLooper.idleFor(Duration.ofMillis(1_800))
        awaitCondition { pending != null }
    }
    private fun drainLoopers() {
        matrixHandler?.let { shadowOf(it.looper).idle() }
        mainLooper.idle()
    }
    private val capturedLevel: Float get() {
        val frame = field("latestLevelFrame").get(service) ?: return 0f
        return frame.javaClass.getDeclaredMethod("getLevel").apply { isAccessible = true }.invoke(frame) as Float
    }
    private fun health(health: VisualizerCaptureHealth, requestId: Int = coordinator.snapshot().requestId) {
        GlyphVisualizerService::class.java.getDeclaredMethod(
            "handleVisualizerCaptureHealth", Int::class.javaPrimitiveType, VisualizerCaptureHealth::class.java
        ).apply { isAccessible = true }.invoke(service, requestId, health)
    }
    private fun startWorker(recovering: Boolean = false) {
        GlyphVisualizerService::class.java.getDeclaredMethod(
            "startVisualizerMode", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }.invoke(service, coordinator.snapshot().requestId, 1, recovering)
    }
    private fun firePendingRecovery() {
        val retry = pending!!
        (field("mainHandler").get(service) as Handler).removeCallbacks(retry)
        retry.run()
    }
    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            drainLoopers()
            Thread.sleep(5)
        }
        drainLoopers()
        assertTrue("Worker/service condition timed out", condition())
    }

    @Before fun setUp() {
        originalState = CaptureUiStore.state
        originalUiVisible = CaptureUiStore.isUiVisible()
        RecoveryVisualizerShadow.reset()
        service = Robolectric.buildService(GlyphVisualizerService::class.java).get()
        field("captureConfig").set(service, defaultServiceCaptureConfig().copy(
            glyphMode = GlyphPatternRegistry.P2_C1_LINEAR,
            experimentalVisualizerSignalWatchdogEnabled = true
        ))
        field("outputMixVisualizer").set(service, OutputMixVisualizer(service))
        field("audioPlaybackVisualizer").set(service, AudioPlaybackVisualizer(service))
        field("notificationController").set(service, CaptureNotificationController(service))
        field("glyphController").set(service, Proxy.newProxyInstance(
            GlyphOutputController::class.java.classLoader, arrayOf(GlyphOutputController::class.java)
        ) { _, method, _ ->
            controllerCalls.add(method.name)
            when (method.returnType) {
                java.lang.Float.TYPE -> 0f
                java.lang.Boolean.TYPE -> false
                FloatArray::class.java -> FloatArray(0)
                else -> null
            }
        })
        coordinator.beginVisualizer(VisualizerStartSource.APP, 1L)
        shadowOf(service.getSystemService(AudioManager::class.java)).setIsMusicActive(true)
        CaptureUiStore.updateRuntime { it.copy(isCapturing = true, activeMode = "VISUALIZER") }
    }

    @After fun tearDown() {
        service.onStartCommand(CaptureIntentCommandCodec.encode(service, CaptureCommand.Stop), 0, 1)
        drainLoopers()
        (field("matrixOutputThread").get(service) as HandlerThread?)?.let {
            it.quitSafely()
            it.join(1_000)
        }
        CaptureUiStore.update { originalState }
        CaptureUiStore.setUiVisible(originalUiVisible)
    }

    @Test fun longSilencePastSixRetriesKeepsServiceAndWorkerAlive() {
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        repeat(8) {
            mainLooper.idleFor(Duration.ofMillis(1_800))
            awaitCondition { pending != null }
            assertTrue(CaptureUiStore.runtimeState.isCapturing)
            assertFalse(shadowOf(service).isStoppedBySelf)
            val previousReads = RecoveryVisualizerShadow.reads.get()
            awaitCondition { RecoveryVisualizerShadow.reads.get() > previousReads }
            val creations = RecoveryVisualizerShadow.creations.get()
            // Execute the scheduled recovery and the next worker's active callback.
            firePendingRecovery()
            awaitCondition { RecoveryVisualizerShadow.creations.get() > creations && CaptureUiStore.runtimeState.isCapturing }
        }
        assertTrue(coordinator.snapshot().requestId > 6)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test fun realSignalResumingDuringCooldownCancelsReconnectWithoutStoppingPolling() {
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        mainLooper.idleFor(Duration.ofMillis(1_800))
        awaitCondition { pending != null }
        val oldRetry = pending!!
        RecoveryVisualizerShadow.signal = true
        mainLooper.idleFor(Duration.ofMillis(100))
        awaitCondition { pending == null && capturedLevel > 0f }
        oldRetry.run()
        mainLooper.idleFor(Duration.ofMinutes(6))
        assertEquals(1, RecoveryVisualizerShadow.creations.get())
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test fun manualStopDuringRetryRejectsQueuedTimerAndOldHealthCallbacks() {
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        mainLooper.idleFor(Duration.ofMillis(1_800))
        awaitCondition { pending != null }
        val retry = pending!!
        val oldId = coordinator.snapshot().requestId
        service.onStartCommand(CaptureIntentCommandCodec.encode(service, CaptureCommand.Stop), 0, 1)
        retry.run()
        health(VisualizerCaptureHealth.API_ERROR, oldId)
        mainLooper.idleFor(Duration.ofMinutes(10))
        assertNull(pending)
        assertEquals(1, RecoveryVisualizerShadow.creations.get())
        assertFalse(CaptureUiStore.runtimeState.isCapturing)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun manualStopWhileRecoveryIsInitializingPreventsLaterResurrection() {
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        mainLooper.idleFor(Duration.ofMillis(1_800))
        awaitCondition { pending != null }
        RecoveryVisualizerShadow.failCreation = true
        firePendingRecovery()
        awaitCondition { RecoveryVisualizerShadow.creations.get() > 0 }
        val oldId = coordinator.snapshot().requestId
        service.onStartCommand(CaptureIntentCommandCodec.encode(service, CaptureCommand.Stop), 0, 1)
        val creationsAtStop = RecoveryVisualizerShadow.creations.get()
        health(VisualizerCaptureHealth.API_ERROR, oldId)
        mainLooper.idleFor(Duration.ofMinutes(10))
        assertNull(pending)
        assertEquals(creationsAtStop, RecoveryVisualizerShadow.creations.get())
        assertFalse(CaptureUiStore.runtimeState.isCapturing)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun rapidSilentVideoSwitchesCancelSupersededRetriesAndRetainBackoff() {
        repeat(50) {
            health(VisualizerCaptureHealth.NO_SIGNAL)
            val superseded = pending!!
            health(VisualizerCaptureHealth.WAITING)
            superseded.run()
        }
        health(VisualizerCaptureHealth.NO_SIGNAL)
        mainLooper.idleFor(Duration.ofSeconds(299))
        assertNotNull(pending)
        assertEquals(0, RecoveryVisualizerShadow.creations.get())
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test fun continuousApiErrorsReconnectAndFailedReinitializationKeepsFutureRecovery() {
        RecoveryVisualizerShadow.waveformStatus = Visualizer.ERROR_DEAD_OBJECT
        startWorker()
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 }
        mainLooper.idleFor(Duration.ofSeconds(2))
        awaitCondition { pending != null }
        RecoveryVisualizerShadow.failCreation = true
        firePendingRecovery()
        awaitCondition { pending != null }
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
        assertFalse(shadowOf(service).isStoppedBySelf)
        // Recovery initialization failures never use the initial startup's terminal retry limit.
        repeat(8) {
            firePendingRecovery()
            awaitCondition { pending != null }
            assertTrue(CaptureUiStore.runtimeState.isCapturing)
            assertFalse(shadowOf(service).isStoppedBySelf)
        }
        RecoveryVisualizerShadow.failCreation = false
        RecoveryVisualizerShadow.waveformStatus = Visualizer.SUCCESS
        RecoveryVisualizerShadow.signal = true
        firePendingRecovery()
        awaitCondition { pending == null && capturedLevel > 0f }
    }

    @Test fun replacingRequestRejectsOldTimerAndCallback() {
        health(VisualizerCaptureHealth.NO_SIGNAL)
        val retry = pending!!
        val oldId = coordinator.snapshot().requestId
        coordinator.beginVisualizer(VisualizerStartSource.QUICK_SETTINGS, 100L)
        retry.run()
        health(VisualizerCaptureHealth.API_ERROR, oldId)
        assertNull(pending)
        assertEquals(0, RecoveryVisualizerShadow.creations.get())
    }

    @Test fun periodicShortApiErrorsKeepTheOriginalSilenceRecoveryDeadline() {
        startSilentWorkerAndAwaitRetry()
        val firstRetry = pending
        val firstDeadline = field("pendingVisualizerRecoveryAtMs").getLong(service)
        repeat(10) {
            RecoveryVisualizerShadow.waveformStatus = Visualizer.ERROR
            awaitCondition { workerHealth() == VisualizerCaptureHealth.API_PENDING }
            mainLooper.idleFor(Duration.ofMillis(100))
            assertSame(firstRetry, pending)
            RecoveryVisualizerShadow.waveformStatus = Visualizer.SUCCESS
            awaitCondition { workerHealth() == VisualizerCaptureHealth.NO_SIGNAL }
            mainLooper.idleFor(Duration.ofMillis(900))
            assertSame(firstRetry, pending)
            assertEquals(firstDeadline, field("pendingVisualizerRecoveryAtMs").getLong(service))
        }
        mainLooper.idleFor(Duration.ofMillis(firstDeadline - SystemClock.uptimeMillis()))
        awaitCondition { RecoveryVisualizerShadow.creations.get() == 2 }
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
    }

    @Test fun signalJustBeforeDeadlineCancelsRecovery() {
        startSilentWorkerAndAwaitRetry()
        val dueAt = field("pendingVisualizerRecoveryAtMs").getLong(service)
        ShadowSystemClock.advanceBy(Duration.ofMillis(dueAt - SystemClock.uptimeMillis() - 1))
        RecoveryVisualizerShadow.signal = true
        awaitWorkerHealthWithoutDispatch(VisualizerCaptureHealth.SIGNAL)
        mainLooper.idleFor(Duration.ofMillis(1))
        assertNull(pending)
        assertEquals(1, RecoveryVisualizerShadow.creations.get())
        assertTrue(generation(captureSession()).shouldRun())
    }

    @Test fun overdueTimerBeforeSignalCallbackChecksWorkerStateAndDoesNotRestart() {
        startSilentWorkerAndAwaitRetry()
        val requestId = coordinator.snapshot().requestId
        val retry = pending
        val dueAt = field("pendingVisualizerRecoveryAtMs").getLong(service)
        // Keep main paused: the timer's timestamp now precedes the SIGNAL notification.
        ShadowSystemClock.advanceBy(Duration.ofMillis(dueAt - SystemClock.uptimeMillis() + 1))
        RecoveryVisualizerShadow.signal = true
        awaitWorkerHealthWithoutDispatch(VisualizerCaptureHealth.SIGNAL)
        assertSame(retry, pending)
        mainLooper.idle()
        assertNull(pending)
        assertEquals(requestId, coordinator.snapshot().requestId)
        assertEquals(1, RecoveryVisualizerShadow.creations.get())
        assertTrue(generation(captureSession()).shouldRun())
    }

    @Test fun healthPublicationAndRecoveryClaimUseTheSameGenerationLock() {
        startSilentWorkerAndAwaitRetry()
        val session = captureSession()
        val generation = generation(session)
        val readEntered = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        RecoveryVisualizerShadow.readEntered = readEntered
        RecoveryVisualizerShadow.releaseRead = releaseRead
        assertTrue(readEntered.await(1, TimeUnit.SECONDS))
        val attempting = CountDownLatch(1)
        var claimed = true
        lateinit var claimant: Thread
        try {
            generation.runIfRunningCurrent {
                claimant = Thread {
                    attempting.countDown()
                    claimed = output.claimRecoveryRestart()
                }.also { it.start() }
                assertTrue(attempting.await(1, TimeUnit.SECONDS))
                // Publish SIGNAL before releasing the lock to the pending recovery decision.
                session.javaClass.getDeclaredMethod("publishCaptureHealth", VisualizerCaptureHealth::class.java)
                    .apply { isAccessible = true }.invoke(session, VisualizerCaptureHealth.SIGNAL)
            }
            claimant.join(1_000)
            assertFalse(claimant.isAlive)
            assertFalse(claimed)
            assertTrue(generation.shouldRun())
        } finally {
            releaseRead.countDown()
        }
    }

    @Test fun maximumBackoffTimerStillExecutesAndSchedulesAnotherRecovery() {
        startSilentWorkerAndAwaitRetry()
        repeat(4) {
            val previousReads = RecoveryVisualizerShadow.reads.get()
            firePendingRecovery()
            awaitCondition { RecoveryVisualizerShadow.reads.get() > previousReads }
            mainLooper.idleFor(Duration.ofMillis(1_800))
            awaitCondition { pending != null }
        }
        assertEquals(300_000L, field("pendingVisualizerRecoveryAtMs").getLong(service) - SystemClock.uptimeMillis())
        val creations = RecoveryVisualizerShadow.creations.get()
        mainLooper.idleFor(Duration.ofMillis(300_000))
        awaitCondition { RecoveryVisualizerShadow.creations.get() > creations }
        awaitCondition { RecoveryVisualizerShadow.reads.get() > 0 && CaptureUiStore.runtimeState.isCapturing }
        mainLooper.idleFor(Duration.ofMillis(1_800))
        awaitCondition { pending != null }
        assertEquals(300_000L, field("pendingVisualizerRecoveryAtMs").getLong(service) - SystemClock.uptimeMillis())
        assertFalse(shadowOf(service).isStoppedBySelf)
    }

    @Test fun lightRecoveryKeepsSessionUntilExplicitStop() = verifyControllerSessionRecovery(matrix = false)

    @Test fun matrixRecoveryRestartsPlaybackWatchdogAndKeepsSessionUntilExplicitStop() =
        verifyControllerSessionRecovery(matrix = true)

    @Test fun spatializerConflictDuringRecoveryPreservesWarningAndContinuesRetrying() {
        startSilentWorkerAndAwaitRetry()
        CaptureUiStore.setUiVisible(true)
        RecoveryVisualizerShadow.failSpatializerCreation = true
        firePendingRecovery()
        awaitCondition { field("pendingVisualizerRecoveryReason").get(service) == VisualizerRecoveryReason.SPATIALIZER_CONFLICT }
        assertNotNull(CaptureUiStore.runtimeState.pendingSpatialAudioWarning)
        assertTrue(CaptureUiStore.runtimeState.logMessage.orEmpty().contains(service.getString(R.string.spatial_audio_warning_message_generic)))
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
        assertFalse(shadowOf(service).isStoppedBySelf)
        // Dismissing the warning must not open the same dialog on every cooldown retry.
        CaptureUiStore.updateRuntime { it.copy(pendingSpatialAudioWarning = null) }
        firePendingRecovery()
        awaitCondition { pending != null }
        assertEquals(VisualizerRecoveryReason.SPATIALIZER_CONFLICT, field("pendingVisualizerRecoveryReason").get(service))
        assertNull(CaptureUiStore.runtimeState.pendingSpatialAudioWarning)
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
    }

    private fun verifyControllerSessionRecovery(matrix: Boolean) {
        var mediaSession: MediaSession? = null
        try {
            if (matrix) {
                val thread = HandlerThread("matrix-recovery-test").also { it.start() }
                field("matrixOutputThread").set(service, thread)
                field("matrixOutputHandler").set(service, Handler(thread.looper))
                field("captureConfig").set(service, defaultServiceCaptureConfig().copy(
                    glyphMode = GlyphPatternRegistry.P3_MATRIX_OPEN_REEL,
                    experimentalVisualizerSignalWatchdogEnabled = true
                ))
                Settings.Secure.putString(service.contentResolver, "enabled_notification_listeners",
                    ComponentName(service, MediaSessionNotificationListenerService::class.java).flattenToString())
                mediaSession = MediaSession(service, "recovery-playback")
                shadowOf(mediaSession.controller).setPackageName("music")
                shadowOf(mediaSession.controller).setPlaybackState(
                    PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
                shadowOf(mediaSession.controller).setMetadata(MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "unchanged-track").build())
                shadowOf(service.getSystemService(MediaSessionManager::class.java)).addController(mediaSession.controller)
            }
            startSilentWorkerAndAwaitRetry()
            val previousReads = RecoveryVisualizerShadow.reads.get()
            firePendingRecovery()
            awaitCondition { RecoveryVisualizerShadow.reads.get() > previousReads }
            assertTrue(CaptureUiStore.runtimeState.isCapturing)
            assertTrue(controllerCalls.count { it == "turnOff" } >= 2)
            assertEquals(0, controllerCalls.count { it == "releaseSession" || it == "unbind" })
            if (matrix) assertNotNull(field("openReelMediaSessionWatchdogRunnable").get(service))
            service.onStartCommand(CaptureIntentCommandCodec.encode(service, CaptureCommand.Stop), 0, 1)
            drainLoopers()
            assertEquals(1, controllerCalls.count { it == "releaseSession" })
            assertNull(field("openReelMediaSessionWatchdogRunnable").get(service))
            assertNull(pending)
            assertFalse(CaptureUiStore.runtimeState.isCapturing)
        } finally {
            mediaSession?.release()
        }
    }

    @Test fun bluetoothOutputKeepsSilenceNonFatalAndRetainsSlowerApiErrorRetry() {
        val audioManager = shadowOf(service.getSystemService(AudioManager::class.java))
        val bluetoothDevice = AudioDeviceInfoBuilder.newBuilder()
            .setType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP).build()
        audioManager.setOutputDevices(listOf(bluetoothDevice))
        audioManager.setAudioDevicesForAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
            ImmutableList.of(bluetoothDevice)
        )
        assertTrue(AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(service))
        health(VisualizerCaptureHealth.API_ERROR)
        mainLooper.idleFor(Duration.ofMillis(699))
        assertEquals(0, RecoveryVisualizerShadow.creations.get())
        assertNotNull(pending)
        health(VisualizerCaptureHealth.WAITING)
        health(VisualizerCaptureHealth.NO_SIGNAL)
        mainLooper.idleFor(Duration.ofSeconds(59))
        assertEquals(0, RecoveryVisualizerShadow.creations.get())
        assertNotNull(pending)
        assertTrue(CaptureUiStore.runtimeState.isCapturing)
        assertFalse(shadowOf(service).isStoppedBySelf)
    }
}

@Implements(Visualizer::class)
class RecoveryVisualizerShadow {
    companion object {
        val creations = AtomicInteger()
        val reads = AtomicInteger()
        @Volatile var signal = false
        @Volatile var failCreation = false
        @Volatile var failSpatializerCreation = false
        @Volatile var waveformStatus = Visualizer.SUCCESS
        @Volatile var readEntered: CountDownLatch? = null
        @Volatile var releaseRead: CountDownLatch? = null
        fun reset() {
            creations.set(0)
            reads.set(0)
            signal = false
            failCreation = false
            failSpatializerCreation = false
            waveformStatus = Visualizer.SUCCESS
            readEntered = null
            releaseRead = null
        }
        @JvmStatic @Implementation fun getCaptureSizeRange() = intArrayOf(128, 128)
    }
    @Implementation fun __constructor__(audioSession: Int) {
        creations.incrementAndGet()
        if (failSpatializerCreation) throw IllegalStateException("Visualizer initialization error: -3")
        if (failCreation) throw IllegalStateException("simulated creation failure")
    }
    @Implementation fun getSamplingRate() = 44_100_000
    @Implementation fun setEnabled(enabled: Boolean) = Visualizer.SUCCESS
    @Implementation fun setCaptureSize(size: Int) = Visualizer.SUCCESS
    @Implementation fun setScalingMode(mode: Int) = Visualizer.SUCCESS
    @Implementation fun setMeasurementMode(mode: Int) = Visualizer.SUCCESS
    @Implementation fun release() {}
    @Implementation fun getWaveForm(waveform: ByteArray): Int {
        readEntered?.countDown()
        releaseRead?.await()
        waveform.fill(if (signal) 160.toByte() else 128.toByte())
        reads.incrementAndGet()
        return waveformStatus
    }
    @Implementation fun getMeasurementPeakRms(measurement: Visualizer.MeasurementPeakRms): Int {
        measurement.mPeak = if (signal) -1_000 else -9_600
        measurement.mRms = measurement.mPeak
        return Visualizer.SUCCESS
    }
}

@Implements(Spatializer::class)
class RecoverySpatializerShadow {
    @Implementation fun isEnabled() = RecoveryVisualizerShadow.failSpatializerCreation
}
