package jp.linkserver.glyphvisualizer.glyph

import java.util.ArrayDeque
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min

internal const val DEFAULT_AUTO_GAIN_TARGET_LEVEL = 0.85f

internal fun effectiveAutoScaleTargetLevel(offset: Float): Float {
    val clampedOffset = offset.coerceIn(0f, 0.4f)
    return DEFAULT_AUTO_GAIN_TARGET_LEVEL *
        ((1f + clampedOffset) / (1f + (2f * clampedOffset)))
}

internal class AutoGainController(
    private val peakWindowMs: Long = 2_000L,
    private val warmupMs: Long = 200L,
    private val minPeak: Float = 0.08f,
    private val maxGain: Float = 6f,
    private val gainDownTauSeconds: Float = 0.18f,
    private val outputCeiling: Float = 0.98f,
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
