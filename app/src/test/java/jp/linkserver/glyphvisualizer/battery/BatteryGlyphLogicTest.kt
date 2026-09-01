package jp.linkserver.glyphvisualizer.battery

import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryGlyphLogicTest {
    @Test
    fun batteryPercentMapsNaturallyToDiscreteChannels() {
        assertEquals(0, BatteryGlyphLogic.litChannelCount(0, 24))
        assertEquals(1, BatteryGlyphLogic.litChannelCount(1, 24))
        assertEquals(6, BatteryGlyphLogic.litChannelCount(25, 24))
        assertEquals(12, BatteryGlyphLogic.litChannelCount(50, 24))
        assertEquals(18, BatteryGlyphLogic.litChannelCount(75, 24))
        assertEquals(24, BatteryGlyphLogic.litChannelCount(100, 24))
        assertEquals(4, BatteryGlyphLogic.litChannelCount(100, 4))
    }

    @Test
    fun catalogDefinesOnlyTheRequestedPhoneFamiliesAndDirections() {
        assertChannels("23111", (0..23).toList())
        assertChannels("23113", (0..23).toList())
        assertChannels("24111", (30 downTo 20).toList())
        assertChannels("25111", (0..5).toList())
        assertChannels("25131", (0..3).toList())

        assertNull(
            GlyphDeviceCatalog.definitionForModelCode("22111")
                ?.let(GlyphDeviceCatalog::batteryIndicatorSpecFor)
        )
        assertNull(
            GlyphDeviceCatalog.definitionForModelCode("25111p")
                ?.let(GlyphDeviceCatalog::batteryIndicatorSpecFor)
        )
    }

    @Test
    fun phone4FramesNeverTouchRecordingLightChannels() {
        val phone4a = batterySpec("25111")
        val phone4b = batterySpec("25131")

        val phone4aFrame = BatteryGlyphLogic.frameForLitCount(phone4a, 6)
        val phone4bFrame = BatteryGlyphLogic.frameForLitCount(phone4b, 4)

        assertEquals(7, phone4aFrame.size)
        assertArrayEquals(IntArray(6) { BatteryGlyphLogic.MAX_BRIGHTNESS }, phone4aFrame.take(6).toIntArray())
        assertEquals(0, phone4aFrame[6])
        assertArrayEquals(IntArray(4) { BatteryGlyphLogic.MAX_BRIGHTNESS }, phone4bFrame)
    }

    @Test
    fun shakeRequiresChargingFaceDownTwoPeaksAndCooldown() {
        val detector = BatteryShakeDetector()

        assertFalse(detector.onSample(false, -9f, 14f, 100L))
        assertFalse(detector.onSample(false, -9f, 14f, 200L))
        assertFalse(detector.onSample(true, 9f, 14f, 300L))
        assertFalse(detector.onSample(true, 9f, 14f, 400L))

        assertFalse(detector.onSample(true, -9f, 14f, 500L))
        assertTrue(detector.onSample(true, -9f, 14f, 600L))

        assertFalse(detector.onSample(true, -9f, 14f, 700L))
        assertFalse(detector.onSample(true, -9f, 14f, 800L))

        assertFalse(detector.onSample(true, -9f, 14f, 2_200L))
        assertTrue(detector.onSample(true, -9f, 14f, 2_300L))
    }

    @Test
    fun fullBarAnimationTimingScalesWithBatteryPercent() {
        assertEquals(250L, BatteryGlyphLogic.animationDurationMs(100))
        assertEquals(125L, BatteryGlyphLogic.animationDurationMs(50))
        assertEquals(0L, BatteryGlyphLogic.animationDurationMs(0))
    }

    private fun assertChannels(modelCode: String, expected: List<Int>) {
        assertEquals(expected, batterySpec(modelCode).orderedChannels)
    }

    private fun batterySpec(modelCode: String) = requireNotNull(
        GlyphDeviceCatalog.definitionForModelCode(modelCode)
    ).let { definition ->
        requireNotNull(GlyphDeviceCatalog.batteryIndicatorSpecFor(definition))
    }
}
