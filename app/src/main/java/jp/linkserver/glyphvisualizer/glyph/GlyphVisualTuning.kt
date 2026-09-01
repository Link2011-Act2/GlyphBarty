package jp.linkserver.glyphvisualizer.glyph

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class GlyphVisualTuning(
    val dynamics: Float = 0f
)

data class GlyphVisualTuningKey(
    val profile: GlyphDeviceProfile,
    val patternId: String
)

/** Production tuning values. Inspector overrides never mutate this table. */
object GlyphVisualTuningDatabase {
    private val entries: Map<GlyphVisualTuningKey, GlyphVisualTuning> = mapOf(
        // Phone (1)
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_LINEAR) to GlyphVisualTuning(dynamics = 0.4f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.4f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_CENTER) to GlyphVisualTuning(dynamics = 0.6f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_CLASSIC) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (2)
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_LINEAR) to GlyphVisualTuning(dynamics = 0.1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_CENTER) to GlyphVisualTuning(dynamics = 0.4f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_LINEAR) to GlyphVisualTuning(dynamics = 0.4f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.4f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_CENTER) to GlyphVisualTuning(dynamics = 0.6f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_CLASSIC) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (2a)
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_LINEAR) to GlyphVisualTuning(dynamics = 0.55f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.55f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_CENTER) to GlyphVisualTuning(dynamics = 0.6f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_CLASSIC) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (3a)
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_LINEAR) to GlyphVisualTuning(dynamics = 0.5f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.5f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_CENTER) to GlyphVisualTuning(dynamics = 0.5f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_LINEAR) to GlyphVisualTuning(dynamics = 0.45f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.45f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_CENTER) to GlyphVisualTuning(dynamics = 0.6f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CLASSIC) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (4a)
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_LINEAR) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_CENTER) to GlyphVisualTuning(dynamics = 0.7f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_SPECTRUM_MARKER) to GlyphVisualTuning(dynamics = 0.8f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (4b)
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_LINEAR) to GlyphVisualTuning(dynamics = 0.5f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.5f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_CENTER) to GlyphVisualTuning(dynamics = 0.8f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_SPECTRUM) to GlyphVisualTuning(dynamics = 0.9f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_SPECTRUM_MARKER) to GlyphVisualTuning(dynamics = 0.8f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (3) matrix
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_BAR) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_FIELD) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_CIRCLE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RIPPLE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_CENTER) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_BOTTOM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTROGRAM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_ANALYZER) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_OSCILLOSCOPE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RADIAL_SPECTRUM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_OPEN_REEL) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RAIN) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_WAVE_FIELD) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SKYLINE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_PULSE_GRID) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f),

        // Phone (4a) Pro matrix
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_BAR) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_FIELD) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_CIRCLE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RIPPLE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_CENTER) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_BOTTOM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTROGRAM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_ANALYZER) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_OSCILLOSCOPE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RADIAL_SPECTRUM) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RAIN) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_WAVE_FIELD) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SKYLINE) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_PULSE_GRID) to GlyphVisualTuning(dynamics = 0.5f), // Previously unspecified dynamics.
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS) to GlyphVisualTuning(dynamics = 0.8f)
    )

    private fun key(
        profile: GlyphDeviceProfile,
        patternId: String
    ): GlyphVisualTuningKey = GlyphVisualTuningKey(profile, patternId)

    fun tuningFor(
        profile: GlyphDeviceProfile,
        patternId: String
    ): GlyphVisualTuning = entries[GlyphVisualTuningKey(profile, patternId)]
        ?: GlyphVisualTuning(dynamics = 0f)
}

fun resolveGlyphVisualTuning(
    profile: GlyphDeviceProfile,
    patternId: String,
    localDynamicsOverrides: Map<GlyphVisualTuningKey, Float> = emptyMap()
): GlyphVisualTuning {
    val key = GlyphVisualTuningKey(profile, patternId)
    val localDynamics = localDynamicsOverrides[key]
    return if (localDynamics != null) {
        GlyphVisualTuning(dynamics = localDynamics.coerceIn(0f, 1f))
    } else {
        GlyphVisualTuningDatabase.tuningFor(profile, patternId)
    }
}

