package jp.linkserver.glyphvisualizer.glyph

import androidx.compose.ui.graphics.vector.PathParser
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
    fun officialLayoutsKeepTheExpectedPhysicalPartCount() {
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
    fun officialSettingsCanvasDimensionsArePreserved() {
        val expectedSizes = mapOf(
            GlyphDeviceProfile.PHONE1 to GlyphPreviewSize(179f, 375f),
            GlyphDeviceProfile.PHONE2 to GlyphPreviewSize(191f, 425f),
            GlyphDeviceProfile.PHONE2A to GlyphPreviewSize(176f, 374f),
            GlyphDeviceProfile.PHONE3A to GlyphPreviewSize(176f, 374f)
        )

        expectedSizes.forEach { (profile, expectedSize) ->
            val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))
            assertEquals(profile.name, expectedSize, layout.canvasSize)
            assertTrue(profile.name, layout.geometryUsesFullCanvas)
        }
    }

    @Test
    fun everyOfficialVectorPathParses() {
        val officialProfiles = listOf(
            GlyphDeviceProfile.PHONE1,
            GlyphDeviceProfile.PHONE2,
            GlyphDeviceProfile.PHONE2A,
            GlyphDeviceProfile.PHONE3A
        )

        officialProfiles.forEach { profile ->
            val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))
            layout.elements
                .filterIsInstance<GlyphLightPreviewElement.VectorPath>()
                .forEach { element ->
                    val nodes = PathParser().parsePathString(element.pathData).toNodes()
                    assertTrue(profile.name, nodes.isNotEmpty())
                }
        }
    }

    @Test
    fun overlayPartBoundsMatchTheOfficialGeometryJson() {
        val phone2a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2A))
        phone2a.specPath(GlyphPreviewSpecRange.C).assertBoundsDp(
            phone2a.canvasSize,
            left = 17.29999f,
            top = 14f,
            width = 43f,
            height = 49f
        )
        phone2a.specPath(GlyphPreviewSpecRange.A).assertBoundsDp(
            phone2a.canvasSize,
            left = 20.93997f,
            top = 109.14999f,
            width = 21f,
            height = 27f
        )
        phone2a.specPath(GlyphPreviewSpecRange.B).assertBoundsDp(
            phone2a.canvasSize,
            left = 157.12f,
            top = 53.5f,
            width = 7f,
            height = 57f
        )

        val phone3a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE3A))
        phone3a.specPath(GlyphPreviewSpecRange.C).assertBoundsDp(
            phone3a.canvasSize,
            left = 17.01999f,
            top = 14.76999f,
            width = 37f,
            height = 45f
        )
        phone3a.specPath(GlyphPreviewSpecRange.B).assertBoundsDp(
            phone3a.canvasSize,
            left = 20.84998f,
            top = 109.88998f,
            width = 21f,
            height = 25f
        )
        phone3a.specPath(GlyphPreviewSpecRange.A).assertBoundsDp(
            phone3a.canvasSize,
            left = 146.53f,
            top = 57.87997f,
            width = 17f,
            height = 61f
        )
    }

    @Test
    fun priorityDeviceSegmentCountsRemainBoundToTheExistingDeviceSpecs() {
        assertSpecSegmentCounts(
            GlyphDeviceProfile.PHONE1,
            mapOf(GlyphPreviewSpecRange.C to 4, GlyphPreviewSpecRange.D1 to 8)
        )
        assertSpecSegmentCounts(
            GlyphDeviceProfile.PHONE2,
            mapOf(GlyphPreviewSpecRange.C to 16, GlyphPreviewSpecRange.D1 to 8)
        )
        assertSpecSegmentCounts(
            GlyphDeviceProfile.PHONE2A,
            mapOf(
                GlyphPreviewSpecRange.C to 24,
                GlyphPreviewSpecRange.A to 1,
                GlyphPreviewSpecRange.B to 1
            )
        )
        assertSpecSegmentCounts(
            GlyphDeviceProfile.PHONE3A,
            mapOf(
                GlyphPreviewSpecRange.C to 20,
                GlyphPreviewSpecRange.A to 11,
                GlyphPreviewSpecRange.B to 5
            )
        )
    }

    @Test
    fun phone2UsesTheElevenOfficialPathsAndKeepsSegmentTraversal() {
        val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2))
        val c1 = layout.specPath(GlyphPreviewSpecRange.C)
        val d1 = layout.specPath(GlyphPreviewSpecRange.D1)

        assertEquals(11, layout.elements.size)
        assertEquals(GlyphPreviewBarDirection.RIGHT_TO_LEFT, c1.segmentDirection)
        assertEquals(GlyphPreviewBarDirection.BOTTOM_TO_TOP, d1.segmentDirection)
        assertEquals(GlyphPreviewRect(0f, 0f, 1f, 1f), c1.bounds)

        val c2ToC6 = layout.elements.filter { element ->
            val binding = element.channels as? GlyphPreviewChannelBinding.Unmapped
            (binding?.offset ?: -1) in 3..7
        }
        assertEquals(5, c2ToC6.size)
        assertEquals(5, c2ToC6.filterIsInstance<GlyphLightPreviewElement.VectorPath>().size)
    }

    @Test
    fun dividedOfficialShapesKeepExistingChannelDirectionsAndCounts() {
        val phone1 = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE1))
        val phone1C = phone1.specPath(GlyphPreviewSpecRange.C)
        val phone1D1 = phone1.specPath(GlyphPreviewSpecRange.D1)
        assertTrue(requireNotNull(phone1C.arcSegments).sweepAngleDegrees > 180f)
        assertEquals(GlyphPreviewBarDirection.BOTTOM_TO_TOP, phone1D1.segmentDirection)

        val phone2a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE2A))
        val phone2aC = phone2a.specPath(GlyphPreviewSpecRange.C)
        assertEquals(GlyphPreviewBarDirection.LEFT_TO_RIGHT, phone2aC.segmentDirection)
        assertTrue("Pacman C uses the official stroked path", phone2aC.strokeWidth > 0f)

        val phone3a = requireNotNull(GlyphLightPreviewGeometry.layoutFor(GlyphDeviceProfile.PHONE3A))
        val phone3aC = phone3a.specPath(GlyphPreviewSpecRange.C)
        val phone3aA = phone3a.specPath(GlyphPreviewSpecRange.A)
        val phone3aB = phone3a.specPath(GlyphPreviewSpecRange.B)
        assertEquals(GlyphPreviewBarDirection.LEFT_TO_RIGHT, phone3aC.segmentDirection)
        assertEquals(GlyphPreviewBarDirection.TOP_TO_BOTTOM, phone3aA.segmentDirection)
        assertEquals(GlyphPreviewBarDirection.RIGHT_TO_LEFT, phone3aB.segmentDirection)
        assertTrue("Barty A remains the right-side eleven-segment path", phone3aA.bounds.left > 0.8f)
        assertTrue("Barty B remains the lower-left five-segment path", phone3aB.bounds.left < 0.2f)
    }

    private fun GlyphLightPreviewLayout.specPath(
        range: GlyphPreviewSpecRange
    ): GlyphLightPreviewElement.VectorPath = elements
        .filterIsInstance<GlyphLightPreviewElement.VectorPath>()
        .first { element ->
            (element.channels as? GlyphPreviewChannelBinding.SpecRange)?.range == range
        }

    private fun GlyphLightPreviewElement.VectorPath.assertBoundsDp(
        canvas: GlyphPreviewSize,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        assertEquals(left / canvas.width, bounds.left, 0.00001f)
        assertEquals(top / canvas.height, bounds.top, 0.00001f)
        assertEquals((left + width) / canvas.width, bounds.right, 0.00001f)
        assertEquals((top + height) / canvas.height, bounds.bottom, 0.00001f)
    }

    private fun assertSpecSegmentCounts(
        profile: GlyphDeviceProfile,
        expected: Map<GlyphPreviewSpecRange, Int>
    ) {
        val spec = requireNotNull(GlyphDeviceCatalog.definitionForProfile(profile)?.lightSpec)
        val layout = requireNotNull(GlyphLightPreviewGeometry.layoutFor(profile))
        val actual = layout.resolve(spec, spec.channelCount)
            .mapNotNull { resolved ->
                val binding = resolved.geometry.channels as? GlyphPreviewChannelBinding.SpecRange
                binding?.range?.let { range -> range to resolved.channels.size }
            }
            .toMap()

        expected.forEach { (range, count) ->
            assertEquals("$profile/$range", count, actual[range])
        }
    }
}
