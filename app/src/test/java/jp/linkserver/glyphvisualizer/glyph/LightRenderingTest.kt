package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LightRenderingTest {
    @Test
    fun meterScaling_preservesFullAndPartialSlotMath() {
        assertEquals(
            SignalScale(fullSlots = 2, edgeBrightness = 2048),
            SignalScalingPipeline.meter(
                level = 0.5f,
                slotCount = 5,
                binaryMode = false,
                fullBrightness = 4095
            )
        )
        assertEquals(
            SignalScale(fullSlots = 2, edgeBrightness = 0),
            SignalScalingPipeline.meter(
                level = 0.5f,
                slotCount = 5,
                binaryMode = true,
                fullBrightness = 4095
            )
        )
    }

    @Test
    fun linearRenderer_preservesPhysicalChannelDirection() {
        val normal = IntArray(5)
        val reversed = IntArray(5)
        val scale = SignalScale(fullSlots = 1, edgeBrightness = 1024)

        LightPatternRenderer.renderLinear(normal, 1..3, scale, reverseDirection = false, fullBrightness = 4095)
        LightPatternRenderer.renderLinear(reversed, 1..3, scale, reverseDirection = true, fullBrightness = 4095)

        assertArrayEquals(intArrayOf(0, 4095, 1024, 0, 0), normal)
        assertArrayEquals(intArrayOf(0, 0, 1024, 4095, 0), reversed)
    }

    @Test
    fun centerRenderer_keepsPhysicalGroupIndexesTogether() {
        val colors = IntArray(8)
        val physicalSlots = listOf(listOf(3, 4), listOf(2, 5), listOf(1, 6))

        LightPatternRenderer.renderCenterSlots(
            colors = colors,
            slots = physicalSlots,
            scale = SignalScale(fullSlots = 1, edgeBrightness = 2048),
            fullBrightness = 4095,
            allowedRange = 1..6
        )

        assertArrayEquals(intArrayOf(0, 0, 2048, 4095, 4095, 2048, 0, 0), colors)
    }

    @Test
    fun brightnessBoost_preservesExistingExponentFormula() {
        val value = 0.25f
        val expected = 1f - (1f - value) * (1f - value)

        assertEquals(expected, SignalScalingPipeline.boost(value, exponent = 2f), 0.000001f)
    }
}
