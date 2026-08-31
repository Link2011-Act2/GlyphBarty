package jp.linkserver.glyphvisualizer.ui

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry

internal data class UiMeterModel(
    val segmentCount: Int,
    val activeSegments: Int,
    val segmentLevels: List<Float>,
    val usesGlyphBrightnessPreview: Boolean,
    val usesSymmetricCenterLayout: Boolean = false,
    val symmetricSeedCount: Int = 1,
    val centerDirectionReversed: Boolean = false
)

internal const val DEFAULT_SPECTRUM_METER_BANDS = 25

private fun spectrumMeterBandCount(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean
): Int {
    return if (GlyphPatternRegistry.isSpectrum(glyphMode)) {
        GlyphPatternRegistry.uiMeterSegmentCount(
            deviceProfile,
            glyphMode,
            recordingLightIncluded
        ).coerceAtLeast(1)
    } else {
        DEFAULT_SPECTRUM_METER_BANDS
    }
}

internal fun normalizedSpectrumMeterBands(
    source: FloatArray,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean
): FloatArray {
    val targetCount = spectrumMeterBandCount(glyphMode, deviceProfile, recordingLightIncluded)
    if (source.isEmpty()) return FloatArray(targetCount)
    if (source.size == targetCount) {
        return FloatArray(targetCount) { index -> source[index].coerceIn(0f, 1f) }
    }
    if (targetCount == 1) return floatArrayOf(source[0].coerceIn(0f, 1f))
    return FloatArray(targetCount) { index ->
        val position = index / (targetCount - 1f)
        sampleSpectrumBand(source, position)
    }
}

private fun sampleSpectrumBand(source: FloatArray, position: Float): Float {
    if (source.isEmpty()) return 0f
    if (source.size == 1) return source[0].coerceIn(0f, 1f)
    val scaled = position.coerceIn(0f, 1f) * (source.size - 1)
    val lo = scaled.toInt().coerceIn(0, source.lastIndex)
    val hi = (lo + 1).coerceIn(0, source.lastIndex)
    val fraction = scaled - lo
    return ((source[lo] * (1f - fraction)) + (source[hi] * fraction)).coerceIn(0f, 1f)
}

internal fun buildUiMeterModel(
    level: Float,
    meterSegments: Int,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean
): UiMeterModel {
    if (!glyphMeterPreviewEnabled) {
        val legacySegments = meterSegments.coerceIn(0, 16)
        return UiMeterModel(
            segmentCount = 16,
            activeSegments = legacySegments,
            segmentLevels = List(16) { index -> if (index < legacySegments) 1f else 0f },
            usesGlyphBrightnessPreview = false
        )
    }

    val segmentCount = GlyphPatternRegistry.uiMeterSegmentCount(
        deviceProfile,
        glyphMode,
        recordingLightIncluded
    ).coerceAtLeast(1)
    val patternKind = GlyphPatternRegistry.kindOf(glyphMode)
    val normalizedLevel = level.coerceIn(0f, 1f)
    val matrixOnOffOnly = false
    val usesGlyphBrightnessPreview = glyphMeterPreviewEnabled && !matrixOnOffOnly
    val usesPartialBrightnessPreview = usesGlyphBrightnessPreview && !binaryMode
    val symmetricSeedCount = if (segmentCount % 2 == 0) 2 else 1
    if (
        patternKind == jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind.CENTER ||
            patternKind == jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind.MATRIX_CIRCLE
    ) {
        return buildSymmetricMeterModel(
            normalizedLevel = normalizedLevel,
            segmentCount = segmentCount,
            seedCount = if (patternKind == jp.linkserver.glyphvisualizer.glyph.GlyphPatternKind.MATRIX_CIRCLE) {
                1
            } else {
                symmetricSeedCount
            },
            usesBrightnessPreview = usesGlyphBrightnessPreview,
            usesPartialBrightnessPreview = usesPartialBrightnessPreview,
            centerDirectionReversed = glyphMeterPreviewEnabled && reverseDirection
        )
    }

    val virtualLit = normalizedLevel * segmentCount
    val fullLit = virtualLit.toInt().coerceIn(0, segmentCount)
    val edgeFraction = (virtualLit - fullLit).coerceIn(0f, 1f)
    val segmentLevels = List(segmentCount) { index ->
        when {
            index < fullLit -> 1f
            index == fullLit && fullLit < segmentCount -> {
                if (usesPartialBrightnessPreview) edgeFraction else 0f
            }
            else -> 0f
        }
    }
    val activeSegments = if (usesGlyphBrightnessPreview) {
        segmentLevels.count { it > 0.001f }
    } else {
        fullLit
    }
    return UiMeterModel(
        segmentCount = segmentCount,
        activeSegments = activeSegments,
        segmentLevels = segmentLevels,
        usesGlyphBrightnessPreview = usesGlyphBrightnessPreview
    )
}

