package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicSpectrumGroupsTest {
    @Test
    fun phone1KeepsCAsOneGroupByDefault() {
        assertEquals(
            listOf(
                listOf(0),
                listOf(1),
                (2..5).toList(),
                (7..14).toList(),
                listOf(6)
            ),
            classicSpectrumGroupsFor(
                profile = GlyphDeviceProfile.PHONE1,
                phone1ClassicCSplitEnabled = false
            )
        )
    }

    @Test
    fun phone1SplitsCIntoFourEvenlyPositionedGroupsWhenEnabled() {
        assertEquals(
            listOf(
                listOf(0),
                listOf(1),
                listOf(2),
                listOf(3),
                listOf(4),
                listOf(5),
                (7..14).toList(),
                listOf(6)
            ),
            classicSpectrumGroupsFor(
                profile = GlyphDeviceProfile.PHONE1,
                phone1ClassicCSplitEnabled = true
            )
        )
    }

    @Test
    fun splitSettingDoesNotChangeOtherProfiles() {
        assertEquals(
            classicSpectrumGroupsFor(
                profile = GlyphDeviceProfile.PHONE2,
                phone1ClassicCSplitEnabled = false
            ),
            classicSpectrumGroupsFor(
                profile = GlyphDeviceProfile.PHONE2,
                phone1ClassicCSplitEnabled = true
            )
        )
    }
}
