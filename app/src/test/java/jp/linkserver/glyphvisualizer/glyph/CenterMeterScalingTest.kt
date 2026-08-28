package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Test

class CenterMeterScalingTest {
    @Test
    fun defaultVisualTuning_keepsCenterLevelUnchanged() {
        assertEquals(
            0.65f,
            applyAdaptiveVisualDynamics(
                agcLevel = 0.65f,
                autoScaleEnabled = true,
                strategy = GlyphAutoScaleStrategy.ADAPTIVE,
                profile = GlyphDeviceProfile.PHONE4A,
                patternId = GlyphPatternRegistry.P4A_CENTER,
                patternKind = GlyphPatternKind.CENTER,
                expander = VisualDynamicsExpander(),
                nowMs = 1_000L,
                windowMs = 30_000f
            ),
            0.0001f
        )
    }
}
