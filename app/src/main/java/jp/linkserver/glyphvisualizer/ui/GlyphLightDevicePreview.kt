package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphLightPreviewElement
import jp.linkserver.glyphvisualizer.glyph.GlyphLightPreviewGeometry
import jp.linkserver.glyphvisualizer.glyph.GlyphPreviewBarDirection
import jp.linkserver.glyphvisualizer.glyph.GlyphPreviewPoint
import jp.linkserver.glyphvisualizer.glyph.GlyphPreviewRect
import jp.linkserver.glyphvisualizer.glyph.GlyphResolvedLightPreviewElement
import kotlin.math.abs
import kotlin.math.min

private const val MAX_LIGHT_PREVIEW_BRIGHTNESS = 4095
private const val SEGMENT_GAP_RATIO = 0.14f

private val DeviceBodyColor = Color(0xFF17191B)
private val DeviceBodyOutlineColor = Color(0xFF555B61)
private val CameraFillColor = Color(0xFF0B0C0D)
private val CameraOutlineColor = Color(0xFF34383C)
private val InactiveGlyphColor = Color(0xFF34383C)

@Composable
internal fun GlyphLightDevicePreview(
    profile: GlyphDeviceProfile,
    brightness: IntArray,
    modifier: Modifier = Modifier
) {
    val layout = remember(profile) { GlyphLightPreviewGeometry.layoutFor(profile) } ?: return
    val lightSpec = remember(profile) {
        GlyphDeviceCatalog.definitionForProfile(profile)?.lightSpec
    } ?: return
    val resolvedElements = remember(profile, brightness.size) {
        layout.resolve(lightSpec, brightness.size)
    }
    val vectorPaths = remember(layout) {
        layout.elements
            .filterIsInstance<GlyphLightPreviewElement.VectorPath>()
            .associate { element ->
                element.pathData to PathParser().parsePathString(element.pathData).toPath()
            }
    }

    Canvas(modifier = modifier.aspectRatio(layout.aspectRatio)) {
        val inset = size.width * 0.025f
        val bodyInset = if (layout.geometryUsesFullCanvas) 0f else inset
        val body = Rect(
            left = bodyInset,
            top = bodyInset,
            right = size.width - bodyInset,
            bottom = size.height - bodyInset
        )
        val geometryBase = if (
            layout.geometryUsesFullCanvas &&
            layout.frameAspectRatio != layout.canvasSize.aspectRatio
        ) {
            Rect(Offset.Zero, size).fitCenter(layout.canvasSize.aspectRatio)
        } else if (layout.geometryUsesFullCanvas) {
            Rect(Offset.Zero, size)
        } else {
            body
        }
        val geometryFrame = geometryBase.scaledFromCenter(layout.contentScale)
        val cornerRadius = layout.bodyCornerRadius * body.width
        drawRoundRect(
            color = DeviceBodyColor,
            topLeft = body.topLeft,
            size = body.size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        drawRoundRect(
            color = DeviceBodyOutlineColor,
            topLeft = body.topLeft,
            size = body.size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = (body.width * 0.012f).coerceAtLeast(1f))
        )

        layout.cameraMarkers.forEach { marker ->
            val center = normalizedPoint(marker.center, geometryFrame)
            val radius = marker.radius * geometryFrame.width
            drawCircle(CameraFillColor, radius = radius, center = center)
            drawCircle(
                color = CameraOutlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = (body.width * 0.012f).coerceAtLeast(1f))
            )
        }

        resolvedElements.forEach { resolved ->
            drawResolvedElement(
                resolved = resolved,
                brightness = brightness,
                body = geometryFrame,
                vectorPaths = vectorPaths,
                showSegmentGaps = layout.showSegmentGaps
            )
        }
    }
}

private fun DrawScope.drawResolvedElement(
    resolved: GlyphResolvedLightPreviewElement,
    brightness: IntArray,
    body: Rect,
    vectorPaths: Map<String, Path>,
    showSegmentGaps: Boolean
) {
    when (val geometry = resolved.geometry) {
        is GlyphLightPreviewElement.Line -> drawSegmentedLine(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body,
            showSegmentGaps = showSegmentGaps
        )
        is GlyphLightPreviewElement.Arc -> drawSegmentedArc(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body,
            showSegmentGaps = showSegmentGaps
        )
        is GlyphLightPreviewElement.Circle -> drawSegmentedCircle(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body,
            showSegmentGaps = showSegmentGaps
        )
        is GlyphLightPreviewElement.Bar -> drawSegmentedBar(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body,
            showSegmentGaps = showSegmentGaps
        )
        is GlyphLightPreviewElement.VectorPath -> drawSegmentedVectorPath(
            geometry = geometry,
            path = vectorPaths.getValue(geometry.pathData),
            channels = resolved.channels,
            brightness = brightness,
            body = body,
            showSegmentGaps = showSegmentGaps
        )
    }
}

