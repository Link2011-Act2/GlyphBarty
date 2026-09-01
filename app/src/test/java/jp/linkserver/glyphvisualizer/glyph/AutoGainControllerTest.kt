package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertArrayEquals
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

    @Test
    fun spectrumGain_usesRawPeakWhilePreservingNormalizedBandShape() {
        val normalizedBands = floatArrayOf(0.25f, 0.5f, 1f)
        val quietRawPeak = 0.2f
        val loudRawPeak = 0.95f
        val quietGain = initializedAgc(quietRawPeak, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS)
            .update(quietRawPeak, 220L, 0.85f, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS, false)
        val loudGain = initializedAgc(loudRawPeak, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS)
            .update(loudRawPeak, 220L, 0.85f, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS, false)

        val quietOutput = applySharedSpectrumGain(normalizedBands, quietRawPeak, quietGain)
        val loudOutput = applySharedSpectrumGain(normalizedBands, loudRawPeak, loudGain)

        assertTrue(quietGain > 1f)
        assertTrue(loudGain < 1f)
        assertEquals(0.85f, quietOutput.max(), 0.0001f)
        assertEquals(0.85f, loudOutput.max(), 0.0001f)
        assertEquals(normalizedBands[0] / normalizedBands[2], quietOutput[0] / quietOutput[2], 0.0001f)
        assertEquals(normalizedBands[1] / normalizedBands[2], loudOutput[1] / loudOutput[2], 0.0001f)
    }

    @Test
    fun spectrumGain_safetyLimitMatchesTheAbsoluteBandsItScales() {
        val agc = initializedQuietAgc()
        val rawPeak = 0.8f
        val gain = agc.update(rawPeak, 220L, 0.85f, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS, false)
        val output = applySharedSpectrumGain(floatArrayOf(0.25f, 0.5f, 1f), rawPeak, gain)

        assertEquals(0.98f / rawPeak, gain, 0.0001f)
        assertArrayEquals(floatArrayOf(0.245f, 0.49f, 0.98f), output, 0.0001f)
    }

    @Test
    fun spectrumGain_recoversOnFiveSecondTimeScaleInsteadOfThirtySeconds() {
        val spectrumAgc = initializedQuietAgc()
        val oldThirtySecondAgc = initializedQuietAgc()

        for (timeMs in 220L..1_220L step 20L) {
            spectrumAgc.update(0.8f, timeMs, 0.85f, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS, false)
            oldThirtySecondAgc.update(0.8f, timeMs, 0.85f, 30f, false)
        }

        var spectrumGain = 0f
        var oldThirtySecondGain = 0f
        for (timeMs in 1_240L..8_240L step 20L) {
            spectrumGain = spectrumAgc.update(
                0.1f,
                timeMs,
                0.85f,
                SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS,
                false
            )
            oldThirtySecondGain = oldThirtySecondAgc.update(0.1f, timeMs, 0.85f, 30f, false)
        }

        assertEquals(5f, SPECTRUM_AUTO_GAIN_UP_TAU_SECONDS, 0f)
        assertTrue(spectrumGain > 3.5f)
        assertTrue(spectrumGain > oldThirtySecondGain + 1.5f)
    }

    private fun initializedQuietAgc(): AutoGainController {
        return initializedAgc(0.1f, 30f)
    }

    private fun initializedAgc(rawPeak: Float, gainUpTauSeconds: Float): AutoGainController {
        return AutoGainController().also { agc ->
            agc.update(rawPeak, 0L, 0.85f, gainUpTauSeconds, false)
            agc.update(rawPeak, 100L, 0.85f, gainUpTauSeconds, false)
            agc.update(rawPeak, 200L, 0.85f, gainUpTauSeconds, false)
        }
    }
}
