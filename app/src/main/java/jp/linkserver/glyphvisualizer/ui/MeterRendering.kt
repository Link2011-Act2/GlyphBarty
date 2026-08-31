package jp.linkserver.glyphvisualizer.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.ui.theme.NothingDotFontFamily
import kotlin.math.abs
import kotlin.math.roundToInt

import jp.linkserver.glyphvisualizer.*

private class NativeLevelMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var normalizedLevel = 0f
    private var activeColor = android.graphics.Color.BLACK
    private var inactiveColor = 0x14000000

    fun configureColors(activeColor: Int, inactiveColor: Int) {
        if (this.activeColor == activeColor && this.inactiveColor == inactiveColor) return
        this.activeColor = activeColor
        this.inactiveColor = inactiveColor
        invalidate()
    }

    fun setLiveFrame(frame: CaptureLiveFrame) {
        setMeterState(frame.level, activeColor, inactiveColor)
    }

    fun setMeterState(level: Float, activeColor: Int, inactiveColor: Int) {
        val nextLevel = level.coerceIn(0f, 1f)
        if (
            normalizedLevel == nextLevel &&
            this.activeColor == activeColor &&
            this.inactiveColor == inactiveColor
        ) {
            return
        }
        normalizedLevel = nextLevel
        this.activeColor = activeColor
        this.inactiveColor = inactiveColor
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val segmentCount = 16
        val gap = 4f * resources.displayMetrics.density
        val totalGap = gap * (segmentCount - 1)
        val segmentWidth = ((width - totalGap) / segmentCount.toFloat()).coerceAtLeast(1f)
        val radius = segmentWidth / 2f
        val activeSegments = (normalizedLevel * segmentCount).toInt().coerceIn(0, segmentCount)

        for (segment in 0 until segmentCount) {
            val left = segment * (segmentWidth + gap)
            rect.set(left, 0f, left + segmentWidth, height.toFloat())
            paint.color = if (segment < activeSegments) activeColor else inactiveColor
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }
}

