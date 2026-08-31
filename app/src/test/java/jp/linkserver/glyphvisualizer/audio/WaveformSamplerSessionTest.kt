package jp.linkserver.glyphvisualizer.audio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformSamplerSessionTest {
    @After
    fun resetGlobalSettingFacade() {
        WaveformSampler.setAutoTimeAxisEnabled(false)
    }

    @Test
    fun captureSessions_doNotShareWaveformHistory() {
        WaveformSampler.setAutoTimeAxisEnabled(true)
        val first = WaveformSampler.createCaptureSession()
        first.mono.downsample(FloatArray(200) { 1f })

        val second = WaveformSampler.createCaptureSession()
        val secondResult = second.mono.downsample(FloatArray(12))

        assertArrayEquals(FloatArray(25), secondResult, 0f)
        first.close()
        second.close()
    }

    @Test
    fun monoLeftAndRightChannels_keepIndependentHistory() {
        WaveformSampler.setAutoTimeAxisEnabled(true)
        val capture = WaveformSampler.createCaptureSession()
        capture.mono.downsample(FloatArray(200) { 1f })

        val monoWithHistory = capture.mono.downsample(FloatArray(12))
        val leftWithoutHistory = capture.left.downsample(FloatArray(12))
        val rightWithoutHistory = capture.right.downsample(FloatArray(12))

        assertTrue(monoWithHistory.any { it != 0f })
        assertArrayEquals(FloatArray(25), leftWithoutHistory, 0f)
        assertArrayEquals(FloatArray(25), rightWithoutHistory, 0f)
        assertFalse(monoWithHistory.contentEquals(leftWithoutHistory))
        capture.close()
    }

    @Test
    fun fixedTimeAxis_keepsThePreviousStatelessOutput() {
        val samples = floatArrayOf(
            -0.1f, 0.2f, 0.3f, -0.8f, 0.4f, 0.9f, -0.2f, -0.5f,
            0.7f, 0.1f, -0.4f, 0.6f, -0.3f, 0.2f, -0.9f, 0.8f
        )
        val session = WaveformSamplerSession(autoTimeAxisEnabledProvider = { false })

        val actual = session.downsample(samples, targetCount = 7)
        val expected = legacyFixedDownsample(samples, targetCount = 7)

        assertArrayEquals(expected, actual, 0f)
    }

    @Test
    fun inactiveCaptureSession_cannotOverwriteTheCurrentTimeAxisReadout() {
        WaveformSampler.setAutoTimeAxisEnabled(true)
        val first = WaveformSampler.createCaptureSession()
        first.right.downsample(sineWave(period = 12, sampleCount = 400))
        val firstMultiplier = WaveformSampler.currentAutoTimeAxisMultiplier()

        val second = WaveformSampler.createCaptureSession()
        second.right.downsample(sineWave(period = 80, sampleCount = 400))
        val secondMultiplier = WaveformSampler.currentAutoTimeAxisMultiplier()
        first.right.downsample(sineWave(period = 12, sampleCount = 400))

        assertNotEquals(firstMultiplier, secondMultiplier)
        assertEquals(secondMultiplier, WaveformSampler.currentAutoTimeAxisMultiplier(), 0f)
        first.close()
        second.close()
    }

    private fun legacyFixedDownsample(samples: FloatArray, targetCount: Int): FloatArray {
        val displayLength = targetCount * 7
        val sourceLength = displayLength.coerceAtMost(samples.size).coerceAtLeast(1)
        val searchEnd = (samples.size - 1).coerceAtMost(samples.size / 2)
        var sourceStart = 0
        for (index in 1..searchEnd) {
            if (samples[index - 1] < 0f && samples[index] >= 0f) {
                sourceStart = index
                break
            }
        }
        return FloatArray(targetCount) { index ->
            val start = (sourceStart + ((index.toFloat() / targetCount) * sourceLength).toInt())
                .coerceIn(sourceStart, samples.lastIndex)
            val end = (
                sourceStart +
                    ((((index + 1f) / targetCount) * sourceLength).toInt() - 1)
                ).coerceIn(start, samples.lastIndex)
            var peak = 0f
            var signedPeak = 0f
            for (sampleIndex in start..end) {
                val sample = samples[sampleIndex].coerceIn(-1f, 1f)
                val amplitude = kotlin.math.abs(sample)
                if (amplitude >= peak) {
                    peak = amplitude
                    signedPeak = sample
                }
            }
            signedPeak
        }
    }

    private fun sineWave(period: Int, sampleCount: Int): FloatArray =
        FloatArray(sampleCount) { index ->
            sin((2.0 * PI * index) / period).toFloat()
        }
}