private fun DrawScope.drawSegmentedVectorPath(
    geometry: GlyphLightPreviewElement.VectorPath,
    path: Path,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect,
    showSegmentGaps: Boolean
) {
    val target = normalizedRect(geometry.bounds, body)
    val source = geometry.sourceBounds
    val scaleX = target.width / source.width
    val scaleY = target.height / source.height

    withTransform({
        translate(target.left, target.top)
        scale(scaleX, scaleY, pivot = Offset.Zero)
        translate(-source.left, -source.top)
    }) {
        if (channels.size == 1) {
            drawOfficialPath(
                path = path,
                color = glyphColor(brightness.getOrElse(channels.first()) { 0 }),
                strokeWidth = geometry.strokeWidth
            )
            return@withTransform
        }

        val gapRatio = if (showSegmentGaps) SEGMENT_GAP_RATIO else 0f
        if (!showSegmentGaps) {
            drawOfficialPath(
                path = path,
                color = InactiveGlyphColor,
                strokeWidth = geometry.strokeWidth
            )
        }

        geometry.arcSegments?.let { arcSegments ->
            val segmentSweep = arcSegments.sweepAngleDegrees / channels.size
            val arcBounds = Rect(
                left = arcSegments.center.x - arcSegments.radiusX,
                top = arcSegments.center.y - arcSegments.radiusY,
                right = arcSegments.center.x + arcSegments.radiusX,
                bottom = arcSegments.center.y + arcSegments.radiusY
            )
            channels.forEachIndexed { index, channel ->
                val channelBrightness = brightness.getOrElse(channel) { 0 }
                if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
                val wedgePath = Path().apply {
                    moveTo(arcSegments.center.x, arcSegments.center.y)
                    arcTo(
                        rect = arcBounds,
                        startAngleDegrees = arcSegments.startAngleDegrees + segmentSweep *
                            (index + gapRatio / 2f),
                        sweepAngleDegrees = segmentSweep * (1f - gapRatio),
                        forceMoveTo = false
                    )
                    close()
                }
                clipPath(wedgePath) {
                    drawOfficialPath(
                        path = path,
                        color = glyphColor(channelBrightness),
                        strokeWidth = geometry.strokeWidth
                    )
                }
            }
            return@withTransform
        }

        val segmentBounds = geometry.segmentBounds
        val vertical = geometry.segmentDirection == GlyphPreviewBarDirection.TOP_TO_BOTTOM ||
            geometry.segmentDirection == GlyphPreviewBarDirection.BOTTOM_TO_TOP
        val segmentLength = if (vertical) {
            segmentBounds.height / channels.size
        } else {
            segmentBounds.width / channels.size
        }
        val gap = segmentLength * gapRatio

        channels.forEachIndexed { index, channel ->
            val channelBrightness = brightness.getOrElse(channel) { 0 }
            if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
            val slot = when (geometry.segmentDirection) {
                GlyphPreviewBarDirection.TOP_TO_BOTTOM,
                GlyphPreviewBarDirection.LEFT_TO_RIGHT -> index
                GlyphPreviewBarDirection.BOTTOM_TO_TOP,
                GlyphPreviewBarDirection.RIGHT_TO_LEFT -> channels.lastIndex - index
            }
            val clipBounds = if (vertical) {
                Rect(
                    left = segmentBounds.left,
                    top = segmentBounds.top + slot * segmentLength + gap / 2f,
                    right = segmentBounds.right,
                    bottom = segmentBounds.top + (slot + 1) * segmentLength - gap / 2f
                )
            } else {
                Rect(
                    left = segmentBounds.left + slot * segmentLength + gap / 2f,
                    top = segmentBounds.top,
                    right = segmentBounds.left + (slot + 1) * segmentLength - gap / 2f,
                    bottom = segmentBounds.bottom
                )
            }
            clipRect(
                left = clipBounds.left,
                top = clipBounds.top,
                right = clipBounds.right,
                bottom = clipBounds.bottom
            ) {
                drawOfficialPath(
                    path = path,
                    color = glyphColor(channelBrightness),
                    strokeWidth = geometry.strokeWidth
                )
            }
        }
    }
}

