package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.max
import kotlin.math.min

enum class GlyphAutoScaleStrategy {
    LEGACY,
    ADAPTIVE
}

const val DEFAULT_LEGACY_AUTO_SCALE_ENABLED = false

fun glyphAutoScaleStrategy(legacyEnabled: Boolean): GlyphAutoScaleStrategy {
    return if (legacyEnabled) {
        GlyphAutoScaleStrategy.LEGACY
    } else {
        GlyphAutoScaleStrategy.ADAPTIVE
    }
}

/** Restored min/max auto scale used before the adaptive P95 gain controller. */
internal class LegacyAutoScaleController {
    private var minTrack = 0f
    private var maxTrack = 1f
    private var lastUpdateMs = 0L

    fun update(
        value: Float,
        nowMs: Long,
        windowMs: Float,
        offset: Float
    ): Float {
        val clamped = value.coerceIn(0f, 1f)
        val elapsedMs = if (lastUpdateMs <= 0L) 0L else (nowMs - lastUpdateMs).coerceAtLeast(0L)
        lastUpdateMs = nowMs
        val drift = (elapsedMs.toFloat() / windowMs.coerceAtLeast(1f)).coerceIn(0f, 1f)

        minTrack = min(clamped, (minTrack + drift).coerceIn(0f, 1f))
        maxTrack = max(clamped, (maxTrack - drift).coerceIn(0f, 1f))
        return normalizeWithAutoScaleOffset(clamped, minTrack, maxTrack, offset)
    }

    fun reset() {
        minTrack = 0f
        maxTrack = 1f
        lastUpdateMs = 0L
    }
}

/** Restored per-band min/max auto scale used by spectrum renderers. */
internal class LegacySpectrumAutoScaleController {
    private var bandMins = FloatArray(0)
    private var bandMaxs = FloatArray(0)
    private var lastUpdateMs = 0L

    fun update(
        input: FloatArray,
        nowMs: Long,
        windowMs: Float,
        offset: Float,
        output: FloatArray = FloatArray(input.size)
    ): FloatArray {
        if (input.isEmpty()) return input
        val elapsedMs = if (lastUpdateMs <= 0L) 0L else (nowMs - lastUpdateMs).coerceAtLeast(0L)
        lastUpdateMs = nowMs
        val drift = (elapsedMs.toFloat() / windowMs.coerceAtLeast(1f)).coerceIn(0f, 1f)

        if (bandMins.size != input.size || bandMaxs.size != input.size) {
            bandMins = input.copyOf()
            bandMaxs = input.copyOf()
        }
        val result = if (output.size == input.size) output else FloatArray(input.size)
        for (index in input.indices) {
            val value = input[index].coerceIn(0f, 1f)
            var minTrack = min(value, (bandMins[index] + drift).coerceIn(0f, 1f))
            var maxTrack = max(value, (bandMaxs[index] - drift).coerceIn(0f, 1f))
            if (maxTrack - minTrack < MIN_AUTO_SCALE_RANGE) {
                minTrack = (value - MIN_AUTO_SCALE_RANGE / 2f).coerceIn(0f, 1f)
                maxTrack = (value + MIN_AUTO_SCALE_RANGE / 2f).coerceIn(0f, 1f)
            }
            bandMins[index] = minTrack
            bandMaxs[index] = maxTrack
            result[index] = normalizeWithAutoScaleOffset(value, minTrack, maxTrack, offset)
        }
        return result
    }

    fun reset() {
        bandMins = FloatArray(0)
        bandMaxs = FloatArray(0)
        lastUpdateMs = 0L
    }
}

internal fun normalizeWithAutoScaleOffset(
    value: Float,
    minTrack: Float,
    maxTrack: Float,
    offset: Float
): Float {
    val range = (maxTrack - minTrack).coerceAtLeast(MIN_AUTO_SCALE_RANGE)
    val clampedOffset = offset.coerceIn(0f, 0.4f)
    val adjustedMin = minTrack - range * clampedOffset
    val adjustedMax = maxTrack + range * clampedOffset
    val adjustedRange = (adjustedMax - adjustedMin).coerceAtLeast(MIN_AUTO_SCALE_RANGE)
    return ((value - adjustedMin) / adjustedRange).coerceIn(0f, 1f)
}

internal fun isAllBrightnessOff(
    level: Float,
    autoScaleEnabled: Boolean,
    strategy: GlyphAutoScaleStrategy
): Boolean {
    val offThreshold = if (!autoScaleEnabled || strategy == GlyphAutoScaleStrategy.LEGACY) {
        LEGACY_ALL_BRIGHTNESS_OFF_THRESHOLD
    } else {
        0f
    }
    return level <= offThreshold
}

internal fun isAllBrightnessDisplayOff(
    displayLevel: Float,
    autoScaleEnabled: Boolean,
    strategy: GlyphAutoScaleStrategy,
    adaptiveGateOn: Boolean
): Boolean {
    return if (autoScaleEnabled && strategy == GlyphAutoScaleStrategy.ADAPTIVE) {
        !adaptiveGateOn
    } else {
        isAllBrightnessOff(displayLevel, autoScaleEnabled, strategy)
    }
}

private const val MIN_AUTO_SCALE_RANGE = 0.05f
private const val LEGACY_ALL_BRIGHTNESS_OFF_THRESHOLD = 0.06f
