package jp.linkserver.glyphvisualizer.audio

data class AudioAnalysisFrame(
    val level: Float,
    val peak: Float,
    val lowEnergy: Float,
    val highEnergy: Float,
    val leftLevel: Float,
    val rightLevel: Float,
    val spectrumBands: FloatArray,
    val spectrumRawPeak: Float,
    val phone4aBaseBandLevel: Float,
    val waveformSamples: FloatArray,
    val leftWaveformSamples: FloatArray,
    val rightWaveformSamples: FloatArray
)

typealias AudioLevelCallback = (
    level: Float,
    peak: Float,
    lowEnergy: Float,
    highEnergy: Float,
    leftLevel: Float,
    rightLevel: Float,
    spectrumBands: FloatArray,
    spectrumRawPeak: Float,
    phone4aBaseBandLevel: Float,
    waveformSamples: FloatArray,
    leftWaveformSamples: FloatArray,
    rightWaveformSamples: FloatArray
) -> Unit

internal fun AudioAnalysisFrame.deliverTo(callback: AudioLevelCallback) {
    callback(
        level,
        peak,
        lowEnergy,
        highEnergy,
        leftLevel,
        rightLevel,
        spectrumBands,
        spectrumRawPeak,
        phone4aBaseBandLevel,
        waveformSamples,
        leftWaveformSamples,
        rightWaveformSamples
    )
}
