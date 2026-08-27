package jp.linkserver.glyphvisualizer.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class AudioPlaybackVisualizer(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioPlaybackVisualizer"
        private const val EMPTY_READ_BACKOFF_MS = 10L
        private const val WORKER_STOP_JOIN_TIMEOUT_MS = 250L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stateLock = Any()

    @Volatile
    private var isRunning = false

    private var activeSessionId = 0L
    private var workerThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null

    @SuppressLint("MissingPermission") // RECORD_AUDIO is checked immediately before AudioRecord creation.
    fun start(
        resultCode: Int,
        data: Intent,
        sensitivityProvider: () -> Float,
        noiseGateProvider: () -> Float,
        dynamicsProvider: () -> Float,
        toneFocusProvider: () -> Float,
        smoothingProvider: () -> Float,
        smoothingBalanceProvider: () -> Float,
        experimentalPerformanceOptimizationsEnabled: Boolean,
        dispatchLevelChangesOnMain: Boolean,
        onStateChanged: (String) -> Unit,
        onLevelChanged: (
            level: Float,
            peak: Float,
            lowEnergy: Float,
            highEnergy: Float,
            leftLevel: Float,
            rightLevel: Float,
            spectrumBands: FloatArray,
            phone4aBaseBandLevel: Float,
            waveformSamples: FloatArray,
            leftWaveformSamples: FloatArray,
            rightWaveformSamples: FloatArray
        ) -> Unit,
        onCaptureFailed: (Throwable) -> Unit = {}
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onStateChanged(context.getString(R.string.status_audio_capture_requires_android10))
            return false
        }

        stop()

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onStateChanged(context.getString(R.string.status_mic_permission_required))
            return false
        }

        val sessionId = synchronized(stateLock) {
            activeSessionId += 1L
            activeSessionId
        }

        var projection: MediaProjection? = null
        var projectionCallback: MediaProjection.Callback? = null
        var record: AudioRecord? = null

        try {
            val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
            projection = projectionManager?.getMediaProjection(resultCode, data)
            val activeProjection = projection
            if (activeProjection == null) {
                onStateChanged(context.getString(R.string.status_media_projection_unavailable))
                return false
            }

            val config = AudioPlaybackCaptureConfiguration.Builder(activeProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()

            val stereoFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44_100)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val monoFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44_100)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            fun createRecord(format: AudioFormat, channelMask: Int): Pair<AudioRecord, Int> {
                val minBuffer = AudioRecord.getMinBufferSize(
                    44_100,
                    channelMask,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = if (minBuffer > 0) max(minBuffer * 2, 4_096) else 4_096
                val record = AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()
                return record to bufferSize
            }

            fun tryCreateRecord(format: AudioFormat, channelMask: Int): Pair<AudioRecord, Int>? {
                return try {
                    createRecord(format, channelMask)
                } catch (error: SecurityException) {
                    throw error
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "AudioRecord creation failed for channelMask=$channelMask", error)
                    null
                }
            }

            var channelCount = 2
            var built = tryCreateRecord(stereoFormat, AudioFormat.CHANNEL_IN_STEREO)
            if (built?.first?.state != AudioRecord.STATE_INITIALIZED) {
                safeReleaseAudioRecord(built?.first)
                channelCount = 1
                built = tryCreateRecord(monoFormat, AudioFormat.CHANNEL_IN_MONO)
            }

            if (built?.first?.state != AudioRecord.STATE_INITIALIZED) {
                safeReleaseAudioRecord(built?.first)
                safeStopProjection(activeProjection, null)
                onStateChanged(context.getString(R.string.status_audio_record_initialization_failed))
                return false
            }

            val activeRecord = built.first
            record = activeRecord
            val bufferSize = built.second
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    reportRuntimeFailure(
                        sessionId = sessionId,
                        error = IllegalStateException("MediaProjection stopped unexpectedly"),
                        onCaptureFailed = onCaptureFailed
                    )
                }
            }
            activeProjection.registerCallback(projectionCallback, mainHandler)
            activeRecord.startRecording()
            if (activeRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("AudioRecord did not enter the recording state")
            }

            val worker = Thread({
                try {
                    val sampleBuffer = ShortArray(bufferSize / 2)
                    var lastSpectrumAnalysis = SpectrumAnalyzer.AnalysisResult(FloatArray(25), 0f, 0f)
                    var lastSpectrumAnalysisAtMs = 0L
                    var smoothedLevel = 0f
                    var displayedLevel = 0f
                    var displayedLeft = 0f
                    var displayedRight = 0f

                    while (isSessionRunning(sessionId) && !Thread.currentThread().isInterrupted) {
                        val read = activeRecord.read(sampleBuffer, 0, sampleBuffer.size)
                        if (read < 0) {
                            throw IllegalStateException("AudioRecord.read failed with code $read")
                        }
                        if (read == 0) {
                            Thread.sleep(EMPTY_READ_BACKOFF_MS)
                            continue
                        }

                        var squareSum = 0.0
                        var maxAmplitude = 0f
                        var lowAccumulator = 0f
                        var highAccumulator = 0f
                        var lowState = 0f
                        var previous = 0f

                        var leftSquareSum = 0.0
                        var rightSquareSum = 0.0
                        var frames = 0

                        val limit = if (channelCount == 2) read - (read % 2) else read
                        val expectedFrames = if (channelCount == 2) (limit / 2) else limit
                        val monoSamples = FloatArray(expectedFrames.coerceAtLeast(1))
                        val leftSamples = FloatArray(expectedFrames.coerceAtLeast(1))
                        val rightSamples = FloatArray(expectedFrames.coerceAtLeast(1))
                        var index = 0
                        while (index < limit) {
                            val leftSample = sampleBuffer[index].toInt() / Short.MAX_VALUE.toFloat()
                            val rightSample = if (channelCount == 2) {
                                sampleBuffer[index + 1].toInt() / Short.MAX_VALUE.toFloat()
                            } else {
                                leftSample
                            }
                            val sample = (leftSample + rightSample) * 0.5f
                            monoSamples[frames] = sample
                            leftSamples[frames] = leftSample
                            rightSamples[frames] = rightSample
                            val amplitude = abs(sample)
                            squareSum += amplitude * amplitude
                            if (amplitude > maxAmplitude) {
                                maxAmplitude = amplitude
                            }
                            leftSquareSum += leftSample * leftSample
                            rightSquareSum += rightSample * rightSample
                            lowState += (sample - lowState) * 0.055f
                            lowAccumulator += abs(lowState)
                            highAccumulator += abs(sample - previous)
                            previous = sample
                            frames += 1
                            index += if (channelCount == 2) 2 else 1
                        }

                        if (frames <= 0) continue

                        val rms = sqrt(squareSum / frames).toFloat()
                        val lowEnergy = (lowAccumulator / frames).coerceIn(0f, 1f)
                        val highEnergy = (highAccumulator / frames).coerceIn(0f, 1f)
                        val leftLevel = sqrt(leftSquareSum / frames).toFloat().coerceIn(0f, 1f)
                        val rightLevel = sqrt(rightSquareSum / frames).toFloat().coerceIn(0f, 1f)
                        val baseLevel = ((rms * 0.52f) + (maxAmplitude * 0.22f) + (lowEnergy * 0.16f) + (highEnergy * 0.10f))
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
                        val normalized = focusedLevel * sensitivityProvider()
                        val gate = noiseGateProvider().coerceIn(0f, 0.95f)
                        val gated = ((normalized - gate) / (1f - gate)).coerceIn(0f, 1f)
                        val bounded = gated.pow(dynamicsProvider().coerceIn(0.6f, 2.4f)).coerceIn(0f, 1f)
                        val smoothing = smoothingProvider().coerceIn(0.05f, 0.6f)
                        val noReleaseSmoothing = smoothing >= 0.54f
                        val primarySmoothing = if (noReleaseSmoothing) 1f else (smoothing * 0.6f).coerceIn(0.04f, 0.4f)
                        val release = if (noReleaseSmoothing) {
                            1f
                        } else {
                            (smoothing * 1.25f).coerceIn(0.0625f, 0.75f)
                        }
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
                        if (leftLevel > displayedLeft) {
                            displayedLeft = leftLevel
                        } else {
                            displayedLeft += (leftLevel - displayedLeft) * release
                        }
                        if (rightLevel > displayedRight) {
                            displayedRight = rightLevel
                        } else {
                            displayedRight += (rightLevel - displayedRight) * release
                        }
                        val peakValue = displayedLevel
                        val nowMs = android.os.SystemClock.elapsedRealtime()
                        val shouldRefreshSpectrum =
                            !experimentalPerformanceOptimizationsEnabled ||
                                lastSpectrumAnalysisAtMs <= 0L ||
                                (nowMs - lastSpectrumAnalysisAtMs) >= 33L
                        val spectrumAnalysis = if (shouldRefreshSpectrum) {
                            SpectrumAnalyzer.analyzeLogBands(
                                samples = monoSamples,
                                sampleRateHz = 44_100,
                                bandCount = 25
                            ).also {
                                lastSpectrumAnalysis = it
                                lastSpectrumAnalysisAtMs = nowMs
                            }
                        } else {
                            lastSpectrumAnalysis
                        }
                        val waveformSamples = WaveformSampler.downsample(monoSamples)
                        val leftWaveformSamples = WaveformSampler.downsample(leftSamples)
                        val rightWaveformSamples = WaveformSampler.downsample(rightSamples)
                        val deliveredLevel = displayedLevel
                        val deliveredPeak = peakValue
                        val deliveredLeft = displayedLeft
                        val deliveredRight = displayedRight
                        val deliverLevelChange = Runnable {
                            if (isSessionRunning(sessionId)) {
                                onLevelChanged(
                                    deliveredLevel,
                                    deliveredPeak,
                                    lowEnergy,
                                    highEnergy,
                                    deliveredLeft,
                                    deliveredRight,
                                    spectrumAnalysis.bands,
                                    spectrumAnalysis.rangePeak,
                                    waveformSamples,
                                    leftWaveformSamples,
                                    rightWaveformSamples
                                )
                            }
                        }
                        if (dispatchLevelChangesOnMain) {
                            mainHandler.post(deliverLevelChange)
                        } else {
                            deliverLevelChange.run()
                        }
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (error: Throwable) {
                    reportRuntimeFailure(sessionId, error, onCaptureFailed)
                }
            }, "glyph-audio-visualizer").apply {
                isDaemon = true
            }

            synchronized(stateLock) {
                mediaProjection = activeProjection
                mediaProjectionCallback = projectionCallback
                audioRecord = activeRecord
                workerThread = worker
                isRunning = true
            }
            worker.start()
            onStateChanged(context.getString(R.string.status_playback_listening))

            return true
        } catch (error: Throwable) {
            AppLogger.e(TAG, "Audio playback capture failed to start", error)
            synchronized(stateLock) {
                if (activeSessionId == sessionId) {
                    isRunning = false
                    workerThread = null
                    audioRecord = null
                    mediaProjection = null
                    mediaProjectionCallback = null
                }
            }
            safeReleaseAudioRecord(record)
            safeStopProjection(projection, projectionCallback)
            onStateChanged(
                when (error) {
                    is SecurityException -> context.getString(R.string.status_mic_permission_required)
                    else -> context.getString(R.string.status_audio_record_initialization_failed)
                }
            )
            return false
        }
    }

    fun stop() {
        val threadToStop: Thread?
        val recordToRelease: AudioRecord?
        val projectionToStop: MediaProjection?
        val callbackToUnregister: MediaProjection.Callback?

        synchronized(stateLock) {
            isRunning = false
            activeSessionId += 1L
            threadToStop = workerThread
            recordToRelease = audioRecord
            projectionToStop = mediaProjection
            callbackToUnregister = mediaProjectionCallback
            workerThread = null
            audioRecord = null
            mediaProjection = null
            mediaProjectionCallback = null
        }

        try {
            if (projectionToStop != null && callbackToUnregister != null) {
                projectionToStop.unregisterCallback(callbackToUnregister)
            }
        } catch (_: Throwable) {
        }

        try {
            recordToRelease?.stop()
        } catch (_: Throwable) {
        }

        threadToStop?.interrupt()
        if (threadToStop != null && threadToStop !== Thread.currentThread()) {
            try {
                threadToStop.join(WORKER_STOP_JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        safeReleaseAudioRecord(recordToRelease)
        safeStopProjection(projectionToStop, null)
    }

    private fun isSessionRunning(sessionId: Long): Boolean {
        return synchronized(stateLock) {
            isRunning && activeSessionId == sessionId
        }
    }

    private fun reportRuntimeFailure(
        sessionId: Long,
        error: Throwable,
        onCaptureFailed: (Throwable) -> Unit
    ) {
        val shouldReport = synchronized(stateLock) {
            if (!isRunning || activeSessionId != sessionId) {
                false
            } else {
                isRunning = false
                true
            }
        }
        if (!shouldReport) return

        AppLogger.e(TAG, "Audio playback capture stopped unexpectedly", error)
        mainHandler.post {
            val isStillCurrentSession = synchronized(stateLock) {
                activeSessionId == sessionId
            }
            if (isStillCurrentSession) {
                onCaptureFailed(error)
            }
        }
    }

    private fun safeReleaseAudioRecord(record: AudioRecord?) {
        try {
            record?.release()
        } catch (_: Throwable) {
        }
    }

    private fun safeStopProjection(
        projection: MediaProjection?,
        callback: MediaProjection.Callback?
    ) {
        try {
            if (callback != null) {
                projection?.unregisterCallback(callback)
            }
        } catch (_: Throwable) {
        }
        try {
            projection?.stop()
        } catch (_: Throwable) {
        }
    }
}
