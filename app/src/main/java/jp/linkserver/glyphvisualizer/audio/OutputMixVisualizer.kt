package jp.linkserver.glyphvisualizer.audio

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class OutputMixVisualizer(
    private val context: Context
) {
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
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var visualizer: Visualizer? = null
    private var workerThread: Thread? = null

    @Volatile
    private var isRunning = false

    fun start(
        sensitivityProvider: () -> Float,
        noiseGateProvider: () -> Float,
        dynamicsProvider: () -> Float,
        toneFocusProvider: () -> Float,
        smoothingProvider: () -> Float,
        smoothingBalanceProvider: () -> Float,
        experimentalVisualizerStabilizationEnabled: Boolean,
        onStateChanged: (String) -> Unit,
        onLevelChanged: (
            level: Float,
            peak: Float,
            lowEnergy: Float,
            highEnergy: Float,
            leftLevel: Float,
            rightLevel: Float,
            spectrumBands: FloatArray,
            phone4aBaseBandLevel: Float
        ) -> Unit,
        onStartFailed: () -> Unit = {},
        onCrashed: () -> Unit = {}
    ): Boolean {
        stop()
        val startAt = SystemClock.elapsedRealtime()

        return try {
            isRunning = true
            workerThread = thread(start = true, isDaemon = true, name = "output-mix-visualizer") {
                var activeStarted = false
                try {
                    AppLogger.i(TAG, "Starting Visualizer(0) output-mix capture. ${AudioRouteDiagnostics.snapshot(context)}")
                    var routeProbe = captureRouteProbe()
                    if (experimentalVisualizerStabilizationEnabled) {
                        routeProbe = waitForStablePlaybackRoute(routeProbe, startAt)
                    }
                    val bluetoothLikelyConnected = routeProbe.bluetoothLikelyConnected
                    val musicActive = routeProbe.musicActive
                    val remoteSubmixPresent = routeProbe.remoteSubmixPresent
                    val prepareDelayMs = when {
                        experimentalVisualizerStabilizationEnabled && remoteSubmixPresent -> EXPERIMENTAL_REMOTE_SUBMIX_PREPARE_DELAY_MS
                        experimentalVisualizerStabilizationEnabled && bluetoothLikelyConnected && musicActive -> EXPERIMENTAL_BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS
                        experimentalVisualizerStabilizationEnabled && bluetoothLikelyConnected -> EXPERIMENTAL_BLUETOOTH_PREPARE_DELAY_MS
                        bluetoothLikelyConnected && musicActive -> BLUETOOTH_ACTIVE_PLAYBACK_PREPARE_DELAY_MS
                        bluetoothLikelyConnected -> BLUETOOTH_PREPARE_DELAY_MS
                        else -> 0L
                    }
                    if (prepareDelayMs > 0L) {
                        AppLogger.i(
                            TAG,
                            "Bluetooth-like output detected; waiting ${prepareDelayMs}ms before Visualizer(0) init (musicActive=$musicActive remoteSubmix=$remoteSubmixPresent experimental=$experimentalVisualizerStabilizationEnabled)"
                        )
                        Thread.sleep(prepareDelayMs)
                        AppLogger.i(
                            TAG,
                            "Visualizer(0) prepare wait finished in ${SystemClock.elapsedRealtime() - startAt}ms"
                        )
                    }
                    if (!isRunning || Thread.currentThread().isInterrupted) return@thread

                    var instance: Visualizer? = null
                    var lastError: Throwable? = null
                    val initAttempts = when {
                        experimentalVisualizerStabilizationEnabled && remoteSubmixPresent -> 8
                        experimentalVisualizerStabilizationEnabled && bluetoothLikelyConnected && musicActive -> 6
                        bluetoothLikelyConnected && musicActive -> 5
                        else -> 3
                    }
                    repeat(initAttempts) {
                        if (instance != null || !isRunning || Thread.currentThread().isInterrupted) return@repeat
                        try {
                            instance = Visualizer(0)
                            AppLogger.i(
                                TAG,
                                "Visualizer(0) init attempt ${it + 1}/$initAttempts succeeded at ${SystemClock.elapsedRealtime() - startAt}ms"
                            )
                        } catch (e: Throwable) {
                            lastError = e
                            AppLogger.w(TAG, "Visualizer(0) init attempt ${it + 1}/$initAttempts failed", e)
                            val retryDelayMs = when {
                                experimentalVisualizerStabilizationEnabled && remoteSubmixPresent -> 450L * (it + 1)
                                experimentalVisualizerStabilizationEnabled && bluetoothLikelyConnected && musicActive -> 280L * (it + 1)
                                bluetoothLikelyConnected && musicActive -> 220L * (it + 1)
                                else -> 120L * (it + 1)
                            }
                            Thread.sleep(retryDelayMs)
                        }
                    }
                    if (!isRunning || Thread.currentThread().isInterrupted) return@thread
                    if (instance == null) throw lastError ?: RuntimeException("Visualizer init failed")
                    val vis = instance!!
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
                    visualizer = vis
                    activeStarted = true
                    mainHandler.post {
                        if (isRunning) {
                            onStateChanged(context.getString(R.string.status_output_mix_listening))
                        }
                    }

                    val waveform = ByteArray(captureSize)
                    val monoSamples = FloatArray(captureSize)
                    val spectrumSamples = FloatArray(captureSize / spectrumDecimation)
                    val measurement = Visualizer.MeasurementPeakRms()
                    var smoothedLevel = 0f
                    var displayedLevel = 0f
                    var waveformErrorLogged = false
                    var measurementErrorLogged = false

                    while (isRunning && !Thread.currentThread().isInterrupted) {
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
                        val toneFocus = toneFocusProvider().coerceIn(-1f, 1f)
                        val focusedLevel = when {
                            toneFocus < 0f -> {
                                val bassMix = -toneFocus
                                (baseLevel * (1f - bassMix)) + (lowEnergy * bassMix)
                            }
                            toneFocus > 0f -> {
                                val trebleMix = toneFocus
                                (baseLevel * (1f - trebleMix)) + (highEnergy * trebleMix)
                            }
                            else -> baseLevel
                        }
                        val rawLevel = focusedLevel * sensitivityProvider()
                        val gate = noiseGateProvider().coerceIn(0f, 0.95f)
                        val gated = ((rawLevel - gate) / (1f - gate)).coerceIn(0f, 1f)
                        val bounded = gated.pow(dynamicsProvider().coerceIn(0.6f, 2.4f)).coerceIn(0f, 1f)
                        val smoothing = smoothingProvider().coerceIn(0.05f, 0.6f)
                        val noReleaseSmoothing = smoothing >= 0.54f
                        val primarySmoothing = if (noReleaseSmoothing) 1f else (smoothing * 0.6f).coerceIn(0.04f, 0.4f)
                        val release = if (noReleaseSmoothing) 1f else smoothing
                        if (bounded > smoothedLevel) {
                            smoothedLevel = bounded
                        } else {
                            smoothedLevel += (bounded - smoothedLevel) * primarySmoothing
                        }
                        if (smoothedLevel > displayedLevel) {
                            displayedLevel = smoothedLevel
                        } else {
                            displayedLevel += (smoothedLevel - displayedLevel) * release
                        }
                        val peakValue = displayedLevel
                        // Decimate monoSamples for spectrum analysis
                        for (i in spectrumSamples.indices) {
                            spectrumSamples[i] = monoSamples[i * spectrumDecimation]
                        }
                        val spectrumAnalysis = SpectrumAnalyzer.analyzeLogBands(
                            samples = spectrumSamples,
                            sampleRateHz = spectrumSampleRate,
                            bandCount = 25
                        )

                        mainHandler.post {
                            onLevelChanged(
                                displayedLevel,
                                peakValue,
                                lowEnergy,
                                highEnergy,
                                displayedLevel,
                                displayedLevel,
                                spectrumAnalysis.bands,
                                spectrumAnalysis.rangePeak
                            )
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
                    if (isRunning) {
                        isRunning = false
                        mainHandler.post {
                            if (activeStarted) {
                                onCrashed()
                            } else {
                                onStartFailed()
                            }
                        }
                    }
                }
            }
            true
        } catch (error: Throwable) {
            stop()
            AppLogger.e(TAG, "Output-mix visualizer failed to start", error)
            onStateChanged(
                context.getString(
                    R.string.status_output_mix_start_failed,
                    error.message ?: context.getString(R.string.status_unknown_error)
                )
            )
            false
        }
    }

    fun stop() {
        AppLogger.i(TAG, "Stopping Visualizer(0) output-mix capture")
        isRunning = false
        val t = workerThread
        workerThread = null
        t?.interrupt()
        try {
            t?.join(150)      // release() 前にスレッド終了を待つ
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        try {
            visualizer?.enabled = false
        } catch (_: Throwable) {
        }
        try {
            visualizer?.release()
        } catch (_: Throwable) {
        }
        visualizer = null
    }

    private data class RouteProbe(
        val signature: String,
        val bluetoothLikelyConnected: Boolean,
        val musicActive: Boolean,
        val remoteSubmixPresent: Boolean
    )

    private fun captureRouteProbe(): RouteProbe {
        return RouteProbe(
            signature = AudioRouteDiagnostics.outputSignature(context),
            bluetoothLikelyConnected = AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(context),
            musicActive = AudioRouteDiagnostics.isMusicActive(context),
            remoteSubmixPresent = AudioRouteDiagnostics.hasRemoteSubmixOutput(context)
        )
    }

    private fun waitForStablePlaybackRoute(initialProbe: RouteProbe, startAt: Long): RouteProbe {
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
        while (isRunning && !Thread.currentThread().isInterrupted && SystemClock.uptimeMillis() < deadlineMs) {
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
