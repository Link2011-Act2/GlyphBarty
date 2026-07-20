package jp.linkserver.glyphvisualizer.glyph

interface GlyphOutputController {
    fun bind()
    fun unbind()
    fun setReverseDirection(reverse: Boolean)
    fun setGlyphMode(mode: String)
    fun setFillOtherGlyphLightsEnabled(enabled: Boolean) {}
    fun setBinaryMode(binary: Boolean)
    fun setBaseIndicatorEnabled(enabled: Boolean) {}
    fun setLevelAutoScaleEnabled(enabled: Boolean) {}
    fun setOutputGamma(gamma: Float) {}
    fun setSpectrumAutoScaleEnabled(enabled: Boolean) {}
    fun setExperimentalPerformanceOptimizationsEnabled(enabled: Boolean) {}
    fun setMatrixSmoothMotionEnabled(enabled: Boolean) {}
    fun setAllBrightnessAutoScaleEnabled(enabled: Boolean) {}
    fun setAutoScaleWindowSeconds(seconds: Float) {}
    fun setAutoScaleOffset(offset: Float) {}
    fun setSmoothing(smoothing: Float, smoothingBalance: Float) {}
    fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?,
        phone4aBaseBandLevel: Float = 0f,
        waveformSamples: FloatArray? = null,
        leftWaveformSamples: FloatArray? = null,
        rightWaveformSamples: FloatArray? = null
    ) {}
    fun previewLevel(): Float = 0f
    fun previewSpectrumBands(): FloatArray = FloatArray(0)
    fun updateLevel(level: Float)
    fun turnOff()
    fun releaseSession() {
        turnOff()
    }
    fun suspendSession() {
        releaseSession()
    }
}
