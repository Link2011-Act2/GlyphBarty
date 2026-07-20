package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.R

enum class GlyphDeviceProfile {
    PHONE1,
    PHONE2,
    PHONE2A,
    PHONE3A,
    PHONE4A,
    PHONE4B,
    PHONE3_MATRIX,
    PHONE4A_PRO_MATRIX
}

enum class GlyphPatternKind {
    LINEAR,
    CENTER,
    SPECTRUM,
    ALL_BRIGHTNESS,
    MATRIX_BAR,
    MATRIX_FIELD,
    MATRIX_CIRCLE
}

enum class GlyphPatternRenderMode {
    LINEAR,
    LINEAR_PEAK,
    PULSE_TRAIN,
    CENTER,
    SPECTRUM,
    CLASSIC,
    ALL_BRIGHTNESS,
    MATRIX_BAR,
    MATRIX_FIELD,
    MATRIX_CIRCLE,
    MATRIX_RIPPLE,
    MATRIX_SPECTRUM,
    MATRIX_SPECTRUM_CENTER,
    MATRIX_SPECTRUM_BOTTOM,
    MATRIX_SPECTROGRAM,
    MATRIX_SPECTRUM_ANALYZER,
    MATRIX_OSCILLOSCOPE,
    MATRIX_RADIAL_SPECTRUM,
    MATRIX_OPEN_REEL,
    MATRIX_RAIN,
    MATRIX_WAVE_FIELD,
    MATRIX_SKYLINE,
    MATRIX_PULSE_GRID
}

enum class GlyphLightZone {
    C,
    A,
    B,
    CAB,
    D1
}

data class GlyphPatternRecipe(
    val renderMode: GlyphPatternRenderMode,
    val lightZones: List<GlyphLightZone> = emptyList()
)

data class GlyphPatternDefinition(
    val id: String,
    val labelRes: Int,
    val kind: GlyphPatternKind,
    val supportedDevices: Set<GlyphDeviceProfile>,
    val recipe: GlyphPatternRecipe
)

/**
 * Add new user-facing Glyph patterns here first.
 *
 * The id is persisted in settings and sent to the Glyph output controllers.
 * Controller rendering still lives in the device-specific controller, but UI
 * availability, default mode, and mode categories are centralized here.
 */
object GlyphPatternRegistry {
    // Phone (2) legacy ids are kept as-is for settings/export compatibility.
    const val P2_C1_LINEAR = "C1_LINEAR"
    const val P2_C1_LINEAR_PEAK = "C1_LINEAR_PEAK"
    const val P2_C1_CENTER = "C1_CENTER"
    const val P2_D1_LINEAR = "D1"
    const val P2_D1_LINEAR_PEAK = "D1_LINEAR_PEAK"
    const val P2_D1_CENTER = "D1_CENTER"
    const val P2_C1_SPECTRUM = "C1_SPECTRUM"
    const val P2_D1_SPECTRUM = "D1_SPECTRUM"
    const val P2_CLASSIC = "CLASSIC"
    const val P2_ALL_BRIGHTNESS = "ALL_BRIGHTNESS"

    const val P3A_C_LINEAR = "P3A_C_LINEAR"
    const val P3A_C_LINEAR_PEAK = "P3A_C_LINEAR_PEAK"
    const val P3A_C_CENTER = "P3A_C_CENTER"
    const val P3A_C_SPECTRUM = "P3A_C_SPECTRUM"
    const val P3A_CAB_LINEAR = "P3A_CAB_LINEAR"
    const val P3A_CAB_LINEAR_PEAK = "P3A_CAB_LINEAR_PEAK"
    const val P3A_CAB_CENTER = "P3A_CAB_CENTER"
    const val P3A_CAB_SPECTRUM = "P3A_CAB_SPECTRUM"
    const val P3A_CLASSIC = "P3A_CLASSIC"
    const val P3A_ALL_BRIGHTNESS = "P3A_ALL_BRIGHTNESS"

