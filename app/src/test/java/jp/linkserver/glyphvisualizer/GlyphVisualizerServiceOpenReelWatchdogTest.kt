package jp.linkserver.glyphvisualizer

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Looper
import android.provider.Settings
import jp.linkserver.glyphvisualizer.audio.MediaPlaybackActivityTracker
import jp.linkserver.glyphvisualizer.glyph.GlyphOutputController
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.lang.reflect.Proxy
import java.time.Duration

/** Drives the existing service watchdog/timer without starting audio capture or a real SDK binding. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class GlyphVisualizerServiceOpenReelWatchdogTest {
    private lateinit var service: GlyphVisualizerService
    private lateinit var session: MediaSession
    private lateinit var originalRuntime: CaptureRuntimeState
    private var suspensions = 0
    private fun field(name: String) = GlyphVisualizerService::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun call(name: String) = GlyphVisualizerService::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(service)
    private fun advance(ms: Long) = ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
    private fun update() { call("updateOpenReelMediaSessionFromWatchdog") }
    private val tracker get() = field("mediaPlaybackActivityTracker").get(service) as MediaPlaybackActivityTracker
    private fun state(state: Int?) {
        shadowOf(session.controller).setPlaybackState(state?.let { PlaybackState.Builder().setState(it, 0, 1f).build() })
    }

    @Before fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        service = Robolectric.buildService(GlyphVisualizerService::class.java).get()
        field("captureConfig").set(service, defaultServiceCaptureConfig().copy(glyphMode = GlyphPatternRegistry.P3_MATRIX_OPEN_REEL))
        val output = Proxy.newProxyInstance(GlyphOutputController::class.java.classLoader,
            arrayOf(GlyphOutputController::class.java)) { _, method, _ ->
            if (method.name == "suspendSession") suspensions++
            when (method.returnType) {
                java.lang.Float.TYPE -> 0f
                java.lang.Boolean.TYPE -> false
                else -> null
            }
        }
        field("glyphController").set(service, output)
        originalRuntime = CaptureUiStore.runtimeState
        CaptureUiStore.updateRuntime { it.copy(isCapturing = true) }
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
            ComponentName(context, MediaSessionNotificationListenerService::class.java).flattenToString())
        session = MediaSession(context, "open-reel-watchdog")
        shadowOf(session.controller).setPackageName("music")
        shadowOf(session.controller).setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "same-track").build())
        shadowOf(context.getSystemService(MediaSessionManager::class.java)).addController(session.controller)
        advance(1000)
    }

    @After fun tearDown() {
        call("resetMediaPlaybackTracking")
        session.release()
        CaptureUiStore.updateRuntime { originalRuntime }
    }

    @Test fun pausedWatchdogHolds29999msExpiresAt30000AndResumesWithExistingConfirmation() {
        state(PlaybackState.STATE_PAUSED)
        call("startOpenReelMediaSessionWatchdog")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(29_999))
        assertEquals(0, suspensions)
        assertFalse(field("openReelPausedTimeoutSuppressed").getBoolean(service))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))
        assertEquals(1, suspensions)
        assertTrue(field("openReelPausedTimeoutSuppressed").getBoolean(service))
        state(PlaybackState.STATE_PLAYING)
        update()
        assertFalse(field("openReelPausedTimeoutSuppressed").getBoolean(service))
        assertFalse(tracker.lastMediaPlaybackActive)
        advance(999); update()
        assertFalse(tracker.lastMediaPlaybackActive)
        advance(1); update()
        assertTrue(tracker.lastMediaPlaybackActive)
        assertEquals(1, suspensions)
    }

    @Test fun bufferingClearsPausedTimerAndModeSwitchStopsTheWatchdog() {
        state(PlaybackState.STATE_PAUSED)
        call("startOpenReelMediaSessionWatchdog")
        assertNotNull(field("openReelPausedTimeoutRunnable").get(service))
        state(PlaybackState.STATE_CONNECTING)
        update()
        assertNull(field("openReelPausedTimeoutRunnable").get(service))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
        assertEquals(0, suspensions)
        assertEquals(0L, tracker.openReelPausedSinceMs)
        field("captureConfig").set(service, defaultServiceCaptureConfig().copy(glyphMode = GlyphPatternRegistry.P3_MATRIX_BAR))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
        assertNull(field("openReelMediaSessionWatchdogRunnable").get(service))
        assertNull(field("openReelPausedTimeoutRunnable").get(service))
    }

    @Test fun missingSessionExpiresAt750msAndUsesResumeConfirmationAfterExpiry() {
        state(PlaybackState.STATE_PLAYING)
        update()
        advance(1000); update()
        assertTrue(tracker.lastMediaPlaybackActive)
        state(null)
        update()
        advance(749); update()
        assertEquals(0, suspensions)
        assertTrue(tracker.lastMediaPlaybackActive)
        advance(1); update()
        assertEquals(1, suspensions)
        state(PlaybackState.STATE_PLAYING)
        update()
        advance(999); update()
        assertFalse(tracker.lastMediaPlaybackActive)
        advance(1); update()
        assertTrue(tracker.lastMediaPlaybackActive)
    }

    @Test fun missingSessionRecoveredWithinGraceDoesNotSuspendOrWaitForConfirmation() {
        state(PlaybackState.STATE_PLAYING)
        update()
        advance(1000); update()
        state(null)
        update()
        advance(500)
        state(PlaybackState.STATE_PLAYING)
        update()
        assertTrue(tracker.lastMediaPlaybackActive)
        assertEquals(0, suspensions)
        assertEquals(0L, tracker.mediaPlaybackResumeCandidateAtMs)
    }
}
