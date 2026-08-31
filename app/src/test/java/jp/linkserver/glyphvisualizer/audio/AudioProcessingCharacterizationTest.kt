package jp.linkserver.glyphvisualizer.audio

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProcessingCharacterizationTest {
    @Test
    fun levelEnvelopeProcessor_matchesThePreviousSharedFormulaAcrossFrames() {
        val inputs = listOf(
            LevelEnvelopeInput(0.32f, 0.18f, 0.47f, 1.75f, 0.08f, 1.45f, -0.4f, 0.30f),
            LevelEnvelopeInput(0.09f, 0.04f, 0.12f, 1.20f, 0.14f, 1.80f, 0.35f, 0.22f),
            LevelEnvelopeInput(0.71f, 0.54f, 0.82f, 2.10f, 0.03f, 0.75f, 0.8f, 0.60f),
            LevelEnvelopeInput(0.02f, 0.01f, 0.03f, 0.90f, 0.20f, 2.30f, 0f, 0.05f)
        )
        val processor = LevelEnvelopeProcessor()
        val legacy = LegacyEnvelopeState()

        inputs.forEach { input ->
            val expected = legacy.process(input)
            val actual = processor.process(input)

            assertEquals(expected.level, actual.level, FLOAT_TOLERANCE)
            assertEquals(expected.peak, actual.peak, FLOAT_TOLERANCE)
            assertEquals(expected.release, actual.release, FLOAT_TOLERANCE)
        }
    }

    @Test
    fun spectrumProcessor_preservesAnalysisAndThirtyThreeMillisecondRefreshCondition() {
        val firstSamples = sineWave(sampleCount = 128, cycles = 3f)
        val secondSamples = sineWave(sampleCount = 128, cycles = 11f)
        val processor = SpectrumAnalysisProcessor()

        val first = processor.analyze(firstSamples, 44_100, true, nowMs = 100L)
        val direct = SpectrumAnalyzer.analyzeLogBands(firstSamples, 44_100, 25)
        val cached = processor.analyze(secondSamples, 44_100, true, nowMs = 132L)
        val refreshed = processor.analyze(secondSamples, 44_100, true, nowMs = 133L)
        val unthrottled = processor.analyze(firstSamples, 44_100, false, nowMs = 134L)

        assertArrayEquals(direct.bands, first.bands, FLOAT_TOLERANCE)
        assertEquals(direct.rangePeak, first.rangePeak, FLOAT_TOLERANCE)
        assertSame(first, cached)
        assertNotSame(cached, refreshed)
        assertFalse(first.bands.contentEquals(refreshed.bands))
        assertNotSame(refreshed, unthrottled)
    }

    @Test
    fun audioAnalysisFrame_deliversTheExistingCallbackArgumentOrder() {
        val spectrum = floatArrayOf(0.1f, 0.2f)
        val mono = floatArrayOf(0.3f)
        val left = floatArrayOf(0.4f)
        val right = floatArrayOf(0.5f)
        val frame = AudioAnalysisFrame(
            level = 0.11f,
            peak = 0.22f,
            lowEnergy = 0.33f,
            highEnergy = 0.44f,
            leftLevel = 0.55f,
            rightLevel = 0.66f,
            spectrumBands = spectrum,
            phone4aBaseBandLevel = 0.77f,
            waveformSamples = mono,
            leftWaveformSamples = left,
            rightWaveformSamples = right
        )
        var delivered = false

        frame.deliverTo { level, peak, lowEnergy, highEnergy, leftLevel, rightLevel,
                          spectrumBands, phone4aBaseBandLevel, waveformSamples,
                          leftWaveformSamples, rightWaveformSamples ->
            assertEquals(frame.level, level, 0f)
            assertEquals(frame.peak, peak, 0f)
            assertEquals(frame.lowEnergy, lowEnergy, 0f)
            assertEquals(frame.highEnergy, highEnergy, 0f)
            assertEquals(frame.leftLevel, leftLevel, 0f)
            assertEquals(frame.rightLevel, rightLevel, 0f)
            assertSame(spectrum, spectrumBands)
            assertEquals(frame.phone4aBaseBandLevel, phone4aBaseBandLevel, 0f)
            assertSame(mono, waveformSamples)
            assertSame(left, leftWaveformSamples)
            assertSame(right, rightWaveformSamples)
            delivered = true
        }

        assertTrue(delivered)
    }

    private class LegacyEnvelopeState {
        private var smoothedLevel = 0f
        private var displayedLevel = 0f

        fun process(input: LevelEnvelopeInput): LevelEnvelopeResult {
            val toneFocus = input.toneFocus.coerceIn(-1f, 1f)
            val focusedLevel = when {
                toneFocus < 0f -> {
                    val bassMix = -toneFocus
                    (input.baseLevel * (1f - bassMix)) + (input.lowEnergy * bassMix)
                }

                toneFocus > 0f -> {
                    val trebleMix = toneFocus
                    (input.baseLevel * (1f - trebleMix)) + (input.highEnergy * trebleMix)
                }

                else -> input.baseLevel
            }
            val normalized = focusedLevel * input.sensitivity
            val gate = input.noiseGate.coerceIn(0f, 0.95f)
            val gated = ((normalized - gate) / (1f - gate)).coerceIn(0f, 1f)
            val bounded = gated.pow(input.dynamics.coerceIn(0.6f, 2.4f)).coerceIn(0f, 1f)
            val smoothing = input.smoothing.coerceIn(0.05f, 0.6f)
            val noReleaseSmoothing = smoothing >= 0.54f
            val primarySmoothing = if (noReleaseSmoothing) {
                1f
            } else {
                (smoothing * 0.6f).coerceIn(0.04f, 0.4f)
            }
            val release = if (noReleaseSmoothing) {
                1f
            } else {
                (smoothing * 1.25f).coerceIn(0.0625f, 0.75f)
            }
            if (bounded > smoothedLevel) {
                smoothedLevel = bounded
            } else {
                smoothedLevel += (bounded - smoothedLevel) * primarySmoothing
            }
            if (smoothedLevel > displayedLevel) {
                displayedLevel = smoothedLevel
            } else {
                displayedLevel += (smoothedLevel - displayedLevel) * release
            }
            return LevelEnvelopeResult(displayedLevel, displayedLevel, release)
        }
    }

    private fun sineWave(sampleCount: Int, cycles: Float): FloatArray =
        FloatArray(sampleCount) { index ->
            sin((2.0 * PI * cycles * index) / sampleCount).toFloat()
        }

    private companion object {
        const val FLOAT_TOLERANCE = 0.000001f
    }
}
