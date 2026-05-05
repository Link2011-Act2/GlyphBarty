package jp.linkserver.glyphvisualizer.glyph

interface GlyphOutputController {
    fun bind()
    fun unbind()
    fun setReverseDirection(reverse: Boolean)
    fun setGlyphMode(mode: String)
    fun setBinaryMode(binary: Boolean)
    fun setLevelAutoScaleEnabled(enabled: Boolean) {}
    fun setOutputGamma(gamma: Float) {}
    fun setSpectrumAutoScaleEnabled(enabled: Boolean) {}
    fun setAllBrightnessAutoScaleEnabled(enabled: Boolean) {}
    fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?
    ) {}
    fun updateLevel(level: Float)
    fun turnOff()
}
