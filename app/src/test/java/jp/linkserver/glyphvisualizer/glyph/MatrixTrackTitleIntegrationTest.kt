package jp.linkserver.glyphvisualizer.glyph

import android.app.Application
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import com.nothing.ketchum.Glyph
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MatrixTrackTitleIntegrationTest {
    private lateinit var context: Application
    private lateinit var session: MediaSession
    private lateinit var controller: GlyphMatrixController
    private val frames = mutableListOf<GlyphPreviewFrame.Matrix>()

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        ShadowBuild.setModel(Glyph.DEVICE_23112)
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
            ComponentName(context, MediaSessionNotificationListenerService::class.java).flattenToString())
        session = MediaSession(context, "title-test")
        shadowOf(session.controller).setPackageName("music")
        shadowOf(session.controller).setPlaybackState(PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
        shadowOf(context.getSystemService(MediaSessionManager::class.java)).addController(session.controller)
        controller = GlyphMatrixController(context, {}, previewDeviceProfile = GlyphDeviceProfile.PHONE3_MATRIX,
            previewFrameListener = { frames += it })
        controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
        controller.setExperimentalPerformanceOptimizationsEnabled(true)
        controller.setMatrixTrackTitleSettings(true, false)
    }

    @After fun tearDown() {
        controller.unbind()
        session.release()
    }

    private fun track(id: String, title: String? = id) {
        shadowOf(session.controller).setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist").build())
    }

    private fun renderAfter(ms: Long, level: Float = 0.5f): GlyphPreviewFrame.Matrix {
        ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
        MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
        controller.updateLevel(level)
        return frames.last()
    }

    private fun playback(state: Int?) {
        shadowOf(session.controller).setPlaybackState(state?.let { PlaybackState.Builder().setState(it, 0, 1f).build() })
    }

    private fun animation() = GlyphMatrixController::class.java.getDeclaredField("trackTitleAnimation")
        .apply { isAccessible = true }.get(controller) as MatrixTrackTitleAnimation

    @Test fun playingSilenceDoesNotInterruptTitleAt1003005001000OrOverTwoSeconds() {
        for (duration in listOf(100L, 300L, 500L, 1000L, 2500L)) {
            controller.turnOff()
            track("initial-$duration", "Initial")
            renderAfter(1000)
            track("next-$duration", "日本語の長い曲名と Artist ABC")
            renderAfter(100)
            val initial = frames.size
            repeat((duration / 50).toInt()) { renderAfter(50, 0f) }
            assertTrue("Title cancelled during ${duration}ms of PLAYING silence", animation().active)
            assertEquals(if (duration < 200) 0.55f else 0.1f, animation().backgroundBrightness, 0.001f)
            assertTrue(frames.size > initial)
            val x = animation().textX
            renderAfter(50)
            assertTrue(animation().active)
            if (x != null) assertTrue(requireNotNull(animation().textX) <= x)
            assertArrayEquals(renderAfter(21_000).pixels, renderAfter(1000).pixels)
        }
    }

    @Test fun temporaryPausedBufferingNoneAndMissingSessionKeepAnimationAndIdentity() {
        track("a", "Initial")
        renderAfter(1000)
        track("b", "日本語の長い曲名 ABC")
        renderAfter(100)
        for (state in listOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_NONE, null)) {
            playback(state)
            renderAfter(100, 0f)
            assertTrue("Title cancelled by transient state $state", animation().active)
        }
        val x = requireNotNull(animation().textX)
        playback(PlaybackState.STATE_PLAYING)
        renderAfter(100, 0f)
        assertTrue(requireNotNull(animation().textX) < x) // No new Phase A or duplicate track event.
        assertEquals(0.1f, animation().backgroundBrightness, 0.001f)
    }

    @Test fun sustainedPauseExplicitStopAndIntentionalSessionEndCancelTitle() {
        for (end in listOf("pause", "stop", "release", "suspend", "off")) {
            if (end != "pause") {
                controller = GlyphMatrixController(context, {}, previewDeviceProfile = GlyphDeviceProfile.PHONE3_MATRIX,
                    previewFrameListener = { frames += it })
                controller.setMatrixTrackTitleSettings(true, false)
            }
            playback(PlaybackState.STATE_PLAYING)
            track("a-$end", "Initial")
            renderAfter(1000)
            track("b-$end", "A long title for pause and stop")
            renderAfter(100)
            assertTrue(animation().active)
            when (end) {
                "pause" -> {
                    playback(PlaybackState.STATE_PAUSED)
                    renderAfter(100, 0f)
                    assertTrue(animation().active)
                    renderAfter(1500, 0f)
                }
                "stop" -> { playback(PlaybackState.STATE_STOPPED); renderAfter(50, 0f) }
                "release" -> controller.releaseSession()
                "suspend" -> controller.suspendSession()
                "off" -> controller.turnOff()
            }
            assertFalse("Title survived $end", animation().active)
            assertEquals(1f, animation().backgroundBrightness, 0f)
        }
    }

    @Test fun mediaSessionToExistingPipelineProducesDimTextAndOriginalFrameAgain() {
        track("a", "A")
        val baseline = renderAfter(1000).pixels
        val center = 12 * 25 + 12
        track("b", "B")
        assertEquals(baseline[center], renderAfter(1000).pixels[center]) // Fade starts at 100%.
        val dimmed = renderAfter(200).pixels
        assertTrue(kotlin.math.abs((baseline[center] * MatrixTrackTitleAnimation.BACKGROUND_LEVEL).toInt() - dimmed[center]) <= 1)
        val scrolling = renderAfter(400).pixels
        assertTrue(scrolling.any { it == 255 })
        val dimmedMaximum = (baseline.max() * MatrixTrackTitleAnimation.BACKGROUND_LEVEL).toInt() + 1
        assertTrue(scrolling.any { it in dimmedMaximum..254 })
        assertTrue(scrolling.any { it in 1..254 })
        assertEquals(0, scrolling[0]) // No LED in this corner.
        assertArrayEquals(baseline, renderAfter(21_000).pixels)
        assertArrayEquals(baseline, renderAfter(1000).pixels) // No repeat for same track.
    }

    @Test fun disablingStoppingAndReenablingClearOldAnimationAndDoNotReplay() {
        track("a", "A")
        val baseline = renderAfter(1000).pixels
        track("b", "B")
        renderAfter(1000)
        assertTrue(renderAfter(600).pixels.any { it == 255 })
        controller.setMatrixTrackTitleSettings(false, true)
        assertArrayEquals(baseline, renderAfter(100).pixels)
        controller.setMatrixTrackTitleSettings(true, true)
        assertArrayEquals(baseline, renderAfter(1000).pixels)
        track("c", "C")
        renderAfter(1000)
        renderAfter(600)
        controller.turnOff()
        assertTrue(frames.last().pixels.all { it == 0 })
        assertArrayEquals(baseline, renderAfter(1000).pixels)
        controller.releaseSession()
        val count = frames.size
        controller.updateLevel(0.5f)
        assertEquals(count, frames.size)
    }

    @Test fun missingTitleAndNotificationAccessLeaveVisualizerRunning() {
        track("a", "A")
        val baseline = renderAfter(1000).pixels
        track("b", null)
        assertArrayEquals(baseline, renderAfter(1000).pixels)
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", "")
        track("c", "C")
        assertArrayEquals(baseline, renderAfter(1000).pixels)
    }

    @Test fun gateExtractsTitleArtistAndDisplayMetadataFallbacks() {
        track("a", "Title")
        var snapshot = MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
        assertEquals("Title", snapshot.title)
        assertEquals("Artist", snapshot.artist)
        assertEquals("music\u001fmedia-id\u001fa", snapshot.trackKey)
        shadowOf(session.controller).setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "Display title")
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Display artist").build())
        snapshot = MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
        assertEquals("Display title", snapshot.title)
        assertEquals("Display artist", snapshot.artist)
        val unicode = "日本語 ひらがな カタカナ か\u3099 👩‍🎤 𠮷 !?"
        track("unicode", unicode)
        snapshot = MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
        assertEquals(unicode, snapshot.title)
    }

    @Test fun japaneseTitleUsesLargeCanvasTextAndReplacementDoesNotKeepOldPixels() {
        track("a", "Initial")
        val baseline = renderAfter(1000).pixels
        track("b", "日本語の曲名")
        renderAfter(1000)
        val scrolling = renderAfter(700).pixels
        assertTrue(scrolling.any { it == 255 })
        val litRows = (0 until 25).count { y -> (0 until 25).any { x -> scrolling[y * 25 + x] == 255 } }
        assertTrue("Large text should use more than the old 5-row font", litRows > 10)
        track("c", "New song")
        val replacement = renderAfter(100).pixels
        assertFalse(replacement.any { it == 255 }) // New text is outside the right edge during Phase A.
        assertTrue(renderAfter(700).pixels.any { it == 255 })
        assertArrayEquals(baseline, renderAfter(21_000).pixels)
        assertArrayEquals(baseline, renderAfter(1000).pixels)
    }
}
