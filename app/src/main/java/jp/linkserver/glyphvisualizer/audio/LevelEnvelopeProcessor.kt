package jp.linkserver.glyphvisualizer.audio

import kotlin.math.pow

internal data class LevelEnvelopeInput(
    val baseLevel: Float,
    val lowEnergy: Float,
    val highEnergy: Float,
    val sensitivity: Float,
    val noiseGate: Float,
    val dynamics: Float,
    val toneFocus: Float,
    val smoothing: Float
)

internal data class LevelEnvelopeResult(
    val level: Float,
    val peak: Float,
    val release: Float
)

/** The byte/PCM source-specific energy calculation deliberately remains outside this class. */
internal class LevelEnvelopeProcessor {
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
        return LevelEnvelopeResult(
            level = displayedLevel,
            peak = displayedLevel,
            release = release
        )
    }
}
