package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

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

    @Test
    fun spectrumContinuousBrightness_appliesConfiguredBoostExponent() {
        val shaped = 0.25f
        val exponent = GlyphLightController.SPECTRUM_BRIGHTNESS_BOOST_EXPONENT
        val boosted = SignalScalingPipeline.boost(shaped, exponent)
        val brightness = LightPatternRenderer.spectrumBrightness(
            shaped = shaped,
            binaryMode = false,
            fullBrightness = 4095,
            boostExponent = exponent
        )

        assertEquals(1.5f, exponent, 0f)
        assertEquals((boosted * 4095).roundToInt(), brightness)
        assertTrue(brightness > (shaped * 4095).toInt())
    }

    @Test
    fun spectrumBinaryBrightness_keepsShapedHalfThresholdWithoutBoost() {
        assertEquals(
            0,
            LightPatternRenderer.spectrumBrightness(
                shaped = 0.4999f,
                binaryMode = true,
                fullBrightness = 4095,
                boostExponent = GlyphLightController.SPECTRUM_BRIGHTNESS_BOOST_EXPONENT
            )
        )
        assertEquals(
            4095,
            LightPatternRenderer.spectrumBrightness(
                shaped = 0.5f,
                binaryMode = true,
                fullBrightness = 4095,
                boostExponent = GlyphLightController.SPECTRUM_BRIGHTNESS_BOOST_EXPONENT
            )
        )
    }

    @Test
    fun spectrumContinuousBrightness_preservesZeroAndFullScaleEndpoints() {
        val exponent = GlyphLightController.SPECTRUM_BRIGHTNESS_BOOST_EXPONENT

        assertEquals(0, LightPatternRenderer.spectrumBrightness(0f, false, 4095, exponent))
        assertEquals(4095, LightPatternRenderer.spectrumBrightness(1f, false, 4095, exponent))
    }
}
