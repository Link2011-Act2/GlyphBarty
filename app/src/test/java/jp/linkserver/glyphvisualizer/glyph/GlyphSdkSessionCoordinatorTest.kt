package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphSdkSessionCoordinatorTest {
    @Test
    fun batteryPreemptsVisualizerAndResumesItAfterRelease() {
        val batteryToken = Any()
        val visualizerToken = Any()
        var suspensions = 0
        var resumptions = 0

        assertTrue(
            GlyphSdkSessionCoordinator.claimVisualizer(
                token = visualizerToken,
                onSuspend = { suspensions += 1 },
                onResume = { resumptions += 1 }
            )
        )
        assertTrue(GlyphSdkSessionCoordinator.tryClaimBattery(batteryToken))

        assertEquals(1, suspensions)
        assertEquals(0, resumptions)
        assertFalse(GlyphSdkSessionCoordinator.tryClaimBattery(Any()))

        GlyphSdkSessionCoordinator.releaseBattery(batteryToken)

        assertEquals(1, resumptions)
        GlyphSdkSessionCoordinator.releaseVisualizer(visualizerToken)
    }

    @Test
    fun visualizerStartedDuringBatteryWaitsUntilBatteryFinishes() {
        val batteryToken = Any()
        val visualizerToken = Any()
        var resumptions = 0

        assertTrue(GlyphSdkSessionCoordinator.tryClaimBattery(batteryToken))
        assertFalse(
            GlyphSdkSessionCoordinator.claimVisualizer(
                token = visualizerToken,
                onSuspend = {},
                onResume = { resumptions += 1 }
            )
        )
        assertEquals(0, resumptions)

        GlyphSdkSessionCoordinator.releaseBattery(batteryToken)

        assertEquals(1, resumptions)
        GlyphSdkSessionCoordinator.releaseVisualizer(visualizerToken)
    }
}
