package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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

    Canvas(modifier = modifier.aspectRatio(layout.aspectRatio)) {
        val inset = size.width * 0.025f
        val body = Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )
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
            val center = normalizedPoint(marker.center, body)
            val radius = marker.radius * body.width
            drawCircle(CameraFillColor, radius = radius, center = center)
            drawCircle(
                color = CameraOutlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = (body.width * 0.012f).coerceAtLeast(1f))
            )
        }

        resolvedElements.forEach { resolved ->
            drawResolvedElement(resolved, brightness, body)
        }
    }
}

private fun DrawScope.drawResolvedElement(
    resolved: GlyphResolvedLightPreviewElement,
    brightness: IntArray,
    body: Rect
) {
    when (val geometry = resolved.geometry) {
        is GlyphLightPreviewElement.Line -> drawSegmentedLine(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body
        )
        is GlyphLightPreviewElement.Arc -> drawSegmentedArc(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body
        )
        is GlyphLightPreviewElement.Circle -> drawSegmentedCircle(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body
        )
        is GlyphLightPreviewElement.Bar -> drawSegmentedBar(
            geometry = geometry,
            channels = resolved.channels,
            brightness = brightness,
            body = body
        )
    }
}

private fun DrawScope.drawSegmentedLine(
    geometry: GlyphLightPreviewElement.Line,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect
) {
    val start = normalizedPoint(geometry.start, body)
    val end = normalizedPoint(geometry.end, body)
    val strokeWidth = (geometry.strokeWidth * body.width).coerceAtLeast(1f)
    channels.forEachIndexed { index, channel ->
        val startFraction = segmentFraction(index, channels.size, start = true)
        val endFraction = segmentFraction(index, channels.size, start = false)
        drawLine(
            color = glyphColor(brightness.getOrElse(channel) { 0 }),
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
    body: Rect
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
    channels.forEachIndexed { index, channel ->
        drawArc(
            color = glyphColor(brightness.getOrElse(channel) { 0 }),
            startAngle = geometry.startAngleDegrees + segmentSweep *
                (index + SEGMENT_GAP_RATIO / 2f),
            sweepAngle = segmentSweep * (1f - SEGMENT_GAP_RATIO),
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
    body: Rect
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

    val segmentSweep = 360f / channels.size
    channels.forEachIndexed { index, channel ->
        drawArc(
            color = glyphColor(brightness.getOrElse(channel) { 0 }),
            startAngle = segmentSweep * (index + SEGMENT_GAP_RATIO / 2f),
            sweepAngle = segmentSweep * (1f - SEGMENT_GAP_RATIO),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(
                width = (radius * 0.7f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawSegmentedBar(
    geometry: GlyphLightPreviewElement.Bar,
    channels: List<Int>,
    brightness: IntArray,
    body: Rect
) {
    val bounds = normalizedRect(geometry.bounds, body)
    val vertical = geometry.direction == GlyphPreviewBarDirection.TOP_TO_BOTTOM ||
        geometry.direction == GlyphPreviewBarDirection.BOTTOM_TO_TOP
    val segmentLength = if (vertical) bounds.height / channels.size else bounds.width / channels.size
    val gap = segmentLength * SEGMENT_GAP_RATIO

    channels.forEachIndexed { index, channel ->
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
        val radius = min(segmentBounds.width, segmentBounds.height) * 0.22f
        drawRoundRect(
            color = glyphColor(brightness.getOrElse(channel) { 0 }),
            topLeft = segmentBounds.topLeft,
            size = segmentBounds.size,
            cornerRadius = CornerRadius(radius, radius)
        )
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

private fun segmentFraction(index: Int, count: Int, start: Boolean): Float {
    val segmentSize = 1f / count.coerceAtLeast(1)
    val gap = segmentSize * SEGMENT_GAP_RATIO
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