    const val P2A_C_LINEAR = "P2A_C_LINEAR"
    const val P2A_C_LINEAR_PEAK = "P2A_C_LINEAR_PEAK"
    const val P2A_C_CENTER = "P2A_C_CENTER"
    const val P2A_C_SPECTRUM = "P2A_C_SPECTRUM"
    const val P2A_CLASSIC = "P2A_CLASSIC"
    const val P2A_ALL_BRIGHTNESS = "P2A_ALL_BRIGHTNESS"

    const val P4A_LINEAR = "P4A_LINEAR"
    const val P4A_LINEAR_PEAK = "P4A_LINEAR_PEAK"
    const val P4A_CENTER = "P4A_CENTER"
    const val P4A_SPECTRUM = "P4A_SPECTRUM"
    const val P4A_ALL_BRIGHTNESS = "P4A_ALL_BRIGHTNESS"

    const val P3_MATRIX_BAR = "P3_MATRIX_BAR"
    const val P3_MATRIX_FIELD = "P3_MATRIX_FIELD"
    const val P3_MATRIX_CIRCLE = "P3_MATRIX_CIRCLE"
    const val P3_MATRIX_RIPPLE = "P3_MATRIX_RIPPLE"
    const val P3_MATRIX_SPECTRUM = "P3_MATRIX_SPECTRUM"
    const val P3_MATRIX_SPECTRUM_CENTER = "P3_MATRIX_SPECTRUM_CENTER"
    const val P3_MATRIX_SPECTRUM_BOTTOM = "P3_MATRIX_SPECTRUM_BOTTOM"
    const val P3_MATRIX_SPECTROGRAM = "P3_MATRIX_SPECTROGRAM"
    const val P3_MATRIX_SPECTRUM_ANALYZER = "P3_MATRIX_SPECTRUM_ANALYZER"
    const val P3_MATRIX_OSCILLOSCOPE = "P3_MATRIX_OSCILLOSCOPE"
    const val P3_MATRIX_RADIAL_SPECTRUM = "P3_MATRIX_RADIAL_SPECTRUM"
    const val P3_MATRIX_OPEN_REEL = "P3_MATRIX_OPEN_REEL"
    const val P3_MATRIX_RAIN = "P3_MATRIX_RAIN"
    const val P3_MATRIX_WAVE_FIELD = "P3_MATRIX_WAVE_FIELD"
    const val P3_MATRIX_SKYLINE = "P3_MATRIX_SKYLINE"
    const val P3_MATRIX_PULSE_GRID = "P3_MATRIX_PULSE_GRID"
    const val P3_MATRIX_ALL_BRIGHTNESS = "P3_MATRIX_ALL_BRIGHTNESS"

    private val matrixDevices = setOf(
        GlyphDeviceProfile.PHONE3_MATRIX,
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX
    )

    private val phone4aStyleBarDevices = setOf(
        GlyphDeviceProfile.PHONE4A,
        GlyphDeviceProfile.PHONE4B
    )

    private fun pattern(
        id: String,
        labelRes: Int,
        kind: GlyphPatternKind,
        supportedDevices: Set<GlyphDeviceProfile>,
        recipe: GlyphPatternRecipe
    ) = GlyphPatternDefinition(
        id = id,
        labelRes = labelRes,
        kind = kind,
        supportedDevices = supportedDevices,
        recipe = recipe
    )

