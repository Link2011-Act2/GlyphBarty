package jp.linkserver.glyphvisualizer.audio

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.BLUETOOTH_STARTUP_STABILIZATION_ENABLED
import jp.linkserver.glyphvisualizer.R
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OutputMixVisualizer(
    private val context: Context
) {
    enum class StartFailureReason {
        OTHER,
        VISUALIZER_CREATION_FAILED,
        UNRECOVERABLE_SPATIALIZER_CONFLICT
    }

    private class VisualizerCreationFailedException(
        cause: Throwable?,
        val unrecoverableSpatializerConflict: Boolean
    ) :
        RuntimeException("Visualizer(0) creation failed", cause)

    companion object {
        private const val TAG = "OutputMixVisualizer"
        private const val BLUETOOTH_PREPARE_DELAY_MS = 80L
        private const val BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS = 300L
        private const val EXPERIMENTAL_BLUETOOTH_PREPARE_DELAY_MS = 180L
        private const val EXPERIMENTAL_BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS = 550L
        private const val EXPERIMENTAL_REMOTE_SUBMIX_PREPARE_DELAY_MS = 1200L
        private const val EXPERIMENTAL_STABILITY_POLL_MS = 120L
        private const val EXPERIMENTAL_BT_STABLE_MS = 450L
        private const val EXPERIMENTAL_REMOTE_SUBMIX_STABLE_MS = 900L
        private const val EXPERIMENTAL_ROUTE_STABILITY_TIMEOUT_MS = 2200L
        private const val STARTUP_SIGNAL_GRACE_MS = 1_800L
        private const val RUNNING_SIGNAL_STALL_MS = 5_000L
        private const val STARTUP_SIGNAL_MIN_LEVEL = 0.0015f
        private const val WORKER_STOP_JOIN_TIMEOUT_MS = 150L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stateLock = Any()
    private val sessionOwner = AudioCaptureSessionOwner()
    private var activeSession: OutputMixCaptureSession? = null

    private inner class OutputMixCaptureSession(
        val generation: AudioCaptureGeneration,
        val waveformSamplers: WaveformSamplerCaptureSession
    ) {
        private val resourceLock = Any()

        @Volatile
        var workerThread: Thread? = null

        private var visualizer: Visualizer? = null

        fun attachVisualizer(candidate: Visualizer): Boolean = synchronized(resourceLock) {
            if (!generation.shouldRun()) {
                false
            } else {
                visualizer = candidate
                true
            }
        }

        fun stopAndRelease() {
            val threadToStop = workerThread
            threadToStop?.interrupt()
            if (threadToStop != null && threadToStop !== Thread.currentThread()) {
                try {
                    threadToStop.join(WORKER_STOP_JOIN_TIMEOUT_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            releaseResources()
        }

        fun releaseResources() {
            val visualizerToRelease = synchronized(resourceLock) {
                visualizer.also { visualizer = null }
            }
            try {
                visualizerToRelease?.enabled = false
            } catch (_: Throwable) {
            }
            try {
                visualizerToRelease?.release()
            } catch (_: Throwable) {
            }
            waveformSamplers.close()
        }
    }

    fun start(
        sensitivityProvider: () -> Float,
        noiseGateProvider: () -> Float,
        dynamicsProvider: () -> Float,
        toneFocusProvider: () -> Float,
        smoothingProvider: () -> Float,
        smoothingBalanceProvider: () -> Float,
        experimentalVisualizerStabilizationEnabled: Boolean,
        experimentalVisualizerSignalWatchdogEnabled: Boolean,
        experimentalPerformanceOptimizationsEnabled: Boolean,
        dispatchLevelChangesOnMain: Boolean,
        onStateChanged: (String) -> Unit,
        onLevelChanged: AudioLevelCallback,
        onStartFailed: (reason: StartFailureReason) -> Unit = {},
        onSignalStalled: () -> Unit = {},
        onCrashed: () -> Unit = {}
    ): Boolean {
        // Keep the old parameter and implementation path for a future return, but gate it off
        // for current builds so saved or externally supplied true values have no effect.
        val stabilizationEnabled =
            BLUETOOTH_STARTUP_STABILIZATION_ENABLED && experimentalVisualizerStabilizationEnabled
        stop()
        val startAt = SystemClock.elapsedRealtime()
        val generation = sessionOwner.begin()
        val session = OutputMixCaptureSession(
            generation = generation,
            waveformSamplers = WaveformSampler.createCaptureSession()
        )

        return try {
            val worker = thread(
                start = false,
                isDaemon = true,
                name = "output-mix-visualizer"
            ) {
                var activeStarted = false
                try {
                    AppLogger.i(TAG, "Starting Visualizer(0) output-mix capture. ${AudioRouteDiagnostics.snapshot(context)}")
                    var routeProbe = captureRouteProbe()
                    if (stabilizationEnabled) {
                        routeProbe = waitForStablePlaybackRoute(
                            initialProbe = routeProbe,
                            startAt = startAt,
                            generation = generation
                        )
                    }
                    val bluetoothLikelyConnected = routeProbe.bluetoothLikelyConnected
                    val musicActive = routeProbe.musicActive
                    val remoteSubmixPresent = routeProbe.remoteSubmixPresent
                    val prepareDelayMs = when {
                        stabilizationEnabled && remoteSubmixPresent -> EXPERIMENTAL_REMOTE_SUBMIX_PREPARE_DELAY_MS
                        stabilizationEnabled && bluetoothLikelyConnected && musicActive -> EXPERIMENTAL_BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS
                        stabilizationEnabled && bluetoothLikelyConnected -> EXPERIMENTAL_BLUETOOTH_PREPARE_DELAY_MS
                        bluetoothLikelyConnected && musicActive -> BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS
                        bluetoothLikelyConnected -> BLUETOOTH_PREPARE_DELAY_MS
                        else -> 0L
                    }
                    if (prepareDelayMs > 0L) {
                        AppLogger.i(
                            TAG,
                            "Bluetooth-like output detected; waiting ${prepareDelayMs}ms before Visualizer(0) init (musicActive=$musicActive remoteSubmix=$remoteSubmixPresent experimental=$stabilizationEnabled)"
                        )
                        Thread.sleep(prepareDelayMs)
                        AppLogger.i(
                            TAG,
                            "Visualizer(0) prepare wait finished in ${SystemClock.elapsedRealtime() - startAt}ms"
                        )
                    }
                    if (!generation.shouldRun() || Thread.currentThread().isInterrupted) {
                        return@thread
                    }

                    var instance: Visualizer? = null
                    var lastError: Throwable? = null
                    var unrecoverableSpatializerConflict = false
                    val initAttempts = when {
                        stabilizationEnabled && remoteSubmixPresent -> 8
                        stabilizationEnabled && bluetoothLikelyConnected && musicActive -> 6
                        bluetoothLikelyConnected && musicActive -> 5
                        else -> 3
                    }
                    for (attemptIndex in 0 until initAttempts) {
                        if (!generation.shouldRun() || Thread.currentThread().isInterrupted) break
                        try {
                            instance = Visualizer(0)
                            AppLogger.i(
                                TAG,
                                "Visualizer(0) init attempt ${attemptIndex + 1}/$initAttempts succeeded at ${SystemClock.elapsedRealtime() - startAt}ms"
                            )
                            break
                        } catch (e: Throwable) {
                            lastError = e
                            AppLogger.w(TAG, "Visualizer(0) init attempt ${attemptIndex + 1}/$initAttempts failed", e)
                            if (isUnrecoverableSpatializerCreationFailure(e)) {
                                unrecoverableSpatializerConflict = true
                                AppLogger.e(
                                    TAG,
                                    "Visualizer(0) error -3 occurred while Framework Spatializer is enabled; aborting startup retries. ${AudioRouteDiagnostics.snapshot(context)}"
                                )
                                break
                            }
                            val retryDelayMs = when {
                                stabilizationEnabled && remoteSubmixPresent -> 450L * (attemptIndex + 1)
                                stabilizationEnabled && bluetoothLikelyConnected && musicActive -> 280L * (attemptIndex + 1)
                                bluetoothLikelyConnected && musicActive -> 220L * (attemptIndex + 1)
                                else -> 120L * (attemptIndex + 1)
                            }
                            Thread.sleep(retryDelayMs)
                        }
                    }
                    if (!generation.shouldRun() || Thread.currentThread().isInterrupted) {
                        try {
                            instance?.enabled = false
                        } catch (_: Throwable) {
                        }
                        try {
                            instance?.release()
                        } catch (_: Throwable) {
                        }
                        return@thread
                    }
                    if (instance == null) {
                        throw VisualizerCreationFailedException(
                            cause = lastError,
                            unrecoverableSpatializerConflict = unrecoverableSpatializerConflict
                        )
                    }
                    val vis = instance!!
                    if (!session.attachVisualizer(vis)) {
                        try {
                            vis.enabled = false
                        } catch (_: Throwable) {
                        }
                        try {
                            vis.release()
                        } catch (_: Throwable) {
                        }
                        return@thread
                    }
                    val captureSize = Visualizer.getCaptureSizeRange()[1]
                    try {
                        vis.enabled = false
                    } catch (_: Throwable) {
                    }
                    try {
                        vis.setCaptureSize(captureSize)
                    } catch (stateError: IllegalStateException) {
                        try {
                            vis.enabled = false
                        } catch (_: Throwable) {
                        }
                        vis.setCaptureSize(captureSize)
                    }
                    vis.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED)
                    vis.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS)
                    vis.enabled = true
                    val samplingHz = (vis.samplingRate / 1000).coerceAtLeast(8_000)
                    val spectrumDecimation = (samplingHz / 8820).coerceAtLeast(1)
                    val spectrumSampleRate = samplingHz / spectrumDecimation
                    AppLogger.i(
                        TAG,
                        "Visualizer configured: captureSize=$captureSize samplingHz=$samplingHz spectrumSampleRate=$spectrumSampleRate totalStartMs=${SystemClock.elapsedRealtime() - startAt}"
                    )
                    activeStarted = true
                    val activeSinceMs = SystemClock.elapsedRealtime()
                    var lastSignalSeenAtMs = activeSinceMs
                    var firstSignalSeen = false
                    var startupStallReported = false
                    var runningStallReported = false
                    mainHandler.post {
                        generation.runIfRunningCurrent {
                            onStateChanged(context.getString(R.string.status_output_mix_listening))
                        }
                    }

                    val waveform = ByteArray(captureSize)
                    val monoSamples = FloatArray(captureSize)
                    val spectrumSamples = FloatArray(captureSize / spectrumDecimation)
                    val spectrumProcessor = SpectrumAnalysisProcessor()
                    val measurement = Visualizer.MeasurementPeakRms()
                    val levelEnvelopeProcessor = LevelEnvelopeProcessor()
                    var waveformErrorLogged = false
                    var measurementErrorLogged = false

                    while (generation.shouldRun() && !Thread.currentThread().isInterrupted) {
                        var waveformRms = 0f
                        var waveformPeak = 0f
                        var lowEnergy = 0f
                        var highEnergy = 0f
                        val waveformStatus = vis.getWaveForm(waveform)
                        if (waveformStatus == Visualizer.SUCCESS) {
                            waveformErrorLogged = false
                            var squareSum = 0.0
                            var lowState = 0f
                            var previous = 0f
                            waveform.forEachIndexed { idx, raw ->
                                val sample = ((raw.toInt() and 0xFF) - 128) / 128f
                                monoSamples[idx] = sample
                                val amplitude = abs(sample)
                                squareSum += amplitude * amplitude
                                if (amplitude > waveformPeak) {
                                    waveformPeak = amplitude
                                }
                                lowState += (sample - lowState) * 0.065f
                                lowEnergy += abs(lowState)
                                highEnergy += abs(sample - previous)
                                previous = sample
                            }
                            waveformRms = sqrt(squareSum / waveform.size).toFloat()
                            lowEnergy = (lowEnergy / waveform.size).coerceIn(0f, 1f)
                            highEnergy = (highEnergy / waveform.size).coerceIn(0f, 1f)
                        } else if (!waveformErrorLogged) {
                            AppLogger.w(TAG, "Visualizer.getWaveForm returned status=$waveformStatus")
                            waveformErrorLogged = true
                        }

                        var measurementPeak = 0f
                        val measurementStatus = vis.getMeasurementPeakRms(measurement)
                        if (measurementStatus == Visualizer.SUCCESS) {
                            measurementErrorLogged = false
                            val peakMb = measurement.mPeak.toFloat()
                            if (peakMb > -9600f) {
                                measurementPeak = 10f.pow(peakMb / 2000f).coerceIn(0f, 1f)
                            }
                        } else if (!measurementErrorLogged) {
                            AppLogger.w(TAG, "Visualizer.getMeasurementPeakRms returned status=$measurementStatus")
                            measurementErrorLogged = true
                        }

                        val baseLevel =
                            (waveformRms * 0.42f) + (waveformPeak * 0.15f) + (measurementPeak * 0.18f) +
                                (lowEnergy * 0.15f) + (highEnergy * 0.10f)
                        val hasSignal =
                            waveformRms > STARTUP_SIGNAL_MIN_LEVEL ||
                                waveformPeak > STARTUP_SIGNAL_MIN_LEVEL ||
                                measurementPeak > STARTUP_SIGNAL_MIN_LEVEL ||
                                lowEnergy > STARTUP_SIGNAL_MIN_LEVEL ||
                                highEnergy > STARTUP_SIGNAL_MIN_LEVEL
                        if (hasSignal) {
                            firstSignalSeen = true
                            lastSignalSeenAtMs = SystemClock.elapsedRealtime()
                            runningStallReported = false
                        }
                        if (
                            !firstSignalSeen &&
                                experimentalVisualizerSignalWatchdogEnabled &&
                                !startupStallReported &&
                                SystemClock.elapsedRealtime() - activeSinceMs >= STARTUP_SIGNAL_GRACE_MS &&
                                AudioRouteDiagnostics.isMusicActive(context)
                        ) {
                            startupStallReported = true
                            AppLogger.w(
                                TAG,
                                "Visualizer reached active state but produced no signal; requesting startup retry. ${AudioRouteDiagnostics.snapshot(context)}"
                            )
                            if (generation.stopWorkerIfCurrent()) {
                                mainHandler.post {
                                    generation.runIfCurrent { onSignalStalled() }
                                }
                            }
                            break
                        }
                        if (
                            firstSignalSeen &&
                                experimentalVisualizerSignalWatchdogEnabled &&
                                !runningStallReported &&
                                SystemClock.elapsedRealtime() - lastSignalSeenAtMs >= RUNNING_SIGNAL_STALL_MS &&
                                AudioRouteDiagnostics.isMusicActive(context)
                        ) {
                            runningStallReported = true
                            AppLogger.w(
                                TAG,
                                "Visualizer signal disappeared while music is active; requesting restart. ${AudioRouteDiagnostics.snapshot(context)}"
                            )
                            if (generation.stopWorkerIfCurrent()) {
                                mainHandler.post {
                                    generation.runIfCurrent { onSignalStalled() }
                                }
                            }
                            break
                        }
                        val envelope = levelEnvelopeProcessor.process(
                            LevelEnvelopeInput(
                                baseLevel = baseLevel,
                                lowEnergy = lowEnergy,
                                highEnergy = highEnergy,
                                sensitivity = sensitivityProvider(),
                                noiseGate = noiseGateProvider(),
                                dynamics = dynamicsProvider(),
                                toneFocus = toneFocusProvider(),
                                smoothing = smoothingProvider()
                            )
                        )
                        // Decimate monoSamples for spectrum analysis
                        for (i in spectrumSamples.indices) {
                            spectrumSamples[i] = monoSamples[i * spectrumDecimation]
                        }
                        val nowMs = SystemClock.elapsedRealtime()
                        val spectrumAnalysis = spectrumProcessor.analyze(
                            samples = spectrumSamples,
                            sampleRateHz = spectrumSampleRate,
                            performanceOptimizationsEnabled =
                                experimentalPerformanceOptimizationsEnabled,
                            nowMs = nowMs
                        )
                        val downsampledWaveform =
                            session.waveformSamplers.mono.downsample(monoSamples)
                        val frame = AudioAnalysisFrame(
                            level = envelope.level,
                            peak = envelope.peak,
                            lowEnergy = lowEnergy,
                            highEnergy = highEnergy,
                            leftLevel = envelope.level,
                            rightLevel = envelope.level,
                            spectrumBands = spectrumAnalysis.bands,
                            spectrumRawPeak = spectrumAnalysis.rawPeak,
                            phone4aBaseBandLevel = spectrumAnalysis.rangePeak,
                            waveformSamples = downsampledWaveform,
                            leftWaveformSamples = downsampledWaveform,
                            rightWaveformSamples = downsampledWaveform
                        )
                        val deliverLevelChange = Runnable {
                            generation.runIfRunningCurrent {
                                frame.deliverTo(onLevelChanged)
                            }
                        }
                        if (dispatchLevelChangesOnMain) {
                            mainHandler.post(deliverLevelChange)
                        } else {
                            deliverLevelChange.run()
                        }

                        try {
                            Thread.sleep(16)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (error: Throwable) {
                    AppLogger.e(TAG, "output-mix-visualizer worker crashed", error)
                    if (generation.stopWorkerIfCurrent()) {
                        mainHandler.post {
                            generation.runIfCurrent {
                                if (activeStarted) {
                                    onCrashed()
                                } else {
                                    onStartFailed(
                                        when {
                                            error is VisualizerCreationFailedException &&
                                                error.unrecoverableSpatializerConflict ->
                                                StartFailureReason.UNRECOVERABLE_SPATIALIZER_CONFLICT

                                            error is VisualizerCreationFailedException ->
                                                StartFailureReason.VISUALIZER_CREATION_FAILED

                                            else -> StartFailureReason.OTHER
                                        }
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    session.releaseResources()
                }
            }
            session.workerThread = worker
            val installed = generation.runIfRunningCurrent {
                synchronized(stateLock) {
                    activeSession = session
                }
            }
            if (!installed) {
                session.stopAndRelease()
                return false
            }
            worker.start()
            true
        } catch (error: Throwable) {
            AppLogger.e(TAG, "Output-mix visualizer failed to start", error)
            synchronized(stateLock) {
                if (activeSession?.generation === generation) {
                    activeSession = null
                }
            }
            session.stopAndRelease()
            generation.runIfRunningCurrent {
                onStateChanged(
                    context.getString(
                        R.string.status_output_mix_start_failed,
                        error.message ?: context.getString(R.string.status_unknown_error)
                    )
                )
            }
            sessionOwner.finish(generation)
            false
        }
    }

    fun stop() {
        AppLogger.i(TAG, "Stopping Visualizer(0) output-mix capture")
        val stoppedGeneration = sessionOwner.stopCurrent()
        val sessionToStop = synchronized(stateLock) {
            activeSession?.takeIf { it.generation === stoppedGeneration }?.also {
                activeSession = null
            }
        }
        sessionToStop?.stopAndRelease()
    }

    private data class RouteProbe(
        val signature: String,
        val bluetoothLikelyConnected: Boolean,
        val musicActive: Boolean,
        val remoteSubmixPresent: Boolean
    )

    private fun isUnrecoverableSpatializerCreationFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        var visualizerNoInit = false
        while (current != null) {
            if (current.message?.contains("error: -3", ignoreCase = true) == true) {
                visualizerNoInit = true
                break
            }
            current = current.cause
        }
        return visualizerNoInit && AudioRouteDiagnostics.isFrameworkSpatializerEnabled(context)
    }

    private fun captureRouteProbe(): RouteProbe {
        return RouteProbe(
            signature = AudioRouteDiagnostics.outputSignature(context),
            bluetoothLikelyConnected = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(context),
            musicActive = AudioRouteDiagnostics.isMusicActive(context),
            remoteSubmixPresent = AudioRouteDiagnostics.hasRemoteSubmixOutput(context)
        )
    }

    private fun waitForStablePlaybackRoute(
        initialProbe: RouteProbe,
        startAt: Long,
        generation: AudioCaptureGeneration
    ): RouteProbe {
        var latest = initialProbe
        var stableSinceMs = SystemClock.uptimeMillis()
        val requiredStableMs = when {
            initialProbe.remoteSubmixPresent -> EXPERIMENTAL_REMOTE_SUBMIX_STABLE_MS
            initialProbe.bluetoothLikelyConnected && initialProbe.musicActive -> EXPERIMENTAL_BT_STABLE_MS
            else -> 0L
        }
        if (requiredStableMs <= 0L) return initialProbe

        val deadlineMs = SystemClock.uptimeMillis() + EXPERIMENTAL_ROUTE_STABILITY_TIMEOUT_MS
        AppLogger.i(
            TAG,
            "Experimental visualizer stabilization enabled; waiting for route stability (requiredStableMs=$requiredStableMs remoteSubmix=${initialProbe.remoteSubmixPresent})"
        )
        while (
            generation.shouldRun() &&
            !Thread.currentThread().isInterrupted &&
            SystemClock.uptimeMillis() < deadlineMs
        ) {
            Thread.sleep(EXPERIMENTAL_STABILITY_POLL_MS)
            val probe = captureRouteProbe()
            val stable =
                probe.signature == latest.signature &&
                    probe.bluetoothLikelyConnected == latest.bluetoothLikelyConnected &&
                    probe.musicActive == latest.musicActive &&
                    probe.remoteSubmixPresent == latest.remoteSubmixPresent
            latest = probe
            if (!stable) {
                stableSinceMs = SystemClock.uptimeMillis()
                continue
            }
            if (latest.bluetoothLikelyConnected && latest.musicActive &&
                SystemClock.uptimeMillis() - stableSinceMs >= requiredStableMs
            ) {
                AppLogger.i(
                    TAG,
                    "Experimental route stability wait completed in ${SystemClock.elapsedRealtime() - startAt}ms ${AudioRouteDiagnostics.snapshot(context)}"
                )
                return latest
            }
        }
        AppLogger.i(
            TAG,
            "Experimental route stability wait timed out after ${SystemClock.elapsedRealtime() - startAt}ms ${AudioRouteDiagnostics.snapshot(context)}"
        )
        return captureRouteProbe()
    }
}
