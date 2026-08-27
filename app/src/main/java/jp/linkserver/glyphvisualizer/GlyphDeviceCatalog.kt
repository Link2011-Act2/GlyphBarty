package jp.linkserver.glyphvisualizer

import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry

enum class GlyphControllerFamily {
    LIGHTS,
    MATRIX
}

data class GlyphLightDeviceSpec(
    val sdkDeviceId: String,
    val channelCount: Int,
    val cRange: IntRange,
    val aRange: IntRange? = null,
    val bRange: IntRange? = null,
    val cabRange: IntRange? = null,
    val d1Range: IntRange? = null,
    val d1CenterChannel: Int? = null,
    val centerSupported: Boolean = false
)

data class GlyphMatrixDeviceSpec(
    val sdkDeviceId: String
)

data class GlyphDeviceDefinition(
    val profile: GlyphDeviceProfile,
    val modelCode: String,
    val presentation: GlyphDevicePresentation,
    val controllerFamily: GlyphControllerFamily,
    val defaultGlyphMode: String,
    val lightSpec: GlyphLightDeviceSpec? = null,
    val matrixSpec: GlyphMatrixDeviceSpec? = null
) {
    init {
        when (controllerFamily) {
            GlyphControllerFamily.LIGHTS -> require(lightSpec != null)
            GlyphControllerFamily.MATRIX -> require(matrixSpec != null)
        }
    }
}

object GlyphDeviceCatalog {
    private data class Entry(
        val matches: () -> Boolean,
        val definition: GlyphDeviceDefinition
    )