fun supportsGlyphVisualDynamics(patternKind: GlyphPatternKind?): Boolean {
    return when (patternKind) {
        GlyphPatternKind.LINEAR,
        GlyphPatternKind.CENTER,
        GlyphPatternKind.ALL_BRIGHTNESS,
        GlyphPatternKind.MATRIX_BAR,
        GlyphPatternKind.MATRIX_FIELD,
        GlyphPatternKind.MATRIX_CIRCLE,
        GlyphPatternKind.SPECTRUM -> true
        null -> false
    }
}

/**
 * Display-only min/max normalizer. Its state is independent from Legacy Auto Scale.
 * The tracked range follows the pre-AGC scalar implementation, without autoScaleOffset.
 */
internal class VisualDynamicsExpander {
    private var minTrack = 0f
    private var maxTrack = 1f
    private var lastUpdateMs = 0L

    fun update(
        value: Float,
        nowMs: Long,
        windowMs: Float
    ): Float {
        val clamped = value.coerceIn(0f, 1f)
        val elapsedMs = if (lastUpdateMs <= 0L) 0L else (nowMs - lastUpdateMs).coerceAtLeast(0L)
        lastUpdateMs = nowMs
        val drift = (elapsedMs.toFloat() / windowMs.coerceAtLeast(1f)).coerceIn(0f, 1f)

        minTrack = min(clamped, (minTrack + drift).coerceIn(0f, 1f))
        maxTrack = max(clamped, (maxTrack - drift).coerceIn(0f, 1f))
        val range = (maxTrack - minTrack).coerceAtLeast(MIN_VISUAL_DYNAMICS_RANGE)
        return ((clamped - minTrack) / range).coerceIn(0f, 1f)
    }

    fun reset() {
        minTrack = 0f
        maxTrack = 1f
        lastUpdateMs = 0L
    }
}

internal class SpectrumVisualDynamicsState {
    private val sharedExpander = VisualDynamicsExpander()
    private var perBandExpanders = emptyArray<VisualDynamicsExpander>()
    private var expandedBands = FloatArray(0)

    fun apply(
        values: FloatArray,
        nowMs: Long,
        windowMs: Float,
        dynamics: Float,
        output: FloatArray = FloatArray(values.size)
    ): FloatArray {
        require(output.size == values.size)
        if (perBandExpanders.size != values.size) {
            reset()
            perBandExpanders = Array(values.size) { VisualDynamicsExpander() }
            expandedBands = FloatArray(values.size)
        }

        val framePeak = values.maxOrNull()?.coerceIn(0f, 1f) ?: 0f
        val expandedPeak = sharedExpander.update(framePeak, nowMs, windowMs)
        for (index in values.indices) {
            expandedBands[index] = perBandExpanders[index].update(
                values[index].coerceIn(0f, 1f),
                nowMs,
                windowMs
            )
        }
        if (framePeak <= 0f) {
            output.fill(0f)
            return output
        }

        val blend = dynamics.coerceIn(0f, 1f)
        val sharedMix = spectrumSharedDynamicsMix(blend)
        val perBandMix = spectrumPerBandDynamicsMix(blend)
        // Shared Spectrum dynamics may boost a non-zero frame peak, but must not map the
        // tracker's current minimum below its natural peak and collapse every band together.
        val boostOnlyExpandedPeak = max(framePeak, expandedPeak)
        val displayPeak = blendVisualDynamics(framePeak, boostOnlyExpandedPeak, sharedMix)
        val commonScale = displayPeak / framePeak

        for (index in values.indices) {
            val sharedBand = (values[index].coerceIn(0f, 1f) * commonScale).coerceIn(0f, 1f)
            output[index] = blendVisualDynamics(sharedBand, expandedBands[index], perBandMix)
        }
        return output
    }

    fun reset() {
        sharedExpander.reset()
        perBandExpanders = emptyArray()
        expandedBands = FloatArray(0)
    }
}

