package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterMeterScalingTest {
    @Test
    fun autoScaleDisabled_keepsOriginalLevel() {
        assertEquals(0.65f, centerMeterDisplayLevel(0.65f, 3, false), 0.0001f)
    }

    @Test
    fun unsupportedSlotCounts_keepOriginalLevel() {
        assertEquals(0.65f, centerMeterDisplayLevel(0.65f, 1, true), 0.0001f)
        assertEquals(0.65f, centerMeterDisplayLevel(0.65f, 5, true), 0.0001f)
    }

    @Test
    fun twoToFourSlots_expandCenterDisplayRange() {
        val input = 0.65f
        val twoSlots = centerMeterDisplayLevel(input, 2, true)
        val threeSlots = centerMeterDisplayLevel(input, 3, true)
        val fourSlots = centerMeterDisplayLevel(input, 4, true)

        assertTrue(twoSlots < threeSlots)
        assertTrue(threeSlots < fourSlots)
        assertEquals(1f, centerMeterDisplayLevel(0.85f, 3, true), 0.0001f)
    }
}
