package jp.linkserver.glyphvisualizer.audio

import kotlin.math.abs

class WaveformSamplerSession internal constructor(
    private val autoTimeAxisEnabledProvider: () -> Boolean,
    private val onMultiplierChanged: (Float) -> Unit = {}
) {
    private companion object {
        const val ZERO_CROSS_SYNC_ENABLED = true
        const val FIXED_WINDOW_MULTIPLIER = 7
        const val AUTO_MIN_WINDOW_MULTIPLIER = 2
        const val AUTO_MAX_WINDOW_MULTIPLIER = 160
        const val AUTO_WINDOW_SMOOTHING = 0.28f
        const val AUTO_HISTORY_CAPACITY = 8_192
    }

    private var autoTimeAxisEnabled = autoTimeAxisEnabledProvider()
    private var smoothedAutoDisplayLength = 0f
    private var waveformHistory = FloatArray(0)

    private fun refreshAutoTimeAxisSetting() {
        val enabled = autoTimeAxisEnabledProvider()
        val changed = autoTimeAxisEnabled != enabled
        autoTimeAxisEnabled = enabled
        if (changed) {
            smoothedAutoDisplayLength = 0f
            waveformHistory = FloatArray(0)
            onMultiplierChanged(1f)
        }
    }

    fun downsample(samples: FloatArray, targetCount: Int = 25): FloatArray {
        refreshAutoTimeAxisSetting()
        if (targetCount <= 0 || samples.isEmpty()) return FloatArray(0)
        if (samples.size == targetCount) return samples.copyOf()
        val sourceSamples = if (autoTimeAxisEnabled) {
            appendHistory(samples)
        } else {
            samples
        }
        val fixedDisplayLength = fixedDisplayLength(targetCount)
        val displayLength = if (autoTimeAxisEnabled) {
            smoothAutoDisplayLength(
                estimateDisplayLengthForWaveform(sourceSamples, targetCount),
                fallback = fixedDisplayLength
            )
        } else {
            fixedDisplayLength
        }
        val currentAutoTimeAxisMultiplier = if (autoTimeAxisEnabled) {
            displayLength / fixedDisplayLength.toFloat()
        } else {
            1f
        }
        onMultiplierChanged(currentAutoTimeAxisMultiplier)
        val sourceLength = displayLength.coerceAtMost(sourceSamples.size).coerceAtLeast(1)
        val sourceStart = if (ZERO_CROSS_SYNC_ENABLED) {
            findDisplayStart(sourceSamples, sourceLength, autoTimeAxisEnabled)
        } else {
            (sourceSamples.size - sourceLength).coerceAtLeast(0)
        }
        val output = FloatArray(targetCount)
        for (index in 0 until targetCount) {
            val start = (sourceStart + ((index.toFloat() / targetCount) * sourceLength).toInt())
                .coerceIn(sourceStart, sourceSamples.lastIndex)
            val end = (sourceStart + ((((index + 1f) / targetCount) * sourceLength).toInt() - 1))
                .coerceIn(start, sourceSamples.lastIndex)
            var peak = 0f
            var signedPeak = 0f
            for (sampleIndex in start..end) {
                val sample = sourceSamples[sampleIndex].coerceIn(-1f, 1f)
                val amplitude = abs(sample)
                if (amplitude >= peak) {
                    peak = amplitude
                    signedPeak = sample
                }
            }
            output[index] = signedPeak
        }
        return output
    }

    private fun estimateDisplayLengthForWaveform(samples: FloatArray, targetCount: Int): Int {
        val recentPeriods = IntArray(4)
        var recentPeriodCount = 0
        var previousCrossing = -1
        for (index in 1..samples.lastIndex) {
            if (samples[index - 1] < 0f && samples[index] >= 0f) {
                if (previousCrossing >= 0) {
                    val period = index - previousCrossing
                    if (period in 6..(samples.size / 2)) {
                        recentPeriods[recentPeriodCount % recentPeriods.size] = period
                        recentPeriodCount += 1
                    }
                }
                previousCrossing = index
            }
        }
        if (recentPeriodCount == 0) {
            return fixedDisplayLength(targetCount)
        }
        val samplesToAverage = recentPeriodCount.coerceAtMost(recentPeriods.size)
        var periodSum = 0
        for (index in 0 until samplesToAverage) {
            periodSum += recentPeriods[index]
        }
        val averagePeriod = periodSum.toFloat() / samplesToAverage
        val cyclesToShow = when {
            averagePeriod < 9f -> 5.4f
            averagePeriod < 16f -> 4.3f
            averagePeriod < 32f -> 3.3f
            averagePeriod < 160f -> 2.2f
            else -> 2.2f
        }
        return (averagePeriod * cyclesToShow)
            .toInt()
            .coerceIn(targetCount * AUTO_MIN_WINDOW_MULTIPLIER, targetCount * AUTO_MAX_WINDOW_MULTIPLIER)
    }

    private fun smoothAutoDisplayLength(estimated: Int, fallback: Int): Int {
        val safeEstimated = if (estimated > 0) estimated.toFloat() else fallback.toFloat()
        smoothedAutoDisplayLength = if (smoothedAutoDisplayLength <= 0f) {
            safeEstimated
        } else {
            smoothedAutoDisplayLength + ((safeEstimated - smoothedAutoDisplayLength) * AUTO_WINDOW_SMOOTHING)
        }
        return smoothedAutoDisplayLength.toInt().coerceAtLeast(1)
    }

    private fun fixedDisplayLength(targetCount: Int): Int {
        return (targetCount * FIXED_WINDOW_MULTIPLIER).coerceAtLeast(targetCount)
    }

    private fun appendHistory(samples: FloatArray): FloatArray {
        val keepFromHistory = (AUTO_HISTORY_CAPACITY - samples.size).coerceAtLeast(0)
        val historyTailCount = waveformHistory.size.coerceAtMost(keepFromHistory)
        val nextSize = historyTailCount + samples.size.coerceAtMost(AUTO_HISTORY_CAPACITY)
        val next = FloatArray(nextSize)
        if (historyTailCount > 0) {
            waveformHistory.copyInto(
                destination = next,
                destinationOffset = 0,
                startIndex = waveformHistory.size - historyTailCount,
                endIndex = waveformHistory.size
            )
        }
        val sourceStart = (samples.size - (nextSize - historyTailCount)).coerceAtLeast(0)
        samples.copyInto(
            destination = next,
            destinationOffset = historyTailCount,
            startIndex = sourceStart,
            endIndex = samples.size
        )
        waveformHistory = next
        return next
    }

    private fun findDisplayStart(samples: FloatArray, sourceLength: Int, preferRecent: Boolean): Int {
        val defaultStart = (samples.size - sourceLength).coerceAtLeast(0)
        if (!preferRecent) {
            val searchEnd = (samples.size - 1).coerceAtMost(samples.size / 2)
            for (index in 1..searchEnd) {
                if (samples[index - 1] < 0f && samples[index] >= 0f) {
                    return index
                }
            }
            return 0
        }
        val searchStart = (defaultStart - sourceLength / 2).coerceAtLeast(1)
        val searchEnd = defaultStart.coerceAtLeast(searchStart).coerceAtMost(samples.lastIndex)
        var bestStart = -1
        for (index in searchStart..searchEnd) {
            if (samples[index - 1] < 0f && samples[index] >= 0f) {
                bestStart = index
            }
        }
        return if (bestStart >= 0) bestStart else defaultStart
    }
}

