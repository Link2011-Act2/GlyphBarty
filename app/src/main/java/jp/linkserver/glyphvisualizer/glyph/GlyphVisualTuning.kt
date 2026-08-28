package jp.linkserver.glyphvisualizer.glyph

import java.util.Locale

data class GlyphVisualTuning(
    val scale: Float = 1f
)

data class GlyphVisualTuningKey(
    val profile: GlyphDeviceProfile,
    val patternKind: GlyphPatternKind
)

/** Production tuning values. Inspector overrides never mutate this table. */
object GlyphVisualTuningDatabase {
    private val entries: Map<GlyphVisualTuningKey, GlyphVisualTuning> = mapOf(
        // Phone (1)
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE1, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (2)
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (2a)
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE2A, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (3a)
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3A, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (4a)
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (4b)
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4B, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (3) matrix
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE3_MATRIX, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f),

        // Phone (4a) Pro matrix
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.LINEAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.CENTER) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.SPECTRUM) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.ALL_BRIGHTNESS) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.MATRIX_BAR) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.MATRIX_FIELD) to GlyphVisualTuning(scale = 1f),
        key(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphPatternKind.MATRIX_CIRCLE) to GlyphVisualTuning(scale = 1f)
    )

    private fun key(
        profile: GlyphDeviceProfile,
        patternKind: GlyphPatternKind
    ): GlyphVisualTuningKey = GlyphVisualTuningKey(profile, patternKind)

    fun tuningFor(
        profile: GlyphDeviceProfile,
        patternKind: GlyphPatternKind
    ): GlyphVisualTuning = entries.getValue(GlyphVisualTuningKey(profile, patternKind))
}

fun resolveGlyphVisualTuning(
    profile: GlyphDeviceProfile,
    patternKind: GlyphPatternKind,
    localScaleOverrides: Map<GlyphVisualTuningKey, Float> = emptyMap()
): GlyphVisualTuning {
    val key = GlyphVisualTuningKey(profile, patternKind)
    val localScale = localScaleOverrides[key]
    return if (localScale != null) {
        GlyphVisualTuning(scale = localScale)
    } else {
        GlyphVisualTuningDatabase.tuningFor(profile, patternKind)
    }
}

internal fun applyGlyphVisualTuning(
    value: Float,
    profile: GlyphDeviceProfile,
    patternKind: GlyphPatternKind?,
    override: GlyphVisualTuning? = null
): Float {
    val tuning = override ?: patternKind?.let {
        GlyphVisualTuningDatabase.tuningFor(profile, it)
    } ?: GlyphVisualTuning()
    return (value * tuning.scale.coerceAtLeast(0f)).coerceIn(0f, 1f)
}

internal fun applyAutoScaleVisualTuning(
    value: Float,
    autoScaleEnabled: Boolean,
    strategy: GlyphAutoScaleStrategy,
    profile: GlyphDeviceProfile,
    patternKind: GlyphPatternKind?,
    override: GlyphVisualTuning? = null
): Float {
    if (!autoScaleEnabled || strategy != GlyphAutoScaleStrategy.ADAPTIVE) {
        return value.coerceIn(0f, 1f)
    }
    return applyGlyphVisualTuning(value, profile, patternKind, override)
}

fun formatGlyphVisualTuningEntry(
    profile: GlyphDeviceProfile,
    patternKind: GlyphPatternKind,
    tuning: GlyphVisualTuning
): String {
    val scale = String.format(Locale.US, "%.2f", tuning.scale)
    return "key(GlyphDeviceProfile.${profile.name}, GlyphPatternKind.${patternKind.name}) " +
        "to GlyphVisualTuning(\n    scale = ${scale}f,\n),"
}
