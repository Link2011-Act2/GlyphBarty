package jp.linkserver.glyphvisualizer.audio

internal class SpectrumAnalysisProcessor(
    private val refreshIntervalMs: Long = 33L,
    private val bandCount: Int = 25
) {
    private var lastAnalysis = SpectrumAnalyzer.AnalysisResult(
        bands = FloatArray(bandCount),
        normalizedRangePeak = 0f,
        rangePeak = 0f
    )
    private var lastAnalysisAtMs = 0L

    fun analyze(
        samples: FloatArray,
        sampleRateHz: Int,
        performanceOptimizationsEnabled: Boolean,
        nowMs: Long
    ): SpectrumAnalyzer.AnalysisResult {
        val shouldRefresh =
            !performanceOptimizationsEnabled ||
                lastAnalysisAtMs <= 0L ||
                (nowMs - lastAnalysisAtMs) >= refreshIntervalMs
        if (!shouldRefresh) return lastAnalysis

        return SpectrumAnalyzer.analyzeLogBands(
            samples = samples,
            sampleRateHz = sampleRateHz,
            bandCount = bandCount
        ).also {
            lastAnalysis = it
            lastAnalysisAtMs = nowMs
        }
    }
}