    val all: List<GlyphPatternDefinition> = listOf(
        pattern(P2_C1_LINEAR, R.string.mode_c1_linear, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.C))),
        pattern(P2_D1_LINEAR, R.string.mode_d1, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.D1))),
        pattern(P2_C1_LINEAR_PEAK, R.string.mode_c1_linear_peak, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.C))),
        pattern(P2_D1_LINEAR_PEAK, R.string.mode_d1_linear_peak, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.D1))),
        pattern(P2_C1_CENTER, R.string.mode_c1_center, GlyphPatternKind.CENTER, setOf(GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.C))),
        pattern(P2_D1_CENTER, R.string.mode_d1_center, GlyphPatternKind.CENTER, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.D1))),
        pattern(P2_C1_SPECTRUM, R.string.mode_c1_spectrum, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.C))),
        pattern(P2_D1_SPECTRUM, R.string.mode_d1_spectrum, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.D1))),
        pattern(P2_CLASSIC, R.string.mode_classic, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.CLASSIC)),
        pattern(P2_ALL_BRIGHTNESS, R.string.mode_all_brightness, GlyphPatternKind.ALL_BRIGHTNESS, setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2), GlyphPatternRecipe(GlyphPatternRenderMode.ALL_BRIGHTNESS)),

        pattern(P2A_C_LINEAR, R.string.mode_c1_linear, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.C))),
        pattern(P2A_C_LINEAR_PEAK, R.string.mode_c1_linear_peak, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.C))),
        pattern(P2A_C_CENTER, R.string.mode_c1_center, GlyphPatternKind.CENTER, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.C))),
        pattern(P2A_C_SPECTRUM, R.string.mode_c1_spectrum, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.C))),
        pattern(P2A_CLASSIC, R.string.mode_classic, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.CLASSIC)),
        pattern(P2A_ALL_BRIGHTNESS, R.string.mode_all_brightness, GlyphPatternKind.ALL_BRIGHTNESS, setOf(GlyphDeviceProfile.PHONE2A), GlyphPatternRecipe(GlyphPatternRenderMode.ALL_BRIGHTNESS)),

        pattern(P3A_C_LINEAR, R.string.mode_p3a_c_linear, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.C))),
        pattern(P3A_CAB_LINEAR, R.string.mode_p3a_cab_linear, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.C, GlyphLightZone.A, GlyphLightZone.B))),
        pattern(P3A_C_LINEAR_PEAK, R.string.mode_c1_linear_peak, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.C))),
        pattern(P3A_CAB_LINEAR_PEAK, R.string.mode_p3a_cab_linear_peak, GlyphPatternKind.LINEAR, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.C, GlyphLightZone.A, GlyphLightZone.B))),
        pattern(P3A_C_CENTER, R.string.mode_p3a_c_center, GlyphPatternKind.CENTER, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.C))),
        pattern(P3A_CAB_CENTER, R.string.mode_p3a_cab_center, GlyphPatternKind.CENTER, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.C, GlyphLightZone.A, GlyphLightZone.B))),
        pattern(P3A_C_SPECTRUM, R.string.mode_p3a_c_spectrum, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.C))),
        pattern(P3A_CAB_SPECTRUM, R.string.mode_p3a_cab_spectrum, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.C, GlyphLightZone.A, GlyphLightZone.B))),
        pattern(P3A_CLASSIC, R.string.mode_classic, GlyphPatternKind.SPECTRUM, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.CLASSIC)),
        pattern(P3A_ALL_BRIGHTNESS, R.string.mode_all_brightness, GlyphPatternKind.ALL_BRIGHTNESS, setOf(GlyphDeviceProfile.PHONE3A), GlyphPatternRecipe(GlyphPatternRenderMode.ALL_BRIGHTNESS)),

        pattern(P4A_LINEAR, R.string.mode_p4a_linear, GlyphPatternKind.LINEAR, phone4aStyleBarDevices, GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR, listOf(GlyphLightZone.C))),
        pattern(P4A_LINEAR_PEAK, R.string.mode_p4a_linear_peak, GlyphPatternKind.LINEAR, phone4aStyleBarDevices, GlyphPatternRecipe(GlyphPatternRenderMode.LINEAR_PEAK, listOf(GlyphLightZone.C))),
        pattern(P4A_CENTER, R.string.mode_p4a_center, GlyphPatternKind.CENTER, phone4aStyleBarDevices, GlyphPatternRecipe(GlyphPatternRenderMode.CENTER, listOf(GlyphLightZone.C))),
        pattern(P4A_SPECTRUM, R.string.mode_p4a_spectrum, GlyphPatternKind.SPECTRUM, phone4aStyleBarDevices, GlyphPatternRecipe(GlyphPatternRenderMode.SPECTRUM, listOf(GlyphLightZone.C))),
        pattern(P4A_ALL_BRIGHTNESS, R.string.mode_p4a_all_brightness, GlyphPatternKind.ALL_BRIGHTNESS, phone4aStyleBarDevices, GlyphPatternRecipe(GlyphPatternRenderMode.ALL_BRIGHTNESS)),

        pattern(P3_MATRIX_SPECTRUM, R.string.mode_matrix_spectrum, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SPECTRUM)),
        pattern(P3_MATRIX_SPECTRUM_CENTER, R.string.mode_matrix_spectrum_center, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER)),
        pattern(P3_MATRIX_SPECTRUM_BOTTOM, R.string.mode_matrix_spectrum_bottom, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM)),
        pattern(P3_MATRIX_SKYLINE, R.string.mode_matrix_skyline, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SKYLINE)),
        pattern(P3_MATRIX_SPECTRUM_ANALYZER, R.string.mode_matrix_spectrum_analyzer, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SPECTRUM_ANALYZER)),
        pattern(P3_MATRIX_OSCILLOSCOPE, R.string.mode_matrix_oscilloscope, GlyphPatternKind.MATRIX_FIELD, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE)),
        pattern(P3_MATRIX_RADIAL_SPECTRUM, R.string.mode_matrix_radial_spectrum, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_RADIAL_SPECTRUM)),
        pattern(P3_MATRIX_OPEN_REEL, R.string.mode_matrix_open_reel, GlyphPatternKind.MATRIX_FIELD, setOf(GlyphDeviceProfile.PHONE3_MATRIX), GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_OPEN_REEL)),
        pattern(P3_MATRIX_SPECTROGRAM, R.string.mode_matrix_spectrogram, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_SPECTROGRAM)),
        pattern(P3_MATRIX_WAVE_FIELD, R.string.mode_matrix_wave_field, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_WAVE_FIELD)),
        pattern(P3_MATRIX_RAIN, R.string.mode_matrix_rain, GlyphPatternKind.MATRIX_FIELD, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_RAIN)),
        pattern(P3_MATRIX_PULSE_GRID, R.string.mode_matrix_pulse_grid, GlyphPatternKind.SPECTRUM, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_PULSE_GRID)),
        pattern(P3_MATRIX_RIPPLE, R.string.mode_matrix_ripple, GlyphPatternKind.MATRIX_CIRCLE, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_RIPPLE)),
        pattern(P3_MATRIX_CIRCLE, R.string.mode_matrix_circle, GlyphPatternKind.MATRIX_CIRCLE, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_CIRCLE)),
        pattern(P3_MATRIX_FIELD, R.string.mode_matrix_field, GlyphPatternKind.MATRIX_FIELD, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_FIELD)),
        pattern(P3_MATRIX_BAR, R.string.mode_matrix_bar, GlyphPatternKind.MATRIX_BAR, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.MATRIX_BAR)),
        pattern(P3_MATRIX_ALL_BRIGHTNESS, R.string.mode_all_brightness, GlyphPatternKind.ALL_BRIGHTNESS, matrixDevices, GlyphPatternRecipe(GlyphPatternRenderMode.ALL_BRIGHTNESS))
    )

    private val byId = all.associateBy { it.id }

    fun patternsFor(profile: GlyphDeviceProfile): List<GlyphPatternDefinition> {
        return all.filter { profile in it.supportedDevices }
    }

    fun definition(id: String): GlyphPatternDefinition? = byId[id]

    fun recipeFor(id: String): GlyphPatternRecipe? = byId[id]?.recipe

    fun isSupported(profile: GlyphDeviceProfile, id: String): Boolean {
        return byId[id]?.supportedDevices?.contains(profile) == true
    }

    fun isSpectrum(id: String): Boolean {
        return byId[id]?.kind == GlyphPatternKind.SPECTRUM
    }

    fun isAllBrightness(id: String): Boolean {
        return byId[id]?.kind == GlyphPatternKind.ALL_BRIGHTNESS
    }

    fun kindOf(id: String): GlyphPatternKind? {
        return byId[id]?.kind
    }

    fun requiresNotificationAccess(id: String): Boolean {
        return id == P3_MATRIX_OPEN_REEL
    }

    fun isLevelAutoScale(id: String): Boolean {
        return when (byId[id]?.kind) {
            GlyphPatternKind.LINEAR,
            GlyphPatternKind.CENTER,
            GlyphPatternKind.MATRIX_BAR,
            GlyphPatternKind.MATRIX_FIELD,
            GlyphPatternKind.MATRIX_CIRCLE -> true
            else -> false
        }
    }

    fun uiMeterSegmentCount(
        profile: GlyphDeviceProfile,
        id: String,
        recordingLightIncluded: Boolean = false
    ): Int {
        val phone4SegmentCount = when (profile) {
            GlyphDeviceProfile.PHONE4A -> if (recordingLightIncluded) 7 else 6
            GlyphDeviceProfile.PHONE4B -> if (recordingLightIncluded) 5 else 4
            else -> null
        }
        return when (id) {
            P2_C1_LINEAR, P2_C1_LINEAR_PEAK, P2_C1_CENTER -> 16
            P2_D1_LINEAR, P2_D1_LINEAR_PEAK, P2_D1_CENTER -> 8
            P2_C1_SPECTRUM -> 16
            P2_D1_SPECTRUM -> 8
            P2_CLASSIC -> if (profile == GlyphDeviceProfile.PHONE1) 5 else 11
            P2_ALL_BRIGHTNESS -> if (profile == GlyphDeviceProfile.PHONE1) 15 else 16
            P2A_C_LINEAR, P2A_C_LINEAR_PEAK, P2A_C_CENTER, P2A_C_SPECTRUM -> 24
            P2A_CLASSIC -> 3
            P3A_C_LINEAR, P3A_C_LINEAR_PEAK, P3A_C_CENTER, P3A_C_SPECTRUM -> 20
            P3A_CAB_LINEAR, P3A_CAB_LINEAR_PEAK, P3A_CAB_CENTER, P3A_CAB_SPECTRUM -> 20
            P3A_CLASSIC -> 3
            P4A_LINEAR, P4A_LINEAR_PEAK, P4A_CENTER, P4A_SPECTRUM -> {
                phone4SegmentCount ?: 6
            }
            P4A_ALL_BRIGHTNESS -> phone4SegmentCount ?: 16
            P3_MATRIX_SPECTRUM,
            P3_MATRIX_SPECTRUM_CENTER,
            P3_MATRIX_SPECTRUM_BOTTOM,
            P3_MATRIX_SPECTROGRAM,
            P3_MATRIX_SPECTRUM_ANALYZER,
            P3_MATRIX_RADIAL_SPECTRUM,
            P3_MATRIX_SKYLINE,
            P3_MATRIX_WAVE_FIELD,
            P3_MATRIX_PULSE_GRID -> when (profile) {
                GlyphDeviceProfile.PHONE3_MATRIX -> 25
                GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> 13
                else -> 25
            }
            else -> when (byId[id]?.kind) {
                GlyphPatternKind.MATRIX_BAR,
                GlyphPatternKind.MATRIX_FIELD,
                GlyphPatternKind.MATRIX_CIRCLE -> when (profile) {
                    GlyphDeviceProfile.PHONE3_MATRIX -> 25
                    GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> 13
                    else -> 16
                }
                GlyphPatternKind.SPECTRUM,
                GlyphPatternKind.ALL_BRIGHTNESS -> 16
                GlyphPatternKind.LINEAR,
                GlyphPatternKind.CENTER -> when (profile) {
                    GlyphDeviceProfile.PHONE1 -> 8
                    GlyphDeviceProfile.PHONE2 -> 16
                    GlyphDeviceProfile.PHONE2A -> 24
                    GlyphDeviceProfile.PHONE3A -> 20
                    GlyphDeviceProfile.PHONE4A,
                    GlyphDeviceProfile.PHONE4B -> phone4SegmentCount ?: 16
                    GlyphDeviceProfile.PHONE3_MATRIX,
                    GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> 16
                }
                else -> 16
            }
        }
    }
}