private fun buildSymmetricMeterModel(
    normalizedLevel: Float,
    segmentCount: Int,
    seedCount: Int,
    usesBrightnessPreview: Boolean,
    usesPartialBrightnessPreview: Boolean,
    centerDirectionReversed: Boolean = false
): UiMeterModel {
    val safeSeedCount = seedCount.coerceIn(1, segmentCount.coerceAtLeast(1))
    val logicalStepCount = (1 + ((segmentCount - safeSeedCount).coerceAtLeast(0) / 2)).coerceAtLeast(1)
    val virtualSteps = normalizedLevel.coerceIn(0f, 1f) * logicalStepCount
    val fullSteps = virtualSteps.toInt().coerceIn(0, logicalStepCount)
    val edgeFraction = (virtualSteps - fullSteps).coerceIn(0f, 1f)
    val segmentLevels = buildSymmetricSegmentLevels(
        segmentCount = segmentCount,
        seedCount = safeSeedCount,
        fullSteps = fullSteps,
        edgeFraction = edgeFraction,
        usesPartialBrightnessPreview = usesPartialBrightnessPreview,
        centerDirectionReversed = centerDirectionReversed
    )
    val activeSegments = if (usesBrightnessPreview) {
        segmentLevels.count { it > 0.001f }
    } else {
        when {
            fullSteps <= 0 -> 0
            else -> (safeSeedCount + ((fullSteps - 1) * 2)).coerceAtMost(segmentCount)
        }
    }
    return UiMeterModel(
        segmentCount = segmentCount,
        activeSegments = activeSegments,
        segmentLevels = segmentLevels,
        usesGlyphBrightnessPreview = usesBrightnessPreview,
        usesSymmetricCenterLayout = true,
        symmetricSeedCount = safeSeedCount,
        centerDirectionReversed = centerDirectionReversed
    )
}

private fun buildSymmetricSegmentLevels(
    segmentCount: Int,
    seedCount: Int,
    fullSteps: Int,
    edgeFraction: Float,
    usesPartialBrightnessPreview: Boolean,
    centerDirectionReversed: Boolean
): List<Float> {
    if (segmentCount <= 0) return emptyList()
    if (fullSteps <= 0 && (!usesPartialBrightnessPreview || edgeFraction <= 0.001f)) {
        return List(segmentCount) { 0f }
    }

    val levels = MutableList(segmentCount) { 0f }
    val slots = symmetricSegmentSlots(segmentCount, seedCount, centerDirectionReversed)

    for (slotIndex in 0 until fullSteps.coerceAtMost(slots.size)) {
        slots[slotIndex].forEach { segment -> levels[segment] = 1f }
    }

    if (usesPartialBrightnessPreview && fullSteps in 0 until slots.size && edgeFraction > 0.001f) {
        slots[fullSteps].forEach { segment -> levels[segment] = edgeFraction }
    }
    return levels
}

private fun symmetricSegmentSlots(
    segmentCount: Int,
    seedCount: Int,
    centerDirectionReversed: Boolean
): List<List<Int>> {
    if (segmentCount <= 0) return emptyList()
    val rightCenter = segmentCount / 2
    val leftCenter = if (seedCount == 2) (rightCenter - 1).coerceAtLeast(0) else rightCenter
    val slots = buildList {
        add(
            if (seedCount == 2) {
                listOf(leftCenter, rightCenter).distinct()
            } else {
                listOf(rightCenter)
            }
        )
        val maxPairDistance = maxOf(leftCenter, segmentCount - 1 - rightCenter)
        for (pairIndex in 1..maxPairDistance) {
            val slot = buildList {
                val left = leftCenter - pairIndex
                val right = rightCenter + pairIndex
                if (left >= 0) add(left)
                if (right < segmentCount) add(right)
            }
            if (slot.isNotEmpty()) add(slot)
        }
    }
    return if (centerDirectionReversed) slots.asReversed() else slots
}

private fun logicalStepCountFor(segmentCount: Int, seedCount: Int): Int {
    val safeSeedCount = seedCount.coerceIn(1, segmentCount.coerceAtLeast(1))
    return (1 + ((segmentCount - safeSeedCount).coerceAtLeast(0) / 2)).coerceAtLeast(1)
}

internal fun symmetricPeakDistanceSteps(segmentCount: Int, seedCount: Int): Float {
    val safeSeedCount = seedCount.coerceIn(1, segmentCount.coerceAtLeast(1))
    val pairCount = ((segmentCount - safeSeedCount).coerceAtLeast(0) / 2).coerceAtLeast(1)
    return if (safeSeedCount == 2) {
        // Even Center layouts first move from the midpoint between the two center segments
        // to the center pair itself, so they need one extra visual step to reach the ends.
        (pairCount + 1).toFloat()
    } else {
        pairCount.toFloat()
    }
}
