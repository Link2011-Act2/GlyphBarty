package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoGainControllerTest {
    @Test
    fun quietInput_usesUnityDuringWarmupThenInitializesImmediately() {
        val agc = AutoGainController()

        assertEquals(1f, agc.update(0.1f, 0L, 0.85f, 30f, false), 0.0001f)
        assertEquals(1f, agc.update(0.1f, 100L, 0.85f, 30f, false), 0.0001f)
        assertEquals(6f, agc.update(0.1f, 200L, 0.85f, 30f, false), 0.0001f)
    }

    @Test
    fun oneFramePeak_isSafetyLimitedWithoutChangingLongTermGain() {
        val agc = initializedQuietAgc()

        val peakGain = agc.update(0.8f, 220L, 0.85f, 30f, false)
        val recoveredGain = agc.update(0.1f, 240L, 0.85f, 30f, false)

        assertEquals(0.98f / 0.8f, peakGain, 0.0001f)
        assertEquals(6f, recoveredGain, 0.0001f)
    }

    @Test
    fun sustainedLoudInput_reducesSmoothedGain() {
        val agc = initializedQuietAgc()

        for (timeMs in 220L..620L step 20L) {
            agc.update(0.8f, timeMs, 0.85f, 30f, false)
        }
        // A lower probe avoids the stateless 0.98 safety cap and exposes smoothed gain.
        val smoothedGain = agc.update(0.2f, 640L, 0.85f, 30f, false)

        assertTrue(smoothedGain < 2f)
    }

    @Test
    fun longSilence_resetsAndRequiresWarmupAgain() {
        val agc = initializedQuietAgc()

        agc.update(0f, 250L, 0.85f, 30f, true)
        agc.update(0f, 3_250L, 0.85f, 30f, true)

        assertEquals(1f, agc.update(0.1f, 3_300L, 0.85f, 30f, false), 0.0001f)
        assertEquals(6f, agc.update(0.1f, 3_500L, 0.85f, 30f, false), 0.0001f)
    }

    @Test
    fun offsetConversion_addsHeadroom() {
        assertEquals(0.85f, effectiveAutoScaleTargetLevel(0f), 0.0001f)
        assertTrue(effectiveAutoScaleTargetLevel(0.4f) < 0.85f)
    }

    private fun initializedQuietAgc(): AutoGainController {
        return AutoGainController().also { agc ->
            agc.update(0.1f, 0L, 0.85f, 30f, false)
            agc.update(0.1f, 100L, 0.85f, 30f, false)
            agc.update(0.1f, 200L, 0.85f, 30f, false)
        }
    }
}
