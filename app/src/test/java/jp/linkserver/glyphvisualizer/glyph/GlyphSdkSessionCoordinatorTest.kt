package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSdkSessionCoordinatorTest {
    @Test
    fun visualizerPreemptsBatteryAndKeepsExclusiveOwnership() {
        val batteryToken = Any()
        val visualizerToken = Any()
        var preemptions = 0

        assertTrue(
            GlyphSdkSessionCoordinator.tryClaimBattery(batteryToken) { preemptions += 1 }
        )
        GlyphSdkSessionCoordinator.claimVisualizer(visualizerToken)

        assertEquals(1, preemptions)
        assertFalse(GlyphSdkSessionCoordinator.tryClaimBattery(Any()) {})

        GlyphSdkSessionCoordinator.releaseVisualizer(visualizerToken)
        val nextBatteryToken = Any()
        assertTrue(GlyphSdkSessionCoordinator.tryClaimBattery(nextBatteryToken) {})
        GlyphSdkSessionCoordinator.releaseBattery(nextBatteryToken)
    }
}