internal fun applyAdaptiveVisualDynamics(
    agcLevel: Float,
    autoScaleEnabled: Boolean,
    strategy: GlyphAutoScaleStrategy,
    profile: GlyphDeviceProfile,
    patternId: String,
    patternKind: GlyphPatternKind?,
    expander: VisualDynamicsExpander,
    nowMs: Long,
    windowMs: Float,
    override: GlyphVisualTuning? = null
): Float {
    val clampedAgcLevel = agcLevel.coerceIn(0f, 1f)
    if (
        !autoScaleEnabled ||
        strategy != GlyphAutoScaleStrategy.ADAPTIVE ||
        !supportsGlyphVisualDynamics(patternKind)
    ) {
        return clampedAgcLevel
    }

    val tuning = override ?: GlyphVisualTuningDatabase.tuningFor(profile, patternId)
    val expandedLevel = expander.update(clampedAgcLevel, nowMs, windowMs)
    return blendVisualDynamics(clampedAgcLevel, expandedLevel, tuning.dynamics)
}

/** Blends shared peak scaling into continuously tracked per-band expansion. */
internal fun applyAdaptiveSpectrumVisualDynamics(
    agcBands: FloatArray,
    autoScaleEnabled: Boolean,
    strategy: GlyphAutoScaleStrategy,
    profile: GlyphDeviceProfile,
    patternId: String,
    patternKind: GlyphPatternKind?,
    state: SpectrumVisualDynamicsState,
    nowMs: Long,
    windowMs: Float,
    override: GlyphVisualTuning? = null,
    output: FloatArray = FloatArray(agcBands.size)
): FloatArray {
    require(output.size == agcBands.size)
    if (
        !autoScaleEnabled ||
        strategy != GlyphAutoScaleStrategy.ADAPTIVE ||
        patternKind != GlyphPatternKind.SPECTRUM
    ) {
        for (index in agcBands.indices) {
            output[index] = agcBands[index].coerceIn(0f, 1f)
        }
        return output
    }

    val tuning = override ?: GlyphVisualTuningDatabase.tuningFor(profile, patternId)
    return state.apply(
        values = agcBands,
        nowMs = nowMs,
        windowMs = windowMs,
        dynamics = tuning.dynamics,
        output = output
    )
}

internal fun blendVisualDynamics(
    agcLevel: Float,
    expandedLevel: Float,
    dynamics: Float
): Float {
    val clampedAgcLevel = agcLevel.coerceIn(0f, 1f)
    val clampedExpandedLevel = expandedLevel.coerceIn(0f, 1f)
    val blend = dynamics.coerceIn(0f, 1f)
    return (clampedAgcLevel + (clampedExpandedLevel - clampedAgcLevel) * blend).coerceIn(0f, 1f)
}

fun formatGlyphVisualTuningEntry(
    profile: GlyphDeviceProfile,
    patternId: String,
    tuning: GlyphVisualTuning
): String {
    val dynamics = String.format(Locale.US, "%.2f", tuning.dynamics.coerceIn(0f, 1f))
    return "key(GlyphDeviceProfile.${profile.name}, \"${patternId}\") " +
        "to GlyphVisualTuning(\n    dynamics = ${dynamics}f,\n),"
}

private const val MIN_VISUAL_DYNAMICS_RANGE = 0.05f

// Spectrum Dynamics tuning knobs. Overall movement uses the full setting range; only the
// per-band contrast is delayed and capped so high Dynamics does not amplify small noise.
internal const val SPECTRUM_PER_BAND_DYNAMICS_START = 0.65f
internal const val SPECTRUM_PER_BAND_DYNAMICS_MAX_MIX = 0.50f

internal fun spectrumSharedDynamicsMix(dynamics: Float): Float {
    return smoothStep(dynamics)
}

internal fun spectrumPerBandDynamicsMix(dynamics: Float): Float {
    val phase = ((dynamics.coerceIn(0f, 1f) - SPECTRUM_PER_BAND_DYNAMICS_START) /
        (1f - SPECTRUM_PER_BAND_DYNAMICS_START)).coerceIn(0f, 1f)
    return smoothStep(phase) * SPECTRUM_PER_BAND_DYNAMICS_MAX_MIX
}

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}