    private val entries = listOf(
        Entry(
            matches = { Common.is25111p() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
                modelCode = "25111p",
                presentation = GlyphDevicePresentation("Phone (4a) Pro", "Glyph Matrix"),
                controllerFamily = GlyphControllerFamily.MATRIX,
                defaultGlyphMode = GlyphPatternRegistry.P3_MATRIX_SPECTRUM,
                matrixSpec = GlyphMatrixDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_25111p
                )
            )
        ),
        Entry(
            matches = { Common.is23112() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE3_MATRIX,
                modelCode = "23112",
                presentation = GlyphDevicePresentation("Phone (3)", "Glyph Matrix"),
                controllerFamily = GlyphControllerFamily.MATRIX,
                defaultGlyphMode = GlyphPatternRegistry.P3_MATRIX_SPECTRUM,
                matrixSpec = GlyphMatrixDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_23112
                )
            )
        ),
        Entry(
            matches = { Common.is25131() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE4B,
                modelCode = "25131",
                presentation = GlyphDevicePresentation("Phone (4b)", "Glyph Bar"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P4A_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_25131,
                    channelCount = 4,
                    cRange = 0..3,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { Common.is25111() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE4A,
                modelCode = "25111",
                presentation = GlyphDevicePresentation("Phone (4a)", "Glyph Bar"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P4A_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_25111,
                    channelCount = 7,
                    cRange = 0..5,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { Common.is24111() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE3A,
                modelCode = "24111",
                presentation = GlyphDevicePresentation("Phone (3a) Series", "Glyph Lights"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P3A_C_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_24111,
                    channelCount = 36,
                    cRange = 0..19,
                    aRange = 20..30,
                    bRange = 31..35,
                    cabRange = 0..35,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { Common.is23113() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE2A,
                modelCode = "23113",
                presentation = GlyphDevicePresentation("Phone (2a) Plus", "Glyph Lights"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P2A_C_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_23113,
                    channelCount = 26,
                    cRange = 0..23,
                    aRange = 24..24,
                    bRange = 25..25,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { Common.is23111() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE2A,
                modelCode = "23111",
                presentation = GlyphDevicePresentation("Phone (2a)", "Glyph Lights"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P2A_C_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_23111,
                    channelCount = 26,
                    cRange = 0..23,
                    aRange = 24..24,
                    bRange = 25..25,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { runCatching { Common.is20111() }.getOrDefault(false) },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE1,
                modelCode = "20111",
                presentation = GlyphDevicePresentation("Phone (1)", "Glyph Lights"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P2_D1_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_20111,
                    channelCount = 15,
                    cRange = 2..5,
                    d1Range = 7..14,
                    d1CenterChannel = 11,
                    centerSupported = true
                )
            )
        ),
        Entry(
            matches = { Common.is22111() },
            definition = GlyphDeviceDefinition(
                profile = GlyphDeviceProfile.PHONE2,
                modelCode = "22111",
                presentation = GlyphDevicePresentation("Phone (2)", "Glyph Lights"),
                controllerFamily = GlyphControllerFamily.LIGHTS,
                defaultGlyphMode = GlyphPatternRegistry.P2_C1_LINEAR,
                lightSpec = GlyphLightDeviceSpec(
                    sdkDeviceId = Glyph.DEVICE_22111,
                    channelCount = 33,
                    cRange = 3..18,
                    d1Range = 25..32,
                    d1CenterChannel = 29,
                    centerSupported = true
                )
            )
        )
    )

    private val fallback = GlyphDeviceDefinition(
        profile = GlyphDeviceProfile.PHONE2,
        modelCode = "unknown",
        presentation = GlyphDevicePresentation("Nothing Phone", "Glyph Interface"),
        controllerFamily = GlyphControllerFamily.LIGHTS,
        defaultGlyphMode = GlyphPatternRegistry.P2_C1_LINEAR,
        lightSpec = GlyphLightDeviceSpec(
            sdkDeviceId = Glyph.DEVICE_22111,
            channelCount = 33,
            cRange = 3..18,
            d1Range = 25..32,
            centerSupported = true
        )
    )

    fun currentOrNull(): GlyphDeviceDefinition? = entries.firstOrNull { it.matches() }?.definition

    fun currentOrFallback(): GlyphDeviceDefinition = currentOrNull() ?: fallback

    fun currentPresentation(): GlyphDevicePresentation = currentOrFallback().presentation

    fun currentProfile(): GlyphDeviceProfile = currentOrFallback().profile

    fun effectiveProfile(
        actualProfile: GlyphDeviceProfile,
        phone4bEmulationEnabled: Boolean
    ): GlyphDeviceProfile {
        return if (actualProfile == GlyphDeviceProfile.PHONE4A && phone4bEmulationEnabled) {
            GlyphDeviceProfile.PHONE4B
        } else {
            actualProfile
        }
    }

    fun effectiveUiProfile(
        actualProfile: GlyphDeviceProfile,
        phone4bEmulationEnabled: Boolean,
        debugDeviceProfileOverride: GlyphDeviceProfile?
    ): GlyphDeviceProfile {
        return debugDeviceProfileOverride
            ?: effectiveProfile(actualProfile, phone4bEmulationEnabled)
    }

    fun effectiveOutputProfile(
        actualProfile: GlyphDeviceProfile,
        phone4bEmulationEnabled: Boolean,
        debugDeviceProfileOverride: GlyphDeviceProfile?
    ): GlyphDeviceProfile {
        return when {
            actualProfile == GlyphDeviceProfile.PHONE4A &&
                (phone4bEmulationEnabled || debugDeviceProfileOverride == GlyphDeviceProfile.PHONE4B) ->
                GlyphDeviceProfile.PHONE4B

            actualProfile == GlyphDeviceProfile.PHONE3_MATRIX &&
                debugDeviceProfileOverride == GlyphDeviceProfile.PHONE4A_PRO_MATRIX ->
                GlyphDeviceProfile.PHONE4A_PRO_MATRIX

            else -> actualProfile
        }
    }

    fun presentationForProfile(profile: GlyphDeviceProfile): GlyphDevicePresentation {
        return entries.firstOrNull { it.definition.profile == profile }
            ?.definition
            ?.presentation
            ?: fallback.presentation
    }

    fun definitionForProfile(profile: GlyphDeviceProfile): GlyphDeviceDefinition? {
        return entries.firstOrNull { it.definition.profile == profile }?.definition
    }

    fun defaultGlyphModeForProfile(profile: GlyphDeviceProfile): String {
        return entries.firstOrNull { it.definition.profile == profile }
            ?.definition
            ?.defaultGlyphMode
            ?: fallback.defaultGlyphMode
    }

    fun defaultGlyphModeForCurrentDevice(): String = currentOrFallback().defaultGlyphMode

    fun normalizeGlyphModeForCurrentDevice(glyphMode: String): String {
        return normalizeGlyphMode(currentProfile(), glyphMode)
    }

    fun normalizeGlyphMode(profile: GlyphDeviceProfile, glyphMode: String): String {
        return if (GlyphPatternRegistry.isSupported(profile, glyphMode)) {
            glyphMode
        } else {
            defaultGlyphModeForProfile(profile)
        }
    }
}
