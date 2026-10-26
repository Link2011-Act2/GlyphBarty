package jp.linkserver.glyphvisualizer.audio

import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackSnapshot
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus
import org.junit.Assert.*
import org.junit.Test

class MediaBrowserTrackTitleChangeDetectorTest {
    private fun track(title: String?, id: String? = null, app: String = "com.brave.browser") =
        PlaybackSnapshot(PlaybackStatus.PLAYING, 0f, app,
            trackKey = id?.let { "$app\u001fmedia-id\u001f$it" }, title = title, artist = "Artist")

    @Test fun sevenCharacterCandidateReplacedAt780msNeverBecomesAnEvent() {
        val detector = MediaTrackTitleChangeDetector()
        assertFalse(detector.observe(track("Previous"), 0))
        assertFalse(detector.observe(track("pending"), 100)) // Synthetic content, not a guessed site name.
        val formal = track("F".repeat(67))
        assertFalse(detector.observe(formal, 880))
        assertFalse(detector.observe(formal.copy(progress = 0.4f), 2_079))
        assertTrue(detector.observe(formal, 2_080))
        assertFalse(detector.observe(formal, 10_000))
    }

    @Test fun onlyTheLatestVideoSurvivesRapidChangesAndReturningToBaselineCancelsPending() {
        val detector = MediaTrackTitleChangeDetector()
        val initial = track("Initial", "a")
        detector.observe(initial, 0)
        assertFalse(detector.observe(track("B", "b"), 100))
        assertFalse(detector.observe(track("C", "c"), 600))
        assertFalse(detector.observe(initial, 1_000))
        assertFalse(detector.observe(initial, 3_000))
        val final = track("Final", "d")
        assertFalse(detector.observe(final, 3_100))
        assertFalse(detector.observe(final, 4_299))
        assertTrue(detector.observe(final, 4_300))
    }

    @Test fun missingMetadataRestartsTheStabilityWindowWithoutAcceptingMissingIdentity() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("Initial", "a"), 0)
        val final = track("Final", "b")
        assertFalse(detector.observe(final, 100))
        assertFalse(detector.observe(track(null, "b"), 900))
        assertFalse(detector.observe(final, 1_000))
        assertFalse(detector.observe(final, 2_199))
        assertTrue(detector.observe(final, 2_200))
    }

    @Test fun pauseAndBufferingDiscardPendingButPreserveTheAcceptedBaseline() {
        for (status in listOf(PlaybackStatus.PAUSED, PlaybackStatus.BUFFERING)) {
            val detector = MediaTrackTitleChangeDetector()
            detector.observe(track("Initial", "a"), 0)
            val final = track("Final", "b")
            detector.observe(final, 100)
            assertFalse(detector.observe(final.copy(status = status), 800))
            assertFalse(detector.observe(final, 2_000))
            assertTrue(detector.observe(final, 3_200))
            assertFalse(detector.observe(final.copy(status = status), 3_300))
            assertFalse(detector.observe(final, 5_000))
        }
    }

    @Test fun stopAndExplicitResetCannotReplayAPendingVideo() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("Initial", "a"), 0)
        val next = track("Next", "b")
        detector.observe(next, 100)
        assertFalse(detector.observe(next.copy(status = PlaybackStatus.STOPPED), 500))
        assertFalse(detector.observe(next, 3_000))
        assertFalse(detector.observe(next, 5_000))
        detector.observe(track("Another", "c"), 5_100)
        detector.reset()
        assertFalse(detector.observe(track("Another", "c"), 8_000))
    }

    @Test fun stableShortTitlesAreAllowedAndLateTitleCorrectionsWithSameIdStillScroll() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("Initial", "a"), 0)
        val short = track("Y", "b")
        assertFalse(detector.observe(short, 100))
        assertTrue(detector.observe(short, 1_300))
        val correction = track("Formal title", "b")
        assertFalse(detector.observe(correction, 2_000))
        assertTrue(detector.observe(correction, 3_200))
        assertFalse(detector.observe(correction, 5_000))
    }

    @Test fun durationAlbumAndProgressChangesDoNotPostponeAnOtherwiseStableTitle() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("Initial"), 0)
        val next = track("Final").copy(trackKey = "metadata-before", durationMs = 90_000)
        assertFalse(detector.observe(next, 100))
        assertTrue(detector.observe(next.copy(trackKey = "metadata-after", durationMs = 90_100, progress = 0.1f), 1_300))
    }

    @Test fun knownBrowserChannelsWaitButMusicAppsAndNativeYoutubeKeepImmediateChanges() {
        for (app in listOf("com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly",
            "com.android.chrome", "com.chrome.beta", "org.chromium.chrome", "com.microsoft.emmx")) {
            val detector = MediaTrackTitleChangeDetector()
            detector.observe(track("Initial", "a", app), 0)
            val next = track("Next", "b", app)
            assertFalse(app, detector.observe(next, 1))
            assertTrue(app, detector.observe(next, 1_201))
        }
        for (app in listOf("com.spotify.music", "com.apple.android.music", "com.soundcloud.android", "com.google.android.youtube")) {
            val detector = MediaTrackTitleChangeDetector()
            detector.observe(track("Initial", "a", app), 0)
            assertTrue(app, detector.observe(track("Next", "b", app), 1))
            assertFalse(app, detector.observe(track("Enriched", "b", app), 2))
        }
    }

    @Test fun switchingFromBrowserToMusicNeverDelaysOrLeaksAPendingBrowserTitle() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("Initial", "a"), 0)
        detector.observe(track("Transient", "b"), 100)
        val music = track("Music", "song", "com.spotify.music")
        assertTrue(detector.observe(music, 101))
        assertFalse(detector.observe(music, 2_000))
    }
}
