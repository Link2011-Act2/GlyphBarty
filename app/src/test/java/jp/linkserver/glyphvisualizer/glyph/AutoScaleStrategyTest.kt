package jp.linkserver.glyphvisualizer.glyph

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
    fun visualTuning_appliesOnlyToAdaptiveAutoScale() {
        val tuning = GlyphVisualTuning(scale = 1.5f)
        val legacy = applyAutoScaleVisualTuning(
            value = 0.4f,
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.LEGACY,
            profile = GlyphDeviceProfile.PHONE2,
            patternKind = GlyphPatternKind.CENTER,
            override = tuning
        )
        val disabled = applyAutoScaleVisualTuning(
            value = 0.4f,
            autoScaleEnabled = false,
            strategy = GlyphAutoScaleStrategy.ADAPTIVE,
            profile = GlyphDeviceProfile.PHONE2,
            patternKind = GlyphPatternKind.CENTER,
            override = tuning
        )
        val adaptive = applyAutoScaleVisualTuning(
            value = 0.4f,
            autoScaleEnabled = true,
            strategy = GlyphAutoScaleStrategy.ADAPTIVE,
            profile = GlyphDeviceProfile.PHONE2,
            patternKind = GlyphPatternKind.CENTER,
            override = tuning
        )

        assertEquals(0.4f, legacy, 0.0001f)
        assertEquals(0.4f, disabled, 0.0001f)
        assertEquals(0.6f, adaptive, 0.0001f)
    }

    @Test
    fun tuningDatabase_containsEveryProfileAndPatternKind() {
        GlyphDeviceProfile.entries.forEach { profile ->
            GlyphPatternKind.entries.forEach { patternKind ->
                val tuning = GlyphVisualTuningDatabase.tuningFor(profile, patternKind)
                assertTrue(tuning.scale > 0f)
                assertEquals(1f, tuning.scale, 0.0001f)
            }
        }
    }

    @Test
    fun tuningClipboardEntry_isKotlinReady() {
        assertEquals(
            "key(GlyphDeviceProfile.PHONE2, GlyphPatternKind.CENTER) to " +
                "GlyphVisualTuning(\n    scale = 1.38f,\n),",
            formatGlyphVisualTuningEntry(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternKind.CENTER,
                GlyphVisualTuning(scale = 1.38f)
            )
        )
    }

    @Test
    fun tuningResolution_usesExactProfileAndPatternEntry() {
        val overrides = mapOf(
            GlyphVisualTuningKey(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternKind.CENTER
            ) to 1.38f,
            GlyphVisualTuningKey(
                GlyphDeviceProfile.PHONE3A,
                GlyphPatternKind.CENTER
            ) to 0.82f
        )

        assertEquals(
            1.38f,
            resolveGlyphVisualTuning(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternKind.CENTER,
                overrides
            ).scale,
            0.0001f
        )
        assertEquals(
            1f,
            resolveGlyphVisualTuning(
                GlyphDeviceProfile.PHONE2,
                GlyphPatternKind.LINEAR,
                overrides
            ).scale,
            0.0001f
        )
    }

    @Test
    fun adaptiveAutoScale_defaultsOffForSettingsCompatibility() {
        assertFalse(DEFAULT_ADAPTIVE_AUTO_SCALE_ENABLED)
        assertEquals(GlyphAutoScaleStrategy.LEGACY, glyphAutoScaleStrategy(false))
        assertEquals(GlyphAutoScaleStrategy.ADAPTIVE, glyphAutoScaleStrategy(true))
    }
}
