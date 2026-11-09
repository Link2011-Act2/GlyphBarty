package jp.linkserver.glyphvisualizer.update

import jp.linkserver.goldy.*
import org.junit.Assert.*
import org.junit.Test

class GoldyProfileWiringTest {
    @Test fun apkUpdaterUsesThePublishedGlyphProfileAndCommonComparison() {
        assertEquals(GitHubRepository("Link2011-Act2", "GlyphBarty"), ApkUpdateProfile.repository)
        assertEquals(UpdatePolicies.glyphBarty().id, ApkUpdateProfile.policy.id)
        assertEquals(ReleaseFeed.Latest, ApkUpdateProfile.policy.feed)
        assertSame(VersionPolicies.Common, ApkUpdateProfile.policy.versions)
        assertSame(ApkSelectionPolicies.GlyphBarty, ApkUpdateProfile.selection)
    }
    @Test fun androidExecutionIsScopedToAppApkUpdates() {
        assertEquals("glyph-apk-updates", ApkUpdateProfile.JOB_NAMESPACE)
        assertEquals("glyph-apk-update-transfer-v1", ApkUpdateProfile.WORK_NAME)
        assertEquals("glyph-apk-update-transfer", ApkUpdateProfile.CHANNEL)
        assertTrue(ApkUpdateProfile.ACTION_VIEW_TRANSFER.startsWith("jp.linkserver.glyphvisualizer."))
    }
}
