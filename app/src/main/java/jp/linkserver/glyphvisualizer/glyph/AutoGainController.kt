package jp.linkserver.glyphvisualizer.glyph

import java.util.ArrayDeque
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min

internal const val DEFAULT_AUTO_GAIN_TARGET_LEVEL = 0.85f
internal const val DEFAULT_AUTO_GAIN_OUTPUT_CEILING = 0.98f
internal const val SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS = 5f
internal const val SPECTRUM_SMOOTHING_ATTACK = 0.4f
internal const val SPECTRUM_SMOOTHING_RELEASE = 0.15f
internal const val SPECTRUM_OVERALL_LEVEL_ATTACK_TAU_MS = 50f
internal const val SPECTRUM_OVERALL_LEVEL_RELEASE_TAU_MS = 220f

internal fun effectiveAutoScaleTargetLevel(offset: Float): Float {
    val clampedOffset = offset.coerceIn(0f, 0.4f)
    return DEFAULT_AUTO_GAIN_TARGET_LEVEL *
        ((1f + clampedOffset) / (1f + (2f * clampedOffset)))
}

internal fun spectrumOverallLevelTarget(rawPeak: Float, sharedGain: Float): Float {
    return (rawPeak.coerceIn(0f, 1f) * sharedGain.coerceAtLeast(0f))
        .coerceIn(0f, DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
}

/** Applies one already-smoothed overall level without changing the normalized spectral shape. */
internal fun applySpectrumOverallLevel(
    normalizedBands: FloatArray,
    overallLevel: Float,
    output: FloatArray = FloatArray(normalizedBands.size)
): FloatArray {
    require(output.size == normalizedBands.size)
    val outputPeak = overallLevel.coerceIn(0f, DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
    for (index in normalizedBands.indices) {
        output[index] = normalizedBands[index].coerceIn(0f, 1f) * outputPeak
    }
    return output
}

internal class SpectrumOverallLevelEnvelope(
    private val attackTauMs: Float = SPECTRUM_OVERALL_LEVEL_ATTACK_TAU_MS,
    private val releaseTauMs: Float = SPECTRUM_OVERALL_LEVEL_RELEASE_TAU_MS
) {
    private var displayLevel = 0f
    private var lastUpdateMs = UNSET_TIME_MS

    fun update(targetLevel: Float, nowMs: Long): Float {
        val target = targetLevel.coerceIn(0f, DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
        if (lastUpdateMs != UNSET_TIME_MS && nowMs < lastUpdateMs) {
            reset()
        }
        if (lastUpdateMs == UNSET_TIME_MS) {
            displayLevel = target
            lastUpdateMs = nowMs
            return displayLevel
        }

        val elapsedMs = (nowMs - lastUpdateMs).coerceAtLeast(0L).toFloat()
        lastUpdateMs = nowMs
        if (elapsedMs <= 0f) return displayLevel

        val tauMs = if (target > displayLevel) attackTauMs else releaseTauMs
        val alpha = (1f - exp(-elapsedMs / tauMs.coerceAtLeast(MIN_TAU_MS))).coerceIn(0f, 1f)
        displayLevel += (target - displayLevel) * alpha
        displayLevel = displayLevel.coerceIn(0f, DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
        return displayLevel
    }

    fun reset() {
        displayLevel = 0f
        lastUpdateMs = UNSET_TIME_MS
    }

    private companion object {
        const val UNSET_TIME_MS = Long.MIN_VALUE
        const val MIN_TAU_MS = 0.001f
    }
}

internal class AutoGainController(
    private val peakWindowMs: Long = 2_000L,
    private val warmupMs: Long = 200L,
    private val minPeak: Float = 0.08f,
    private val maxGain: Float = 6f,
    private val gainDownTauSeconds: Float = 0.18f,
    private val outputCeiling: Float = DEFAULT_AUTO_GAIN_OUTPUT_CEILING,
    private val silenceResetMs: Long = 3_000L
) {
    private data class TimedSample(
        val timestampMs: Long,
        val value: Float
    )

    private val samples = ArrayDeque<TimedSample>()
    private var gain = 1f
    private var initialized = false
    private var warmupStartedAtMs = UNSET_TIME_MS
    private var lastUpdateMs = UNSET_TIME_MS
    private var silenceStartedAtMs = UNSET_TIME_MS
    private var silenceResetApplied = false

    /**
     * Updates the long-term P95 gain and returns the gain that is safe to apply to this frame.
     * The instantaneous safety limit is intentionally stateless, so a one-frame peak cannot
     * make the smoothed gain remain low for the much slower gain-up period.
     */
    fun update(
        referenceRaw: Float,
        nowMs: Long,
        targetLevel: Float,
        gainUpTauSeconds: Float,
        holdGainIncrease: Boolean
    ): Float {
        val raw = referenceRaw.coerceIn(0f, 1f)
        if (lastUpdateMs != UNSET_TIME_MS && nowMs < lastUpdateMs) {
            reset()
        }

        evictExpiredSamples(nowMs)
        if (holdGainIncrease) {
            if (silenceStartedAtMs == UNSET_TIME_MS) {
                silenceStartedAtMs = nowMs
            }
            if (!silenceResetApplied && nowMs - silenceStartedAtMs >= silenceResetMs) {
                clearGainState()
                silenceResetApplied = true
            }
            // Do not let the first frame after a held interval consume the entire silence as dt.
            lastUpdateMs = nowMs
            return safetyLimitedGain(raw)
        }

        silenceStartedAtMs = UNSET_TIME_MS
        silenceResetApplied = false
        addSample(nowMs, raw)

        if (!initialized) {
            if (warmupStartedAtMs == UNSET_TIME_MS) {
                warmupStartedAtMs = nowMs
            }
            if (nowMs - warmupStartedAtMs >= warmupMs) {
                gain = desiredGain(targetLevel)
                initialized = true
            }
            lastUpdateMs = nowMs
            return safetyLimitedGain(raw)
        }

        val elapsedSeconds = if (lastUpdateMs == UNSET_TIME_MS) {
            0f
        } else {
            ((nowMs - lastUpdateMs).coerceAtLeast(0L) / 1_000f)
        }
        lastUpdateMs = nowMs

        val desired = desiredGain(targetLevel)
        if (elapsedSeconds > 0f) {
            val tauSeconds = if (desired < gain) {
                gainDownTauSeconds
            } else {
                gainUpTauSeconds.coerceAtLeast(MIN_TAU_SECONDS)
            }
            val alpha = (1f - exp(-elapsedSeconds / tauSeconds)).coerceIn(0f, 1f)
            gain += (desired - gain) * alpha
            gain = gain.coerceIn(MIN_GAIN, maxGain)
        }

        return safetyLimitedGain(raw)
    }

    fun reset() {
        clearGainState()
        silenceStartedAtMs = UNSET_TIME_MS
        silenceResetApplied = false
    }

    private fun clearGainState() {
        samples.clear()
        gain = 1f
        initialized = false
        warmupStartedAtMs = UNSET_TIME_MS
        lastUpdateMs = UNSET_TIME_MS
    }

    private fun addSample(nowMs: Long, raw: Float) {
        samples.addLast(TimedSample(nowMs, raw))
        evictExpiredSamples(nowMs)
    }

    private fun evictExpiredSamples(nowMs: Long) {
        while (samples.isNotEmpty() && nowMs - samples.first.timestampMs > peakWindowMs) {
            samples.removeFirst()
        }
    }

    private fun desiredGain(targetLevel: Float): Float {
        val effectiveTarget = targetLevel.coerceIn(MIN_TARGET_LEVEL, outputCeiling)
        val measuredPeak = percentile95().coerceAtLeast(minPeak)
        return (effectiveTarget / measuredPeak).coerceIn(MIN_GAIN, maxGain)
    }

    private fun percentile95(): Float {
        if (samples.isEmpty()) return 0f
        val sorted = FloatArray(samples.size)
        var index = 0
        samples.forEach { sample ->
            sorted[index++] = sample.value
        }
        sorted.sort()
        val percentileIndex = floor((sorted.lastIndex * 0.95f).toDouble())
            .toInt()
            .coerceIn(0, sorted.lastIndex)
        return sorted[percentileIndex]
    }

    private fun safetyLimitedGain(raw: Float): Float {
        if (raw <= 0f) return gain
        val safetyGain = outputCeiling / raw
        return min(gain, safetyGain).coerceIn(0f, maxGain)
    }

    private companion object {
        const val UNSET_TIME_MS = Long.MIN_VALUE
        const val MIN_GAIN = 0.1f
        const val MIN_TARGET_LEVEL = 0.01f
        const val MIN_TAU_SECONDS = 0.001f
    }
}
