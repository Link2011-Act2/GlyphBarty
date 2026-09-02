package jp.linkserver.glyphvisualizer.update

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubReleaseCheckerTest {
    @Test
    fun detectReleaseChannel_returnsCanonicalChannelNames() {
        assertEquals("IntDev", detectReleaseChannel("2.1.0-IntDev_rev2"))
        assertEquals("IntDev", detectReleaseChannel("2.1.0-internal"))
        assertEquals("Beta", detectReleaseChannel("2.1.0-Beta"))
        assertEquals("Stable", detectReleaseChannel("2.1.0-Stable"))
        assertEquals("Release", detectReleaseChannel("2.1.0-Release"))
        assertEquals("Unknown", detectReleaseChannel("2.1.0"))
    }
}
