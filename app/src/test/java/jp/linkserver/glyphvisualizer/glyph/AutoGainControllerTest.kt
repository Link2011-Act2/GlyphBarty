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

        val quietOutput = applySpectrumOverallLevel(
            normalizedBands,
            spectrumOverallLevelTarget(quietRawPeak, quietGain)
        )
        val loudOutput = applySpectrumOverallLevel(
            normalizedBands,
            spectrumOverallLevelTarget(loudRawPeak, loudGain)
        )

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
        val output = applySpectrumOverallLevel(
            floatArrayOf(0.25f, 0.5f, 1f),
            spectrumOverallLevelTarget(rawPeak, gain)
        )

        assertEquals(0.98f / rawPeak, gain, 0.0001f)
        assertArrayEquals(floatArrayOf(0.245f, 0.49f, 0.98f), output, 0.0001f)
    }

    @Test
    fun spectrumOverallEnvelope_rawPeakDropDoesNotImmediatelyBecomeTheDisplayDrop() {
        val envelope = SpectrumOverallLevelEnvelope()
        envelope.update(spectrumOverallLevelTarget(0.8f, 1f), 0L)

        val displayOverall = envelope.update(spectrumOverallLevelTarget(0.2f, 1f), 16L)

        assertTrue(displayOverall < 0.8f)
        assertTrue(displayOverall > 0.2f)
    }

    @Test
    fun spectrumOverallEnvelope_attackTracksFasterThanRelease() {
        val attackEnvelope = SpectrumOverallLevelEnvelope()
        val releaseEnvelope = SpectrumOverallLevelEnvelope()
        attackEnvelope.update(0.2f, 0L)
        releaseEnvelope.update(0.8f, 0L)

        val attacked = attackEnvelope.update(0.8f, 50L)
        val released = releaseEnvelope.update(0.2f, 50L)
        val attackMovement = attacked - 0.2f
        val releaseMovement = 0.8f - released

        assertEquals(50f, SPECTRUM_OVERALL_LEVEL_ATTACK_TAU_MS, 0f)
        assertEquals(220f, SPECTRUM_OVERALL_LEVEL_RELEASE_TAU_MS, 0f)
        assertTrue(attackMovement > releaseMovement)
    }

    @Test
    fun spectrumOverallEnvelope_explicitSilenceResetReturnsToZero() {
        val envelope = SpectrumOverallLevelEnvelope()
        envelope.update(0.8f, 0L)
        envelope.update(0.2f, 16L)

        envelope.reset()

        assertEquals(0f, envelope.update(0f, 32L), 0f)
    }

    @Test
    fun spectrumOverallEnvelope_hasNearlyTheSameResponseAt16And33Milliseconds() {
        val responseAt16Ms = simulateOverallEnvelope(
            initialLevel = 0.8f,
            targetLevel = 0.2f,
            frameIntervalMs = 16L,
            totalElapsedMs = 528L
        )
        val responseAt33Ms = simulateOverallEnvelope(
            initialLevel = 0.8f,
            targetLevel = 0.2f,
            frameIntervalMs = 33L,
            totalElapsedMs = 528L
        )

        assertEquals(responseAt16Ms, responseAt33Ms, 0.0001f)
    }

    @Test
    fun spectrumOverallEnvelope_preservesTheSharedGainSafetyCeiling() {
        val envelope = SpectrumOverallLevelEnvelope()
        val unsafeTarget = spectrumOverallLevelTarget(rawPeak = 1f, sharedGain = 100f)
        var displayOverall = envelope.update(0.2f, 0L)

        for (timeMs in 16L..1_600L step 16L) {
            displayOverall = envelope.update(unsafeTarget, timeMs)
            assertTrue(displayOverall <= DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
        }
        val output = applySpectrumOverallLevel(floatArrayOf(0.25f, 0.5f, 1f), displayOverall)

        assertEquals(DEFAULT_AUTO_GAIN_OUTPUT_CEILING, unsafeTarget, 0f)
        assertTrue(output.max() <= DEFAULT_AUTO_GAIN_OUTPUT_CEILING)
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

    private fun simulateOverallEnvelope(
        initialLevel: Float,
        targetLevel: Float,
        frameIntervalMs: Long,
        totalElapsedMs: Long
    ): Float {
        val envelope = SpectrumOverallLevelEnvelope()
        envelope.update(initialLevel, 0L)
        var output = initialLevel
        var nowMs = frameIntervalMs
        while (nowMs <= totalElapsedMs) {
            output = envelope.update(targetLevel, nowMs)
            nowMs += frameIntervalMs
        }
        return output
    }
}
