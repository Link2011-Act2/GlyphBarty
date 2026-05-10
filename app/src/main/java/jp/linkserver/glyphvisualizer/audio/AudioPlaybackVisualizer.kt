package jp.linkserver.glyphvisualizer.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import jp.linkserver.glyphvisualizer.R
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class AudioPlaybackVisualizer(
    private val context: Context
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isRunning = false

    private var workerThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null

    fun start(
        resultCode: Int,
        data: Intent,
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
            spectrumBands: FloatArray,
            phone4aBaseBandLevel: Float
        ) -> Unit
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onStateChanged(context.getString(R.string.status_audio_capture_requires_android10))
            return false
        }

        stop()

        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        val projection = projectionManager?.getMediaProjection(resultCode, data)
        if (projection == null) {
            onStateChanged(context.getString(R.string.status_media_projection_unavailable))
            return false
        }

        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
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
            val bufferSize = max(minBuffer * 2, 4_096)
            val record = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
            return record to bufferSize
        }

        var channelCount = 2
        var built = createRecord(stereoFormat, AudioFormat.CHANNEL_IN_STEREO)
        var record = built.first
        var bufferSize = built.second
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            channelCount = 1
            built = createRecord(monoFormat, AudioFormat.CHANNEL_IN_MONO)
            record = built.first
            bufferSize = built.second
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            projection.stop()
            onStateChanged(context.getString(R.string.status_audio_record_initialization_failed))
            return false
        }

        mediaProjection = projection
        audioRecord = record
        isRunning = true

        record.startRecording()
        onStateChanged(context.getString(R.string.status_playback_listening))

        workerThread = thread(
            start = true,
            isDaemon = true,
            name = "glyph-audio-visualizer"
        ) {
            val sampleBuffer = ShortArray(bufferSize / 2)
            var smoothedLevel = 0f
            var displayedLevel = 0f
            var displayedLeft = 0f
            var displayedRight = 0f

            while (isRunning) {
                val read = record.read(sampleBuffer, 0, sampleBuffer.size)
                if (read <= 0) {
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
                val spectrumAnalysis = SpectrumAnalyzer.analyzeLogBands(
                    samples = monoSamples,
                    sampleRateHz = 44_100,
                    bandCount = 25
                )

                mainHandler.post {
                    onLevelChanged(
                        displayedLevel,
                        peakValue,
                        lowEnergy,
                        highEnergy,
                        displayedLeft,
                        displayedRight,
                        spectrumAnalysis.bands,
                        spectrumAnalysis.normalizedRangePeak
                    )
                }
            }
        }

        return true
    }

    fun stop() {
        isRunning = false

        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }

        audioRecord?.release()
        audioRecord = null

        mediaProjection?.stop()
        mediaProjection = null

        workerThread?.interrupt()
        workerThread = null
    }
}
