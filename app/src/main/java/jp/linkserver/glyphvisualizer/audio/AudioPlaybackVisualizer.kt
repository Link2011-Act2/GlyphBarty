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
    private val sessionOwner = AudioCaptureSessionOwner()

    private var activeSession: PlaybackCaptureSession? = null

    private inner class PlaybackCaptureSession(
        val generation: AudioCaptureGeneration,
        val audioRecord: AudioRecord,
        val mediaProjection: MediaProjection,
        val mediaProjectionCallback: MediaProjection.Callback,
        val waveformSamplers: WaveformSamplerCaptureSession
    ) {
        @Volatile
        var workerThread: Thread? = null

        fun stopAndRelease() {
            try {
                mediaProjection.unregisterCallback(mediaProjectionCallback)
            } catch (_: Throwable) {
            }
            try {
                audioRecord.stop()
            } catch (_: Throwable) {
            }

            val threadToStop = workerThread
            threadToStop?.interrupt()
            if (threadToStop != null && threadToStop !== Thread.currentThread()) {
                try {
                    threadToStop.join(WORKER_STOP_JOIN_TIMEOUT_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }

            safeReleaseAudioRecord(audioRecord)
            safeStopProjection(mediaProjection, null)
            waveformSamplers.close()
        }
    }

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
        onLevelChanged: AudioLevelCallback,
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

        val generation = sessionOwner.begin()

        var projection: MediaProjection? = null
        var projectionCallback: MediaProjection.Callback? = null
        var record: AudioRecord? = null
        var waveformSamplers: WaveformSamplerCaptureSession? = null

        try {
            val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
            projection = projectionManager?.getMediaProjection(resultCode, data)
            val activeProjection = projection
            if (activeProjection == null) {
                generation.runIfRunningCurrent {
                    onStateChanged(context.getString(R.string.status_media_projection_unavailable))
                }
                sessionOwner.finish(generation)
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
                generation.runIfRunningCurrent {
                    onStateChanged(
                        context.getString(R.string.status_audio_record_initialization_failed)
                    )
                }
                sessionOwner.finish(generation)
                return false
            }

            val activeRecord = built.first
            record = activeRecord
            val bufferSize = built.second
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    reportRuntimeFailure(
                        generation = generation,
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

            val sessionSamplers = WaveformSampler.createCaptureSession()
            waveformSamplers = sessionSamplers
            val session = PlaybackCaptureSession(
                generation = generation,
                audioRecord = activeRecord,
                mediaProjection = activeProjection,
                mediaProjectionCallback = projectionCallback,
                waveformSamplers = sessionSamplers
            )

            val worker = Thread({
                try {
                    val sampleBuffer = ShortArray(bufferSize / 2)
                    val spectrumProcessor = SpectrumAnalysisProcessor()
                    val levelEnvelopeProcessor = LevelEnvelopeProcessor()
                    var displayedLeft = 0f
                    var displayedRight = 0f

                    while (generation.shouldRun() && !Thread.currentThread().isInterrupted) {
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
                        if (leftLevel > displayedLeft) {
                            displayedLeft = leftLevel
                        } else {
                            displayedLeft += (leftLevel - displayedLeft) * envelope.release
                        }
                        if (rightLevel > displayedRight) {
                            displayedRight = rightLevel
                        } else {
                            displayedRight += (rightLevel - displayedRight) * envelope.release
                        }
                        val nowMs = android.os.SystemClock.elapsedRealtime()
                        val spectrumAnalysis = spectrumProcessor.analyze(
                            samples = monoSamples,
                            sampleRateHz = 44_100,
                            performanceOptimizationsEnabled =
                                experimentalPerformanceOptimizationsEnabled,
                            nowMs = nowMs
                        )
                        val frame = AudioAnalysisFrame(
                            level = envelope.level,
                            peak = envelope.peak,
                            lowEnergy = lowEnergy,
                            highEnergy = highEnergy,
                            leftLevel = displayedLeft,
                            rightLevel = displayedRight,
                            spectrumBands = spectrumAnalysis.bands,
                            phone4aBaseBandLevel = spectrumAnalysis.rangePeak,
                            waveformSamples = sessionSamplers.mono.downsample(monoSamples),
                            leftWaveformSamples = sessionSamplers.left.downsample(leftSamples),
                            rightWaveformSamples = sessionSamplers.right.downsample(rightSamples)
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
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (error: Throwable) {
                    reportRuntimeFailure(generation, error, onCaptureFailed)
                }
            }, "glyph-audio-visualizer").apply {
                isDaemon = true
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
            generation.runIfRunningCurrent {
                onStateChanged(context.getString(R.string.status_playback_listening))
            }

            return true
        } catch (error: Throwable) {
            AppLogger.e(TAG, "Audio playback capture failed to start", error)
            synchronized(stateLock) {
                if (activeSession?.generation === generation) {
                    activeSession = null
                }
            }
            safeReleaseAudioRecord(record)
            safeStopProjection(projection, projectionCallback)
            waveformSamplers?.close()
            generation.runIfRunningCurrent {
                onStateChanged(
                    when (error) {
                        is SecurityException ->
                            context.getString(R.string.status_mic_permission_required)

                        else ->
                            context.getString(R.string.status_audio_record_initialization_failed)
                    }
                )
            }
            sessionOwner.finish(generation)
            return false
        }
    }

    fun stop() {
        val stoppedGeneration = sessionOwner.stopCurrent()
        val sessionToStop = synchronized(stateLock) {
            activeSession?.takeIf { it.generation === stoppedGeneration }?.also {
                activeSession = null
            }
        }
        sessionToStop?.stopAndRelease()
    }

    private fun reportRuntimeFailure(
        generation: AudioCaptureGeneration,
        error: Throwable,
        onCaptureFailed: (Throwable) -> Unit
    ) {
        if (!generation.stopWorkerIfCurrent()) return

        AppLogger.e(TAG, "Audio playback capture stopped unexpectedly", error)
        mainHandler.post {
            generation.runIfCurrent {
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
