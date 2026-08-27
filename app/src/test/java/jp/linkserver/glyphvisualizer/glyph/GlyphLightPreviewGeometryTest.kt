package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.GlyphLightDeviceSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphLightPreviewGeometryTest {
    private val lightProfiles = listOf(
        GlyphDeviceProfile.PHONE1,
        GlyphDeviceProfile.PHONE2,
        GlyphDeviceProfile.PHONE2A,
        GlyphDeviceProfile.PHONE3A,
        GlyphDeviceProfile.PHONE4A,
        GlyphDeviceProfile.PHONE4B
    )

    @Test
    fun everyLightsProfileHasADeviceLayout() {
        lightProfiles.forEach { profile ->
            assertNotNull(profile.name, GlyphLightPreviewGeometry.layoutFor(profile))
        }
    }

    @Test
    fun layoutsResolveEverySpecChannelExactlyOnce() {
        lightProfiles.forEach { profile ->
            val spec = requireNotNull(GlyphDeviceCatalog.definitionForProfile(profile)?.lightSpec)
            val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))
            val resolvedChannels = layout.resolve(spec, spec.channelCount)
                .flatMap { it.channels }

            assertEquals(profile.name, resolvedChannels.toSet().size, resolvedChannels.size)
            assertEquals(profile.name, (0 until spec.channelCount).toSet(), resolvedChannels.toSet())
        }
    }

    @Test
    fun optionalPhone4bRecordingChannelIsResolvedFromTheFrameSize() {
        val profile = GlyphDeviceProfile.PHONE4B
        val spec = requireNotNull(GlyphDeviceCatalog.definitionForProfile(profile)?.lightSpec)
        val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))

        val resolvedChannels = layout.resolve(spec, frameChannelCount = 5)
            .flatMap { it.channels }

        assertEquals((0 until 5).toSet(), resolvedChannels.toSet())
    }

    @Test
    fun specBindingsFollowTheProvidedSpecInsteadOfEmbeddedIndices() {
        val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2A))
        val shiftedSpec = GlyphLightDeviceSpec(
            sdkDeviceId = "test",
            channelCount = 9,
            cRange = 3..5,
            aRange = 0..0,
            bRange = 2..2
        )

        val resolved = layout.resolve(shiftedSpec, shiftedSpec.channelCount)
        val byRange = resolved.associate { element ->
            val binding = element.geometry.channels as GlyphPreviewChannelBinding.SpecRange
            binding.range to element.channels
        }

        assertEquals(listOf(3, 4, 5), byRange[GlyphPreviewSpecRange.C])
        assertEquals(listOf(0), byRange[GlyphPreviewSpecRange.A])
        assertEquals(listOf(2), byRange[GlyphPreviewSpecRange.B])
    }

    @Test
    fun sketchBasedLayoutsKeepTheExpectedPhysicalSegmentCount() {
        val expectedSegmentCounts = mapOf(
            GlyphDeviceProfile.PHONE1 to 5,
            GlyphDeviceProfile.PHONE2 to 11,
            GlyphDeviceProfile.PHONE2A to 3,
            GlyphDeviceProfile.PHONE3A to 3
        )

        expectedSegmentCounts.forEach { (profile, expectedCount) ->
            val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))
            assertEquals(profile.name, expectedCount, layout.elements.size)
        }
    }

    @Test
    fun phone2CentralSegmentsUseTheSketchSkeletonInsteadOfOneCircularRing() {
        val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2))
        val c1 = layout.specArc(GlyphPreviewSpecRange.C)

        assertTrue("C1 must be a shallow upper arc", c1.radiusY < c1.radiusX / 3f)
        assertTrue("C1 must stay clear of the separate C2/C3 cluster", c1.center.x - c1.radiusX >= 0.28f)
        assertTrue("C1_1 must traverse from right toward C1_16 on the left", c1.sweepAngleDegrees < 0f)

        val c2ToC6 = layout.elements.filter { element ->
            val binding = element.channels as? GlyphPreviewChannelBinding.Unmapped
            (binding?.offset ?: -1) in 3..7
        }
        assertEquals(5, c2ToC6.size)
        assertEquals(3, c2ToC6.filterIsInstance<GlyphLightPreviewElement.Arc>().size)
        assertEquals(2, c2ToC6.filterIsInstance<GlyphLightPreviewElement.Line>().size)
    }

    @Test
    fun dividedSketchSegmentsFollowTheirLabeledEndpointDirection() {
        val phone1 = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE1))
        val phone1C = phone1.specArc(GlyphPreviewSpecRange.C)
        val phone1D1 = phone1.specLine(GlyphPreviewSpecRange.D1)
        assertTrue(phone1C.startAngleDegrees in 0f..90f)
        assertTrue(phone1C.sweepAngleDegrees > 180f)
        assertTrue("D1_1 is below D1_8", phone1D1.start.y > phone1D1.end.y)

        val phone2a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2A))
        val phone2aC = phone2a.specArc(GlyphPreviewSpecRange.C)
        assertTrue("C1 starts below C24", phone2aC.startAngleDegrees < 180f)
        assertTrue(phone2aC.sweepAngleDegrees > 0f)

        val phone3a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE3A))
        val phone3aC = phone3a.specArc(GlyphPreviewSpecRange.C)
        val phone3aA = phone3a.specArc(GlyphPreviewSpecRange.A)
        val phone3aB = phone3a.specArc(GlyphPreviewSpecRange.B)
        assertTrue("A1 traverses from top to A11 at the bottom", phone3aA.sweepAngleDegrees > 0f)
        assertTrue("B1 traverses from lower-right to B5 at upper-left", phone3aB.sweepAngleDegrees > 0f)
        assertTrue("B is a short arc", phone3aB.sweepAngleDegrees < 90f)
        assertTrue("B sits below the C arc", phone3aB.center.y > phone3aC.center.y)
    }

    private fun GlyphLightPreviewLayout.specArc(
        range: GlyphPreviewSpecRange
    ): GlyphLightPreviewElement.Arc = elements
        .filterIsInstance<GlyphLightPreviewElement.Arc>()
        .first { element ->
            (element.channels as? GlyphPreviewChannelBinding.SpecRange)?.range == range
        }

    private fun GlyphLightPreviewLayout.specLine(
        range: GlyphPreviewSpecRange
    ): GlyphLightPreviewElement.Line = elements
        .filterIsInstance<GlyphLightPreviewElement.Line>()
        .first { element ->
            (element.channels as? GlyphPreviewChannelBinding.SpecRange)?.range == range
        }
}
