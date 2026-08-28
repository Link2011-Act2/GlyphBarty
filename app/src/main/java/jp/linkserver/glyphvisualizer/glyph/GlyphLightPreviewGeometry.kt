package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.GlyphLightDeviceSpec

internal data class GlyphPreviewPoint(
    val x: Float,
    val y: Float
)

internal data class GlyphPreviewSize(
    val width: Float,
    val height: Float
) {
    init {
        require(width > 0f)
        require(height > 0f)
    }

    val aspectRatio: Float
        get() = width / height
}

internal data class GlyphPreviewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top
}

internal data class GlyphPreviewCameraMarker(
    val center: GlyphPreviewPoint,
    val radius: Float
)

internal data class GlyphPreviewArcSegments(
    val center: GlyphPreviewPoint,
    val radiusX: Float,
    val radiusY: Float,
    val startAngleDegrees: Float,
    val sweepAngleDegrees: Float
)

internal enum class GlyphPreviewSpecRange {
    C,
    A,
    B,
    D1
}

internal sealed interface GlyphPreviewChannelBinding {
    val reversed: Boolean

    data class SpecRange(
        val range: GlyphPreviewSpecRange,
        override val reversed: Boolean = false
    ) : GlyphPreviewChannelBinding

    /**
     * Selects channels that are not covered by C/A/B/D1 in ascending index order.
     * This keeps legacy single-channel Glyphs tied to the existing device spec instead
     * of duplicating their numeric indices in the visual layout.
     */
    data class Unmapped(
        val offset: Int,
        val count: Int = 1,
        override val reversed: Boolean = false
    ) : GlyphPreviewChannelBinding
}

internal enum class GlyphPreviewBarDirection {
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
}

internal sealed interface GlyphLightPreviewElement {
    val channels: GlyphPreviewChannelBinding

    data class Line(
        override val channels: GlyphPreviewChannelBinding,
        val start: GlyphPreviewPoint,
        val end: GlyphPreviewPoint,
        val strokeWidth: Float
    ) : GlyphLightPreviewElement

    data class Arc(
        override val channels: GlyphPreviewChannelBinding,
        val center: GlyphPreviewPoint,
        val radiusX: Float,
        val radiusY: Float,
        val startAngleDegrees: Float,
        val sweepAngleDegrees: Float,
        val strokeWidth: Float
    ) : GlyphLightPreviewElement

    data class Circle(
        override val channels: GlyphPreviewChannelBinding,
        val center: GlyphPreviewPoint,
        val radius: Float
    ) : GlyphLightPreviewElement

    data class Bar(
        override val channels: GlyphPreviewChannelBinding,
        val bounds: GlyphPreviewRect,
        val direction: GlyphPreviewBarDirection
    ) : GlyphLightPreviewElement

    /**
     * A filled official Settings vector placed into the device preview canvas.
     * Multi-channel paths are clipped into independent slots while the binding
     * continues to supply Barty's existing SDK channel order.
     */
    data class VectorPath(
        override val channels: GlyphPreviewChannelBinding,
        val pathData: String,
        val sourceBounds: GlyphPreviewRect,
        val bounds: GlyphPreviewRect,
        val segmentBounds: GlyphPreviewRect = sourceBounds,
        val segmentDirection: GlyphPreviewBarDirection = GlyphPreviewBarDirection.LEFT_TO_RIGHT,
        val strokeWidth: Float = 0f,
        val arcSegments: GlyphPreviewArcSegments? = null
    ) : GlyphLightPreviewElement
}

internal data class GlyphResolvedLightPreviewElement(
    val geometry: GlyphLightPreviewElement,
    val channels: List<Int>
)

internal data class GlyphLightPreviewLayout(
    val canvasSize: GlyphPreviewSize,
    val bodyCornerRadius: Float,
    val cameraMarkers: List<GlyphPreviewCameraMarker>,
    val elements: List<GlyphLightPreviewElement>,
    val geometryUsesFullCanvas: Boolean = false,
    val contentScale: Float = 1f,
    val frameAspectRatio: Float = canvasSize.aspectRatio,
    val showSegmentGaps: Boolean = false
) {
    init {
        require(contentScale > 0f)
        require(frameAspectRatio > 0f)
    }

    constructor(
        aspectRatio: Float,
        bodyCornerRadius: Float,
        cameraMarkers: List<GlyphPreviewCameraMarker>,
        elements: List<GlyphLightPreviewElement>,
        showSegmentGaps: Boolean = false
    ) : this(
        canvasSize = GlyphPreviewSize(aspectRatio, 1f),
        bodyCornerRadius = bodyCornerRadius,
        cameraMarkers = cameraMarkers,
        elements = elements,
        showSegmentGaps = showSegmentGaps
    )

    val aspectRatio: Float
        get() = frameAspectRatio

    fun resolve(
        spec: GlyphLightDeviceSpec,
        frameChannelCount: Int
    ): List<GlyphResolvedLightPreviewElement> {
        val validIndices = 0 until frameChannelCount.coerceAtLeast(0)
        val specRanges = mapOf(
            GlyphPreviewSpecRange.C to spec.cRange,
            GlyphPreviewSpecRange.A to spec.aRange,
            GlyphPreviewSpecRange.B to spec.bRange,
            GlyphPreviewSpecRange.D1 to spec.d1Range
        )
        val mappedChannels = specRanges.values
            .filterNotNull()
            .flatMapTo(mutableSetOf()) { range -> range.filter { it in validIndices } }
        val unmappedChannels = validIndices.filterNot { it in mappedChannels }

        return elements.mapNotNull { element ->
            val binding = element.channels
            val resolved = when (binding) {
                is GlyphPreviewChannelBinding.SpecRange -> specRanges[binding.range]
                    ?.filter { it in validIndices }
                    .orEmpty()
                is GlyphPreviewChannelBinding.Unmapped -> unmappedChannels
                    .drop(binding.offset.coerceAtLeast(0))
                    .take(binding.count.coerceAtLeast(0))
            }.let { channels ->
                if (binding.reversed) channels.reversed() else channels
            }
            if (resolved.isEmpty()) null else GlyphResolvedLightPreviewElement(element, resolved)
        }
    }
}