class WaveformSamplerCaptureSession internal constructor(
    val mono: WaveformSamplerSession,
    val left: WaveformSamplerSession,
    val right: WaveformSamplerSession,
    private val closeAction: () -> Unit
) : AutoCloseable {
    @Volatile
    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        closeAction()
    }
}

/** Compatibility facade for the existing setting and UI readout; sample history is session-owned. */
object WaveformSampler {
    private val stateLock = Any()

    @Volatile
    private var autoTimeAxisEnabled = false

    @Volatile
    private var currentAutoTimeAxisMultiplier = 1f

    private var nextCaptureSessionId = 0L
    private var activeCaptureSessionId = 0L

    fun setAutoTimeAxisEnabled(enabled: Boolean) {
        val changed = autoTimeAxisEnabled != enabled
        autoTimeAxisEnabled = enabled
        if (changed) {
            currentAutoTimeAxisMultiplier = 1f
        }
    }

    fun currentAutoTimeAxisMultiplier(): Float = currentAutoTimeAxisMultiplier

    fun createCaptureSession(): WaveformSamplerCaptureSession {
        val captureSessionId = synchronized(stateLock) {
            (++nextCaptureSessionId).also {
                activeCaptureSessionId = it
                currentAutoTimeAxisMultiplier = 1f
            }
        }
        fun createChannel(): WaveformSamplerSession =
            WaveformSamplerSession(
                autoTimeAxisEnabledProvider = { autoTimeAxisEnabled },
                onMultiplierChanged = { multiplier ->
                    publishMultiplier(captureSessionId, multiplier)
                }
            )

        return WaveformSamplerCaptureSession(
            mono = createChannel(),
            left = createChannel(),
            right = createChannel(),
            closeAction = { closeCaptureSession(captureSessionId) }
        )
    }

    private fun publishMultiplier(captureSessionId: Long, multiplier: Float) {
        synchronized(stateLock) {
            if (activeCaptureSessionId == captureSessionId) {
                currentAutoTimeAxisMultiplier = multiplier
            }
        }
    }

    private fun closeCaptureSession(captureSessionId: Long) {
        synchronized(stateLock) {
            if (activeCaptureSessionId == captureSessionId) {
                activeCaptureSessionId = 0L
                currentAutoTimeAxisMultiplier = 1f
            }
        }
    }
}
