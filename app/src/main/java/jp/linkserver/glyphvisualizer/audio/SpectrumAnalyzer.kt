package jp.linkserver.glyphvisualizer.audio

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object SpectrumAnalyzer {
    fun computeLogBands(
        samples: FloatArray,
        sampleRateHz: Int,
        bandCount: Int,
        minFreqHz: Float = 40f
    ): FloatArray {
        if (samples.size < 64 || bandCount <= 0 || sampleRateHz <= 0) {
            return FloatArray(bandCount.coerceAtLeast(0))
        }

        val n = highestPowerOfTwo(samples.size.coerceAtMost(256))
        val start = samples.size - n
        val magnitudes = FloatArray(n / 2)

        // Hann window + naive DFT (n<=256 に制限して計算量を抑える)
        for (k in 1 until n / 2) {
            var re = 0.0
            var im = 0.0
            for (i in 0 until n) {
                val w = 0.5 - 0.5 * cos((2.0 * Math.PI * i) / (n - 1).coerceAtLeast(1))
                val s = samples[start + i] * w
                val phase = (2.0 * Math.PI * k * i) / n
                re += s * cos(phase)
                im -= s * sin(phase)
            }
            magnitudes[k] = (sqrt(re * re + im * im) / n).toFloat()
        }

        val nyquist = sampleRateHz / 2f
        val safeMin = minFreqHz.coerceAtLeast(20f)
        val safeMax = nyquist.coerceAtLeast(safeMin + 1f)
        val ratio = (safeMax / safeMin).coerceAtLeast(1.0001f)

        val bands = FloatArray(bandCount)
        var maxBand = 1e-6f
        for (band in 0 until bandCount) {
            val f0 = safeMin * ratio.pow(band / bandCount.toFloat())
            val f1 = safeMin * ratio.pow((band + 1f) / bandCount.toFloat())
            val k0 = ((f0 / sampleRateHz) * n).toInt().coerceIn(1, magnitudes.lastIndex)
            val k1 = ((f1 / sampleRateHz) * n).toInt().coerceIn(k0, magnitudes.lastIndex)

            var sum = 0f
            var count = 0
            for (k in k0..k1) {
                sum += magnitudes[k]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            // 見た目用に軽く圧縮
            val v = sqrt(avg.coerceAtLeast(0f))
            bands[band] = v
            if (v > maxBand) maxBand = v
        }

        if (maxBand > 0f) {
            for (i in bands.indices) {
                bands[i] = (bands[i] / maxBand).coerceIn(0f, 1f)
            }
        }
        return bands
    }

    private fun highestPowerOfTwo(v: Int): Int {
        var n = 1
        while (n * 2 <= v) n *= 2
        return n
    }
}
