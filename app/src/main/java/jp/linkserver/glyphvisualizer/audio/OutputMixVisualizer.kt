package jp.linkserver.glyphvisualizer.audio

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
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
        onStateChanged: (String) -> Unit,
        onLevelChanged: (
            level: Float,
            peak: Float,
            lowEnergy: Float,
            highEnergy: Float,
            leftLevel: Float,
            rightLevel: Float,
            spectrumBands: FloatArray
        ) -> Unit,
        onCrashed: () -> Unit = {}
    ): Boolean {
        stop()

        return try {
            // Visualizer(0) は OutputMix セッションの解放タイミングによって
            // 間欠的に失敗するため、最大3回リトライする
            var instance: Visualizer? = null
            var lastError: Throwable? = null
            repeat(3) {
                if (instance != null) return@repeat
                try {
                    instance = Visualizer(0)
                } catch (e: Throwable) {
                    lastError = e
                    AppLogger.w(TAG, "Visualizer(0) init attempt ${it + 1}/3 failed", e)
                    Thread.sleep(40)
                }
            }
            if (instance == null) throw lastError ?: RuntimeException("Visualizer init failed")
            val vis = instance!!
            val captureSize = Visualizer.getCaptureSizeRange()[1]
            // Some devices may return an already-enabled state; force disabled before config.
            try {
                vis.enabled = false
            } catch (_: Throwable) {
            }
            try {
                vis.setCaptureSize(captureSize)
            } catch (stateError: IllegalStateException) {
                // Retry once after forcing disabled state again.
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
            // Downsample to ~8820 Hz for SpectrumAnalyzer (n=256 gives 34 Hz/bin resolution)
            val spectrumDecimation = (samplingHz / 8820).coerceAtLeast(1)
            val spectrumSampleRate = samplingHz / spectrumDecimation
            visualizer = vis
            isRunning = true

            workerThread = thread(start = true, isDaemon = true, name = "output-mix-visualizer") {
                try {
                    val waveform = ByteArray(captureSize)
                    val monoSamples = FloatArray(captureSize)
                    val spectrumSamples = FloatArray(captureSize / spectrumDecimation)
                    val measurement = Visualizer.MeasurementPeakRms()
                    var smoothedLevel = 0f
                    var displayedLevel = 0f

                    while (isRunning && !Thread.currentThread().isInterrupted) {
                        var waveformRms = 0f
                        var waveformPeak = 0f
                        var lowEnergy = 0f
                        var highEnergy = 0f
                        if (vis.getWaveForm(waveform) == Visualizer.SUCCESS) {
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
                        }

                        var measurementPeak = 0f
                        if (vis.getMeasurementPeakRms(measurement) == Visualizer.SUCCESS) {
                            val peakMb = measurement.mPeak.toFloat()
                            if (peakMb > -9600f) {
                                measurementPeak = 10f.pow(peakMb / 2000f).coerceIn(0f, 1f)
                            }
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
                        val spectrumBands = SpectrumAnalyzer.computeLogBands(
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
                                spectrumBands
                            )
                        }

                        try {
                            Thread.sleep(16)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                } catch (error: Throwable) {
                    AppLogger.e(TAG, "output-mix-visualizer worker crashed", error)
                    if (isRunning) {
                        isRunning = false
                        mainHandler.post { onCrashed() }
                    }
                }
            }
            onStateChanged(context.getString(R.string.status_output_mix_listening))
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
}