/**
 * Visual-only layouts based on normalized device coordinates. Numeric channel ranges
 * intentionally live in [GlyphLightDeviceSpec]; these definitions only describe shape,
 * relative placement, and traversal direction.
 */
internal object GlyphLightPreviewGeometry {
    private fun point(x: Float, y: Float) = GlyphPreviewPoint(x, y)

    private fun camera(x: Float, y: Float, radius: Float) =
        GlyphPreviewCameraMarker(point(x, y), radius)

    private fun spec(range: GlyphPreviewSpecRange, reversed: Boolean = false) =
        GlyphPreviewChannelBinding.SpecRange(range, reversed)

    private fun unmapped(offset: Int, count: Int = 1) =
        GlyphPreviewChannelBinding.Unmapped(offset, count)

    private val layouts = mapOf(
        GlyphDeviceProfile.PHONE1 to GlyphLightPreviewLayout(
            aspectRatio = 0.53f,
            bodyCornerRadius = 0.075f,
            cameraMarkers = listOf(
                camera(0.28f, 0.12f, 0.055f),
                camera(0.28f, 0.20f, 0.055f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(0),
                    center = point(0.28f, 0.16f),
                    radiusX = 0.105f,
                    radiusY = 0.11f,
                    startAngleDegrees = 45f,
                    sweepAngleDegrees = 270f,
                    strokeWidth = 0.038f
                ),
                GlyphLightPreviewElement.Line(
                    channels = unmapped(1),
                    start = point(0.66f, 0.19f),
                    end = point(0.76f, 0.105f),
                    strokeWidth = 0.038f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.C),
                    center = point(0.50f, 0.52f),
                    radiusX = 0.35f,
                    radiusY = 0.185f,
                    startAngleDegrees = 40f,
                    sweepAngleDegrees = 280f,
                    strokeWidth = 0.036f
                ),
                GlyphLightPreviewElement.Line(
                    channels = spec(GlyphPreviewSpecRange.D1),
                    start = point(0.50f, 0.91f),
                    end = point(0.50f, 0.77f),
                    strokeWidth = 0.038f
                ),
                GlyphLightPreviewElement.Circle(
                    channels = unmapped(2),
                    center = point(0.50f, 0.945f),
                    radius = 0.017f
                )
            )
        ),
        GlyphDeviceProfile.PHONE2 to GlyphLightPreviewLayout(
            aspectRatio = 0.52f,
            bodyCornerRadius = 0.075f,
            cameraMarkers = listOf(
                camera(0.28f, 0.105f, 0.052f),
                camera(0.28f, 0.18f, 0.052f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(0),
                    center = point(0.28f, 0.105f),
                    radiusX = 0.095f,
                    radiusY = 0.065f,
                    startAngleDegrees = 185f,
                    sweepAngleDegrees = 125f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(1),
                    center = point(0.28f, 0.19f),
                    radiusX = 0.10f,
                    radiusY = 0.065f,
                    startAngleDegrees = 300f,
                    sweepAngleDegrees = 200f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Line(
                    channels = unmapped(2),
                    start = point(0.67f, 0.17f),
                    end = point(0.76f, 0.095f),
                    strokeWidth = 0.036f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.C),
                    center = point(0.58f, 0.47f),
                    radiusX = 0.27f,
                    radiusY = 0.085f,
                    startAngleDegrees = 5f,
                    sweepAngleDegrees = -125f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(3),
                    center = point(0.38f, 0.50f),
                    radiusX = 0.20f,
                    radiusY = 0.11f,
                    startAngleDegrees = 205f,
                    sweepAngleDegrees = 65f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Line(
                    channels = unmapped(4),
                    start = point(0.18f, 0.47f),
                    end = point(0.18f, 0.57f),
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(5),
                    center = point(0.46f, 0.61f),
                    radiusX = 0.29f,
                    radiusY = 0.15f,
                    startAngleDegrees = 150f,
                    sweepAngleDegrees = -65f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = unmapped(6),
                    center = point(0.57f, 0.61f),
                    radiusX = 0.23f,
                    radiusY = 0.15f,
                    startAngleDegrees = 80f,
                    sweepAngleDegrees = -50f,
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Line(
                    channels = unmapped(7),
                    start = point(0.82f, 0.47f),
                    end = point(0.82f, 0.59f),
                    strokeWidth = 0.034f
                ),
                GlyphLightPreviewElement.Line(
                    channels = spec(GlyphPreviewSpecRange.D1),
                    start = point(0.50f, 0.90f),
                    end = point(0.50f, 0.75f),
                    strokeWidth = 0.036f
                ),
                GlyphLightPreviewElement.Circle(
                    channels = unmapped(8),
                    center = point(0.50f, 0.93f),
                    radius = 0.016f
                )
            )
        ),
        GlyphDeviceProfile.PHONE2A to GlyphLightPreviewLayout(
            aspectRatio = 0.51f,
            bodyCornerRadius = 0.075f,
            cameraMarkers = listOf(
                camera(0.41f, 0.17f, 0.067f),
                camera(0.59f, 0.17f, 0.067f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.C),
                    center = point(0.42f, 0.19f),
                    radiusX = 0.24f,
                    radiusY = 0.13f,
                    startAngleDegrees = 155f,
                    sweepAngleDegrees = 100f,
                    strokeWidth = 0.035f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.A),
                    center = point(0.38f, 0.19f),
                    radiusX = 0.18f,
                    radiusY = 0.11f,
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 60f,
                    strokeWidth = 0.038f
                ),
                GlyphLightPreviewElement.Bar(
                    channels = spec(GlyphPreviewSpecRange.B),
                    bounds = GlyphPreviewRect(0.80f, 0.105f, 0.845f, 0.255f),
                    direction = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                )
            )
        ),
        GlyphDeviceProfile.PHONE3A to GlyphLightPreviewLayout(
            aspectRatio = 0.51f,
            bodyCornerRadius = 0.07f,
            cameraMarkers = listOf(
                camera(0.38f, 0.17f, 0.055f),
                camera(0.37f, 0.245f, 0.055f),
                camera(0.57f, 0.22f, 0.060f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.C),
                    center = point(0.42f, 0.19f),
                    radiusX = 0.24f,
                    radiusY = 0.13f,
                    startAngleDegrees = 155f,
                    sweepAngleDegrees = 100f,
                    strokeWidth = 0.035f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.A),
                    center = point(0.51f, 0.19f),
                    radiusX = 0.25f,
                    radiusY = 0.14f,
                    startAngleDegrees = 310f,
                    sweepAngleDegrees = 100f,
                    strokeWidth = 0.035f
                ),
                GlyphLightPreviewElement.Arc(
                    channels = spec(GlyphPreviewSpecRange.B),
                    center = point(0.36f, 0.22f),
                    radiusX = 0.14f,
                    radiusY = 0.10f,
                    startAngleDegrees = 75f,
                    sweepAngleDegrees = 70f,
                    strokeWidth = 0.038f
                )
            )
        ),
        GlyphDeviceProfile.PHONE4A to GlyphLightPreviewLayout(
            aspectRatio = 0.51f,
            bodyCornerRadius = 0.07f,
            showSegmentGaps = true,
            cameraMarkers = listOf(
                camera(0.37f, 0.15f, 0.058f),
                camera(0.51f, 0.15f, 0.058f),
                camera(0.65f, 0.15f, 0.068f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Bar(
                    channels = spec(GlyphPreviewSpecRange.C),
                    bounds = GlyphPreviewRect(0.79f, 0.085f, 0.85f, 0.245f),
                    direction = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                ),
                GlyphLightPreviewElement.Bar(
                    channels = unmapped(0),
                    bounds = GlyphPreviewRect(0.79f, 0.252f, 0.85f, 0.282f),
                    direction = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                )
            )
        ),
        GlyphDeviceProfile.PHONE4B to GlyphLightPreviewLayout(
            aspectRatio = 0.49f,
            bodyCornerRadius = 0.075f,
            showSegmentGaps = true,
            cameraMarkers = listOf(
                camera(0.19f, 0.085f, 0.060f),
                camera(0.19f, 0.18f, 0.060f)
            ),
            elements = listOf(
                GlyphLightPreviewElement.Bar(
                    channels = spec(GlyphPreviewSpecRange.C),
                    bounds = GlyphPreviewRect(0.77f, 0.085f, 0.835f, 0.22f),
                    direction = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                ),
                GlyphLightPreviewElement.Bar(
                    channels = unmapped(0),
                    bounds = GlyphPreviewRect(0.77f, 0.228f, 0.835f, 0.263f),
                    direction = GlyphPreviewBarDirection.TOP_TO_BOTTOM
                )
            )
        )
    )

    fun layoutFor(profile: GlyphDeviceProfile): GlyphLightPreviewLayout? =
        GlyphOfficialLightPreviewLayouts.layoutFor(profile) ?: layouts[profile]
}
