package jp.linkserver.glyphvisualizer.glyph

interface GlyphOutputController {
    fun bind()
    fun unbind()
    fun setReverseDirection(reverse: Boolean)
    fun setGlyphMode(mode: String)
    fun setBinaryMode(binary: Boolean)
    fun setBaseIndicatorEnabled(enabled: Boolean) {}
    fun setLevelAutoScaleEnabled(enabled: Boolean) {}
    fun setOutputGamma(gamma: Float) {}
    fun setSpectrumAutoScaleEnabled(enabled: Boolean) {}
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
        phone4aBaseBandLevel: Float = 0f
    ) {}
    fun previewLevel(): Float = 0f
    fun previewSpectrumBands(): FloatArray = FloatArray(0)
    fun updateLevel(level: Float)
    fun turnOff()
}
