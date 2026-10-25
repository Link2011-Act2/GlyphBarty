package jp.linkserver.glyphvisualizer.glyph

import com.nothing.ketchum.Glyph
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixSdkTrackTitleRendererTest {
    @Test fun officialSetTextRendersScrollsCachesAndExitsOnBothDevices() {
        for ((model, length) in listOf(Glyph.DEVICE_23112 to 25, Glyph.DEVICE_25111p to 13)) {
            ShadowBuild.setModel(model)
            val renderer = MatrixSdkTrackTitleRenderer(RuntimeEnvironment.getApplication())
            val width = renderer.prepare("ABC 123", null, length)
            assertTrue(width > 0)
            assertTrue(renderer.render(length).all { it == 0 })
            val visible = renderer.render(0)
            assertEquals(length * length, visible.size)
            assertTrue(visible.any { it > 0 })
            assertTrue(visible.all { it in 0..255 })
            assertSame(visible, renderer.render(0))
            assertFalse(visible.contentEquals(renderer.render(-2)))
            assertTrue(renderer.render(-width).all { it == 0 })
        }
    }

    @Test fun artistIsOptionalAndLongTextIsTrimmedToTwentySeconds() {
        ShadowBuild.setModel(Glyph.DEVICE_23112)
        val renderer = MatrixSdkTrackTitleRenderer(RuntimeEnvironment.getApplication())
        val titleWidth = renderer.prepare("Title", null, 25)
        assertEquals(titleWidth, renderer.prepare("Title", "", 25))
        assertTrue(renderer.prepare("Title", "Artist", 25) > titleWidth)
        assertTrue(renderer.prepare("Very long title ".repeat(200), "Artist", 25) <= 215)
    }

    @Test fun japaneseUsesSdkStandardResultWithoutSubstitutingAFont() {
        ShadowBuild.setModel(Glyph.DEVICE_23112)
        val renderer = MatrixSdkTrackTitleRenderer(RuntimeEnvironment.getApplication())
        // The shipped SDK skips undefined characters. This is not a claim of Japanese support.
        assertEquals(0, renderer.prepare("日本語", null, 25))
        assertTrue(renderer.prepare("日本語 ABC", null, 25) > 0)
    }

    @Test fun logicalThirteenPixelPreviewCanUsePhoneThreeSdkWithoutRescalingLetters() {
        ShadowBuild.setModel(Glyph.DEVICE_23112)
        val renderer = MatrixSdkTrackTitleRenderer(RuntimeEnvironment.getApplication())
        assertTrue(renderer.prepare("A", null, 13) > 0)
        assertEquals(169, renderer.render(0).size)
        assertTrue(renderer.render(0).any { it > 0 })
    }
}
