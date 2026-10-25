package jp.linkserver.glyphvisualizer.audio

import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackSnapshot
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus
import org.junit.Assert.*
import org.junit.Test

class MediaTrackTitleChangeDetectorTest {
    private fun track(id: String?, title: String? = id) = PlaybackSnapshot(
        PlaybackStatus.PLAYING, 0f, "music", trackKey = id?.let { "music\u001fmedia-id\u001f$it" },
        title = title, artist = "Artist"
    )

    @Test fun startupAndRepeatedSnapshotsDoNotTrigger() {
        val detector = MediaTrackTitleChangeDetector()
        val a = track("a")
        assertFalse(detector.observe(a))
        assertFalse(detector.observe(a.copy(progress = 0.5f, durationMs = 100_000)))
        assertFalse(detector.observe(a.copy(title = "Enriched title", artist = "New artist")))
        assertTrue(detector.observe(track("b")))
        assertFalse(detector.observe(track("b")))
    }

    @Test fun pauseBufferAndResumeDoNotRepeatButNewTrackAfterPauseDoes() {
        val detector = MediaTrackTitleChangeDetector()
        val a = track("a")
        detector.observe(a)
        assertFalse(detector.observe(a.copy(status = PlaybackStatus.PAUSED)))
        assertFalse(detector.observe(a.copy(status = PlaybackStatus.BUFFERING)))
        assertFalse(detector.observe(a))
        assertFalse(detector.observe(track("b").copy(status = PlaybackStatus.PAUSED)))
        assertTrue(detector.observe(track("b")))
    }

    @Test fun rapidChangesAndSameTitleWithDifferentIdsTriggerOnceEach() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("a", "Same"))
        assertTrue(detector.observe(track("b", "Same")))
        assertTrue(detector.observe(track("c")))
        assertTrue(detector.observe(track("a", "Same")))
        assertFalse(detector.observe(track("a", "Same")))
    }

    @Test fun missingMetadataAndFallbackEnrichmentAreNotChanges() {
        val detector = MediaTrackTitleChangeDetector()
        val a = track(null, "Title").copy(trackKey = "metadata-before")
        detector.observe(a)
        assertFalse(detector.observe(track(null, null)))
        assertFalse(detector.observe(a.copy(trackKey = "metadata-after", durationMs = 101L)))
        assertFalse(detector.observe(track("a", "Title")))
        assertTrue(detector.observe(track("b", null))) // Cancel old text, do not display a missing title.
        assertFalse(detector.observe(track("b", "Title B")))
    }

    @Test fun titleOnlyMetadataWorksAndStartupWithoutMetadataDoesNotTrigger() {
        val detector = MediaTrackTitleChangeDetector()
        assertFalse(detector.observe(track(null, null)))
        assertFalse(detector.observe(track(null, "A")))
        assertTrue(detector.observe(track(null, "B")))
    }

    @Test fun stopDisconnectAndExplicitResetEstablishNewBaseline() {
        val detector = MediaTrackTitleChangeDetector()
        detector.observe(track("a"))
        assertFalse(detector.observe(track(null).copy(status = PlaybackStatus.STOPPED)))
        assertFalse(detector.observe(track("b")))
        assertFalse(detector.observe(track(null).copy(status = PlaybackStatus.NONE)))
        assertFalse(detector.observe(track("c")))
        detector.reset()
        assertFalse(detector.observe(track("d")))
    }
}