private fun DrawScope.drawOfficialPath(path: Path, color: Color, strokeWidth: Float) {
    if (strokeWidth > 0f) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
    } else {
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawSegmentedLine(
    geometry: GlyphLightPreviewElement.Line,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect,
    showSegmentGaps: Boolean
) {
    val start = normalizedPoint(geometry.start, body)
    val end = normalizedPoint(geometry.end, body)
    val strokeWidth = (geometry.strokeWidth * body.width).coerceAtLeast(1f)
    if (channels.size == 1) {
        drawLine(
            color = glyphColor(brightness.getOrElse(channels.first()) { 0 }),
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        return
    }

    val gapRatio = if (showSegmentGaps) SEGMENT_GAP_RATIO else 0f
    if (!showSegmentGaps) {
        drawLine(
            color = InactiveGlyphColor,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
    channels.forEachIndexed { index, channel ->
        val channelBrightness = brightness.getOrElse(channel) { 0 }
        if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
        val startFraction = segmentFraction(index, channels.size, gapRatio, start = true)
        val endFraction = segmentFraction(index, channels.size, gapRatio, start = false)
        drawLine(
            color = glyphColor(channelBrightness),
            start = interpolate(start, end, startFraction),
            end = interpolate(start, end, endFraction),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSegmentedArc(
    geometry: GlyphLightPreviewElement.Arc,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect,
    showSegmentGaps: Boolean
) {
    val center = normalizedPoint(geometry.center, body)
    val arcSize = Size(
        width = geometry.radiusX * body.width * 2f,
        height = geometry.radiusY * body.height * 2f
    )
    val topLeft = Offset(
        x = center.x - arcSize.width / 2f,
        y = center.y - arcSize.height / 2f
    )
    val segmentSweep = geometry.sweepAngleDegrees / channels.size.coerceAtLeast(1)
    val strokeWidth = (geometry.strokeWidth * body.width).coerceAtLeast(1f)
    if (channels.size == 1) {
        drawArc(
            color = glyphColor(brightness.getOrElse(channels.first()) { 0 }),
            startAngle = geometry.startAngleDegrees,
            sweepAngle = geometry.sweepAngleDegrees,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        return
    }

    val gapRatio = if (showSegmentGaps) SEGMENT_GAP_RATIO else 0f
    if (!showSegmentGaps) {
        drawArc(
            color = InactiveGlyphColor,
            startAngle = geometry.startAngleDegrees,
            sweepAngle = geometry.sweepAngleDegrees,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
    channels.forEachIndexed { index, channel ->
        val channelBrightness = brightness.getOrElse(channel) { 0 }
        if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
        drawArc(
            color = glyphColor(channelBrightness),
            startAngle = geometry.startAngleDegrees + segmentSweep *
                (index + gapRatio / 2f),
            sweepAngle = segmentSweep * (1f - gapRatio),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawSegmentedCircle(
    geometry: GlyphLightPreviewElement.Circle,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect,
    showSegmentGaps: Boolean
) {
    val center = normalizedPoint(geometry.center, body)
    val radius = geometry.radius * body.width
    if (channels.size == 1) {
        drawCircle(
            color = glyphColor(brightness.getOrElse(channels.first()) { 0 }),
            radius = radius,
            center = center
        )
        return
    }

    val strokeWidth = (radius * 0.7f).coerceAtLeast(1f)
    val gapRatio = if (showSegmentGaps) SEGMENT_GAP_RATIO else 0f
    if (!showSegmentGaps) {
        drawCircle(
            color = InactiveGlyphColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }
    val segmentSweep = 360f / channels.size
    channels.forEachIndexed { index, channel ->
        val channelBrightness = brightness.getOrElse(channel) { 0 }
        if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
        drawArc(
            color = glyphColor(channelBrightness),
            startAngle = segmentSweep * (index + gapRatio / 2f),
            sweepAngle = segmentSweep * (1f - gapRatio),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawSegmentedBar(
    geometry: GlyphLightPreviewElement.Bar,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect,
    showSegmentGaps: Boolean
) {
    val bounds = normalizedRect(geometry.bounds, body)
    val baseRadius = min(bounds.width, bounds.height) * 0.22f
    if (channels.size == 1) {
        drawRoundRect(
            color = glyphColor(brightness.getOrElse(channels.first()) { 0 }),
            topLeft = bounds.topLeft,
            size = bounds.size,
            cornerRadius = CornerRadius(baseRadius, baseRadius)
        )
        return
    }

    if (!showSegmentGaps) {
        drawRoundRect(
            color = InactiveGlyphColor,
            topLeft = bounds.topLeft,
            size = bounds.size,
            cornerRadius = CornerRadius(baseRadius, baseRadius)
        )
    }
    val vertical = geometry.direction == GlyphPreviewBarDirection.TOP_TO_BOTTOM ||
        geometry.direction == GlyphPreviewBarDirection.BOTTOM_TO_TOP
    val segmentLength = if (vertical) bounds.height / channels.size else bounds.width / channels.size
    val gapRatio = if (showSegmentGaps) SEGMENT_GAP_RATIO else 0f
    val gap = segmentLength * gapRatio

    channels.forEachIndexed { index, channel ->
        val channelBrightness = brightness.getOrElse(channel) { 0 }
        if (!showSegmentGaps && channelBrightness <= 0) return@forEachIndexed
        val slot = when (geometry.direction) {
            GlyphPreviewBarDirection.TOP_TO_BOTTOM,
            GlyphPreviewBarDirection.LEFT_TO_RIGHT -> index
            GlyphPreviewBarDirection.BOTTOM_TO_TOP,
            GlyphPreviewBarDirection.RIGHT_TO_LEFT -> channels.lastIndex - index
        }
        val segmentBounds = if (vertical) {
            Rect(
                left = bounds.left,
                top = bounds.top + slot * segmentLength + gap / 2f,
                right = bounds.right,
                bottom = bounds.top + (slot + 1) * segmentLength - gap / 2f
            )
        } else {
            Rect(
                left = bounds.left + slot * segmentLength + gap / 2f,
                top = bounds.top,
                right = bounds.left + (slot + 1) * segmentLength - gap / 2f,
                bottom = bounds.bottom
            )
        }
        if (showSegmentGaps) {
            val radius = min(segmentBounds.width, segmentBounds.height) * 0.22f
            drawRoundRect(
                color = glyphColor(channelBrightness),
                topLeft = segmentBounds.topLeft,
                size = segmentBounds.size,
                cornerRadius = CornerRadius(radius, radius)
            )
        } else {
            val outerPath = Path().apply {
                addRoundRect(RoundRect(bounds, CornerRadius(baseRadius, baseRadius)))
            }
            clipPath(outerPath) {
                drawRect(
                    color = glyphColor(channelBrightness),
                    topLeft = segmentBounds.topLeft,
                    size = segmentBounds.size
                )
            }
        }
    }
}

private fun normalizedPoint(point: GlyphPreviewPoint, body: Rect): Offset = Offset(
    x = body.left + point.x * body.width,
    y = body.top + point.y * body.height
)

private fun normalizedRect(rect: GlyphPreviewRect, body: Rect): Rect = Rect(
    left = body.left + rect.left * body.width,
    top = body.top + rect.top * body.height,
    right = body.left + rect.right * body.width,
    bottom = body.top + rect.bottom * body.height
)

private fun Rect.scaledFromCenter(scale: Float): Rect {
    if (scale == 1f) return this
    val horizontalInset = width * (1f - scale) / 2f
    val verticalInset = height * (1f - scale) / 2f
    return Rect(
        left = left + horizontalInset,
        top = top + verticalInset,
        right = right - horizontalInset,
        bottom = bottom - verticalInset
    )
}

private fun Rect.fitCenter(aspectRatio: Float): Rect {
    val currentAspectRatio = width / height
    return if (currentAspectRatio > aspectRatio) {
        val fittedWidth = height * aspectRatio
        val horizontalInset = (width - fittedWidth) / 2f
        Rect(left + horizontalInset, top, right - horizontalInset, bottom)
    } else {
        val fittedHeight = width / aspectRatio
        val verticalInset = (height - fittedHeight) / 2f
        Rect(left, top + verticalInset, right, bottom - verticalInset)
    }
}

private fun segmentFraction(
    index: Int,
    count: Int,
    gapRatio: Float,
    start: Boolean
): Float {
    val segmentSize = 1f / count.coerceAtLeast(1)
    val gap = segmentSize * gapRatio
    return if (start) {
        index * segmentSize + gap / 2f
    } else {
        (index + 1) * segmentSize - gap / 2f
    }
}

private fun interpolate(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * fraction,
    y = start.y + (end.y - start.y) * fraction
)

private fun glyphColor(brightness: Int): Color {
    val ratio = brightness.coerceIn(0, MAX_LIGHT_PREVIEW_BRIGHTNESS) /
        MAX_LIGHT_PREVIEW_BRIGHTNESS.toFloat()
    return lerp(InactiveGlyphColor, Color.White, ratio)
}
