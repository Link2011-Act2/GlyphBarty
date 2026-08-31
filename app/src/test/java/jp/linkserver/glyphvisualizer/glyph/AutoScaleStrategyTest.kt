package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoScaleStrategyTest {
    @Test
    fun legacyTracker_restoresMinMaxNormalization() {
        val tracker = LegacyAutoScaleController()

        assertEquals(0.25f, tracker.update(0.25f, 1_000L, 30_000f, 0f), 0.0001f)
        assertEquals(0f, tracker.update(0.4f, 31_000L, 30_000f, 0f), 0.0001f)
    }

    @Test
    fun legacySpectrum_tracksEachBandIndependently() {
        val tracker = LegacySpectrumAutoScaleController()
        val output = tracker.update(
            input = floatArrayOf(0.2f, 0.8f),
            nowMs = 1_000L,
            windowMs = 30_000f,
            offset = 0f
        )

        assertEquals(0.5f, output[0], 0.0001f)
        assertEquals(0.5f, output[1], 0.0001f)
    }

    @Test
    fun visualDynamics_requiresAdaptiveAutoScale() {
        val tuning = GlyphVisualTuning(dynamics = 1f)
        val legacy = applyAdaptiveVisualDynamics(
            agcLevel = 0.4f,
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.LEGACY,
            profile = GlyphDeviceProfile.PHONE2,
            patternId = GlyphPatternRegistry.P2_C1_CENTER,
            patternKind = GlyphPatternKind.CENTER,
            expander = VisualDynamicsExpander(),
            nowMs = 1_000L,
            windowMs = 30_000f,
            override = tuning
        )
        val disabled = applyAdaptiveVisualDynamics(
            agcLevel = 0.4f,
            autoScaleEnabled = false,
            strategy = GlyphAutoScaleStrategy.ADAPTIVE,
            profile = GlyphDeviceProfile.PHONE2,
            patternId = GlyphPatternRegistry.P2_C1_CENTER,
            patternKind = GlyphPatternKind.CENTER,
            expander = VisualDynamicsExpander(),
            nowMs = 1_000L,
            windowMs = 30_000f,
            override = tuning
        )
        val spectrum = applyAdaptiveVisualDynamics(
            agcLevel = 0.4f,
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.ADAPTIVE,
            profile = GlyphDeviceProfile.PHONE2,
            patternId = GlyphPatternRegistry.P2_C1_SPECTRUM,
            patternKind = GlyphPatternKind.SPECTRUM,
            expander = VisualDynamicsExpander(),
            nowMs = 1_000L,
            windowMs = 30_000f,
            override = tuning
        )

        assertEquals(0.4f, legacy, 0.0001f)
        assertEquals(0.4f, disabled, 0.0001f)
        assertEquals(0.4f, spectrum, 0.0001f)
    }

    @Test
    fun spectrumVisualDynamics_blendsSharedThenPerBandWithWarmTrackers() {
        fun apply(
            state: SpectrumVisualDynamicsState,
            bands: FloatArray,
            dynamics: Float,
            nowMs: Long
        ) = applyAdaptiveSpectrumVisualDynamics(
            agcBands = bands,
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.ADAPTIVE,
            profile = GlyphDeviceProfile.PHONE2,
            patternId = GlyphPatternRegistry.P2_C1_SPECTRUM,
            patternKind = GlyphPatternKind.SPECTRUM,
            state = state,
            nowMs = nowMs,
            windowMs = 30_000f,
            override = GlyphVisualTuning(dynamics = dynamics)
        )

        val sharedState = SpectrumVisualDynamicsState()
        val initial = apply(sharedState, floatArrayOf(0.4f, 0.6f), dynamics = 0f, nowMs = 1_000L)
        apply(sharedState, floatArrayOf(0.4f, 0.6f), dynamics = 0f, nowMs = 31_000L)
        val shared = apply(sharedState, floatArrayOf(0.3f, 0.9f), dynamics = 0.5f, nowMs = 31_000L)

        val perBandState = SpectrumVisualDynamicsState()
        apply(perBandState, floatArrayOf(0.4f, 0.6f), dynamics = 0f, nowMs = 1_000L)
        apply(perBandState, floatArrayOf(0.4f, 0.6f), dynamics = 0f, nowMs = 31_000L)
        val nearPerBand = apply(
            perBandState,
            floatArrayOf(0.3f, 0.9f),
            dynamics = 1f,
            nowMs = 31_000L
        )
        perBandState.reset()
        val afterReset = apply(
            perBandState,
            floatArrayOf(0.3f, 0.9f),
            dynamics = 1f,
            nowMs = 31_000L
        )
        val legacy = applyAdaptiveSpectrumVisualDynamics(
            agcBands = floatArrayOf(0.2f, 0.8f),
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.LEGACY,
            profile = GlyphDeviceProfile.PHONE2,
            patternId = GlyphPatternRegistry.P2_C1_SPECTRUM,
            patternKind = GlyphPatternKind.SPECTRUM,
            state = SpectrumVisualDynamicsState(),
            nowMs = 31_000L,
            windowMs = 30_000f,
            override = GlyphVisualTuning(dynamics = 1f)
        )

        assertTrue(supportsGlyphVisualDynamics(GlyphPatternKind.SPECTRUM))
        assertArrayEquals(floatArrayOf(0.4f, 0.6f), initial, 0.0001f)
        assertArrayEquals(floatArrayOf(1f / 3f, 1f), shared, 0.0001f)
        assertEquals(0.3f / 0.9f, shared[0] / shared[1], 0.0001f)
        assertArrayEquals(floatArrayOf(1f / 60f, 1f), nearPerBand, 0.0001f)
        assertArrayEquals(floatArrayOf(0.3f, 0.9f), afterReset, 0.0001f)
        assertArrayEquals(floatArrayOf(0.2f, 0.8f), legacy, 0.0001f)
    }

    @Test
    fun visualDynamicsExpander_expandsTrackedRangeAndResetsIndependently() {
        val expander = VisualDynamicsExpander()
        expander.update(0.5f, 1_000L, 30_000f)
        expander.update(0.5f, 31_000L, 30_000f)

        assertEquals(1f, expander.update(0.75f, 31_000L, 30_000f), 0.0001f)
        assertEquals(0.5f, expander.update(0.625f, 31_000L, 30_000f), 0.0001f)

        expander.reset()
        assertEquals(0.625f, expander.update(0.625f, 31_000L, 30_000f), 0.0001f)
    }

    @Test
    fun visualDynamics_blendsNaturalAndExpandedLevels() {
        assertEquals(0.75f, blendVisualDynamics(0.75f, 1f, 0f), 0.0001f)
        assertEquals(0.875f, blendVisualDynamics(0.75f, 1f, 0.5f), 0.0001f)
        assertEquals(1f, blendVisualDynamics(0.75f, 1f, 1f), 0.0001f)
    }

    @Test
    fun tuningDatabase_hasSafeFallbackForUnknownPattern() {
        GlyphDeviceProfile.entries.forEach { profile ->
            GlyphPatternRegistry.patternsFor(profile).forEach { pattern ->
                val tuning = GlyphVisualTuningDatabase.tuningFor(profile, pattern.id)
                assertTrue(tuning.dynamics in 0f..1f)
            }
            assertEquals(0f, GlyphVisualTuningDatabase.tuningFor(profile, "new_pattern").dynamics, 0.0001f)
        }
    }

    @Test
    fun tuningDatabase_keepsPatternIdsIndependent() {
        assertEquals(
            0.3f,
            GlyphVisualTuningDatabase.tuningFor(
                GlyphDeviceProfile.PHONE4A,
                GlyphPatternRegistry.P4A_LINEAR
            ).dynamics,
            0.0001f
        )
        assertEquals(
            0.6f,
            GlyphVisualTuningDatabase.tuningFor(
                GlyphDeviceProfile.PHONE4A,
                GlyphPatternRegistry.P4A_CENTER
            ).dynamics,
            0.0001f
        )
        assertEquals(
            0.8f,
            GlyphVisualTuningDatabase.tuningFor(
                GlyphDeviceProfile.PHONE4B,
                GlyphPatternRegistry.P4A_CENTER
            ).dynamics,
            0.0001f
        )
    }

    @Test
    fun tuningClipboardEntry_isKotlinReady() {
        assertEquals(
            "key(GlyphDeviceProfile.PHONE2, \"C1_CENTER\") to " +
                "GlyphVisualTuning(\n    dynamics = 0.75f,\n),",
            formatGlyphVisualTuningEntry(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternRegistry.P2_C1_CENTER,
                GlyphVisualTuning(dynamics = 0.75f)
            )
        )
    }

    @Test
    fun tuningResolution_usesExactProfileAndPatternEntry() {
        val overrides = mapOf(
            GlyphVisualTuningKey(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternRegistry.P2_C1_CENTER
            ) to 0.75f,
            GlyphVisualTuningKey(
                GlyphDeviceProfile.PHONE3A,
                GlyphPatternRegistry.P3A_C_CENTER
            ) to 0.25f
        )

        assertEquals(
            0.75f,
            resolveGlyphVisualTuning(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternRegistry.P2_C1_CENTER,
                overrides
            ).dynamics,
            0.0001f
        )
        assertEquals(
            0.5f,
            resolveGlyphVisualTuning(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternRegistry.P2_C1_LINEAR,
                overrides
            ).dynamics,
            0.0001f
        )
    }

    @Test
    fun adaptiveAutoScale_defaultsOffForSettingsCompatibility() {
        assertFalse(DEFAULT_ADAPTIVE_AUTO_SCALE_ENABLED)
        assertEquals(GlyphAutoScaleStrategy.LEGACY, glyphAutoScaleStrategy(false))
        assertEquals(GlyphAutoScaleStrategy.ADAPTIVE, glyphAutoScaleStrategy(true))
    }

    @Test
    fun allBrightness_usesLegacyThresholdUnlessAdaptiveAutoScaleIsActive() {
        assertTrue(isAllBrightnessOff(0.06f, true, GlyphAutoScaleStrategy.LEGACY))
        assertFalse(isAllBrightnessOff(0.061f, true, GlyphAutoScaleStrategy.LEGACY))
        assertTrue(isAllBrightnessOff(0.06f, false, GlyphAutoScaleStrategy.ADAPTIVE))
        assertFalse(isAllBrightnessOff(0.061f, false, GlyphAutoScaleStrategy.ADAPTIVE))

        assertTrue(isAllBrightnessOff(0f, true, GlyphAutoScaleStrategy.ADAPTIVE))
        assertFalse(isAllBrightnessOff(0.001f, true, GlyphAutoScaleStrategy.ADAPTIVE))
        assertFalse(isAllBrightnessOff(0.06f, true, GlyphAutoScaleStrategy.ADAPTIVE))
    }

    @Test
    fun adaptiveAllBrightness_usesRawGateInsteadOfDynamicsOutput() {
        assertFalse(
            isAllBrightnessDisplayOff(
                displayLevel = 0f,
                autoScaleEnabled = true,
                strategy = GlyphAutoScaleStrategy.ADAPTIVE,
                adaptiveGateOn = true
            )
        )
        assertTrue(
            isAllBrightnessDisplayOff(
                displayLevel = 1f,
                autoScaleEnabled = true,
                strategy = GlyphAutoScaleStrategy.ADAPTIVE,
                adaptiveGateOn = false
            )
        )
    }
}