private class NativeDetailedMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var targetLevel = 0f
    private var targetPeak = 0f
    private var displayedLevel = 0f
    private var displayedPeak = 0f
    private var meterModel: UiMeterModel? = null
    private var inactiveColor = 0x14000000
    private var activeColor = android.graphics.Color.BLACK
    private var peakColor = android.graphics.Color.GRAY
    private var sweepColor = 0x1A000000
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var deviceProfile = GlyphDeviceCatalog.currentProfile()
    private var binaryMode = false
    private var glyphMeterPreviewEnabled = false
    private var recordingLightIncluded = false
    private var reverseDirection = false
    private var compactMode = false
    private var lastFrame = CaptureLiveFrame()
    private var animationScheduled = false
    private var lastAnimationFrameNanos = 0L
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        animationScheduled = false
        stepAnimation(frameTimeNanos)
    }

    fun configure(
        glyphMode: String,
        deviceProfile: GlyphDeviceProfile,
        binaryMode: Boolean,
        glyphMeterPreviewEnabled: Boolean,
        recordingLightIncluded: Boolean,
        reverseDirection: Boolean,
        compactMode: Boolean,
        inactiveColor: Int,
        activeColor: Int,
        peakColor: Int,
        sweepColor: Int
    ) {
        val changed =
            this.glyphMode != glyphMode ||
                this.deviceProfile != deviceProfile ||
                this.binaryMode != binaryMode ||
                this.glyphMeterPreviewEnabled != glyphMeterPreviewEnabled ||
                this.recordingLightIncluded != recordingLightIncluded ||
                this.reverseDirection != reverseDirection ||
                this.compactMode != compactMode ||
                this.inactiveColor != inactiveColor ||
                this.activeColor != activeColor ||
                this.peakColor != peakColor ||
                this.sweepColor != sweepColor
        this.glyphMode = glyphMode
        this.deviceProfile = deviceProfile
        this.binaryMode = binaryMode
        this.glyphMeterPreviewEnabled = glyphMeterPreviewEnabled
        this.recordingLightIncluded = recordingLightIncluded
        this.reverseDirection = reverseDirection
        this.compactMode = compactMode
        this.inactiveColor = inactiveColor
        this.activeColor = activeColor
        this.peakColor = peakColor
        this.sweepColor = sweepColor
        if (changed) {
            setLiveFrame(lastFrame)
            invalidate()
        }
    }

    fun setLiveFrame(frame: CaptureLiveFrame) {
        lastFrame = frame
        setMeterState(
            level = frame.level,
            peak = frame.peak,
            meterModel = buildUiMeterModel(
                level = frame.level,
                meterSegments = frame.meterSegments,
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection
            ),
            inactiveColor = inactiveColor,
            activeColor = activeColor,
            peakColor = peakColor,
            sweepColor = sweepColor
        )
    }

    fun setMeterState(
        level: Float,
        peak: Float,
        meterModel: UiMeterModel,
        inactiveColor: Int,
        activeColor: Int,
        peakColor: Int,
        sweepColor: Int
    ) {
        val nextLevel = level.coerceIn(0f, 1f)
        val nextPeak = peak.coerceIn(0f, 1f)
        if (
            this.targetLevel == nextLevel &&
            this.targetPeak == nextPeak &&
            this.meterModel == meterModel &&
            this.inactiveColor == inactiveColor &&
            this.activeColor == activeColor &&
            this.peakColor == peakColor &&
            this.sweepColor == sweepColor
        ) {
            return
        }
        this.targetLevel = nextLevel
        this.targetPeak = nextPeak
        this.meterModel = meterModel
        this.inactiveColor = inactiveColor
        this.activeColor = activeColor
        this.peakColor = peakColor
        this.sweepColor = sweepColor
        scheduleAnimation()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val latestModel = meterModel ?: return
        val model = if (!latestModel.usesGlyphBrightnessPreview && !glyphMeterPreviewEnabled) {
            buildUiMeterModel(
                level = displayedLevel,
                meterSegments = (displayedLevel * 16f).roundToInt().coerceIn(0, 16),
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = false,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection
            )
        } else {
            latestModel
        }
        val segmentCount = model.segmentCount.coerceAtLeast(1)
        val centerIndex = segmentCount / 2
        val density = resources.displayMetrics.density
        val segmentGap = (if (compactMode) 4f else 10f) * density
        val totalGap = segmentGap * (segmentCount - 1)
        val segmentWidth = ((width - totalGap) / segmentCount.toFloat()).coerceAtLeast(1f)
        val maxHeight = height.toFloat()
        val radius = segmentWidth / 2f

        for (segment in 0 until segmentCount) {
            val left = segment * (segmentWidth + segmentGap)
            val segmentRatio = if (compactMode) {
                1f
            } else if (model.usesSymmetricCenterLayout) {
                val leftCenterIndex = if (model.symmetricSeedCount == 2) {
                    (centerIndex - 1).coerceAtLeast(0)
                } else {
                    centerIndex
                }
                val nearestCenterDistance = minOf(
                    kotlin.math.abs(segment - leftCenterIndex),
                    kotlin.math.abs(segment - centerIndex)
                ).toFloat()
                val maxDistance = maxOf(leftCenterIndex, segmentCount - 1 - centerIndex)
                    .toFloat()
                    .coerceAtLeast(1f)
                val outwardRatio = (nearestCenterDistance / maxDistance).coerceIn(0f, 1f)
                val mountainRatio = if (model.centerDirectionReversed) 1f - outwardRatio else outwardRatio
                (0.2f + mountainRatio * 0.8f).coerceIn(0.2f, 1f)
            } else {
                (segment + 1) / segmentCount.toFloat()
            }
            val barHeight = maxHeight * segmentRatio
            val top = maxHeight - barHeight
            val intensity = model.segmentLevels.getOrElse(segment) { 0f }.coerceIn(0f, 1f)

            rect.set(left, top, left + segmentWidth, maxHeight)
            paint.color = inactiveColor
            canvas.drawRoundRect(rect, radius, radius, paint)

            if (intensity > 0.001f) {
                val activeAlpha = if (compactMode) {
                    0.18f + intensity * 0.82f
                } else {
                    0.28f + intensity * 0.72f
                }
                paint.color = applyAlpha(activeColor, activeAlpha)
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }

        drawPeak(canvas, model, segmentCount, centerIndex, segmentWidth, segmentGap, maxHeight, density)

        if (!compactMode && !model.usesGlyphBrightnessPreview) {
            val sweepHeight = maxHeight * displayedLevel.coerceIn(0f, 1f)
            rect.set(0f, maxHeight - sweepHeight, width.toFloat(), maxHeight)
            paint.color = sweepColor
            canvas.drawRoundRect(rect, 40f, 40f, paint)
        }
    }

    override fun onDetachedFromWindow() {
        if (animationScheduled) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            animationScheduled = false
        }
        super.onDetachedFromWindow()
    }

    private fun scheduleAnimation() {
        if (!isAttachedToWindow) {
            displayedLevel = targetLevel
            displayedPeak = targetPeak
            invalidate()
            return
        }
        if (!animationScheduled) {
            animationScheduled = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun stepAnimation(frameTimeNanos: Long) {
        val deltaSeconds = if (lastAnimationFrameNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastAnimationFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        }
        lastAnimationFrameNanos = frameTimeNanos
        displayedLevel = approach(displayedLevel, targetLevel, deltaSeconds, speed = 18f)
        displayedPeak = approach(displayedPeak, targetPeak, deltaSeconds, speed = 8f)
        invalidate()
        if (abs(displayedLevel - targetLevel) > 0.002f || abs(displayedPeak - targetPeak) > 0.002f) {
            scheduleAnimation()
        } else {
            displayedLevel = targetLevel
            displayedPeak = targetPeak
            lastAnimationFrameNanos = 0L
            invalidate()
        }
    }

    private fun approach(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
        val factor = (1f - kotlin.math.exp(-speed * deltaSeconds)).coerceIn(0f, 1f)
        return current + (target - current) * factor
    }

    private fun drawPeak(
        canvas: android.graphics.Canvas,
        model: UiMeterModel,
        segmentCount: Int,
        centerIndex: Int,
        segmentWidth: Float,
        segmentGap: Float,
        maxHeight: Float,
        density: Float
    ) {
        paint.color = peakColor
        paint.strokeWidth = (if (compactMode) 3f else 6f) * density
        paint.strokeCap = Paint.Cap.ROUND

        if (model.usesSymmetricCenterLayout) {
            val centerPeakX = centerIndex * (segmentWidth + segmentGap) + segmentWidth / 2f
            val leftCenterPeakX = if (model.symmetricSeedCount == 2) {
                (centerIndex - 1).coerceAtLeast(0) * (segmentWidth + segmentGap) + segmentWidth / 2f
            } else {
                centerPeakX
            }
            val betweenCentersPeakX = (leftCenterPeakX + centerPeakX) / 2f
            val maxPairDistance = symmetricPeakDistanceSteps(segmentCount, model.symmetricSeedCount)
            val peakHalfWidth = if (model.centerDirectionReversed) segmentWidth / 2f else 0f
            val peakProgress = if (model.centerDirectionReversed) 1f - displayedPeak else displayedPeak
            val peakDistance = peakProgress.coerceIn(0f, 1f) * maxPairDistance
            val leftPeakX: Float
            val rightPeakX: Float

            if (peakDistance <= 0.001f) {
                leftPeakX = if (model.symmetricSeedCount == 2) betweenCentersPeakX else centerPeakX
                rightPeakX = leftPeakX
            } else if (model.symmetricSeedCount == 2) {
                val unitSpan = segmentWidth + segmentGap
                if (peakDistance <= 1f) {
                    val halfGap = (centerPeakX - leftCenterPeakX) / 2f
                    leftPeakX = betweenCentersPeakX - halfGap * peakDistance
                    rightPeakX = betweenCentersPeakX + halfGap * peakDistance
                } else {
                    val extraTravel = unitSpan * (peakDistance - 1f)
                    leftPeakX = leftCenterPeakX - extraTravel
                    rightPeakX = centerPeakX + extraTravel
                }
            } else {
                val travelPerSide = (segmentWidth + segmentGap) * peakDistance
                leftPeakX = centerPeakX - travelPerSide
                rightPeakX = centerPeakX + travelPerSide
            }

            canvas.drawLine(
                (leftPeakX - peakHalfWidth).coerceIn(0f, width.toFloat()),
                0f,
                (leftPeakX - peakHalfWidth).coerceIn(0f, width.toFloat()),
                maxHeight,
                paint
            )
            canvas.drawLine(
                (rightPeakX + peakHalfWidth).coerceIn(0f, width.toFloat()),
                0f,
                (rightPeakX + peakHalfWidth).coerceIn(0f, width.toFloat()),
                maxHeight,
                paint
            )
        } else {
            val peakX = displayedPeak.coerceIn(0f, 1f) * (segmentCount - 1).coerceAtLeast(0) * (segmentWidth + segmentGap) +
                segmentWidth / 2f
            canvas.drawLine(peakX, 0f, peakX, maxHeight, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val safeAlpha = (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (safeAlpha shl 24)
    }
}

private class NativeSpectrumMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var deviceProfile = GlyphDeviceCatalog.currentProfile()
    private var recordingLightIncluded = false
    private var inactiveColor = 0x14000000
    private var activeColor = android.graphics.Color.BLACK
    private var targetLevel = 0f
    private var displayedLevel = 0f
    private var targetBands = FloatArray(DEFAULT_SPECTRUM_METER_BANDS)
    private var displayedBands = FloatArray(DEFAULT_SPECTRUM_METER_BANDS)
    private var animationScheduled = false
    private var lastAnimationFrameNanos = 0L
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        animationScheduled = false
        stepAnimation(frameTimeNanos)
    }

    fun configure(
        glyphMode: String,
        deviceProfile: GlyphDeviceProfile,
        recordingLightIncluded: Boolean,
        inactiveColor: Int,
        activeColor: Int
    ) {
        val changed =
            this.glyphMode != glyphMode ||
                this.deviceProfile != deviceProfile ||
                this.recordingLightIncluded != recordingLightIncluded ||
                this.inactiveColor != inactiveColor ||
                this.activeColor != activeColor
        this.glyphMode = glyphMode
        this.deviceProfile = deviceProfile
        this.recordingLightIncluded = recordingLightIncluded
        this.inactiveColor = inactiveColor
        this.activeColor = activeColor
        if (changed) {
            targetBands = normalizedSpectrumMeterBands(
                targetBands,
                glyphMode,
                deviceProfile,
                recordingLightIncluded
            )
            displayedBands = normalizedSpectrumMeterBands(
                displayedBands,
                glyphMode,
                deviceProfile,
                recordingLightIncluded
            )
            invalidate()
        }
    }

    fun setLiveFrame(frame: CaptureLiveFrame) {
        val nextBands = normalizedSpectrumMeterBands(
            frame.spectrumBands,
            glyphMode,
            deviceProfile,
            recordingLightIncluded
        )
        targetLevel = frame.level.coerceIn(0f, 1f)
        if (!targetBands.contentEquals(nextBands)) {
            targetBands = nextBands
        }
        if (displayedBands.size != targetBands.size) {
            displayedBands = targetBands.copyOf()
        }
        scheduleAnimation()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val bandCount = displayedBands.size.coerceAtLeast(1)
        val density = resources.displayMetrics.density
        val gap = 3f * density
        val totalGap = gap * (bandCount - 1)
        val barWidth = ((width - totalGap) / bandCount.toFloat()).coerceAtLeast(1f)
        val maxHeight = height.toFloat()
        val radius = barWidth / 2f
        for (i in 0 until bandCount) {
            val left = i * (barWidth + gap)
            rect.set(left, 0f, left + barWidth, maxHeight)
            paint.color = inactiveColor
            canvas.drawRoundRect(rect, radius, radius, paint)

            val value = (displayedBands[i].coerceIn(0f, 1f) * displayedLevel.coerceIn(0f, 1f)).coerceIn(0f, 1f)
            if (value <= 0.001f) continue
            val barHeight = maxHeight * value
            rect.set(left, maxHeight - barHeight, left + barWidth, maxHeight)
            paint.color = activeColor
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        if (animationScheduled) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            animationScheduled = false
        }
        super.onDetachedFromWindow()
    }

    private fun scheduleAnimation() {
        if (!isAttachedToWindow) {
            displayedLevel = targetLevel
            displayedBands = targetBands.copyOf()
            invalidate()
            return
        }
        if (!animationScheduled) {
            animationScheduled = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun stepAnimation(frameTimeNanos: Long) {
        val deltaSeconds = if (lastAnimationFrameNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastAnimationFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        }
        lastAnimationFrameNanos = frameTimeNanos
        val factor = (1f - kotlin.math.exp(-18f * deltaSeconds)).coerceIn(0f, 1f)
        displayedLevel += (targetLevel - displayedLevel) * factor
        if (displayedBands.size != targetBands.size) {
            displayedBands = targetBands.copyOf()
        } else {
            for (i in displayedBands.indices) {
                displayedBands[i] += (targetBands[i] - displayedBands[i]) * factor
            }
        }
        invalidate()
        val levelSettled = abs(displayedLevel - targetLevel) <= 0.002f
        val bandsSettled = displayedBands.indices.all { abs(displayedBands[it] - targetBands[it]) <= 0.002f }
        if (levelSettled && bandsSettled) {
            displayedLevel = targetLevel
            displayedBands = targetBands.copyOf()
            lastAnimationFrameNanos = 0L
            invalidate()
        } else {
            scheduleAnimation()
        }
    }
}

private class NativeMeterStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val levelColumn = LinearLayout(context)
    private val segmentsColumn = LinearLayout(context)
    private val levelLabelView = TextView(context)
    private val levelValueView = TextView(context)
    private val segmentsLabelView = TextView(context)
    private val segmentsValueView = TextView(context)
    private val spacerView = View(context)
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var deviceProfile = GlyphDeviceCatalog.currentProfile()
    private var binaryMode = false
    private var glyphMeterPreviewEnabled = false
    private var recordingLightIncluded = false
    private var reverseDirection = false
    private var lightweightMode = false
    private var spectrumMode = false
    private var compact = false
    private var valueColor = android.graphics.Color.BLACK
    private var labelColor = android.graphics.Color.DKGRAY
    private var useNothingFont = false
    private var lastFrame = CaptureLiveFrame()

    init {
        gravity = Gravity.CENTER
        setupStatColumn(levelColumn, levelLabelView, levelValueView)
        setupStatColumn(segmentsColumn, segmentsLabelView, segmentsValueView)
    }

    fun configure(
        levelLabel: String,
        segmentsLabel: String,
        glyphMode: String,
        deviceProfile: GlyphDeviceProfile,
        binaryMode: Boolean,
        glyphMeterPreviewEnabled: Boolean,
        recordingLightIncluded: Boolean,
        reverseDirection: Boolean,
        lightweightMode: Boolean,
        spectrumMode: Boolean = false,
        valueColor: Int,
        labelColor: Int,
        useNothingFont: Boolean,
        compact: Boolean
    ) {
        levelLabelView.text = levelLabel
        segmentsLabelView.text = segmentsLabel
        this.glyphMode = glyphMode
        this.deviceProfile = deviceProfile
        this.binaryMode = binaryMode
        this.glyphMeterPreviewEnabled = glyphMeterPreviewEnabled
        this.recordingLightIncluded = recordingLightIncluded
        this.reverseDirection = reverseDirection
        this.lightweightMode = lightweightMode
        this.spectrumMode = spectrumMode
        this.valueColor = valueColor
        this.labelColor = labelColor
        this.useNothingFont = useNothingFont
        if (this.compact != compact || childCount == 0) {
            this.compact = compact
            rebuildLayout()
        } else {
            this.compact = compact
        }
        setTextAppearance()
        setLiveFrame(lastFrame)
    }

    fun setLiveFrame(frame: CaptureLiveFrame) {
        lastFrame = frame
        val activeSegments: Int
        val segmentCount: Int
        if (spectrumMode) {
            val bands = normalizedSpectrumMeterBands(
                frame.spectrumBands,
                glyphMode,
                deviceProfile,
                recordingLightIncluded
            )
            segmentCount = bands.size
            activeSegments = bands.count { it * frame.level.coerceIn(0f, 1f) > 0.001f }
        } else if (lightweightMode) {
            segmentCount = 16
            activeSegments = (frame.level.coerceIn(0f, 1f) * segmentCount).toInt().coerceIn(0, segmentCount)
        } else {
            val model = buildUiMeterModel(
                level = frame.level,
                meterSegments = frame.meterSegments,
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection
            )
            activeSegments = model.activeSegments
            segmentCount = model.segmentCount
        }
        levelValueView.text = "${(frame.level.coerceIn(0f, 1f) * 100f).toInt()}%"
        segmentsValueView.text = "$activeSegments / $segmentCount"
    }

    private fun setupStatColumn(column: LinearLayout, labelView: TextView, valueView: TextView) {
        column.orientation = VERTICAL
        column.gravity = Gravity.CENTER
        labelView.includeFontPadding = false
        valueView.includeFontPadding = false
        column.addView(labelView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        column.addView(valueView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    private fun rebuildLayout() {
        removeAllViews()
        orientation = if (compact) VERTICAL else HORIZONTAL
        gravity = if (compact) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.CENTER_VERTICAL
        levelColumn.gravity = if (compact) Gravity.END else Gravity.START
        segmentsColumn.gravity = if (compact) Gravity.END else Gravity.CENTER
        val firstParams = if (compact) {
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        } else {
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        val secondParams = if (compact) {
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2f * resources.displayMetrics.density).toInt()
            }
        } else {
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        addView(levelColumn, firstParams)
        if (!compact) {
            addView(spacerView, LayoutParams(0, 1, 1f))
        }
        addView(segmentsColumn, secondParams)
    }

    private fun setTextAppearance() {
        val labelSize = if (compact) 9f else 11f
        val valueSize = if (compact) 16f else 20f
        val valueTypeface = if (useNothingFont) {
            ResourcesCompat.getFont(context, R.font.ndot_55) ?: android.graphics.Typeface.DEFAULT
        } else {
            android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        listOf(levelLabelView, segmentsLabelView).forEach {
            it.setTextColor(labelColor)
            it.textSize = labelSize
            it.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            it.gravity = if (compact) Gravity.END else if (it === levelLabelView) Gravity.START else Gravity.END
        }
        listOf(levelValueView, segmentsValueView).forEach {
            it.setTextColor(valueColor)
            it.textSize = valueSize
            it.typeface = valueTypeface
            it.gravity = if (compact) Gravity.END else if (it === levelValueView) Gravity.START else Gravity.END
        }
    }
}

@Composable
internal fun MeterCanvas(
    level: Float,
    peak: Float,
    meterModel: UiMeterModel,
    nothingStyleEnabled: Boolean
) {
    val meterVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val meterOnSurfaceColor = MaterialTheme.colorScheme.onSurface
    val meterPrimaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val peakColor = MaterialTheme.colorScheme.tertiary
    val sweepColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "meter-level"
    )
    val animatedPeak by animateFloatAsState(
        targetValue = peak,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "meter-peak"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segmentCount = meterModel.segmentCount.coerceAtLeast(1)
                val segmentGap = 10.dp.toPx()
                val totalGap = segmentGap * (segmentCount - 1)
                val segmentWidth = (size.width - totalGap) / segmentCount.toFloat()
                val maxHeight = size.height
                val centerIndex = segmentCount / 2

                for (segment in 0 until segmentCount) {
                    val left = segment * (segmentWidth + segmentGap)
                    val segmentRatio = if (meterModel.usesSymmetricCenterLayout) {
                        val leftCenterIndex = if (meterModel.symmetricSeedCount == 2) {
                            (centerIndex - 1).coerceAtLeast(0)
                        } else {
                            centerIndex
                        }
                        val nearestCenterDistance = minOf(
                            kotlin.math.abs(segment - leftCenterIndex),
                            kotlin.math.abs(segment - centerIndex)
                        ).toFloat()
                        val maxDistance = maxOf(leftCenterIndex, segmentCount - 1 - centerIndex).toFloat().coerceAtLeast(1f)
                        val outwardRatio = (nearestCenterDistance / maxDistance).coerceIn(0f, 1f)
                        val mountainRatio = if (meterModel.centerDirectionReversed) {
                            1f - outwardRatio
                        } else {
                            outwardRatio
                        }
                        (0.2f + mountainRatio * 0.8f).coerceIn(0.2f, 1f)
                    } else {
                        (segment + 1) / segmentCount.toFloat()
                    }
                    val barHeight = maxHeight * segmentRatio
                    val top = maxHeight - barHeight
                    val intensity = meterModel.segmentLevels.getOrElse(segment) { 0f }.coerceIn(0f, 1f)

                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(left, top),
                        size = Size(segmentWidth, barHeight),
                        cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                    )

                    if (intensity > 0.001f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    meterVariantColor.copy(alpha = 0.32f + (0.40f * intensity)),
                                    meterOnSurfaceColor.copy(alpha = 0.44f + (0.42f * intensity)),
                                    meterPrimaryColor.copy(alpha = 0.22f + (0.78f * intensity))
                                )
                            ),
                            topLeft = Offset(left, top),
                            size = Size(segmentWidth, barHeight),
                            cornerRadius = CornerRadius(segmentWidth / 2f, segmentWidth / 2f)
                        )
                    }
                }

                if (meterModel.usesSymmetricCenterLayout) {
                    val centerPeakX = centerIndex * (segmentWidth + segmentGap) + (segmentWidth / 2f)
                    val leftCenterPeakX = if (meterModel.symmetricSeedCount == 2) {
                        (centerIndex - 1).coerceAtLeast(0) * (segmentWidth + segmentGap) + (segmentWidth / 2f)
                    } else {
                        centerPeakX
                    }
                    val betweenCentersPeakX = (leftCenterPeakX + centerPeakX) / 2f
                    val maxPairDistance = symmetricPeakDistanceSteps(segmentCount, meterModel.symmetricSeedCount)
                    val peakHalfWidth = if (meterModel.centerDirectionReversed) segmentWidth / 2f else 0f
                    val peakProgress = if (meterModel.centerDirectionReversed) {
                        1f - animatedPeak.coerceIn(0f, 1f)
                    } else {
                        animatedPeak.coerceIn(0f, 1f)
                    }
                    val peakDistance = peakProgress * maxPairDistance
                    if (peakDistance <= 0.001f) {
                        if (meterModel.centerDirectionReversed) {
                            val leftRestingPeakX = if (meterModel.symmetricSeedCount == 2) leftCenterPeakX else centerPeakX
                            val rightRestingPeakX = centerPeakX
                            drawLine(
                                color = peakColor,
                                start = Offset((leftRestingPeakX - peakHalfWidth).coerceIn(0f, size.width), 0f),
                                end = Offset((leftRestingPeakX - peakHalfWidth).coerceIn(0f, size.width), size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = peakColor,
                                start = Offset((rightRestingPeakX + peakHalfWidth).coerceIn(0f, size.width), 0f),
                                end = Offset((rightRestingPeakX + peakHalfWidth).coerceIn(0f, size.width), size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        } else if (meterModel.symmetricSeedCount == 2 && meterModel.activeSegments > 0) {
                            drawLine(
                                color = peakColor,
                                start = Offset(betweenCentersPeakX, 0f),
                                end = Offset(betweenCentersPeakX, size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        } else {
                            val restingPeakX = if (meterModel.symmetricSeedCount == 2) betweenCentersPeakX else centerPeakX
                            drawLine(
                                color = peakColor,
                                start = Offset(restingPeakX, 0f),
                                end = Offset(restingPeakX, size.height),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    } else {
                        val leftPeakX: Float
                        val rightPeakX: Float
                        if (meterModel.symmetricSeedCount == 2) {
                            val unitSpan = segmentWidth + segmentGap
                            if (peakDistance <= 1f) {
                                val halfGap = (centerPeakX - leftCenterPeakX) / 2f
                                leftPeakX = betweenCentersPeakX - (halfGap * peakDistance)
                                rightPeakX = betweenCentersPeakX + (halfGap * peakDistance)
                            } else {
                                val extraTravel = unitSpan * (peakDistance - 1f)
                                leftPeakX = leftCenterPeakX - extraTravel
                                rightPeakX = centerPeakX + extraTravel
                            }
                        } else {
                            val travelPerSide = (segmentWidth + segmentGap) * peakDistance
                            leftPeakX = centerPeakX - travelPerSide
                            rightPeakX = centerPeakX + travelPerSide
                        }
                        val clampedLeftPeakX = (leftPeakX - peakHalfWidth).coerceIn(0f, size.width)
                        val clampedRightPeakX = (rightPeakX + peakHalfWidth).coerceIn(0f, size.width)
                        drawLine(
                            color = peakColor,
                            start = Offset(clampedLeftPeakX, 0f),
                            end = Offset(clampedLeftPeakX, size.height),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = peakColor,
                            start = Offset(clampedRightPeakX, 0f),
                            end = Offset(clampedRightPeakX, size.height),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    val peakX = ((animatedPeak.coerceIn(0f, 1f) * (segmentCount - 1).coerceAtLeast(0).toFloat()) * (segmentWidth + segmentGap)) +
                        (segmentWidth / 2f)
                    drawLine(
                        color = peakColor,
                        start = Offset(peakX, 0f),
                        end = Offset(peakX, size.height),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                if (!meterModel.usesGlyphBrightnessPreview) {
                    val sweepHeight = maxHeight * animatedLevel.coerceIn(0f, 1f)
                    drawRoundRect(
                        color = sweepColor,
                        topLeft = Offset(0f, maxHeight - sweepHeight),
                        size = Size(size.width, sweepHeight),
                        cornerRadius = CornerRadius(40f, 40f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (animatedLevel * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(
                    R.string.meter_segments_value,
                    meterModel.activeSegments,
                    meterModel.segmentCount
                ),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
internal fun DirectNativeDetailedMeterCanvas(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            DirectNativeDetailedMeterBar(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectNativeMeterStats(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection,
                lightweightMode = false,
                spectrumMode = false,
                nothingStyleEnabled = nothingStyleEnabled,
                compact = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun SpectrumMeterCanvas(
    level: Float,
    spectrumBands: FloatArray,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            SpectrumMeterBar(
                level = level,
                spectrumBands = spectrumBands,
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                recordingLightIncluded = recordingLightIncluded,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bands = normalizedSpectrumMeterBands(
                spectrumBands,
                glyphMode,
                deviceProfile,
                recordingLightIncluded
            )
            val activeBands = bands.count { it * level.coerceIn(0f, 1f) > 0.001f }
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (level.coerceIn(0f, 1f) * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(R.string.meter_segments_value, activeBands, bands.size),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
internal fun SpectrumMeterBar(
    level: Float,
    spectrumBands: FloatArray,
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val activeColor = MaterialTheme.colorScheme.primary
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "spectrum-meter-level"
    )
    val bands = normalizedSpectrumMeterBands(
        spectrumBands,
        glyphMode,
        deviceProfile,
        recordingLightIncluded
    )
    Canvas(modifier = modifier) {
        val bandCount = bands.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val totalGap = gap * (bandCount - 1)
        val barWidth = ((size.width - totalGap) / bandCount.toFloat()).coerceAtLeast(1f)
        val maxHeight = size.height
        for (i in 0 until bandCount) {
            val left = i * (barWidth + gap)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, maxHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            val value = (bands[i].coerceIn(0f, 1f) * animatedLevel).coerceIn(0f, 1f)
            if (value <= 0.001f) continue
            val barHeight = maxHeight * value
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, maxHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
internal fun DirectNativeSpectrumMeterCanvas(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            DirectNativeSpectrumMeterBar(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                recordingLightIncluded = recordingLightIncluded,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectNativeMeterStats(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = false,
                glyphMeterPreviewEnabled = true,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = false,
                lightweightMode = false,
                nothingStyleEnabled = nothingStyleEnabled,
                compact = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun DirectNativeSpectrumMeterBar(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    recordingLightIncluded: Boolean,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).toArgb()
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current
    val meterView = remember { NativeSpectrumMeterView(context) }
    val listener = remember(meterView) {
        { frame: CaptureLiveFrame -> meterView.setLiveFrame(frame) }
    }

    DisposableEffect(meterView) {
        CaptureUiStore.registerDirectMeterFrameListener(listener)
        onDispose {
            CaptureUiStore.unregisterDirectMeterFrameListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { meterView },
        update = {
            it.configure(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                recordingLightIncluded = recordingLightIncluded,
                inactiveColor = inactiveColor,
                activeColor = activeColor
            )
        }
    )
}

@Composable
internal fun DirectNativeDetailedMeterBar(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).toArgb()
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val peakColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val sweepColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f).toArgb()
    val context = LocalContext.current
    val meterView = remember { NativeDetailedMeterView(context) }
    val listener = remember(meterView) {
        { frame: CaptureLiveFrame -> meterView.setLiveFrame(frame) }
    }

    DisposableEffect(meterView) {
        CaptureUiStore.registerDirectMeterFrameListener(listener)
        onDispose {
            CaptureUiStore.unregisterDirectMeterFrameListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { meterView },
        update = {
            it.configure(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection,
                compactMode = compact,
                inactiveColor = inactiveColor,
                activeColor = activeColor,
                peakColor = peakColor,
                sweepColor = sweepColor
            )
        }
    )
}

@Composable
internal fun DirectNativeMeterStats(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    lightweightMode: Boolean,
    spectrumMode: Boolean = false,
    nothingStyleEnabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val valueColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val levelLabel = stringResource(R.string.meter_label_level)
    val segmentsLabel = stringResource(R.string.meter_label_segments)
    val statsView = remember { NativeMeterStatsView(context) }
    val listener = remember(statsView) {
        { frame: CaptureLiveFrame -> statsView.setLiveFrame(frame) }
    }

    DisposableEffect(statsView) {
        CaptureUiStore.registerDirectMeterFrameListener(listener)
        onDispose {
            CaptureUiStore.unregisterDirectMeterFrameListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { statsView },
        update = {
            it.configure(
                levelLabel = levelLabel,
                segmentsLabel = segmentsLabel,
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection,
                lightweightMode = lightweightMode,
                spectrumMode = spectrumMode,
                valueColor = valueColor,
                labelColor = labelColor,
                useNothingFont = nothingStyleEnabled,
                compact = compact
            )
        }
    )
}

@Composable
private fun NativeDetailedMeterCanvas(
    level: Float,
    peak: Float,
    meterModel: UiMeterModel,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(18.dp)
        ) {
            NativeDetailedMeterBar(
                level = level,
                peak = peak,
                meterModel = meterModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (level * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(
                    R.string.meter_segments_value,
                    meterModel.activeSegments,
                    meterModel.segmentCount
                ),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
private fun NativeDetailedMeterBar(
    level: Float,
    peak: Float,
    meterModel: UiMeterModel,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).toArgb()
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val peakColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val sweepColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f).toArgb()

    AndroidView(
        modifier = modifier,
        factory = { NativeDetailedMeterView(it) },
        update = { meterView ->
            meterView.setMeterState(
                level = level,
                peak = peak,
                meterModel = meterModel,
                inactiveColor = inactiveColor,
                activeColor = activeColor,
                peakColor = peakColor,
                sweepColor = sweepColor
            )
        }
    )
}

@Composable
internal fun DirectNativeMeterCanvas(
    glyphMode: String,
    deviceProfile: GlyphDeviceProfile,
    binaryMode: Boolean,
    glyphMeterPreviewEnabled: Boolean,
    recordingLightIncluded: Boolean,
    reverseDirection: Boolean,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            DirectNativeMeterBar(
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectNativeMeterStats(
                glyphMode = glyphMode,
                deviceProfile = deviceProfile,
                binaryMode = binaryMode,
                glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
                recordingLightIncluded = recordingLightIncluded,
                reverseDirection = reverseDirection,
                lightweightMode = true,
                nothingStyleEnabled = nothingStyleEnabled,
                compact = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun DirectNativeMeterBar(
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).toArgb()
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current
    val meterView = remember { NativeLevelMeterView(context) }
    val listener = remember(meterView) {
        { frame: CaptureLiveFrame -> meterView.setLiveFrame(frame) }
    }

    DisposableEffect(meterView) {
        CaptureUiStore.registerDirectMeterFrameListener(listener)
        onDispose {
            CaptureUiStore.unregisterDirectMeterFrameListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { meterView },
        update = {
            it.configureColors(
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    )
}

@Composable
private fun NativeMeterCanvas(
    level: Float,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            NativeMeterBar(
                level = level,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (level * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(
                    R.string.meter_segments_value,
                    (level.coerceIn(0f, 1f) * 16f).toInt().coerceIn(0, 16),
                    16
                ),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
private fun NativeMeterBar(
    level: Float,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f).toArgb()
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { NativeLevelMeterView(it) },
        update = { meterView ->
            meterView.setMeterState(
                level = level,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    )
}

@Composable
internal fun LightweightMeterCanvas(
    level: Float,
    nothingStyleEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(meterBackgroundBrush(nothingStyleEnabled))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            LightweightMeterBar(
                level = level,
                nothingStyleEnabled = nothingStyleEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeterStat(
                label = stringResource(R.string.meter_label_level),
                value = stringResource(R.string.percent_value, (level * 100).toInt()),
                nothingStyleEnabled = nothingStyleEnabled
            )
            MeterStat(
                label = stringResource(R.string.meter_label_segments),
                value = stringResource(
                    R.string.meter_segments_value,
                    (level.coerceIn(0f, 1f) * 16f).toInt().coerceIn(0, 16),
                    16
                ),
                nothingStyleEnabled = nothingStyleEnabled
            )
        }
    }
}

@Composable
internal fun LightweightMeterBar(
    level: Float,
    nothingStyleEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val activeColor = MaterialTheme.colorScheme.primary
    val activeSegments = (level.coerceIn(0f, 1f) * 16f).toInt().coerceIn(0, 16)

    Canvas(modifier = modifier) {
        val segmentCount = 16
        val gap = 4.dp.toPx()
        val totalGap = gap * (segmentCount - 1)
        val widthPerSegment = ((size.width - totalGap) / segmentCount.toFloat()).coerceAtLeast(1f)
        val radius = CornerRadius(widthPerSegment / 2f, widthPerSegment / 2f)

        for (segment in 0 until segmentCount) {
            val left = segment * (widthPerSegment + gap)
            drawRoundRect(
                color = if (segment < activeSegments) activeColor else inactiveColor,
                topLeft = Offset(left, 0f),
                size = Size(widthPerSegment, size.height),
                cornerRadius = radius
            )
        }
    }
}

@Composable
private fun meterBackgroundBrush(nothingStyleEnabled: Boolean): Brush {
    return if (nothingStyleEnabled) {
        val nothingMeterSurface = if (isSystemInDarkTheme()) {
            Color(0xFF2A2A2A)
        } else {
            Color(0xFFF2F2F2)
        }
        Brush.linearGradient(
            listOf(
                nothingMeterSurface,
                nothingMeterSurface
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
internal fun MeterStat(
    label: String,
    value: String,
    nothingStyleEnabled: Boolean,
    compact: Boolean = false
) {
    val valueStyle = if (nothingStyleEnabled) {
        (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).copy(
            fontFamily = NothingDotFontFamily,
            fontWeight = FontWeight.Normal
        )
    } else {
        if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 9.sp else 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
