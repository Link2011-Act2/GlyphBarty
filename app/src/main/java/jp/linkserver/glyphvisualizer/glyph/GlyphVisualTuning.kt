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
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_D1_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_CLASSIC) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternRegistry.P2_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (2)
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_C1_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_D1_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_CLASSIC) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternRegistry.P2_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (2a)
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_C_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_CLASSIC) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternRegistry.P2A_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (3a)
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_C_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_LINEAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_LINEAR_PEAK) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CAB_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_CLASSIC) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternRegistry.P3A_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (4a)
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_LINEAR) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_CENTER) to GlyphVisualTuning(dynamics = 0.6f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_SPECTRUM_MARKER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternRegistry.P4A_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (4b)
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_LINEAR) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_LINEAR_PEAK) to GlyphVisualTuning(dynamics = 0.3f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_CENTER) to GlyphVisualTuning(dynamics = 0.7f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_SPECTRUM_MARKER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternRegistry.P4A_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (3) matrix
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_BAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_FIELD) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_CIRCLE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RIPPLE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_BOTTOM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTROGRAM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_ANALYZER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_OSCILLOSCOPE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RADIAL_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_OPEN_REEL) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_RAIN) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_WAVE_FIELD) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_SKYLINE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_PULSE_GRID) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS) to GlyphVisualTuning(),

        // Phone (4a) Pro matrix
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_BAR) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_FIELD) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_CIRCLE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RIPPLE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_CENTER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_BOTTOM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTROGRAM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SPECTRUM_ANALYZER) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_OSCILLOSCOPE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RADIAL_SPECTRUM) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_RAIN) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_WAVE_FIELD) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_SKYLINE) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_PULSE_GRID) to GlyphVisualTuning(),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS) to GlyphVisualTuning()
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
        GlyphPatternKind.MATRIX_CIRCLE -> true
        GlyphPatternKind.SPECTRUM,
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
