package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.audio.SpectrumAnalyzer
import org.junit.Assert.*
import org.junit.Test

class MatrixSpectrumSignalGateTest {
    @Test fun normalizedNoiseShapeIsNotAnAbsoluteSignalMeasurement() {
        val samples = FloatArray(256) { if (it % 7 == 0) 0.00002f else -0.00001f }
        val analysis = SpectrumAnalyzer.analyzeLogBands(samples, 44100, 25)
        assertEquals(1f, analysis.bands.max(), 0f) // This was mistakenly used as the Matrix raw peak.
        val gate = MatrixSpectrumSignalGate()
        gate.update(analysis.rawPeak, samples, 1000)
        assertTrue(gate.quiet)
        assertTrue(gate.holdGainIncrease)
    }

    @Test fun silenceReleasesIn120msAndSmallMusicReopensImmediately() {
        val gate = MatrixSpectrumSignalGate()
        gate.update(0.2f, FloatArray(25) { 0.1f }, 1000)
        assertFalse(gate.quiet)
        gate.update(0f, FloatArray(25), 1100)
        assertFalse(gate.quiet)
        assertTrue(gate.holdGainIncrease)
        gate.update(0f, FloatArray(25), 1219)
        assertFalse(gate.quiet)
        gate.update(0f, FloatArray(25), 1220)
        assertTrue(gate.quiet)
        gate.update(0.004f, FloatArray(25) { 0.0001f }, 1221)
        assertFalse(gate.quiet) // Raw PCM preserves a very soft instrument even below the FFT threshold.
    }

    @Test fun absentWaveformUsesRawSpectrumAndQuietDoesNotAccumulateGainSamples() {
        val gate = MatrixSpectrumSignalGate()
        gate.update(0.01f, null, 1000)
        assertFalse(gate.quiet)
        for (now in 1100L..6000L step 100L) gate.update(0.0001f, null, now)
        assertTrue(gate.quiet)
        assertTrue(gate.holdGainIncrease)
        gate.update(0.01f, null, 6001)
        assertFalse(gate.quiet)
    }
}
