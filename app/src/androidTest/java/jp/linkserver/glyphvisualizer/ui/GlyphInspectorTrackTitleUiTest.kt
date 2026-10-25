package jp.linkserver.glyphvisualizer.ui

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.CaptureUiStore
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import jp.linkserver.glyphvisualizer.glyph.*
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GlyphInspectorTrackTitleUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun step(ms: Long = 100) {
        android.os.SystemClock.sleep(ms)
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun pixels(): Long {
        val image = compose.onNodeWithTag("inspector_matrix_pixels").performScrollTo().captureToImage().toPixelMap()
        var hash = 1125899906842597L
        for (y in 0 until image.height) for (x in 0 until image.width) hash = 31 * hash + image[x, y].toArgb()
        return hash
    }

    @Test fun phoneThreeManualFreezeReplayAndLocalSettings() = manual(GlyphDeviceProfile.PHONE3_MATRIX)
    @Test fun proManualSweepAndProfileSwitchReleaseLocalPreview() = manual(GlyphDeviceProfile.PHONE4A_PRO_MATRIX)

    private fun manual(profile: GlyphDeviceProfile) {
        val initialSettings = CaptureUiStore.state
        var visible by mutableStateOf(true)
        compose.setContent {
            GlyphBartyTheme(dynamicColor = false) {
                if (visible) GlyphInterfaceInspectorScreen(Brush.linearGradient(listOf(Color.Black, Color.DarkGray)),
                    false, profile, { visible = false })
            }
        }
        compose.onNodeWithTag("inspector_input_MANUAL").performScrollTo().performClick()
        step()
        compose.onNodeWithTag("inspector_title_enabled").performScrollTo().assertIsOn()
        compose.onNodeWithTag("inspector_test_title").performScrollTo().performTextInput("日本語 ABC Long Title ".repeat(5))
        compose.onNodeWithTag("inspector_test_artist").performScrollTo().performTextInput("歌手 Artist")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("inspector_title_replay").performScrollTo().performClick()
        step(50)
        step(800)
        val scrolling = pixels()
        compose.onNodeWithTag("inspector_freeze").performScrollTo().performClick()
        step()
        compose.onNodeWithTag("inspector_title_replay").performScrollTo().assertIsNotEnabled()
        val frozen = pixels()
        step(500)
        assertEquals(frozen, pixels())
        compose.onNodeWithTag("inspector_freeze").performScrollTo().performClick()
        step(600)
        assertNotEquals(frozen, pixels())
        compose.onNodeWithTag("inspector_title_replay").performScrollTo().performClick()
        step(50)
        step(800)
        compose.onNodeWithTag("inspector_title_enabled").performScrollTo().performClick().assertIsOff()
        step()
        compose.onNodeWithTag("inspector_title_replay").performScrollTo().assertIsNotEnabled()
        assertNotEquals(scrolling, pixels())
        compose.onNodeWithTag("inspector_title_enabled").performScrollTo().performClick()
        compose.onNodeWithTag("inspector_input_SWEEP").performScrollTo().performClick()
        step()
        compose.onNodeWithTag("inspector_title_replay").performScrollTo().performClick()
        step(50)
        step(800)
        compose.onNodeWithTag("inspector_matrix_pixels").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("inspector_profile").performScrollTo().performClick()
        step()
        compose.onNodeWithText(GlyphDeviceCatalog.presentationForProfile(GlyphDeviceProfile.PHONE2).deviceLabel,
            substring = false).performScrollTo().performClick()
        step()
        compose.onNodeWithTag("inspector_title_enabled").assertDoesNotExist()
        compose.runOnIdle { visible = false }
        step()
        assertEquals(initialSettings.matrixTrackTitleEnabled, CaptureUiStore.state.matrixTrackTitleEnabled)
        assertEquals(initialSettings.matrixTrackArtistEnabled, CaptureUiStore.state.matrixTrackArtistEnabled)
    }

    @Test fun liveMirrorShowsOnlyPublishedPixelsAndFreezeReturnsToNewestFrame() {
        compose.setContent {
            GlyphBartyTheme(dynamicColor = false) {
                GlyphInterfaceInspectorScreen(Brush.linearGradient(listOf(Color.Black, Color.DarkGray)), false,
                    GlyphDeviceProfile.PHONE3_MATRIX, {})
            }
        }
        fun publish(level: Int) = compose.runOnIdle {
            val logical = IntArray(169) { listOf(0, 42, 85, level)[it % 4] }
            val physical = IntArray(625)
            GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(logical, 25, physical)
            GlyphPreviewFrameStore.publishMatrix(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, GlyphDeviceProfile.PHONE3_MATRIX,
                GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS, 25, physical, force = true)
        }
        publish(127)
        step()
        compose.onNodeWithTag("inspector_title_enabled").assertDoesNotExist()
        val initial = pixels()
        compose.onNodeWithTag("inspector_freeze").performScrollTo().performClick()
        step()
        publish(0)
        step()
        assertEquals(initial, pixels())
        compose.onNodeWithTag("inspector_freeze").performScrollTo().performClick()
        step()
        assertNotEquals(initial, pixels())
    }

    @Test fun liveVirtualUsesRealMediaSessionWithoutAudioAndCancelsOnStop() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        val previousListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        automation.adoptShellPermissionIdentity("android.permission.MEDIA_CONTENT_CONTROL", "android.permission.WRITE_SECURE_SETTINGS")
        val session = MediaSession(context, "inspector-ui-title")
        try {
            Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
                ComponentName(context, MediaSessionNotificationListenerService::class.java).flattenToString())
            fun track(id: String, title: String) {
                session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, "歌手 Artist").build())
                MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
            }
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
            session.isActive = true
            track("a", "Initial")
            compose.setContent {
                GlyphBartyTheme(dynamicColor = false) {
                    GlyphInterfaceInspectorScreen(Brush.linearGradient(listOf(Color.Black, Color.DarkGray)), false,
                        GlyphDeviceProfile.PHONE4A_PRO_MATRIX, {})
                }
            }
                compose.onNodeWithTag("inspector_input_LIVE_VIRTUAL").performScrollTo().performClick()
            step(400)
            compose.onNodeWithTag("inspector_test_title").assertDoesNotExist()
            val silent = pixels()
            track("b", "日本語の長い曲名 ABC 123 Artist")
            step(100)
            step(1000)
            assertNotEquals(silent, pixels())
            session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0, 0f).build())
            MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
            step(400)
            assertEquals(silent, pixels())
        } finally {
            session.release()
            Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", previousListeners)
            automation.dropShellPermissionIdentity()
        }
    }
}
