package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.pow
import kotlin.math.roundToInt

internal data class SignalScale(
    val fullSlots: Int,
    val edgeBrightness: Int
)

internal object SignalScalingPipeline {
    fun meter(
        level: Float,
        slotCount: Int,
        binaryMode: Boolean,
        fullBrightness: Int
    ): SignalScale {
        if (slotCount <= 0) return SignalScale(fullSlots = 0, edgeBrightness = 0)
        val virtualSlots = level.coerceIn(0f, 1f) * slotCount
        val fullSlots = virtualSlots.toInt().coerceIn(0, slotCount)
        val edgeBrightness = if (binaryMode) {
            0
        } else {
            ((virtualSlots - fullSlots) * fullBrightness).roundToInt().coerceIn(0, fullBrightness)
        }
        return SignalScale(fullSlots = fullSlots, edgeBrightness = edgeBrightness)
    }

    fun brightnessForSlot(
        index: Int,
        fullSlots: Int,
        slotCount: Int,
        edgeBrightness: Int,
        fullBrightness: Int
    ): Int {
        return when {
            index < fullSlots -> fullBrightness
            index == fullSlots && fullSlots < slotCount -> edgeBrightness
            else -> 0
        }
    }

    fun brightnessForSlot(
        index: Int,
        fullSlots: Int,
        edgeBrightness: Int,
        fullBrightness: Int
    ): Int {
        return when {
            index < fullSlots -> fullBrightness
            index == fullSlots -> edgeBrightness
            else -> 0
        }
    }

    fun boost(value: Float, exponent: Float): Float {
        val clamped = value.coerceIn(0f, 1f)
        return 1f - (1f - clamped).pow(exponent)
    }
}

internal object LightPatternRenderer {
    fun spectrumBrightness(
        shaped: Float,
        binaryMode: Boolean,
        fullBrightness: Int,
        boostExponent: Float
    ): Int {
        if (binaryMode) return if (shaped >= 0.5f) fullBrightness else 0
        val boosted = SignalScalingPipeline.boost(shaped, boostExponent)
        return (boosted * fullBrightness).roundToInt().coerceIn(0, fullBrightness)
    }

    fun renderLinear(
        colors: IntArray,
        range: IntRange,
        scale: SignalScale,
        reverseDirection: Boolean,
        fullBrightness: Int
    ) {
        val channels = if (reverseDirection) range.reversed().toList() else range.toList()
        channels.forEachIndexed { index, channel ->
            val brightness = SignalScalingPipeline.brightnessForSlot(
                index = index,
                fullSlots = scale.fullSlots,
                slotCount = channels.size,
                edgeBrightness = scale.edgeBrightness,
                fullBrightness = fullBrightness
            )
            if (brightness > 0 && channel in colors.indices) {
                colors[channel] = brightness
            }
        }
    }

    fun renderCenterSlots(
        colors: IntArray,
        slots: List<List<Int>>,
        scale: SignalScale,
        fullBrightness: Int,
        allowedRange: IntRange? = null
    ) {
        slots.forEachIndexed { index, channels ->
            val brightness = SignalScalingPipeline.brightnessForSlot(
                index = index,
                fullSlots = scale.fullSlots,
                slotCount = slots.size,
                edgeBrightness = scale.edgeBrightness,
                fullBrightness = fullBrightness
            )
            channels.forEach { channel ->
                if (
                    brightness > 0 &&
                    channel in colors.indices &&
                    (allowedRange == null || channel in allowedRange)
                ) {
                    colors[channel] = brightness
                }
            }
        }
    }

    fun renderAllBrightness(colors: IntArray, brightness: Int) {
        colors.fill(brightness)
    }
}
