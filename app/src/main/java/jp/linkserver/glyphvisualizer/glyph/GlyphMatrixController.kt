package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class GlyphMatrixController(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit,
    private val initialPhone4aProEmulationEnabled: Boolean = false,
    private val ownerHandler: Handler? = null
) : GlyphOutputController {

    companion object {
        private const val TAG = "GlyphMatrixController"
        private const val COLOR_ON = 255
        private const val COLOR_OFF = 0
        private const val SILENCE_BLACKOUT_MS = 120L
        private const val SILENCE_RELEASE_MS = 450L
        private const val SILENCE_ACTIVITY_THRESHOLD = 0.003f
        private const val RIPPLE_SILENCE_DRAIN_MS = 900L
        private const val SILENCE_DRAIN_BRIGHTNESS_THRESHOLD = 0.02f
        private const val DEFAULT_AUTO_SCALE_WINDOW_MS = 30_000f
        private const val ALL_BRIGHTNESS_OFF_THRESHOLD = 0.06f
        private const val ALL_BRIGHTNESS_MIN_LIGHT = 240
        private const val ALL_BRIGHTNESS_MAX_LIGHT = 4095
        private const val ALL_BRIGHTNESS_RESPONSE_GAMMA = 1.8f
        private const val ALL_BRIGHTNESS_MIN_LIGHT_MATRIX = 60
        private const val ALL_BRIGHTNESS_MAX_LIGHT_MATRIX = 255
        private const val FRAME_INTERVAL_SMOOTH_MS = 12L // allow higher effective fps when callbacks are uneven
        private const val FRAME_INTERVAL_REDUCED_MS = 33L // ~30fps
        private const val SPECTROGRAM_REFERENCE_MATRIX_LENGTH = 25
        private const val SPECTROGRAM_REFERENCE_SHIFT_INTERVAL_MS = FRAME_INTERVAL_REDUCED_MS
        private const val SIGNATURE_EDGE_BRIGHTNESS_STEP = 32
        private const val SIGNATURE_ALL_BRIGHTNESS_STEP = 8
        private const val RAIN_TAIL_LENGTH = 4
        private const val MATRIX_WAVE_FIELD_BRIGHTNESS_BOOST = 1.55f
        private const val MATRIX_RIPPLE_BRIGHTNESS_BOOST = 1.7f
        private const val MATRIX_RAIN_BRIGHTNESS_BOOST = 1.45f
        private const val MATRIX_SPECTROGRAM_BRIGHTNESS_BOOST = 1.3f
        private const val MATRIX_SPECTRUM_ANALYZER_BRIGHTNESS_BOOST = 1.35f
        private const val MATRIX_PULSE_GRID_BRIGHTNESS_BOOST = 1.45f
    }

    private val glyphMatrixManager = GlyphMatrixManager.getInstance(context.applicationContext)
    private var isBound = false
    private var isSessionOpen = false
    private var reverseDirection = false
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var binaryMode = false
    private var experimentalPerformanceOptimizationsEnabled = false
    private var matrixSmoothMotionEnabled = false
    private var outputGamma = ALL_BRIGHTNESS_RESPONSE_GAMMA
    private var levelAutoScaleEnabled = false
    private var spectrumAutoScaleEnabled = false
    private var allBrightnessAutoScaleEnabled = false
    private var autoScaleWindowMs = DEFAULT_AUTO_SCALE_WINDOW_MS
    private var autoScaleOffset = 0f
    private var levelMin = 0f
    private var levelMax = 1f
    private var lastLevelUpdateMs = 0L
    private var lastPreviewLevel = 0f
    private var allBrightnessMin = 0f
    private var allBrightnessMax = 1f
    private var lastAllBrightnessUpdateMs = 0L
    private var physicalMatrixLength = 0
    private var matrixLength = 0
    private var pendingLevel = -1f
    private var lastRenderAt = 0L
    private var lastLitRows = -1
    private var lastMatrixBrightness = -1
    private var failureCount = 0
    private var frameBuffer = IntArray(0)
    private var lowEnergy = 0f
    private var highEnergy = 0f
    private var leftLevel = 0f
    private var rightLevel = 0f
    private var spectrumBands = FloatArray(0)
    private var waveformSamples = FloatArray(0)
    private var leftWaveformSamples = FloatArray(0)
    private var rightWaveformSamples = FloatArray(0)
    private var smoothedSpectrumBands = FloatArray(0)
    private var spectrumBandMins = FloatArray(0)
    private var spectrumBandMaxs = FloatArray(0)
    private var lastSpectrumUpdateMs = 0L
    private var silenceStartedAt = 0L
    private var matrixTurnedOffForSilence = false
    private var matrixReleasedForSilence = false
    private var matrixProfile = GlyphDeviceProfile.PHONE3_MATRIX
    private var phone4aProEmulatedOnPhone3 = false
    
    private var lastSentFrameBuffer = IntArray(0)
    private var physicalFrameBuffer = IntArray(0)
    private var cachedMaxPixelsByColumn: IntArray? = null
    private var cachedMaxPixelsLength = -1
    private var normalizedSpectrumBands = FloatArray(0)
    private var cachedCircleRingIndexLength = -1
    private var cachedCircleRingIndexNormal: IntArray? = null
    private var cachedCircleRingIndexReverse: IntArray? = null
    private var cachedSpectrumBandIndexLength = -1
    private var cachedSpectrumBandIndexBandCount = -1
    private var cachedSpectrumBandIndexNormal: IntArray? = null
    private var cachedSpectrumBandIndexReverse: IntArray? = null
    private var cachedSpectrumBandIndexCenterNormal: IntArray? = null
    private var cachedSpectrumBandIndexCenterReverse: IntArray? = null
    private var lastRenderSignature = Long.MIN_VALUE
    private var cachedBrightnessLut = IntArray(0)
    private var cachedBrightnessLutGamma = Float.NaN
    private var lastRenderedMode: GlyphPatternRenderMode? = null
    private var lastRowBrightnessByRow = IntArray(0)
    private var lastSpectrumFullPxByColumn = IntArray(0)
    private var lastSpectrumEdgeBrightnessByColumn = IntArray(0)
    private var lastCircleBrightnessByRing = IntArray(0)
    private var cachedCircleRingPixelsLength = -1
    private var cachedCircleRingPixelsNormal: Array<IntArray>? = null
    private var cachedCircleRingPixelsReverse: Array<IntArray>? = null
    private var rainHeadByColumn = FloatArray(0)
    private var rainBrightnessByColumn = FloatArray(0)
    private var rainSpeedByColumn = FloatArray(0)
    private var lastRainUpdateMs = 0L
    private var spectrogramHistory = FloatArray(0)
    private var lastSpectrogramShiftMs = 0L
    private var wavePhase = 0f
    private var pulsePhase = 0f
    private var ripplePhase = 0f
    private var openReelStartMs = 0L
    private var openReelPhase = 0f
    private var lastOpenReelUpdateMs = 0L
    private var openReelDisplayedProgress = Float.NaN
    private var pulseGridSeed = 0
    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            runOnOwnerThread { handleServiceConnected() }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            runOnOwnerThread { handleServiceDisconnected() }
        }
    }

    private fun runOnOwnerThread(action: () -> Unit) {
        val handler = ownerHandler
        if (handler == null || Looper.myLooper() == handler.looper) {
            action()
        } else {
            handler.post { action() }
        }
    }

    private fun handleServiceConnected() {
        if (!isBound) return
        invalidateLastSentFrame()
        val currentDevice = GlyphDeviceCatalog.currentOrNull()
        val actualMatrixProfile = when (currentDevice?.profile) {
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> GlyphDeviceProfile.PHONE4A_PRO_MATRIX
            GlyphDeviceProfile.PHONE3_MATRIX -> GlyphDeviceProfile.PHONE3_MATRIX
            else -> {
                onStatusChanged(context.getString(R.string.status_glyph_matrix_device_unsupported, Build.MODEL))
                return
            }
        }

        physicalMatrixLength = Common.getDeviceMatrixLength()
        if (physicalMatrixLength <= 0) {
            onStatusChanged(context.getString(R.string.status_glyph_matrix_length_unavailable))
            return
        }
        phone4aProEmulatedOnPhone3 = initialPhone4aProEmulationEnabled &&
            actualMatrixProfile == GlyphDeviceProfile.PHONE3_MATRIX &&
            physicalMatrixLength >= GlyphMatrixProfileEmulator.PHONE4A_PRO_MATRIX_LENGTH
        matrixProfile = if (phone4aProEmulatedOnPhone3) {
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        } else {
            actualMatrixProfile
        }
        matrixLength = if (phone4aProEmulatedOnPhone3) {
            GlyphMatrixProfileEmulator.PHONE4A_PRO_MATRIX_LENGTH
        } else {
            physicalMatrixLength
        }
        frameBuffer = IntArray(matrixLength * matrixLength)
        physicalFrameBuffer = IntArray(physicalMatrixLength * physicalMatrixLength)

        val targetDeviceCode = currentDevice.matrixSpec?.sdkDeviceId ?: return
        val registered = glyphMatrixManager.register(targetDeviceCode)
        if (!registered) {
            onStatusChanged(context.getString(R.string.status_glyph_matrix_registration_failed))
            return
        }

        isSessionOpen = true
        failureCount = 0
        lastLitRows = -1
        lastMatrixBrightness = -1
        lastRenderSignature = Long.MIN_VALUE
        lastRenderedMode = null
        pulseGridSeed = ((SystemClock.elapsedRealtimeNanos() xor matrixLength.toLong()) and 0x7fffffffL).toInt()
        silenceStartedAt = 0L
        matrixTurnedOffForSilence = false
        matrixReleasedForSilence = false
        try {
            glyphMatrixManager.setGlyphMatrixTimeout(true)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "setGlyphMatrixTimeout(true) failed", error)
        }
        onStatusChanged(context.getString(R.string.status_glyph_matrix_session_ready, Build.MODEL))

        val pending = pendingLevel
        if (pending >= 0f) {
            pendingLevel = -1f
            updateLevel(pending)
        }
    }

    private fun handleServiceDisconnected() {
        invalidateLastSentFrame()
        isSessionOpen = false
        onStatusChanged(context.getString(R.string.status_glyph_matrix_service_disconnected))
    }

    override fun bind() {
        if (isBound) return
        isBound = true
        glyphMatrixManager.init(callback)
        onStatusChanged(context.getString(R.string.status_glyph_matrix_connecting))
    }

    override fun unbind() {
        releaseSession()
        if (isBound) {
            glyphMatrixManager.unInit()
            isBound = false
            isSessionOpen = false
        }
    }

    override fun setReverseDirection(reverse: Boolean) {
        if (reverseDirection != reverse) {
            reverseDirection = reverse
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
        }
    }

    override fun setGlyphMode(mode: String) {
        if (glyphMode != mode) {
            glyphMode = mode
            lastLitRows = -1
            lastMatrixBrightness = -1
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
            resetLevelScaleTracking()
            resetSpectrumScaleTracking()
            resetAllBrightnessScaleTracking()
            resetPatternVisualState()
        }
    }

    override fun setBinaryMode(binary: Boolean) {
        if (binaryMode != binary) {
            binaryMode = binary
            lastMatrixBrightness = -1
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
            resetPatternVisualState()
        }
    }

    override fun setExperimentalPerformanceOptimizationsEnabled(enabled: Boolean) {
        if (experimentalPerformanceOptimizationsEnabled != enabled) {
            experimentalPerformanceOptimizationsEnabled = enabled
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
            resetPatternVisualState()
        }
    }

    override fun setMatrixSmoothMotionEnabled(enabled: Boolean) {
        if (matrixSmoothMotionEnabled != enabled) {
            matrixSmoothMotionEnabled = enabled
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
            resetPatternVisualState()
        }
    }

    override fun setOutputGamma(gamma: Float) {
        outputGamma = gamma.coerceIn(0.6f, 2.6f)
        cachedBrightnessLutGamma = Float.NaN
    }

    override fun setLevelAutoScaleEnabled(enabled: Boolean) {
        if (levelAutoScaleEnabled != enabled) {
            levelAutoScaleEnabled = enabled
            resetLevelScaleTracking()
        }
    }

    override fun setSpectrumAutoScaleEnabled(enabled: Boolean) {
        if (spectrumAutoScaleEnabled != enabled) {
            spectrumAutoScaleEnabled = enabled
            resetSpectrumScaleTracking()
        }
    }

    override fun setAllBrightnessAutoScaleEnabled(enabled: Boolean) {
        if (allBrightnessAutoScaleEnabled != enabled) {
            allBrightnessAutoScaleEnabled = enabled
            resetAllBrightnessScaleTracking()
        }
    }

    override fun setAutoScaleWindowSeconds(seconds: Float) {
        val nextWindowMs = seconds.coerceIn(5f, 60f) * 1_000f
        if (autoScaleWindowMs != nextWindowMs) {
            autoScaleWindowMs = nextWindowMs
        }
    }

    override fun setAutoScaleOffset(offset: Float) {
        val nextOffset = offset.coerceIn(0f, 0.4f)
        if (autoScaleOffset != nextOffset) {
            autoScaleOffset = nextOffset
            resetLevelScaleTracking()
            resetSpectrumScaleTracking()
            resetAllBrightnessScaleTracking()
        }
    }

    override fun setSmoothing(smoothing: Float, smoothingBalance: Float) = Unit

    override fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?,
        phone4aBaseBandLevel: Float,
        waveformSamples: FloatArray?,
        leftWaveformSamples: FloatArray?,
        rightWaveformSamples: FloatArray?
    ) {
        this.lowEnergy = lowEnergy.coerceIn(0f, 1f)
        this.highEnergy = highEnergy.coerceIn(0f, 1f)
        this.leftLevel = leftLevel.coerceIn(0f, 1f)
        this.rightLevel = rightLevel.coerceIn(0f, 1f)
        val raw = spectrumBands ?: FloatArray(0)
        val resampled = if (matrixProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX && raw.size > 13) {
            downsampleBands(raw, 13)
        } else {
            raw
        }
        this.spectrumBands = normalizeSpectrumBands(applySpectrumSmoothing(resampled))
        this.waveformSamples = waveformSamples?.copyOf() ?: FloatArray(0)
        this.leftWaveformSamples = leftWaveformSamples?.copyOf() ?: FloatArray(0)
        this.rightWaveformSamples = rightWaveformSamples?.copyOf() ?: FloatArray(0)
    }

    private fun applySpectrumSmoothing(input: FloatArray): FloatArray {
        if (input.isEmpty()) return input
        if (smoothedSpectrumBands.size != input.size) {
            smoothedSpectrumBands = input.copyOf()
            return smoothedSpectrumBands
        }
        val attack = 0.4f
        val release = 0.15f
        for (i in input.indices) {
            val v = input[i].coerceIn(0f, 1f)
            val alpha = if (v > smoothedSpectrumBands[i]) attack else release
            smoothedSpectrumBands[i] += (v - smoothedSpectrumBands[i]) * alpha
        }
        return smoothedSpectrumBands
    }

    private fun downsampleBands(input: FloatArray, targetCount: Int): FloatArray {
        if (input.size <= targetCount) return input
        val out = FloatArray(targetCount)
        for (i in 0 until targetCount) {
            val f0 = (i.toFloat() / targetCount) * input.size
            val f1 = ((i + 1f) / targetCount) * input.size
            val k0 = f0.toInt().coerceIn(0, input.lastIndex)
            val k1 = (f1.toInt() - 1).coerceIn(k0, input.lastIndex)
            var sum = 0f
            var count = 0
            for (k in k0..k1) { sum += input[k]; count++ }
            out[i] = if (count > 0) sum / count else 0f
        }
        return out
    }

    private fun normalizeSpectrumBands(input: FloatArray): FloatArray {
        if (input.isEmpty()) return input
        if (!spectrumAutoScaleEnabled) return input

        val now = SystemClock.elapsedRealtime()
        val elapsedMs = if (lastSpectrumUpdateMs <= 0L) 0L else (now - lastSpectrumUpdateMs).coerceAtLeast(0L)
        lastSpectrumUpdateMs = now
        val drift = (elapsedMs.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        if (spectrumBandMins.size != input.size || spectrumBandMaxs.size != input.size) {
            spectrumBandMins = input.copyOf()
            spectrumBandMaxs = input.copyOf()
        }
        if (normalizedSpectrumBands.size != input.size) {
            normalizedSpectrumBands = FloatArray(input.size)
        }

        for (i in input.indices) {
            val v = input[i].coerceIn(0f, 1f)
            var minTrack = min(v, (spectrumBandMins[i] + drift).coerceIn(0f, 1f))
            var maxTrack = max(v, (spectrumBandMaxs[i] - drift).coerceIn(0f, 1f))
            if ((maxTrack - minTrack) < 0.05f) {
                minTrack = (v - 0.025f).coerceIn(0f, 1f)
                maxTrack = (v + 0.025f).coerceIn(0f, 1f)
            }
            spectrumBandMins[i] = minTrack
            spectrumBandMaxs[i] = maxTrack

            normalizedSpectrumBands[i] = normalizeWithAutoScaleOffset(v, minTrack, maxTrack)
        }
        return normalizedSpectrumBands
    }

    private fun resetSpectrumScaleTracking() {
        spectrumBandMins = FloatArray(0)
        spectrumBandMaxs = FloatArray(0)
        smoothedSpectrumBands = FloatArray(0)
        lastSpectrumUpdateMs = 0L
    }

    private fun resetAllBrightnessScaleTracking() {
        allBrightnessMin = 0f
        allBrightnessMax = 1f
        lastAllBrightnessUpdateMs = 0L
    }

    private fun resetLevelScaleTracking() {
        levelMin = 0f
        levelMax = 1f
        lastLevelUpdateMs = 0L
    }

    private fun normalizeLevelForMode(level: Float): Float {
        if (!levelAutoScaleEnabled || !isLevelAutoScaleMode()) return level
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastLevelUpdateMs <= 0L) 0L else (now - lastLevelUpdateMs).coerceAtLeast(0L)
        lastLevelUpdateMs = now
        val drift = (elapsed.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        levelMin = min(level, (levelMin + drift).coerceIn(0f, 1f))
        levelMax = max(level, (levelMax - drift).coerceIn(0f, 1f))

        return normalizeWithAutoScaleOffset(level, levelMin, levelMax)
    }

    private fun isLevelAutoScaleMode(): Boolean {
        return GlyphPatternRegistry.isLevelAutoScale(glyphMode)
    }

    private fun normalizeAllBrightnessLevel(level: Float): Float {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastAllBrightnessUpdateMs <= 0L) 0L else (now - lastAllBrightnessUpdateMs).coerceAtLeast(0L)
        lastAllBrightnessUpdateMs = now
        val drift = (elapsed.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        allBrightnessMin = min(level, (allBrightnessMin + drift).coerceIn(0f, 1f))
        allBrightnessMax = max(level, (allBrightnessMax - drift).coerceIn(0f, 1f))

        return normalizeWithAutoScaleOffset(level, allBrightnessMin, allBrightnessMax)
    }

    private fun normalizeWithAutoScaleOffset(value: Float, minTrack: Float, maxTrack: Float): Float {
        val range = (maxTrack - minTrack).coerceAtLeast(0.05f)
        val adjustedMin = minTrack - (range * autoScaleOffset)
        val adjustedMax = maxTrack + (range * autoScaleOffset)
        val adjustedRange = (adjustedMax - adjustedMin).coerceAtLeast(0.05f)
        return ((value - adjustedMin) / adjustedRange).coerceIn(0f, 1f)
    }

    override fun updateLevel(level: Float) {
        if (!isSessionOpen || matrixLength <= 0) {
            pendingLevel = level
            return
        }
        pendingLevel = -1f

        val now = SystemClock.elapsedRealtime()

        val clamped = level.coerceIn(0f, 1f)
        var renderLevel = normalizeLevelForMode(clamped)
        val maxBand = if (spectrumBands.isNotEmpty()) spectrumBands.maxOrNull() ?: 0f else 0f
        val activity = max(max(clamped, max(leftLevel, rightLevel)), maxBand)
        val renderMode = GlyphPatternRegistry.recipeFor(glyphMode)?.renderMode
            ?: GlyphPatternRenderMode.MATRIX_BAR
        val openReelPlayback = if (renderMode == GlyphPatternRenderMode.MATRIX_OPEN_REEL) {
            MediaSessionPlaybackGate.currentPlaybackSnapshot(context)
        } else {
            null
        }
        val holdOpenReelFrameForPause =
            openReelPlayback?.status == MediaSessionPlaybackGate.PlaybackStatus.PAUSED
        val silenceDrainsBeforeRelease = renderMode == GlyphPatternRenderMode.MATRIX_RAIN ||
            renderMode == GlyphPatternRenderMode.MATRIX_SPECTROGRAM ||
            renderMode == GlyphPatternRenderMode.MATRIX_RIPPLE
        val isSilent = activity < SILENCE_ACTIVITY_THRESHOLD && !holdOpenReelFrameForPause
        val silenceElapsedMs = if (isSilent) {
            if (silenceStartedAt <= 0L) silenceStartedAt = now
            now - silenceStartedAt
        } else {
            0L
        }
        val renderingSilenceDrain = isSilent && silenceDrainsBeforeRelease
        val rippleDrainProgress = if (renderingSilenceDrain && renderMode == GlyphPatternRenderMode.MATRIX_RIPPLE) {
            (silenceElapsedMs / RIPPLE_SILENCE_DRAIN_MS.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        if (isSilent && !silenceDrainsBeforeRelease) {
            if (!matrixTurnedOffForSilence && now - silenceStartedAt >= SILENCE_BLACKOUT_MS) {
                blackoutMatrixForSilence()
            }
            if (!matrixReleasedForSilence && now - silenceStartedAt >= SILENCE_RELEASE_MS) {
                releaseMatrixForSilence()
            }
            return
        }
        if (isSilent && matrixReleasedForSilence) {
            return
        }
        if (!isSilent) {
            silenceStartedAt = 0L
        }
        if (renderingSilenceDrain) {
            renderLevel = when (renderMode) {
                GlyphPatternRenderMode.MATRIX_RIPPLE -> (1f - (rippleDrainProgress * 0.15f)).coerceIn(0f, 1f)
                else -> 0f
            }
        } else {
            lastPreviewLevel = renderLevel
        }
        if (matrixTurnedOffForSilence && !isSilent) {
            matrixTurnedOffForSilence = false
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
        }

        if (matrixReleasedForSilence && !isSilent) {
            val registered = try {
                glyphMatrixManager.register(currentDeviceCode())
            } catch (error: Throwable) {
                AppLogger.w(TAG, "register during silence resume failed", error)
                false
            }
            if (!registered) {
                onStatusChanged(context.getString(R.string.status_glyph_matrix_resume_failed))
                return
            }
            try {
                glyphMatrixManager.setGlyphMatrixTimeout(true)
            } catch (error: GlyphException) {
                AppLogger.w(TAG, "setGlyphMatrixTimeout during silence resume failed", error)
            }
            matrixReleasedForSilence = false
            matrixTurnedOffForSilence = false
            invalidateLastSentFrame()
            lastRenderSignature = Long.MIN_VALUE
            lastRenderedMode = null
        }

        val frameIntervalMs = if (matrixSmoothMotionEnabled) {
            FRAME_INTERVAL_SMOOTH_MS
        } else {
            FRAME_INTERVAL_REDUCED_MS
        }
        if (now - lastRenderAt < frameIntervalMs) return
        lastRenderAt = now

        val litRows = (renderLevel * matrixLength).roundToInt().coerceIn(0, matrixLength)
        val renderBrightness = matrixBrightnessFor(renderLevel)
        lastLitRows = litRows
        lastMatrixBrightness = renderBrightness

        if (frameBuffer.size != matrixLength * matrixLength) {
            frameBuffer = IntArray(matrixLength * matrixLength)
        }

        val barWidth = (matrixLength / 8).coerceAtLeast(1)
        val leftCenter = (matrixLength * 0.35f).roundToInt().coerceIn(0, matrixLength - 1)
        val rightCenter = (matrixLength * 0.65f).roundToInt().coerceIn(0, matrixLength - 1)
        val virtualRows = renderLevel.coerceIn(0f, 1f) * matrixLength
        val fullRows = virtualRows.toInt().coerceIn(0, matrixLength)
        val edgeRowBrightness = if (binaryMode) {
            COLOR_OFF
        } else {
            ((virtualRows - fullRows) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
        }
        val circleRingCount = (((matrixLength - 1) / 2f).toInt()).coerceAtLeast(1)
        val virtualRings = renderLevel.coerceIn(0f, 1f) * circleRingCount
        val fullRings = virtualRings.toInt().coerceIn(0, circleRingCount)
        val edgeRingBrightness = if (binaryMode) {
            COLOR_OFF
        } else {
            ((virtualRings - fullRings) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
        }
        val allBrightnessFrameBrightness = if (renderMode == GlyphPatternRenderMode.ALL_BRIGHTNESS) {
            val normalizedRaw = if (
                renderLevel > ALL_BRIGHTNESS_OFF_THRESHOLD && allBrightnessAutoScaleEnabled
            ) {
                normalizeAllBrightnessLevel(renderLevel)
            } else {
                renderLevel
            }
            if (renderLevel <= ALL_BRIGHTNESS_OFF_THRESHOLD || normalizedRaw <= ALL_BRIGHTNESS_OFF_THRESHOLD) {
                COLOR_OFF
            } else {
                val normalized = ((normalizedRaw - ALL_BRIGHTNESS_OFF_THRESHOLD) / (1f - ALL_BRIGHTNESS_OFF_THRESHOLD))
                    .coerceIn(0f, 1f)
                val shaped = normalized.pow(outputGamma)
                (ALL_BRIGHTNESS_MIN_LIGHT_MATRIX +
                    ((ALL_BRIGHTNESS_MAX_LIGHT_MATRIX - ALL_BRIGHTNESS_MIN_LIGHT_MATRIX) * shaped))
                    .roundToInt()
                    .coerceIn(COLOR_OFF, COLOR_ON)
            }
        } else {
            COLOR_OFF
        }
        if (experimentalPerformanceOptimizationsEnabled) {
            val renderSignature = when (renderMode) {
                GlyphPatternRenderMode.MATRIX_SPECTRUM ->
                    computeSpectrumRenderSignature(renderLevel, centerLowToHigh = false)
                GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER ->
                    computeSpectrumRenderSignature(renderLevel, centerLowToHigh = true)
                GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM ->
                    computeSpectrumRenderSignature(renderLevel, centerLowToHigh = false)
                else -> computeRenderSignature(
                    renderMode = renderMode,
                    fullRows = fullRows,
                    edgeRowBrightness = edgeRowBrightness,
                    fullRings = fullRings,
                    edgeRingBrightness = edgeRingBrightness,
                    allBrightness = allBrightnessFrameBrightness
                )
            }
            if (renderSignature != null && renderSignature == lastRenderSignature) {
                return
            }
            lastRenderSignature = renderSignature ?: Long.MIN_VALUE
        } else {
            lastRenderSignature = Long.MIN_VALUE
        }

        val supportsDiffRendering =
            experimentalPerformanceOptimizationsEnabled && when (renderMode) {
                GlyphPatternRenderMode.MATRIX_BAR,
                GlyphPatternRenderMode.MATRIX_FIELD,
                GlyphPatternRenderMode.MATRIX_CIRCLE,
                GlyphPatternRenderMode.MATRIX_SPECTRUM,
                GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER,
                GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM,
                GlyphPatternRenderMode.ALL_BRIGHTNESS -> true
                else -> false
            }
        val diffModeContinuing = supportsDiffRendering && lastRenderedMode == renderMode

        if (!diffModeContinuing && renderMode != GlyphPatternRenderMode.ALL_BRIGHTNESS) {
            frameBuffer.fill(COLOR_OFF)
        }
        var silenceDrainComplete = false

        fun rowBrightnessForMeter(index: Int): Int {
            return when {
                index < fullRows -> COLOR_ON
                index == fullRows && fullRows < matrixLength -> edgeRowBrightness
                else -> COLOR_OFF
            }
        }

        fun sampleBandForRatio(ratio: Float): Float {
            if (spectrumBands.isNotEmpty()) {
                if (spectrumBands.size == 1) return spectrumBands[0].coerceIn(0f, 1f)
                val scaled = ratio.coerceIn(0f, 1f) * (spectrumBands.size - 1)
                val lo = scaled.toInt().coerceIn(0, spectrumBands.lastIndex)
                val hi = (lo + 1).coerceIn(0, spectrumBands.lastIndex)
                val t = scaled - lo
                return ((spectrumBands[lo] * (1f - t)) + (spectrumBands[hi] * t)).coerceIn(0f, 1f)
            }
            return ((lowEnergy * (1f - ratio)) + (highEnergy * ratio)).coerceIn(0f, 1f)
        }

        fun drawBar(centerX: Int) {
            val startX = (centerX - barWidth / 2).coerceAtLeast(0)
            val endXExclusive = (startX + barWidth).coerceAtMost(matrixLength)

            if (supportsDiffRendering) {
                ensureRowBrightnessCache()
                for (row in 0 until matrixLength) {
                    val brightness = rowBrightnessForMeter(row)
                    val y = if (reverseDirection) row else (matrixLength - 1 - row)
                    if (diffModeContinuing && lastRowBrightnessByRow[y] == brightness) continue
                    val rowOffset = y * matrixLength
                    for (x in startX until endXExclusive) {
                        frameBuffer[rowOffset + x] = brightness
                    }
                    lastRowBrightnessByRow[y] = brightness
                }
                return
            }

            for (row in 0 until matrixLength) {
                val brightness = rowBrightnessForMeter(row)
                if (brightness <= 0) continue
                val y = if (reverseDirection) row else (matrixLength - 1 - row)
                val rowOffset = y * matrixLength
                for (x in startX until endXExclusive) {
                    frameBuffer[rowOffset + x] = brightness
                }
            }
        }

        fun drawSpectrum(centerLowToHigh: Boolean, anchorBottom: Boolean = false) {
            val centerY = matrixLength / 2
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            val bandIndexByColumn = if (spectrumBands.isNotEmpty() && experimentalPerformanceOptimizationsEnabled) {
                buildSpectrumBandIndexMap(matrixLength, spectrumBands.size, centerLowToHigh, reverseDirection)
            } else {
                null
            }
            if (supportsDiffRendering) {
                ensureSpectrumColumnCaches()
            }

            fun sampleBandForColumn(x: Int): Float {
                if (spectrumBands.isNotEmpty()) {
                    val idx = bandIndexByColumn?.get(x) ?: run {
                        val sampledX = if (!centerLowToHigh && reverseDirection) {
                            matrixLength - 1 - x
                        } else {
                            x
                        }
                        val rawRatio = if (centerLowToHigh) {
                            val center = (matrixLength - 1f) / 2f
                            if (center <= 0f) 0f else (kotlin.math.abs(sampledX - center) / center).coerceIn(0f, 1f)
                        } else {
                            if (matrixLength <= 1) 0f else (sampledX / (matrixLength - 1f)).coerceIn(0f, 1f)
                        }
                        val ratio = if (centerLowToHigh && reverseDirection) 1f - rawRatio else rawRatio
                        (ratio * (spectrumBands.size - 1)).roundToInt().coerceIn(0, spectrumBands.lastIndex)
                    }
                    return spectrumBands[idx].coerceIn(0f, 1f)
                }
                val sampledX = if (!centerLowToHigh && reverseDirection) {
                    matrixLength - 1 - x
                } else {
                    x
                }
                val rawRatio = if (centerLowToHigh) {
                    val center = (matrixLength - 1f) / 2f
                    if (center <= 0f) 0f else (kotlin.math.abs(sampledX - center) / center).coerceIn(0f, 1f)
                } else {
                    if (matrixLength <= 1) 0f else (sampledX / (matrixLength - 1f)).coerceIn(0f, 1f)
                }
                val ratio = if (centerLowToHigh && reverseDirection) 1f - rawRatio else rawRatio
                return ((lowEnergy * (1f - ratio)) + (highEnergy * ratio)).coerceIn(0f, 1f)
            }

            for (x in 0 until matrixLength) {
                val band = sampleBandForColumn(x)
                val maxPx = maxPixelsByColumn[x].coerceAtLeast(1)
                val weightedLevel = (renderLevel * band).coerceIn(0f, 1f)
                val virtualPx = maxPx * weightedLevel
                val fullPx = virtualPx.toInt().coerceIn(0, maxPx)
                val edgeBrightness = if (binaryMode) {
                    COLOR_OFF
                } else {
                    ((virtualPx - fullPx) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
                }
                if (supportsDiffRendering &&
                    diffModeContinuing &&
                    lastSpectrumFullPxByColumn[x] == fullPx &&
                    lastSpectrumEdgeBrightnessByColumn[x] == edgeBrightness
                ) continue
                if (supportsDiffRendering) {
                    clearSpectrumColumn(x)
                }
                if (fullPx <= 0 && edgeBrightness <= 0) {
                    if (supportsDiffRendering) {
                        lastSpectrumFullPxByColumn[x] = fullPx
                        lastSpectrumEdgeBrightnessByColumn[x] = edgeBrightness
                    }
                    continue
                }

                if (anchorBottom) {
                    val columnHeight = maxPx.coerceAtLeast(1)
                    val topPadding = (matrixLength - columnHeight) / 2
                    val bottomY = (topPadding + columnHeight - 1).coerceIn(0, matrixLength - 1)
                    for (step in 0 until fullPx.coerceAtMost(columnHeight)) {
                        val y = (bottomY - step).coerceIn(0, matrixLength - 1)
                        frameBuffer[y * matrixLength + x] = COLOR_ON
                    }
                    if (edgeBrightness > 0 && fullPx < columnHeight) {
                        val y = (bottomY - fullPx).coerceIn(0, matrixLength - 1)
                        frameBuffer[y * matrixLength + x] = edgeBrightness
                    }
                } else {
                    // 奇数画素で中央軸対称にする
                    val oddFullPx = if (fullPx % 2 == 0) (fullPx - 1).coerceAtLeast(0) else fullPx
                    val fullHalf = oddFullPx / 2

                    // 中央を軸に、まず full の明るさで埋める
                    for (dy in 0..fullHalf) {
                        val yTop = (centerY - dy).coerceIn(0, matrixLength - 1)
                        val yBottom = (centerY + dy).coerceIn(0, matrixLength - 1)
                        frameBuffer[yTop * matrixLength + x] = COLOR_ON
                        frameBuffer[yBottom * matrixLength + x] = COLOR_ON
                    }

                    // 端の1段だけ partial 明るさにして、Matrix Bar に近い見た目へ寄せる
                    if (edgeBrightness > 0 && oddFullPx < maxPx) {
                        val edgeOffset = fullHalf + 1
                        val yTop = (centerY - edgeOffset).coerceIn(0, matrixLength - 1)
                        val yBottom = (centerY + edgeOffset).coerceIn(0, matrixLength - 1)
                        frameBuffer[yTop * matrixLength + x] = edgeBrightness
                        frameBuffer[yBottom * matrixLength + x] = edgeBrightness
                    }
                }
                if (supportsDiffRendering) {
                    lastSpectrumFullPxByColumn[x] = fullPx
                    lastSpectrumEdgeBrightnessByColumn[x] = edgeBrightness
                }
            }
        }

        fun drawSkyline() {
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            for (x in 0 until matrixLength) {
                val ratio = if (matrixLength <= 1) 0f else x / (matrixLength - 1f)
                val smoothedBand = (
                    sampleBandForRatio((ratio - 0.08f).coerceIn(0f, 1f)) * 0.2f +
                        sampleBandForRatio((ratio - 0.04f).coerceIn(0f, 1f)) * 0.3f +
                        sampleBandForRatio(ratio) * 0.35f +
                        sampleBandForRatio((ratio + 0.04f).coerceIn(0f, 1f)) * 0.1f +
                        sampleBandForRatio((ratio + 0.08f).coerceIn(0f, 1f)) * 0.05f
                    ).coerceIn(0f, 1f)
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                val weightedLevel = (renderLevel * (0.18f + smoothedBand * 0.82f)).coerceIn(0f, 1f)
                val virtualPx = columnHeight * weightedLevel
                val fullPx = virtualPx.toInt().coerceIn(0, columnHeight)
                val edgeBrightness = if (binaryMode) {
                    COLOR_OFF
                } else {
                    ((virtualPx - fullPx) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
                }
                val topPadding = (matrixLength - columnHeight) / 2
                val bottomY = (topPadding + columnHeight - 1).coerceIn(0, matrixLength - 1)
                for (step in 0 until fullPx.coerceAtMost(columnHeight)) {
                    val y = (bottomY - step).coerceIn(0, matrixLength - 1)
                    frameBuffer[y * matrixLength + x] = COLOR_ON
                }
                if (edgeBrightness > 0 && fullPx < columnHeight) {
                    val y = (bottomY - fullPx).coerceIn(0, matrixLength - 1)
                    frameBuffer[y * matrixLength + x] = edgeBrightness
                }
            }
        }

        fun drawWaveField() {
            val elapsedMs = if (lastRenderAt <= 0L) frameIntervalMs else frameIntervalMs
            wavePhase += (elapsedMs.toFloat() / 1000f) * (2.8f + (highEnergy * 4.2f))
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            for (x in 0 until matrixLength) {
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                val topPadding = (matrixLength - columnHeight) / 2
                val ratio = if (matrixLength <= 1) 0f else x / (matrixLength - 1f)
                val band = sampleBandForRatio(ratio)
                for (localY in 0 until columnHeight) {
                    val y = topPadding + localY
                    val normX = if (matrixLength <= 1) 0f else x / (matrixLength - 1f)
                    val normY = if (columnHeight <= 1) 0f else localY / (columnHeight - 1f)
                    val wave = ((sin(wavePhase + (normX * 6.5f) - (normY * 5.5f) + band * 2.5f) + 1f) * 0.5f)
                    val envelope = ((0.08f + renderLevel * 0.92f) * (0.10f + band * 0.90f)).coerceIn(0f, 1f)
                    val brightness = (wave * envelope * MATRIX_WAVE_FIELD_BRIGHTNESS_BOOST).coerceIn(0f, 1f)
                    if (brightness <= 0.02f) continue
                    frameBuffer[y * matrixLength + x] = brightnessToMatrixColor(brightness)
                }
            }
        }

        fun drawSpectrogram() {
            ensureSpectrogramState()
            val rowCount = matrixLength.coerceAtLeast(1)
            val insertAt = if (reverseDirection) rowCount - 1 else 0
            val shiftIntervalMs = spectrogramShiftIntervalMs(rowCount)
            val shiftCount = if (lastSpectrogramShiftMs <= 0L) {
                lastSpectrogramShiftMs = now
                1
            } else {
                val elapsed = (now - lastSpectrogramShiftMs).coerceAtLeast(0L)
                val steps = (elapsed / shiftIntervalMs).toInt().coerceIn(0, rowCount)
                if (steps > 0) {
                    lastSpectrogramShiftMs += shiftIntervalMs * steps
                }
                steps
            }
            repeat(shiftCount) {
                shiftSpectrogramHistory(rowCount)
            }
            val insertOffset = insertAt * rowCount
            for (row in 0 until rowCount) {
                val bandRatio = if (rowCount <= 1) {
                    0.5f
                } else {
                    1f - (row / (rowCount - 1f))
                }
                spectrogramHistory[insertOffset + row] = if (renderingSilenceDrain) {
                    0f
                } else {
                    val band = sampleBandForRatio(bandRatio)
                    (
                        (0.03f + band * 0.97f) *
                            (0.08f + renderLevel * 0.92f) *
                            MATRIX_SPECTROGRAM_BRIGHTNESS_BOOST
                        ).coerceIn(0f, 1f)
                }
            }

            for (x in 0 until matrixLength) {
                val columnOffset = x * rowCount
                for (y in 0 until matrixLength) {
                    val historyRow = y.coerceIn(0, rowCount - 1)
                    val brightness = spectrogramHistory[columnOffset + historyRow]
                    if (brightness <= 0.02f) continue
                    frameBuffer[y * matrixLength + x] = brightnessToMatrixColor(brightness)
                }
            }
            if (renderingSilenceDrain) {
                silenceDrainComplete = spectrogramHistory.all { it <= SILENCE_DRAIN_BRIGHTNESS_THRESHOLD }
            }
        }

        fun drawSpectrumAnalyzer() {
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            var previousY = -1f
            var previousBrightness = 0f

            fun plotAnalyzerPoint(x: Int, yFloat: Float, brightness: Float) {
                if (x !in 0 until matrixLength || brightness <= 0.01f) return
                val primaryY = yFloat.roundToInt().coerceIn(0, matrixLength - 1)
                val primary = brightnessToMatrixColor(brightness)
                val rowOffset = primaryY * matrixLength
                frameBuffer[rowOffset + x] = max(frameBuffer[rowOffset + x], primary)

                val glow = brightnessToMatrixColor((brightness * 0.45f).coerceIn(0f, 1f))
                if (glow > COLOR_OFF) {
                    if (primaryY > 0) {
                        val upperOffset = (primaryY - 1) * matrixLength
                        frameBuffer[upperOffset + x] = max(frameBuffer[upperOffset + x], glow)
                    }
                    if (primaryY < matrixLength - 1) {
                        val lowerOffset = (primaryY + 1) * matrixLength
                        frameBuffer[lowerOffset + x] = max(frameBuffer[lowerOffset + x], glow)
                    }
                }
            }

            for (x in 0 until matrixLength) {
                val ratio = if (matrixLength <= 1) {
                    0.5f
                } else {
                    x / (matrixLength - 1f)
                }
                val smoothedBand = (
                    sampleBandForRatio((ratio - 0.06f).coerceIn(0f, 1f)) * 0.18f +
                        sampleBandForRatio((ratio - 0.02f).coerceIn(0f, 1f)) * 0.28f +
                        sampleBandForRatio(ratio) * 0.34f +
                        sampleBandForRatio((ratio + 0.02f).coerceIn(0f, 1f)) * 0.14f +
                        sampleBandForRatio((ratio + 0.06f).coerceIn(0f, 1f)) * 0.06f
                    ).coerceIn(0f, 1f)
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                val topPadding = (matrixLength - columnHeight) / 2
                val curveLevel = (0.06f + (smoothedBand * renderLevel * 0.94f)).coerceIn(0f, 1f)
                val yFloat = (topPadding + (1f - curveLevel) * (columnHeight - 1)).coerceIn(
                    topPadding.toFloat(),
                    (topPadding + columnHeight - 1).toFloat()
                )
                val brightness = if (binaryMode) {
                    1f
                } else {
                    ((0.22f + smoothedBand * 0.78f) * MATRIX_SPECTRUM_ANALYZER_BRIGHTNESS_BOOST)
                        .coerceIn(0f, 1f)
                }

                if (previousY >= 0f) {
                    val steps = max(1, abs(yFloat - previousY).roundToInt())
                    for (step in 1..steps) {
                        val t = step / steps.toFloat()
                        val interpY = previousY + ((yFloat - previousY) * t)
                        val interpBrightness = (previousBrightness * (1f - t)) + (brightness * t)
                        plotAnalyzerPoint(x, interpY, interpBrightness)
                    }
                } else {
                    plotAnalyzerPoint(x, yFloat, brightness)
                }
                previousY = yFloat
                previousBrightness = brightness
            }
        }

        fun drawOscilloscope() {
            if (waveformSamples.isEmpty()) return
            val verticalPadding = when {
                matrixLength >= 21 -> 2
                matrixLength >= 9 -> 1
                else -> 0
            }
            val topY = verticalPadding.coerceAtMost((matrixLength - 1) / 2)
            val bottomY = (matrixLength - 1 - verticalPadding).coerceAtLeast(topY)
            val drawableHeight = (bottomY - topY + 1).coerceAtLeast(1)
            val centerY = topY + ((drawableHeight - 1) / 2f)
            val amplitudeRange = ((drawableHeight - 1) * 0.40f).coerceAtLeast(0.5f)

            fun sampleWaveform(ratio: Float): Float {
                if (waveformSamples.size == 1) return waveformSamples[0].coerceIn(-1f, 1f)
                val scaled = ratio.coerceIn(0f, 1f) * (waveformSamples.size - 1)
                val lo = scaled.toInt().coerceIn(0, waveformSamples.lastIndex)
                val hi = (lo + 1).coerceIn(0, waveformSamples.lastIndex)
                val t = scaled - lo
                return ((waveformSamples[lo] * (1f - t)) + (waveformSamples[hi] * t)).coerceIn(-1f, 1f)
            }

            fun plotWavePoint(xFloat: Float, yFloat: Float, visible: Boolean) {
                if (!visible) return
                if (binaryMode) {
                    val x = xFloat.roundToInt().coerceIn(0, matrixLength - 1)
                    val y = yFloat.roundToInt().coerceIn(0, matrixLength - 1)
                    frameBuffer[y * matrixLength + x] = COLOR_ON
                    return
                }
                val clampedX = xFloat.coerceIn(0f, (matrixLength - 1).toFloat())
                val leftX = clampedX.toInt().coerceIn(0, matrixLength - 1)
                val rightX = (leftX + 1).coerceIn(0, matrixLength - 1)
                val xFraction = (clampedX - leftX).coerceIn(0f, 1f)

                fun plotAt(x: Int, xWeight: Float) {
                    if (x !in 0 until matrixLength || xWeight <= 0f) return
                    val clampedY = yFloat.coerceIn(topY.toFloat(), bottomY.toFloat())
                    val lowerY = clampedY.toInt().coerceIn(0, matrixLength - 1)
                    val upperY = (lowerY + 1).coerceIn(0, matrixLength - 1)
                    val yFraction = (clampedY - lowerY).coerceIn(0f, 1f)
                    val lowerBrightness = brightnessToMatrixColor(((1f - yFraction) * xWeight).coerceIn(0f, 1f))
                    frameBuffer[lowerY * matrixLength + x] = max(frameBuffer[lowerY * matrixLength + x], lowerBrightness)
                    if (upperY != lowerY && yFraction > 0f) {
                        val upperBrightness = brightnessToMatrixColor((yFraction * xWeight).coerceIn(0f, 1f))
                        frameBuffer[upperY * matrixLength + x] = max(frameBuffer[upperY * matrixLength + x], upperBrightness)
                    }
                }

                plotAt(leftX, 1f - xFraction)
                if (rightX != leftX && xFraction > 0f) {
                    plotAt(rightX, xFraction)
                }
            }

            var previousY = -1f
            var previousX = -1f
            var previousVisible = false
            for (x in 0 until matrixLength) {
                val sourceX = if (reverseDirection) matrixLength - 1 - x else x
                val ratio = if (matrixLength <= 1) 0.5f else sourceX / (matrixLength - 1f)
                val sample = sampleWaveform(ratio)
                val amplitudeScale = (0.72f + renderLevel * 0.24f).coerceIn(0.65f, 0.96f)
                val yFloat = (centerY - (sample * amplitudeRange * amplitudeScale)).coerceIn(
                    topY.toFloat(),
                    bottomY.toFloat()
                )
                val sampleStrength = abs(sample)
                val visible = sampleStrength > 0.015f || renderLevel > 0.02f

                if (previousY >= 0f) {
                    val steps = max(1, max(abs(yFloat - previousY), abs(x - previousX)).roundToInt())
                    for (step in 1..steps) {
                        val t = step / steps.toFloat()
                        val interpX = previousX + ((x - previousX) * t)
                        val interpY = previousY + ((yFloat - previousY) * t)
                        plotWavePoint(interpX, interpY, visible || previousVisible)
                    }
                } else {
                    plotWavePoint(x.toFloat(), yFloat, visible)
                }
                previousY = yFloat
                previousX = x.toFloat()
                previousVisible = visible
            }
        }

        fun drawRadialSpectrum() {
            val center = (matrixLength - 1f) / 2f
            val innerRadius = (matrixLength * 0.22f).coerceAtLeast(2f)
            val outerRadius = (matrixLength * 0.69f).coerceAtMost(center * 1.42f)
            val availableLength = if (reverseDirection) outerRadius else (outerRadius - innerRadius).coerceAtLeast(1f)
            for (y in 0 until matrixLength) {
                for (x in 0 until matrixLength) {
                    val dx = x - center
                    val dy = y - center
                    val radius = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if ((!reverseDirection && radius < innerRadius) || radius > outerRadius) continue
                    val angleRatio = ((atan2(dy, dx) + Math.PI).toFloat() / (Math.PI.toFloat() * 2f))
                        .coerceIn(0f, 1f)
                    val rotatedRatio = (angleRatio + 0.75f) % 1f
                    val symmetricRatio = (abs(rotatedRatio - 0.5f) * 2f).coerceIn(0f, 1f)
                    val band = sampleBandForRatio(symmetricRatio)
                    val virtualLength = availableLength * (band * renderLevel).coerceIn(0f, 1f)
                    val radialPosition = if (reverseDirection) outerRadius - radius else radius - innerRadius
                    val distanceFromTip = virtualLength - radialPosition
                    val brightness = when {
                        radialPosition < virtualLength.toInt() -> COLOR_ON
                        binaryMode -> COLOR_OFF
                        distanceFromTip in -1f..0f ->
                            ((distanceFromTip + 1f) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
                        else -> COLOR_OFF
                    }
                    if (brightness > COLOR_OFF) {
                        frameBuffer[y * matrixLength + x] = max(frameBuffer[y * matrixLength + x], brightness)
                    }
                }
            }
        }

        fun drawOpenReel() {
            if (openReelStartMs <= 0L) {
                openReelStartMs = now
            }
            val fallbackProgress = (((now - openReelStartMs).coerceAtLeast(0L) % 180_000L) / 180_000f)
                .coerceIn(0f, 1f)
            val targetProgress = openReelPlayback?.progress ?: fallbackProgress
            if (openReelDisplayedProgress.isNaN()) {
                openReelDisplayedProgress = targetProgress
            }
            val openReelDeltaMs = if (lastOpenReelUpdateMs <= 0L) {
                frameIntervalMs
            } else {
                (now - lastOpenReelUpdateMs).coerceIn(1L, 120L)
            }
            lastOpenReelUpdateMs = now
            val openReelDeltaSeconds = openReelDeltaMs / 1000f
            val progressDelta = targetProgress - openReelDisplayedProgress
            val catchingUp = abs(progressDelta) > 0.012f
            if (catchingUp) {
                val catchUpStep = (openReelDeltaSeconds * 0.72f).coerceAtLeast(0.002f)
                openReelDisplayedProgress += progressDelta.coerceIn(-catchUpStep, catchUpStep)
            } else {
                openReelDisplayedProgress = targetProgress
            }
            val progress = openReelDisplayedProgress.coerceIn(0f, 1f)
            val centerX = (matrixLength - 1f) / 2f
            val centerY = (matrixLength - 1f) / 2f
            val reelRadius = (matrixLength * 0.36f).coerceAtMost(centerX + 0.8f)
                .coerceAtLeast(3f)
            val hubRadius = (matrixLength * 0.07f).coerceAtLeast(0.75f)
            val tapeProgress = progress.coerceIn(0f, 1f)
            val tapeExitAngle = 0.92f - tapeProgress * 0.82f
            val baseRotationSpeed = 2.094f + progress * 4.189f
            val rotationDirection = if (catchingUp && progressDelta < 0f) {
                1f
            } else {
                -1f
            }
            val playbackPaused = openReelPlayback?.status == MediaSessionPlaybackGate.PlaybackStatus.PAUSED
            val catchUpBoost = if (catchingUp) {
                (baseRotationSpeed * 1.7f + abs(progressDelta) * 18f).coerceAtMost(18f)
            } else {
                0f
            }
            val rotationSpeed = when {
                catchingUp -> baseRotationSpeed + catchUpBoost
                playbackPaused -> 0f
                else -> baseRotationSpeed
            }
            openReelPhase += rotationDirection * rotationSpeed * openReelDeltaSeconds
            val phase = openReelPhase

            fun putPixel(x: Int, y: Int, brightness: Float) {
                if (x !in 0 until matrixLength || y !in 0 until matrixLength) return
                val value = if (binaryMode) {
                    if (brightness > 0f) COLOR_ON else COLOR_OFF
                } else {
                    brightnessToMatrixColor(brightness.coerceIn(0f, 1f))
                }
                val index = y * matrixLength + x
                frameBuffer[index] = max(frameBuffer[index], value)
            }

            fun plotPoint(x: Float, y: Float, brightness: Float) {
                val xi = x.roundToInt()
                val yi = y.roundToInt()
                putPixel(xi, yi, brightness)
            }

            fun drawDottedCircle(radius: Float, brightness: Float) {
                val steps = max(40, (radius * 12f).roundToInt())
                for (step in 0 until steps) {
                    val angle = step / steps.toFloat() * Math.PI.toFloat() * 2f
                    plotPoint(
                        centerX + cos(angle) * radius,
                        centerY + sin(angle) * radius,
                        brightness
                    )
                }
            }

            fun drawLine(
                x0: Float,
                y0: Float,
                x1: Float,
                y1: Float,
                brightness: Float,
                endBrightness: Float = brightness
            ) {
                val steps = max(1, max(abs(x1 - x0), abs(y1 - y0)).roundToInt() * 2)
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val lineBrightness = brightness + (endBrightness - brightness) * t
                    plotPoint(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, lineBrightness)
                }
            }

            drawDottedCircle(reelRadius, 1f)
            for (y in 0 until matrixLength) {
                for (x in 0 until matrixLength) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (distance <= hubRadius) {
                        putPixel(x, y, 1f)
                    }
                }
            }

            for (slot in 0 until 2) {
                val angle = phase + slot * Math.PI.toFloat()
                val slotInner = hubRadius + 0.9f
                val slotOuter = (reelRadius * 0.70f).coerceAtLeast(slotInner + 1.2f)
                drawLine(
                    centerX + cos(angle) * slotInner,
                    centerY + sin(angle) * slotInner,
                    centerX + cos(angle) * slotOuter,
                    centerY + sin(angle) * slotOuter,
                    1f
                )
            }

            val tapeStartX = centerX + cos(tapeExitAngle) * reelRadius
            val tapeStartY = centerY + sin(tapeExitAngle) * reelRadius
            val tapeEndX = (matrixLength - 1f).coerceAtLeast(tapeStartX)
            val tapeEndY = centerY + reelRadius * (0.46f - tapeProgress * 0.38f)
            drawLine(tapeStartX, tapeStartY, tapeEndX, tapeEndY, 0.78f, 0.66f)
        }

        fun drawPulseGrid() {
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            val centerX = (matrixLength - 1f) / 2f
            val centerY = (matrixLength - 1f) / 2f
            val maxRadius = max(centerX, centerY).coerceAtLeast(1f)
            for (x in 0 until matrixLength) {
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                val topPadding = (matrixLength - columnHeight) / 2
                for (localY in 0 until columnHeight) {
                    val y = topPadding + localY
                    val dx = x - centerX
                    val dy = y - centerY
                    val radialRatio = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        .div(maxRadius)
                        .coerceIn(0f, 1f)
                    val verticalRatio = if (columnHeight <= 1) 0.5f else (localY / (columnHeight - 1f)).coerceIn(0f, 1f)
                    val hash = ((x * 73856093) xor (y * 19349663) xor (matrixLength * 83492791) xor pulseGridSeed) and 0x7fffffff
                    val randomUnit = (hash % 10_000) / 9_999f
                    val randomOffset = (randomUnit - 0.5f) * 0.34f
                    val structuredRatio = ((radialRatio * 0.72f) + (verticalRatio * 0.18f) + 0.05f + randomOffset)
                        .coerceIn(0f, 1f)
                    val bandRatio = if (reverseDirection) 1f - structuredRatio else structuredRatio
                    val pixelBand = sampleBandForRatio(bandRatio)
                    val brightness = (
                        (0.05f + pixelBand * 0.95f) *
                            (0.10f + renderLevel * 0.90f) *
                            MATRIX_PULSE_GRID_BRIGHTNESS_BOOST
                        ).coerceIn(0f, 1f)
                    if (brightness > 0.015f) {
                        frameBuffer[y * matrixLength + x] = brightnessToMatrixColor(brightness)
                    }
                }
            }
        }

        fun drawRipple() {
            val elapsedMs = if (lastRenderAt <= 0L) frameIntervalMs else frameIntervalMs
            ripplePhase += (elapsedMs.toFloat() / 1000f) * (2.1f + renderLevel * 3.4f)
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            val centerX = (matrixLength - 1f) / 2f
            val centerY = (matrixLength - 1f) / 2f
            val maxRadius = max(centerX, centerY).coerceAtLeast(1f)
            val activeRadius = if (renderingSilenceDrain) {
                (0.12f + rippleDrainProgress * 1.22f).coerceIn(0f, 1.34f)
            } else {
                (0.12f + renderLevel * 0.88f).coerceIn(0f, 1f)
            }
            val drainFade = if (renderingSilenceDrain) {
                (1f - rippleDrainProgress).coerceIn(0f, 1f)
            } else {
                1f
            }
            for (x in 0 until matrixLength) {
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                val topPadding = (matrixLength - columnHeight) / 2
                for (localY in 0 until columnHeight) {
                    val y = topPadding + localY
                    val dx = x - centerX
                    val dy = y - centerY
                    val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val radius = (distance / maxRadius).coerceIn(0f, 1f)
                    val ripple = ((sin((radius * 11f) - (ripplePhase * 4.2f)) + 1f) * 0.5f)
                    val radiusWindow = (1f - (abs(radius - activeRadius) * 2.4f)).coerceIn(0f, 1f)
                    val envelope = ((0.32f + renderLevel * 0.68f) * (0.48f + radiusWindow * 0.52f) * drainFade).coerceIn(0f, 1f)
                    val brightness = (ripple * envelope * MATRIX_RIPPLE_BRIGHTNESS_BOOST).coerceIn(0f, 1f)
                    if (brightness <= 0.03f) continue
                    frameBuffer[y * matrixLength + x] = brightnessToMatrixColor(brightness)
                }
            }
            if (renderingSilenceDrain) {
                silenceDrainComplete = rippleDrainProgress >= 1f
            }
        }

        fun drawRain() {
            ensureRainState()
            val elapsedMs = if (lastRainUpdateMs <= 0L) frameIntervalMs else (now - lastRainUpdateMs).coerceAtLeast(1L)
            lastRainUpdateMs = now
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
            for (x in 0 until matrixLength) {
                val ratio = if (matrixLength <= 1) 0f else x / (matrixLength - 1f)
                val band = sampleBandForRatio(ratio)
                val intensity = ((0.26f + renderLevel * 0.74f) * (0.36f + band * 0.64f) * MATRIX_RAIN_BRIGHTNESS_BOOST)
                    .coerceIn(0f, 1f)
                val columnHeight = maxPixelsByColumn[x].coerceAtLeast(1)
                if (rainHeadByColumn[x] < 0f) {
                    val spawnWindow = ((intensity * 10f).roundToInt()).coerceIn(0, 9)
                    val bucket = ((now / frameIntervalMs) + (x * 7L)) % 10L
                    if (!renderingSilenceDrain && intensity > 0.16f && bucket <= spawnWindow.toLong()) {
                        rainHeadByColumn[x] = 0f
                        rainBrightnessByColumn[x] = (0.62f + intensity * 0.38f).coerceIn(0f, 1f)
                        rainSpeedByColumn[x] = (0.45f + intensity * 1.35f).coerceIn(0.35f, 2.1f)
                    }
                } else {
                    rainHeadByColumn[x] += rainSpeedByColumn[x] * (elapsedMs / 33f)
                    rainBrightnessByColumn[x] = (rainBrightnessByColumn[x] - (elapsedMs / 1000f) * 0.08f).coerceAtLeast(0.32f)
                    if (rainHeadByColumn[x] > columnHeight + RAIN_TAIL_LENGTH) {
                        rainHeadByColumn[x] = -1f
                        rainBrightnessByColumn[x] = 0f
                        rainSpeedByColumn[x] = 0f
                    }
                }
                if (rainHeadByColumn[x] < 0f) continue
                val topPadding = (matrixLength - columnHeight) / 2
                val bottomY = (topPadding + columnHeight - 1).coerceIn(0, matrixLength - 1)
                val headRow = rainHeadByColumn[x].roundToInt()
                for (tail in 0..RAIN_TAIL_LENGTH) {
                    val progress = 1f - (tail / (RAIN_TAIL_LENGTH + 1f))
                    val localBrightness = (rainBrightnessByColumn[x] * progress).coerceIn(0f, 1f)
                    val localRow = headRow - tail
                    if (localRow < 0 || localRow >= columnHeight) continue
                    val y = if (reverseDirection) {
                        (bottomY - localRow).coerceIn(0, matrixLength - 1)
                    } else {
                        (topPadding + localRow).coerceIn(0, matrixLength - 1)
                    }
                    frameBuffer[y * matrixLength + x] = max(
                        frameBuffer[y * matrixLength + x],
                        brightnessToMatrixColor(localBrightness)
                    )
                }
            }
            if (renderingSilenceDrain) {
                silenceDrainComplete = rainHeadByColumn.all { it < 0f }
            }
        }

        when (renderMode) {
            GlyphPatternRenderMode.MATRIX_FIELD -> {
                // 行ごとに full / partial / off を持たせ、Linear のように先端だけ半点灯させる
                if (supportsDiffRendering) {
                    ensureRowBrightnessCache()
                }
                for (row in 0 until matrixLength) {
                    val brightness = rowBrightnessForMeter(row)
                    val y = if (reverseDirection) row else (matrixLength - 1 - row)
                    if (supportsDiffRendering && diffModeContinuing && lastRowBrightnessByRow[y] == brightness) continue
                    val rowOffset = y * matrixLength
                    for (x in 0 until matrixLength) {
                        frameBuffer[rowOffset + x] = brightness
                    }
                    if (supportsDiffRendering) {
                        lastRowBrightnessByRow[y] = brightness
                    }
                }
            }
            GlyphPatternRenderMode.MATRIX_CIRCLE -> {
                fun ringBrightnessForMeter(index: Int): Int {
                    return when {
                        index < fullRings -> COLOR_ON
                        index == fullRings && fullRings < circleRingCount -> edgeRingBrightness
                        else -> COLOR_OFF
                    }
                }

                if (supportsDiffRendering) {
                    ensureCircleRingBrightnessCache(circleRingCount)
                    val ringPixelsByRing = buildCircleRingPixelBuckets(matrixLength, reverseDirection)
                    for (ringIndex in 0 until circleRingCount) {
                        val brightness = ringBrightnessForMeter(ringIndex)
                        if (diffModeContinuing && lastCircleBrightnessByRing[ringIndex] == brightness) continue
                        for (pixelIndex in ringPixelsByRing[ringIndex]) {
                            frameBuffer[pixelIndex] = brightness
                        }
                        lastCircleBrightnessByRing[ringIndex] = brightness
                    }
                } else {
                    val ringIndexByPixel = if (experimentalPerformanceOptimizationsEnabled) {
                        buildCircleRingIndexMap(matrixLength, reverseDirection)
                    } else {
                        null
                    }
                    val center = (matrixLength - 1) / 2f
                    val maxRadius = (matrixLength - 1) / 2f
                    for (index in frameBuffer.indices) {
                        val ringIndex = ringIndexByPixel?.get(index) ?: run {
                            val y = index / matrixLength
                            val x = index % matrixLength
                            val dx = x - center
                            val dy = y - center
                            val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            if (distance > maxRadius) {
                                -1
                            } else if (reverseDirection) {
                                ((maxRadius - distance).coerceAtLeast(0f)).toInt().coerceIn(0, circleRingCount - 1)
                            } else {
                                distance.toInt().coerceIn(0, circleRingCount - 1)
                            }
                        }
                        if (ringIndex < 0) continue
                        val brightness = ringBrightnessForMeter(ringIndex)
                        if (brightness > 0) {
                            frameBuffer[index] = brightness
                        }
                    }
                }
            }
            GlyphPatternRenderMode.MATRIX_RIPPLE -> drawRipple()
            GlyphPatternRenderMode.MATRIX_SPECTRUM -> drawSpectrum(centerLowToHigh = false)
            GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER -> drawSpectrum(centerLowToHigh = true)
            GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM -> drawSpectrum(centerLowToHigh = false, anchorBottom = true)
            GlyphPatternRenderMode.MATRIX_SPECTROGRAM -> drawSpectrogram()
            GlyphPatternRenderMode.MATRIX_SPECTRUM_ANALYZER -> drawSpectrumAnalyzer()
            GlyphPatternRenderMode.MATRIX_OSCILLOSCOPE -> drawOscilloscope()
            GlyphPatternRenderMode.MATRIX_RADIAL_SPECTRUM -> drawRadialSpectrum()
            GlyphPatternRenderMode.MATRIX_OPEN_REEL -> drawOpenReel()
            GlyphPatternRenderMode.MATRIX_RAIN -> drawRain()
            GlyphPatternRenderMode.MATRIX_WAVE_FIELD -> drawWaveField()
            GlyphPatternRenderMode.MATRIX_SKYLINE -> drawSkyline()
            GlyphPatternRenderMode.MATRIX_PULSE_GRID -> drawPulseGrid()
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> {
                frameBuffer.fill(allBrightnessFrameBrightness)
                lastMatrixBrightness = allBrightnessFrameBrightness
                if (allBrightnessFrameBrightness == COLOR_OFF) {
                    lastPreviewLevel = 0f
                }
            }
            else -> drawBar(matrixLength / 2)
        }
        lastRenderedMode = renderMode

        val deviceFrame = frameForPhysicalDevice()
        submitMatrixFrame(deviceFrame)
        if (
            renderingSilenceDrain &&
            silenceDrainComplete &&
            isSessionOpen &&
            !matrixReleasedForSilence
        ) {
            releaseMatrixForSilence()
        }
    }

    private fun submitMatrixFrame(deviceFrame: IntArray) {
        if (
            lastSentFrameBuffer.size == deviceFrame.size &&
            lastSentFrameBuffer.contentEquals(deviceFrame)
        ) {
            return
        }

        try {
            glyphMatrixManager.setAppMatrixFrame(deviceFrame)
            if (lastSentFrameBuffer.size != deviceFrame.size) {
                lastSentFrameBuffer = deviceFrame.copyOf()
            } else {
                deviceFrame.copyInto(lastSentFrameBuffer)
            }
            failureCount = 0
        } catch (error: GlyphException) {
            invalidateLastSentFrame()
            failureCount += 1
            if (failureCount >= 3) {
                AppLogger.e(TAG, "setAppMatrixFrame repeatedly failed. disabling matrix output", error)
                releaseSession()
                onStatusChanged(context.getString(R.string.status_glyph_matrix_update_failed))
            }
        } catch (error: Throwable) {
            invalidateLastSentFrame()
            failureCount += 1
            if (failureCount >= 3) {
                AppLogger.e(TAG, "setAppMatrixFrame crashed repeatedly. disabling matrix output", error)
                releaseSession()
                onStatusChanged(context.getString(R.string.status_glyph_matrix_update_crashed))
            }
        }
    }

    private fun invalidateLastSentFrame() {
        if (lastSentFrameBuffer.isNotEmpty()) {
            lastSentFrameBuffer = IntArray(0)
        }
        lastRenderSignature = Long.MIN_VALUE
        lastRenderedMode = null
    }

    override fun turnOff() {
        invalidateLastSentFrame()
        lastPreviewLevel = 0f
        silenceStartedAt = 0L
        lastRenderSignature = Long.MIN_VALUE
        lastRenderedMode = null
        resetPatternVisualState()
        try {
            glyphMatrixManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff ignored", error)
        }
    }

    override fun releaseSession() {
        turnOff()
        try {
            glyphMatrixManager.closeAppMatrix()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "closeAppMatrix during session release failed", error)
        }
        isSessionOpen = false
        matrixTurnedOffForSilence = false
        matrixReleasedForSilence = false
    }

    override fun suspendSession() {
        releaseMatrixForSilence()
    }

    private fun releaseMatrixForSilence() {
        invalidateLastSentFrame()
        try {
            glyphMatrixManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff during silence release failed", error)
        }
        try {
            glyphMatrixManager.closeAppMatrix()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "closeAppMatrix during silence release failed", error)
        }
        matrixTurnedOffForSilence = true
        matrixReleasedForSilence = true
    }

    private fun blackoutMatrixForSilence() {
        invalidateLastSentFrame()
        try {
            glyphMatrixManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff during silence blackout failed", error)
        }
        matrixTurnedOffForSilence = true
    }

    private fun currentDeviceCode(): String {
        return requireNotNull(GlyphDeviceCatalog.currentOrNull()?.matrixSpec?.sdkDeviceId) {
            "Matrix device spec is unavailable for the current device."
        }
    }

    private fun matrixBrightnessFor(level: Float): Int {
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0f) return COLOR_OFF
        if (binaryMode) return COLOR_ON
        if (experimentalPerformanceOptimizationsEnabled) {
            ensureBrightnessLut()
            val index = (clamped * 255f).roundToInt().coerceIn(0, 255)
            return cachedBrightnessLut[index]
        }
        return (clamped.pow(outputGamma) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
    }

    private fun ensureBrightnessLut() {
        if (cachedBrightnessLut.size == 256 && cachedBrightnessLutGamma == outputGamma) return
        cachedBrightnessLut = IntArray(256) { index ->
            val clamped = index / 255f
            if (clamped <= 0f) {
                COLOR_OFF
            } else {
                (clamped.pow(outputGamma) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
            }
        }
        cachedBrightnessLutGamma = outputGamma
    }

    private fun ensureRowBrightnessCache() {
        if (lastRowBrightnessByRow.size != matrixLength) {
            lastRowBrightnessByRow = IntArray(matrixLength) { -1 }
        }
    }

    private fun ensureSpectrumColumnCaches() {
        if (lastSpectrumFullPxByColumn.size != matrixLength) {
            lastSpectrumFullPxByColumn = IntArray(matrixLength) { -1 }
        }
        if (lastSpectrumEdgeBrightnessByColumn.size != matrixLength) {
            lastSpectrumEdgeBrightnessByColumn = IntArray(matrixLength) { -1 }
        }
    }

    private fun ensureCircleRingBrightnessCache(ringCount: Int) {
        if (lastCircleBrightnessByRing.size != ringCount) {
            lastCircleBrightnessByRing = IntArray(ringCount) { -1 }
        }
    }

    private fun clearSpectrumColumn(x: Int) {
        for (y in 0 until matrixLength) {
            frameBuffer[y * matrixLength + x] = COLOR_OFF
        }
    }

    private fun frameForPhysicalDevice(): IntArray {
        if (!phone4aProEmulatedOnPhone3) return frameBuffer
        val requiredSize = physicalMatrixLength * physicalMatrixLength
        if (physicalFrameBuffer.size != requiredSize) {
            physicalFrameBuffer = IntArray(requiredSize)
        }
        GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(
            source = frameBuffer,
            physicalMatrixLength = physicalMatrixLength,
            destination = physicalFrameBuffer
        )
        return physicalFrameBuffer
    }

    private fun computeSpectrumRenderSignature(renderLevel: Float, centerLowToHigh: Boolean): Long {
        val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixProfile)
        val bandIndexByColumn = if (spectrumBands.isNotEmpty()) {
            buildSpectrumBandIndexMap(matrixLength, spectrumBands.size, centerLowToHigh, reverseDirection)
        } else {
            null
        }
        var signature = if (centerLowToHigh) (6L shl 60) else (5L shl 60)
        for (x in 0 until matrixLength) {
            val band = if (spectrumBands.isNotEmpty()) {
                spectrumBands[bandIndexByColumn!![x]].coerceIn(0f, 1f)
            } else {
                val sampledX = if (!centerLowToHigh && reverseDirection) {
                    matrixLength - 1 - x
                } else {
                    x
                }
                val rawRatio = if (centerLowToHigh) {
                    val center = (matrixLength - 1f) / 2f
                    if (center <= 0f) 0f else (kotlin.math.abs(sampledX - center) / center).coerceIn(0f, 1f)
                } else {
                    if (matrixLength <= 1) 0f else (sampledX / (matrixLength - 1f)).coerceIn(0f, 1f)
                }
                val ratio = if (centerLowToHigh && reverseDirection) 1f - rawRatio else rawRatio
                ((lowEnergy * (1f - ratio)) + (highEnergy * ratio)).coerceIn(0f, 1f)
            }
            val maxPx = maxPixelsByColumn[x].coerceAtLeast(1)
            val weightedLevel = (renderLevel * band).coerceIn(0f, 1f)
            val virtualPx = maxPx * weightedLevel
            val fullPx = virtualPx.toInt().coerceIn(0, maxPx)
            val edgeBrightness = if (binaryMode) {
                COLOR_OFF
            } else {
                ((virtualPx - fullPx) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
            }
            val quantizedEdgeBrightness = quantizeForSignature(edgeBrightness, SIGNATURE_EDGE_BRIGHTNESS_STEP)
            signature = (signature * 1315423911L) xor ((fullPx.toLong() shl 8) or quantizedEdgeBrightness.toLong())
        }
        return signature
    }

    private fun computeRenderSignature(
        renderMode: GlyphPatternRenderMode,
        fullRows: Int,
        edgeRowBrightness: Int,
        fullRings: Int,
        edgeRingBrightness: Int,
        allBrightness: Int
    ): Long? {
        val reverseBit = if (reverseDirection) 1L else 0L
        val quantizedEdgeRowBrightness = quantizeForSignature(edgeRowBrightness, SIGNATURE_EDGE_BRIGHTNESS_STEP)
        val quantizedEdgeRingBrightness = quantizeForSignature(edgeRingBrightness, SIGNATURE_EDGE_BRIGHTNESS_STEP)
        val quantizedAllBrightness = quantizeForSignature(allBrightness, SIGNATURE_ALL_BRIGHTNESS_STEP)
        return when (renderMode) {
            GlyphPatternRenderMode.MATRIX_BAR ->
                (1L shl 60) or (reverseBit shl 59) or (fullRows.toLong() shl 16) or quantizedEdgeRowBrightness.toLong()
            GlyphPatternRenderMode.MATRIX_FIELD ->
                (2L shl 60) or (reverseBit shl 59) or (fullRows.toLong() shl 16) or quantizedEdgeRowBrightness.toLong()
            GlyphPatternRenderMode.MATRIX_CIRCLE ->
                (3L shl 60) or (reverseBit shl 59) or (fullRings.toLong() shl 16) or quantizedEdgeRingBrightness.toLong()
            GlyphPatternRenderMode.ALL_BRIGHTNESS ->
                (4L shl 60) or quantizedAllBrightness.toLong()
            else -> null
        }
    }

    private fun brightnessToMatrixColor(value: Float): Int {
        val safe = value.coerceIn(0f, 1f)
        if (binaryMode) {
            return if (safe >= 0.5f) COLOR_ON else COLOR_OFF
        }
        return (safe.pow(outputGamma) * COLOR_ON).roundToInt().coerceIn(COLOR_OFF, COLOR_ON)
    }

    private fun ensureRainState() {
        if (rainHeadByColumn.size == matrixLength) return
        rainHeadByColumn = FloatArray(matrixLength) { -1f }
        rainBrightnessByColumn = FloatArray(matrixLength)
        rainSpeedByColumn = FloatArray(matrixLength)
        lastRainUpdateMs = 0L
    }

    private fun ensureSpectrogramState() {
        val requiredSize = matrixLength * matrixLength
        if (spectrogramHistory.size == requiredSize) return
        spectrogramHistory = FloatArray(requiredSize)
        lastSpectrogramShiftMs = 0L
    }

    private fun spectrogramShiftIntervalMs(rowCount: Int): Long {
        val targetTravelMs = SPECTROGRAM_REFERENCE_MATRIX_LENGTH *
            SPECTROGRAM_REFERENCE_SHIFT_INTERVAL_MS
        return ((targetTravelMs + (rowCount / 2)) / rowCount.coerceAtLeast(1)).coerceAtLeast(1L)
    }

    private fun shiftSpectrogramHistory(rowCount: Int) {
        if (reverseDirection) {
            for (x in 0 until rowCount - 1) {
                val dstOffset = x * rowCount
                val srcOffset = (x + 1) * rowCount
                spectrogramHistory.copyInto(
                    destination = spectrogramHistory,
                    destinationOffset = dstOffset,
                    startIndex = srcOffset,
                    endIndex = srcOffset + rowCount
                )
            }
        } else {
            for (x in (rowCount - 1) downTo 1) {
                val dstOffset = x * rowCount
                val srcOffset = (x - 1) * rowCount
                spectrogramHistory.copyInto(
                    destination = spectrogramHistory,
                    destinationOffset = dstOffset,
                    startIndex = srcOffset,
                    endIndex = srcOffset + rowCount
                )
            }
        }
    }

    private fun resetPatternVisualState() {
        rainHeadByColumn = FloatArray(0)
        rainBrightnessByColumn = FloatArray(0)
        rainSpeedByColumn = FloatArray(0)
        spectrogramHistory = FloatArray(0)
        lastSpectrogramShiftMs = 0L
        lastRainUpdateMs = 0L
        wavePhase = 0f
        pulsePhase = 0f
        ripplePhase = 0f
        openReelStartMs = 0L
        openReelPhase = 0f
        lastOpenReelUpdateMs = 0L
        openReelDisplayedProgress = Float.NaN
    }

    private fun quantizeForSignature(value: Int, step: Int): Int {
        if (value <= 0 || step <= 1) return value.coerceAtLeast(0)
        return ((value + (step / 2)) / step) * step
    }

    private fun buildCircleRingIndexMap(length: Int, reverseDirection: Boolean): IntArray {
        if (cachedCircleRingIndexLength != length || cachedCircleRingIndexNormal == null || cachedCircleRingIndexReverse == null) {
            val center = (length - 1) / 2f
            val maxRadius = (length - 1) / 2f
            val ringCount = maxRadius.toInt().coerceAtLeast(1)
            val normal = IntArray(length * length)
            val reverse = IntArray(length * length)
            for (index in normal.indices) {
                val y = index / length
                val x = index % length
                val dx = x - center
                val dy = y - center
                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (distance > maxRadius) {
                    normal[index] = -1
                    reverse[index] = -1
                } else {
                    normal[index] = distance.toInt().coerceIn(0, ringCount - 1)
                    reverse[index] = ((maxRadius - distance).coerceAtLeast(0f)).toInt().coerceIn(0, ringCount - 1)
                }
            }
            cachedCircleRingIndexLength = length
            cachedCircleRingIndexNormal = normal
            cachedCircleRingIndexReverse = reverse
        }
        return if (reverseDirection) {
            cachedCircleRingIndexReverse!!
        } else {
            cachedCircleRingIndexNormal!!
        }
    }

    private fun buildCircleRingPixelBuckets(length: Int, reverseDirection: Boolean): Array<IntArray> {
        if (cachedCircleRingPixelsLength != length ||
            cachedCircleRingPixelsNormal == null ||
            cachedCircleRingPixelsReverse == null
        ) {
            val ringCount = (((length - 1) / 2f).toInt()).coerceAtLeast(1)
            val normalIndexMap = buildCircleRingIndexMap(length, reverseDirection = false)
            val reverseIndexMap = buildCircleRingIndexMap(length, reverseDirection = true)

            fun buildBuckets(indexMap: IntArray): Array<IntArray> {
                val counts = IntArray(ringCount)
                for (ringIndex in indexMap) {
                    if (ringIndex >= 0) {
                        counts[ringIndex] += 1
                    }
                }
                val buckets = Array(ringCount) { ring -> IntArray(counts[ring]) }
                val offsets = IntArray(ringCount)
                for (pixelIndex in indexMap.indices) {
                    val ringIndex = indexMap[pixelIndex]
                    if (ringIndex < 0) continue
                    buckets[ringIndex][offsets[ringIndex]++] = pixelIndex
                }
                return buckets
            }

            cachedCircleRingPixelsLength = length
            cachedCircleRingPixelsNormal = buildBuckets(normalIndexMap)
            cachedCircleRingPixelsReverse = buildBuckets(reverseIndexMap)
        }
        return if (reverseDirection) {
            cachedCircleRingPixelsReverse!!
        } else {
            cachedCircleRingPixelsNormal!!
        }
    }

    private fun buildSpectrumBandIndexMap(
        length: Int,
        bandCount: Int,
        centerLowToHigh: Boolean,
        reverseDirection: Boolean
    ): IntArray {
        if (cachedSpectrumBandIndexLength != length || cachedSpectrumBandIndexBandCount != bandCount ||
            cachedSpectrumBandIndexNormal == null || cachedSpectrumBandIndexReverse == null ||
            cachedSpectrumBandIndexCenterNormal == null || cachedSpectrumBandIndexCenterReverse == null
        ) {
            val normal = IntArray(length)
            val reverse = IntArray(length)
            val centerNormal = IntArray(length)
            val centerReverse = IntArray(length)
            val center = (length - 1f) / 2f
            for (x in 0 until length) {
                val ratioNormal = if (length <= 1) 0f else (x / (length - 1f)).coerceIn(0f, 1f)
                val ratioReverse = if (length <= 1) 0f else ((length - 1 - x) / (length - 1f)).coerceIn(0f, 1f)
                val centerRatio = if (center <= 0f) 0f else (kotlin.math.abs(x - center) / center).coerceIn(0f, 1f)
                normal[x] = (ratioNormal * (bandCount - 1)).roundToInt().coerceIn(0, bandCount - 1)
                reverse[x] = (ratioReverse * (bandCount - 1)).roundToInt().coerceIn(0, bandCount - 1)
                centerNormal[x] = (centerRatio * (bandCount - 1)).roundToInt().coerceIn(0, bandCount - 1)
                centerReverse[x] = ((1f - centerRatio) * (bandCount - 1)).roundToInt().coerceIn(0, bandCount - 1)
            }
            cachedSpectrumBandIndexLength = length
            cachedSpectrumBandIndexBandCount = bandCount
            cachedSpectrumBandIndexNormal = normal
            cachedSpectrumBandIndexReverse = reverse
            cachedSpectrumBandIndexCenterNormal = centerNormal
            cachedSpectrumBandIndexCenterReverse = centerReverse
        }
        return when {
            centerLowToHigh && reverseDirection -> cachedSpectrumBandIndexCenterReverse!!
            centerLowToHigh -> cachedSpectrumBandIndexCenterNormal!!
            reverseDirection -> cachedSpectrumBandIndexReverse!!
            else -> cachedSpectrumBandIndexNormal!!
        }
    }

    override fun previewLevel(): Float = lastPreviewLevel.coerceIn(0f, 1f)

    override fun previewSpectrumBands(): FloatArray = spectrumBands.copyOf()

    private fun buildColumnMaxPixels(length: Int, deviceProfile: GlyphDeviceProfile): IntArray {
        if (cachedMaxPixelsLength == length && cachedMaxPixelsByColumn != null) {
            return cachedMaxPixelsByColumn!!
        }

        val profile = when (deviceProfile) {
            // Phone (3)
            GlyphDeviceProfile.PHONE3_MATRIX -> intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25,
                25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7
            )
            // Phone (4a) Pro: 端=5, 2番目=9, 3-4番目=11, 5番目〜中央=13
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX ->
                GlyphMatrixProfileEmulator.phone4aProColumnHeights()
            else -> IntArray(length) { length }
        }

        val out = if (length == profile.size) {
            profile
        } else {
            val arr = IntArray(length)
            for (i in 0 until length) {
                val src = if (length <= 1) (profile.lastIndex / 2f) else i * (profile.lastIndex.toFloat() / (length - 1f))
                val srcIdx = src.roundToInt().coerceIn(0, profile.lastIndex)
                val scaled = (profile[srcIdx] * (length / profile.size.toFloat())).roundToInt().coerceAtLeast(1)
                arr[i] = if (scaled % 2 == 0) (scaled - 1).coerceAtLeast(1) else scaled
            }
            arr
        }

        cachedMaxPixelsLength = length
        cachedMaxPixelsByColumn = out
        return out
    }
}
