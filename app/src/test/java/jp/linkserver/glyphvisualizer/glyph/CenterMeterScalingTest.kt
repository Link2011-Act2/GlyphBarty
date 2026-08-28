package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Test

class CenterMeterScalingTest {
    @Test
    fun defaultVisualTuning_keepsCenterLevelUnchanged() {
        assertEquals(
            0.65f,
            applyAutoScaleVisualTuning(
                value = 0.65f,
                autoScaleEnabled = true,
                strategy = GlyphAutoScaleStrategy.ADAPTIVE,
                profile = GlyphDeviceProfile.PHONE4A,
                patternKind = GlyphPatternKind.CENTER
            ),
            0.0001f
        )
    }
}
