package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.R
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class GlyphLightController(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit
) : GlyphOutputController {
    companion object {
        private const val TAG = "GlyphLightController"
        private const val MIN_LIGHT = 800
        private const val MAX_LIGHT = 4095
        private const val SILENCE_RELEASE_MS = 3_000L
        private const val SILENCE_ACTIVITY_THRESHOLD = 0.003f
        private const val DEFAULT_AUTO_SCALE_WINDOW_MS = 30_000f
        private const val ALL_BRIGHTNESS_OFF_THRESHOLD = 0.06f
        private const val ALL_BRIGHTNESS_MIN_LIGHT = 240
        private const val ALL_BRIGHTNESS_RESPONSE_GAMMA = 1.8f
        private const val PHONE4A_BASE_INDICATOR_EPSILON = 0.000001f
        private const val PHONE4A_BASE_INDICATOR_GAMMA = 2.0f
        private const val PHONE4A_BASE_INDICATOR_TRACKING_ATTACK = 0.82f
        private const val PHONE4A_BASE_INDICATOR_TRACKING_RELEASE = 0.28f
        private const val PHONE4A_BASE_INDICATOR_ONSET_GAIN = 6.2f
        private const val PHONE4A_BASE_INDICATOR_ONSET_GATE = 0.075f
        private const val PHONE4A_BASE_INDICATOR_MIN_RISE = 0.018f
        private const val PHONE4A_BASE_INDICATOR_MIN_INPUT_LEVEL = 0.10f
        private const val PHONE4A_BASE_INDICATOR_ONSET_PEAK_FALLOFF = 0.86f
        private const val PHONE4A_BASE_INDICATOR_MIN_PEAK_REFERENCE = 0.30f
        private const val PHONE4A_BASE_INDICATOR_OUTPUT_OFF_THRESHOLD = 0.018f
        private const val PHONE4A_BASE_INDICATOR_MIN_ENVELOPE_DECAY = 0.50f
        private const val PHONE4A_BASE_INDICATOR_MAX_ENVELOPE_DECAY = 0.70f
        private const val PHONE4A_BASE_INDICATOR_RAW_GATE = 0.055f
        private const val PHONE4A_BASE_INDICATOR_RAW_GAIN = 2.4f
    }

    private interface BaseIndicatorRenderer {
        fun accepts(profile: GlyphDeviceProfile): Boolean
        fun setSmoothing(smoothing: Float, smoothingBalance: Float)
        fun updateAnalysis(level: Float)
        fun reset()
        fun apply(colors: IntArray, binaryMode: Boolean, outputGamma: Float)
    }

    private class Phone4aBaseIndicatorRenderer : BaseIndicatorRenderer {
        private var bandLevel = 0f
        private var trackedBandLevel = 0f
        private var previousTrackedBandLevel = 0f
        private var onsetPeakLevel = PHONE4A_BASE_INDICATOR_EPSILON
        private var outputEnvelope = 0f
        private var smoothing = 0.45f
        private var smoothingBalance = 0f

        override fun accepts(profile: GlyphDeviceProfile): Boolean {
            return profile == GlyphDeviceProfile.PHONE4A
        }

        override fun setSmoothing(smoothing: Float, smoothingBalance: Float) {
            this.smoothing = smoothing.coerceIn(0.05f, 0.6f)
            this.smoothingBalance = smoothingBalance.coerceIn(-1f, 1f)
        }

        override fun updateAnalysis(level: Float) {
            bandLevel = level.coerceIn(0f, 1f)
        }

        override fun reset() {
            bandLevel = 0f
            trackedBandLevel = 0f
            previousTrackedBandLevel = 0f
            onsetPeakLevel = PHONE4A_BASE_INDICATOR_EPSILON
            outputEnvelope = 0f
        }

        override fun apply(colors: IntArray, binaryMode: Boolean, outputGamma: Float) {
            if (colors.isEmpty()) return

            val current = bandLevel.coerceIn(0f, 1f)
            val trackingAlpha = if (current > trackedBandLevel) {
                PHONE4A_BASE_INDICATOR_TRACKING_ATTACK
            } else {
                PHONE4A_BASE_INDICATOR_TRACKING_RELEASE
            }
            trackedBandLevel += (current - trackedBandLevel) * trackingAlpha

            val rawRise = if (trackedBandLevel >= PHONE4A_BASE_INDICATOR_MIN_INPUT_LEVEL) {
                (trackedBandLevel - previousTrackedBandLevel).coerceAtLeast(0f)
            } else {
                0f
            }
            previousTrackedBandLevel = trackedBandLevel
            val rise = if (rawRise >= PHONE4A_BASE_INDICATOR_MIN_RISE) rawRise else 0f

            val onset = (rise * PHONE4A_BASE_INDICATOR_ONSET_GAIN).coerceIn(0f, 1f)
            val gatedOnset = if (onset <= PHONE4A_BASE_INDICATOR_ONSET_GATE) {
                0f
            } else {
                (
                    (onset - PHONE4A_BASE_INDICATOR_ONSET_GATE) /
                        (1f - PHONE4A_BASE_INDICATOR_ONSET_GATE)
                    ).coerceIn(0f, 1f)
            }
            onsetPeakLevel = max(
                gatedOnset,
                onsetPeakLevel * PHONE4A_BASE_INDICATOR_ONSET_PEAK_FALLOFF
            ).coerceAtLeast(PHONE4A_BASE_INDICATOR_EPSILON)
            val normalized = (
                gatedOnset / max(onsetPeakLevel, PHONE4A_BASE_INDICATOR_MIN_PEAK_REFERENCE)
            ).coerceIn(0f, 1f)
            val envelopeDecay = (0.46f + (smoothing.coerceIn(0.05f, 0.6f) * 0.50f))
                .coerceIn(
                    PHONE4A_BASE_INDICATOR_MIN_ENVELOPE_DECAY,
                    PHONE4A_BASE_INDICATOR_MAX_ENVELOPE_DECAY
                )
            outputEnvelope = max(normalized, outputEnvelope * envelopeDecay)
            val gammaMapped = outputEnvelope.pow(PHONE4A_BASE_INDICATOR_GAMMA)
            val brightness = if (binaryMode) {
                if (gammaMapped >= 0.5f) MAX_LIGHT else 0
            } else {
                if (gammaMapped <= PHONE4A_BASE_INDICATOR_OUTPUT_OFF_THRESHOLD) {
                    0
                } else {
                    (gammaMapped * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
                }
            }

            colors[colors.lastIndex] = brightness
        }
    }

    private data class DeviceSpec(
        val profile: GlyphDeviceProfile,
        val deviceId: String,
        val channelCount: Int,
        val cRange: IntRange,
        val aRange: IntRange? = null,
        val bRange: IntRange? = null,
        val cabRange: IntRange? = null,
        val d1Range: IntRange? = null,
        val centerSupported: Boolean = false
    )

    private val glyphManager = GlyphManager.getInstance(context.applicationContext)
    private var isBound = false
    private var isSessionOpen = false
    private var reverseDirection = true
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var binaryMode = false
    private var baseIndicatorEnabled = false
    private var outputGamma = ALL_BRIGHTNESS_RESPONSE_GAMMA
    private var levelAutoScaleEnabled = false
    private var spectrumAutoScaleEnabled = false
    private var allBrightnessAutoScaleEnabled = false
    private var smoothing = 0.45f
    private var smoothingBalance = 0f
    private var autoScaleWindowMs = DEFAULT_AUTO_SCALE_WINDOW_MS
    private var levelMin = 0f
    private var levelMax = 1f
    private var lastLevelUpdateMs = 0L
    private var lastPreviewLevel = 0f
    private var baseIndicatorMin = 0f
    private var baseIndicatorMax = 1f
    private var lastBaseIndicatorUpdateMs = 0L
    private var allBrightnessMin = 0f
    private var allBrightnessMax = 1f
    private var lastAllBrightnessUpdateMs = 0L
    private var lowEnergy = 0f
    private var highEnergy = 0f
    private var spectrumBands = FloatArray(0)
    private var smoothedSpectrumBands = FloatArray(0)
    private var spectrumBandMins = FloatArray(0)
    private var spectrumBandMaxs = FloatArray(0)
    private var lastSpectrumUpdateMs = 0L
    private var silenceStartedAt = 0L
    private var sessionReleasedForSilence = false

    private var deviceSpec: DeviceSpec? = null
    private var cLinearFrame: GlyphFrame? = null
    private var cabLinearFrame: GlyphFrame? = null
    private var aLinearFrame: GlyphFrame? = null
    private var bLinearFrame: GlyphFrame? = null
    private var d1Frame: GlyphFrame? = null
    private var fullGlyphBrightness = IntArray(0)
    private val baseIndicatorRenderers: List<BaseIndicatorRenderer> = listOf(
        Phone4aBaseIndicatorRenderer()
    )
    // SDK 接続待ちの間に届いたレベルを保持し、接続後に再送する
    @Volatile private var pendingLevel: Float = -1f

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            val spec = resolveDeviceSpec()
            if (spec == null) {
                onStatusChanged(context.getString(R.string.status_glyph_device_unsupported, Build.MODEL))
                return
            }

            deviceSpec = spec
            fullGlyphBrightness = IntArray(spec.channelCount)

            cLinearFrame = GlyphFrame.Builder(spec.deviceId)
                .buildChannel(spec.cRange.first, spec.cRange.last)
                .build()

            cabLinearFrame = spec.cabRange?.let { range ->
                GlyphFrame.Builder(spec.deviceId)
                    .buildChannel(range.first, range.last)
                    .build()
            }

            aLinearFrame = spec.aRange?.let { range ->
                GlyphFrame.Builder(spec.deviceId)
                    .buildChannel(range.first, range.last)
                    .build()
            }

            bLinearFrame = spec.bRange?.let { range ->
                GlyphFrame.Builder(spec.deviceId)
                    .buildChannel(range.first, range.last)
                    .build()
            }

            d1Frame = spec.d1Range?.let { range ->
                GlyphFrame.Builder(spec.deviceId)
                    .buildChannel(range.first, range.last)
                    .build()
            }

            val registered = glyphManager.register(spec.deviceId)
            if (!registered) {
                onStatusChanged(context.getString(R.string.status_glyph_sdk_registration_failed))
                return
            }

            try {
                glyphManager.openSession()
                isSessionOpen = true
                silenceStartedAt = 0L
                sessionReleasedForSilence = false
                onStatusChanged(context.getString(R.string.status_glyph_session_ready, Build.MODEL))
                // 接続完了前に届いていたレベルを即反映
                val pending = pendingLevel
                if (pending >= 0f) {
                    pendingLevel = -1f
                    updateLevel(pending)
                }
            } catch (error: GlyphException) {
                onStatusChanged(
                    context.getString(
                        R.string.status_glyph_session_open_failed,
                        error.message ?: context.getString(R.string.status_unknown_error)
                    )
                )
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            isSessionOpen = false
            silenceStartedAt = 0L
            sessionReleasedForSilence = false
            onStatusChanged(context.getString(R.string.status_glyph_service_disconnected))
        }
    }

    override fun bind() {
        if (isBound) return
        isBound = true
        glyphManager.init(callback)
        onStatusChanged(context.getString(R.string.status_glyph_service_connecting))
    }

    override fun unbind() {
        turnOff()
        closeSession()
        if (isBound) {
            glyphManager.unInit()
            isBound = false
        }
    }

    override fun setReverseDirection(reverse: Boolean) {
        reverseDirection = reverse
    }

    override fun setGlyphMode(mode: String) {
        if (glyphMode != mode) {
            glyphMode = mode
            resetSpectrumScaleTracking()
            resetLevelScaleTracking()
            resetBaseIndicatorTracking()
        }
    }

    override fun setBinaryMode(binary: Boolean) {
        binaryMode = binary
    }

    override fun setBaseIndicatorEnabled(enabled: Boolean) {
        baseIndicatorEnabled = enabled
    }

    override fun setOutputGamma(gamma: Float) {
        outputGamma = gamma.coerceIn(0.6f, 2.6f)
    }

    override fun setLevelAutoScaleEnabled(enabled: Boolean) {
        if (levelAutoScaleEnabled != enabled) {
            levelAutoScaleEnabled = enabled
            resetLevelScaleTracking()
            resetBaseIndicatorTracking()
        }
    }

    override fun setSpectrumAutoScaleEnabled(enabled: Boolean) {
        if (spectrumAutoScaleEnabled != enabled) {
            spectrumAutoScaleEnabled = enabled
            resetSpectrumScaleTracking()
            resetBaseIndicatorTracking()
        }
    }

    override fun setAllBrightnessAutoScaleEnabled(enabled: Boolean) {
        if (allBrightnessAutoScaleEnabled != enabled) {
            allBrightnessAutoScaleEnabled = enabled
            resetAllBrightnessScaleTracking()
            resetBaseIndicatorTracking()
        }
    }

    override fun setAutoScaleWindowSeconds(seconds: Float) {
        val nextWindowMs = seconds.coerceIn(10f, 60f) * 1_000f
        if (autoScaleWindowMs != nextWindowMs) {
            autoScaleWindowMs = nextWindowMs
            resetBaseIndicatorTracking()
        }
    }

    override fun setSmoothing(smoothing: Float, smoothingBalance: Float) {
        this.smoothing = smoothing.coerceIn(0.05f, 0.6f)
        this.smoothingBalance = smoothingBalance.coerceIn(-1f, 1f)
        baseIndicatorRenderers.forEach {
            it.setSmoothing(this.smoothing, this.smoothingBalance)
        }
    }

    override fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?,
        phone4aBaseBandLevel: Float
    ) {
        this.lowEnergy = lowEnergy.coerceIn(0f, 1f)
        updateBaseIndicatorAnalysis(phone4aBaseBandLevel)
        this.highEnergy = highEnergy.coerceIn(0f, 1f)
        val raw = spectrumBands ?: FloatArray(0)
        this.spectrumBands = normalizeSpectrumBands(applySpectrumSmoothing(raw))
    }

    private fun applySpectrumSmoothing(input: FloatArray): FloatArray {
        if (input.isEmpty()) return input
        if (smoothedSpectrumBands.size != input.size) {
            smoothedSpectrumBands = input.copyOf()
            return input.copyOf()
        }
        val attack = 0.4f
        val release = 0.15f
        for (i in input.indices) {
            val v = input[i].coerceIn(0f, 1f)
            val alpha = if (v > smoothedSpectrumBands[i]) attack else release
            smoothedSpectrumBands[i] += (v - smoothedSpectrumBands[i]) * alpha
        }
        return smoothedSpectrumBands.copyOf()
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

        val out = FloatArray(input.size)
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

            val range = (maxTrack - minTrack).coerceAtLeast(0.05f)
            out[i] = ((v - minTrack) / range).coerceIn(0f, 1f)
        }
        return out
    }

    private fun resetSpectrumScaleTracking() {
        spectrumBandMins = FloatArray(0)
        spectrumBandMaxs = FloatArray(0)
        smoothedSpectrumBands = FloatArray(0)
        lastSpectrumUpdateMs = 0L
        resetAllBrightnessScaleTracking()
    }

    override fun updateLevel(level: Float) {
        val now = SystemClock.elapsedRealtime()
        val clamped = level.coerceIn(0f, 1f)
        val maxBand = if (spectrumBands.isNotEmpty()) spectrumBands.maxOrNull() ?: 0f else 0f
        val activity = max(clamped, maxBand)

        if (activity < SILENCE_ACTIVITY_THRESHOLD) {
            if (silenceStartedAt <= 0L) silenceStartedAt = now
            if (!sessionReleasedForSilence && now - silenceStartedAt >= SILENCE_RELEASE_MS) {
                releaseSessionForSilence()
            }
            return
        }
        silenceStartedAt = 0L

        if (sessionReleasedForSilence && !reopenSessionAfterSilence()) {
            pendingLevel = level
            return
        }

        if (!isSessionOpen) {
            pendingLevel = level  // SDK 未接続時は保留しておく
            return
        }
        pendingLevel = -1f
        val renderLevel = normalizeLevelForMode(clamped)
        lastPreviewLevel = renderLevel

        val spec = deviceSpec ?: return

        try {
            renderLightPattern(spec, renderLevel)
        } catch (_: GlyphException) {
        }
    }

    private fun normalizeLevelForMode(level: Float): Float {
        if (!levelAutoScaleEnabled || !isLevelAutoScaleMode()) return level
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastLevelUpdateMs <= 0L) 0L else (now - lastLevelUpdateMs).coerceAtLeast(0L)
        lastLevelUpdateMs = now
        val drift = (elapsed.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        levelMin = min(level, (levelMin + drift).coerceIn(0f, 1f))
        levelMax = max(level, (levelMax - drift).coerceIn(0f, 1f))

        val range = (levelMax - levelMin).coerceAtLeast(0.05f)
        return ((level - levelMin) / range).coerceIn(0f, 1f)
    }

    private fun resetLevelScaleTracking() {
        levelMin = 0f
        levelMax = 1f
        lastLevelUpdateMs = 0L
    }

    private fun isLevelAutoScaleMode(): Boolean {
        return GlyphPatternRegistry.isLevelAutoScale(glyphMode)
    }

    private fun renderLightPattern(spec: DeviceSpec, level: Float) {
        val recipe = GlyphPatternRegistry.recipeFor(glyphMode)
            ?: GlyphPatternRegistry.recipeFor(GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice())
            ?: return
        val ranges = resolveLightRanges(spec, recipe.lightZones)

        if (spec.profile == GlyphDeviceProfile.PHONE4A) {
            updatePhone4aFrame(spec, level) { colors ->
                applyRecipeToColors(colors, ranges, recipe.renderMode, level)
            }
            return
        }

        when (recipe.renderMode) {
            GlyphPatternRenderMode.LINEAR -> updateLinearRanges(level, ranges)
            GlyphPatternRenderMode.CENTER -> {
                if (ranges.size <= 1) {
                    updateCenterRange(level, ranges.firstOrNull() ?: spec.cRange)
                } else {
                    updateCenterRanges(level, ranges)
                }
            }
            GlyphPatternRenderMode.SPECTRUM -> updateSpectrumRanges(level, ranges)
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> updateAllBrightness(level)
            else -> updateLinearRanges(level, ranges.ifEmpty { listOf(spec.cRange) })
        }
    }

    private fun resolveLightRanges(spec: DeviceSpec, zones: List<GlyphLightZone>): List<IntRange> {
        val resolved = zones.mapNotNull { zone ->
            when (zone) {
                GlyphLightZone.C -> spec.cRange
                GlyphLightZone.A -> spec.aRange
                GlyphLightZone.B -> spec.bRange
                GlyphLightZone.CAB -> spec.cabRange
                GlyphLightZone.D1 -> spec.d1Range
            }
        }
        return if (resolved.isEmpty()) listOf(spec.cRange) else resolved
    }

    private fun applyRecipeToColors(
        colors: IntArray,
        ranges: List<IntRange>,
        renderMode: GlyphPatternRenderMode,
        level: Float
    ) {
        when (renderMode) {
            GlyphPatternRenderMode.LINEAR -> ranges.forEach { applyLinearRange(colors, it, level) }
            GlyphPatternRenderMode.CENTER -> ranges.forEach { applyCenterRange(colors, it, level) }
            GlyphPatternRenderMode.SPECTRUM -> ranges.forEach { applySpectrumRange(colors, it, level) }
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> {
                val primaryRange = ranges.firstOrNull() ?: return
                applyAllBrightnessRange(colors, primaryRange, level)
            }
            else -> ranges.forEach { applyLinearRange(colors, it, level) }
        }
    }

    private fun updateLinearRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            turnOff()
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyLinearRange(colors, range, clamped)
        }
        glyphManager.setFrameColors(colors)
    }

    private fun applyLinearRange(colors: IntArray, range: IntRange, level: Float) {
        val count = range.count()
        if (count <= 0) return

        val virtualLit = level * count
        val fullLit = virtualLit.toInt().coerceIn(0, count)
        val edgeBrightness = if (binaryMode) 0 else ((virtualLit - fullLit) * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)

        val channels = if (shouldReverseLightOrder()) range.reversed().toList() else range.toList()
        channels.forEachIndexed { index, channel ->
            val brightness = when {
                index < fullLit -> MAX_LIGHT
                index == fullLit && fullLit < count -> edgeBrightness
                else -> 0
            }
            if (brightness > 0 && channel in colors.indices) {
                colors[channel] = brightness
            }
        }
    }

    private fun updateSpectrumRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            turnOff()
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applySpectrumRange(colors, range, clamped)
        }
        glyphManager.setFrameColors(colors)
    }

    private fun applySpectrumRange(colors: IntArray, range: IntRange, level: Float) {
        val channels = if (shouldReverseLightOrder()) range.reversed().toList() else range.toList()
        if (channels.isEmpty()) return

        val count = channels.size
        channels.forEachIndexed { index, channel ->
            if (channel !in colors.indices) return@forEachIndexed

            val position = if (count <= 1) 0f else index / (count - 1f)
            val bandValue = sampleSpectrumAt(position)
            val weighted = (bandValue * level).coerceIn(0f, 1f)
            val shaped = weighted.pow(outputGamma)
            val brightness = if (binaryMode) {
                if (shaped >= 0.5f) MAX_LIGHT else 0
            } else {
                (shaped * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
            }
            if (brightness > colors[channel]) {
                colors[channel] = brightness
            }
        }
    }

    private fun sampleSpectrumAt(position: Float): Float {
        val bands = spectrumBands
        if (bands.isNotEmpty()) {
            if (bands.size == 1) return bands[0].coerceIn(0f, 1f)
            val clampedPos = position.coerceIn(0f, 1f)
            val scaled = clampedPos * (bands.size - 1)
            val lo = scaled.toInt().coerceIn(0, bands.lastIndex)
            val hi = (lo + 1).coerceIn(0, bands.lastIndex)
            val t = scaled - lo
            val interpolated = (bands[lo] * (1f - t)) + (bands[hi] * t)
            return interpolated.coerceIn(0f, 1f)
        }

        return ((lowEnergy * (1f - position)) + (highEnergy * position)).coerceIn(0f, 1f)
    }

    private fun shouldReverseLightOrder(): Boolean {
        return when (deviceSpec?.profile) {
            GlyphDeviceProfile.PHONE4A -> !reverseDirection
            else -> reverseDirection
        }
    }

    private fun updateAllBrightness(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= ALL_BRIGHTNESS_OFF_THRESHOLD) {
            turnOff()
            return
        }
        val normalizedRaw = if (allBrightnessAutoScaleEnabled) {
            normalizeAllBrightnessLevel(clamped)
        } else clamped
        if (normalizedRaw <= ALL_BRIGHTNESS_OFF_THRESHOLD) {
            turnOff()
            return
        }
        val normalized = ((normalizedRaw - ALL_BRIGHTNESS_OFF_THRESHOLD) / (1f - ALL_BRIGHTNESS_OFF_THRESHOLD))
            .coerceIn(0f, 1f)
        val shaped = normalized.pow(outputGamma)
        val brightness = if (binaryMode) {
            MAX_LIGHT
        } else {
            (ALL_BRIGHTNESS_MIN_LIGHT + ((MAX_LIGHT - ALL_BRIGHTNESS_MIN_LIGHT) * shaped)).roundToInt()
        }
        fullGlyphBrightness.fill(brightness)
        glyphManager.setFrameColors(fullGlyphBrightness)
    }

    private fun applyAllBrightnessRange(colors: IntArray, range: IntRange, level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= ALL_BRIGHTNESS_OFF_THRESHOLD) return
        val normalizedRaw = if (allBrightnessAutoScaleEnabled) {
            normalizeAllBrightnessLevel(clamped)
        } else clamped
        if (normalizedRaw <= ALL_BRIGHTNESS_OFF_THRESHOLD) return
        val normalized = ((normalizedRaw - ALL_BRIGHTNESS_OFF_THRESHOLD) / (1f - ALL_BRIGHTNESS_OFF_THRESHOLD))
            .coerceIn(0f, 1f)
        val shaped = normalized.pow(outputGamma)
        val brightness = if (binaryMode) {
            MAX_LIGHT
        } else {
            (ALL_BRIGHTNESS_MIN_LIGHT + ((MAX_LIGHT - ALL_BRIGHTNESS_MIN_LIGHT) * shaped)).roundToInt()
        }
        for (channel in range) {
            if (channel in colors.indices) {
                colors[channel] = brightness
            }
        }
    }

    private fun normalizeAllBrightnessLevel(level: Float): Float {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastAllBrightnessUpdateMs <= 0L) 0L else (now - lastAllBrightnessUpdateMs).coerceAtLeast(0L)
        lastAllBrightnessUpdateMs = now
        val drift = (elapsed.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        allBrightnessMin = min(level, (allBrightnessMin + drift).coerceIn(0f, 1f))
        allBrightnessMax = max(level, (allBrightnessMax - drift).coerceIn(0f, 1f))

        val range = (allBrightnessMax - allBrightnessMin).coerceAtLeast(0.05f)
        return ((level - allBrightnessMin) / range).coerceIn(0f, 1f)
    }

    private fun resetAllBrightnessScaleTracking() {
        allBrightnessMin = 0f
        allBrightnessMax = 1f
        lastAllBrightnessUpdateMs = 0L
    }

    private fun updateCenterRange(level: Float, channelRange: IntRange) {
        val spec = deviceSpec ?: return
        if (!spec.centerSupported || channelRange.count() < 2) {
            updateLinearRanges(level, listOf(channelRange))
            return
        }

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            turnOff()
            return
        }

        if (spec.profile == GlyphDeviceProfile.PHONE2 && 29 in channelRange && channelRange.count() == 8) {
            val centerChannel = 29
            val leftChannels = (channelRange.first until centerChannel).toList().asReversed()
            val rightChannels = ((centerChannel + 1)..channelRange.last).toList()
            val pairCount = min(leftChannels.size, rightChannels.size)
            val totalSlots = 1 + pairCount
            val virtualSlots = clamped * totalSlots
            val fullSlots = virtualSlots.toInt().coerceIn(0, totalSlots)
            val edgeBrightness = if (binaryMode) {
                0
            } else {
                ((virtualSlots - fullSlots) * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
            }

            val colors = IntArray(spec.channelCount)
            val centerBrightness = if (fullSlots >= 1) MAX_LIGHT else edgeBrightness
            if (centerBrightness > 0 && centerChannel in colors.indices) {
                colors[centerChannel] = centerBrightness
            }

            for (pair in 1..pairCount) {
                val brightness = when {
                    pair < fullSlots -> MAX_LIGHT
                    pair == fullSlots -> edgeBrightness
                    else -> 0
                }
                if (brightness > 0) {
                    val leftChannel = leftChannels[pair - 1]
                    val rightChannel = rightChannels[pair - 1]
                    if (leftChannel in colors.indices) colors[leftChannel] = brightness
                    if (rightChannel in colors.indices) colors[rightChannel] = brightness
                }
            }

            glyphManager.setFrameColors(colors)
            return
        }

        val pairCount = channelRange.count() / 2
        val centerLeft = channelRange.first + pairCount - 1
        val centerRight = centerLeft + 1

        val virtualPairs = clamped * pairCount
        val fullPairs = virtualPairs.toInt()
        val edgeBrightness = if (binaryMode) 0 else ((virtualPairs - fullPairs) * MAX_LIGHT).roundToInt()

        val colors = IntArray(spec.channelCount)
        for (pair in 1..pairCount) {
            val brightness = when {
                pair <= fullPairs -> MAX_LIGHT
                pair == fullPairs + 1 -> edgeBrightness
                else -> 0
            }
            if (brightness > 0) {
                val leftChannel = centerLeft - (pair - 1)
                val rightChannel = centerRight + (pair - 1)
                if (leftChannel in channelRange) colors[leftChannel] = brightness
                if (rightChannel in channelRange) colors[rightChannel] = brightness
            }
        }
        glyphManager.setFrameColors(colors)
    }

    private fun updateCenterRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            turnOff()
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyCenterRange(colors, range, clamped)
        }
        glyphManager.setFrameColors(colors)
    }

    private fun applyCenterRange(colors: IntArray, channelRange: IntRange, level: Float) {
        val count = channelRange.count()
        if (count < 1) return

        val channels = channelRange.toList()

        if (count % 2 == 1) {
            // 奇数: 中心1点 + 両側ペア展開
            // B(5ch): B3が中心 → (B2,B4) → (B1,B5)
            // A(11ch): A6が中心 → (A5,A7) → ... → (A1,A11)
            val centerIdx = count / 2
            val pairCount = count / 2
            val totalSlots = 1 + pairCount

            val virtualSlots = level * totalSlots
            val fullSlots = virtualSlots.toInt()
            val edgeBrightness = if (binaryMode) 0
                else ((virtualSlots - fullSlots) * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)

            // スロット0: 中心
            val centerBrightness = if (fullSlots >= 1) MAX_LIGHT else edgeBrightness
            if (centerBrightness > 0 && channels[centerIdx] in colors.indices) {
                colors[channels[centerIdx]] = centerBrightness
            }

            // スロット1..pairCount: ペア展開
            for (pair in 1..pairCount) {
                val brightness = when {
                    pair < fullSlots -> MAX_LIGHT
                    pair == fullSlots -> edgeBrightness
                    else -> 0
                }
                if (brightness > 0) {
                    val left = channels[centerIdx - pair]
                    val right = channels[centerIdx + pair]
                    if (left in colors.indices) colors[left] = brightness
                    if (right in colors.indices) colors[right] = brightness
                }
            }
        } else {
            // 偶数: 中心ペア + 両側展開
            val pairCount = count / 2
            val centerLeft = channelRange.first + pairCount - 1
            val centerRight = centerLeft + 1
            val virtualPairs = level * pairCount
            val fullPairs = virtualPairs.toInt()
            val edgeBrightness = if (binaryMode) 0
                else ((virtualPairs - fullPairs) * MAX_LIGHT).roundToInt()

            for (pair in 1..pairCount) {
                val brightness = when {
                    pair <= fullPairs -> MAX_LIGHT
                    pair == fullPairs + 1 -> edgeBrightness
                    else -> 0
                }
                if (brightness > 0) {
                    val leftChannel = centerLeft - (pair - 1)
                    val rightChannel = centerRight + (pair - 1)
                    if (leftChannel in channelRange && leftChannel in colors.indices) {
                        colors[leftChannel] = brightness
                    }
                    if (rightChannel in channelRange && rightChannel in colors.indices) {
                        colors[rightChannel] = brightness
                    }
                }
            }
        }
    }

    override fun turnOff() {
        lastPreviewLevel = 0f
        resetBaseIndicators()
        silenceStartedAt = 0L
        sessionReleasedForSilence = false
        try {
            glyphManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff ignored because session is not ready", error)
        }
    }

    private fun updatePhone4aFrame(
        spec: DeviceSpec,
        level: Float,
        populateMain: (IntArray) -> Unit
    ) {
        val colors = IntArray(spec.channelCount)
        populateMain(colors)
        if (baseIndicatorEnabled) {
            applyBaseIndicator(spec.profile, colors)
        }
        if (colors.none { it > 0 }) {
            turnOff()
        } else {
            glyphManager.setFrameColors(colors)
        }
    }

    private fun updateBaseIndicatorAnalysis(level: Float) {
        val profile = deviceSpec?.profile ?: return
        val rawLevel = level.coerceIn(0f, 1f)
        val gatedLevel = if (rawLevel <= PHONE4A_BASE_INDICATOR_RAW_GATE) {
            0f
        } else {
            ((rawLevel - PHONE4A_BASE_INDICATOR_RAW_GATE) * PHONE4A_BASE_INDICATOR_RAW_GAIN)
                .coerceIn(0f, 1f)
        }
        baseIndicatorRenderers
            .firstOrNull { it.accepts(profile) }
            ?.updateAnalysis(normalizeBaseIndicatorLevel(gatedLevel))
    }

    private fun applyBaseIndicator(profile: GlyphDeviceProfile, colors: IntArray) {
        baseIndicatorRenderers
            .firstOrNull { it.accepts(profile) }
            ?.apply(colors, binaryMode, outputGamma)
    }

    private fun resetBaseIndicators() {
        baseIndicatorRenderers.forEach { it.reset() }
        resetBaseIndicatorTracking()
    }

    private fun normalizeBaseIndicatorLevel(level: Float): Float {
        if (!isBaseIndicatorAutoScaleEnabled()) return level

        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastBaseIndicatorUpdateMs <= 0L) 0L else {
            (now - lastBaseIndicatorUpdateMs).coerceAtLeast(0L)
        }
        lastBaseIndicatorUpdateMs = now
        val drift = (elapsed.toFloat() / autoScaleWindowMs).coerceIn(0f, 1f)

        baseIndicatorMin = min(level, (baseIndicatorMin + drift).coerceIn(0f, 1f))
        baseIndicatorMax = max(level, (baseIndicatorMax - drift).coerceIn(0f, 1f))

        val range = (baseIndicatorMax - baseIndicatorMin).coerceAtLeast(0.05f)
        return ((level - baseIndicatorMin) / range).coerceIn(0f, 1f)
    }

    private fun isBaseIndicatorAutoScaleEnabled(): Boolean {
        return when (GlyphPatternRegistry.kindOf(glyphMode)) {
            GlyphPatternKind.SPECTRUM -> spectrumAutoScaleEnabled
            GlyphPatternKind.ALL_BRIGHTNESS -> allBrightnessAutoScaleEnabled
            GlyphPatternKind.LINEAR,
            GlyphPatternKind.CENTER,
            GlyphPatternKind.MATRIX_BAR,
            GlyphPatternKind.MATRIX_FIELD,
            GlyphPatternKind.MATRIX_CIRCLE -> levelAutoScaleEnabled
            null -> false
        }
    }

    private fun resetBaseIndicatorTracking() {
        baseIndicatorMin = 0f
        baseIndicatorMax = 1f
        lastBaseIndicatorUpdateMs = 0L
    }

    override fun previewLevel(): Float = lastPreviewLevel.coerceIn(0f, 1f)

    override fun previewSpectrumBands(): FloatArray = spectrumBands.copyOf()

    private fun releaseSessionForSilence() {
        try {
            glyphManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff during silence release failed", error)
        }
        closeSession()
        sessionReleasedForSilence = true
    }

    private fun reopenSessionAfterSilence(): Boolean {
        val spec = deviceSpec ?: return false
        return try {
            val registered = glyphManager.register(spec.deviceId)
            if (!registered) {
                onStatusChanged(context.getString(R.string.status_glyph_resume_failed))
                false
            } else {
                glyphManager.openSession()
                isSessionOpen = true
                sessionReleasedForSilence = false
                true
            }
        } catch (error: Throwable) {
            AppLogger.w(TAG, "reopenSessionAfterSilence failed", error)
            false
        }
    }

    private fun closeSession() {
        if (!isSessionOpen) return
        try {
            glyphManager.closeSession()
        } catch (_: GlyphException) {
        } finally {
            isSessionOpen = false
        }
    }

    private fun resolveDeviceSpec(): DeviceSpec? {
        val currentDevice = GlyphDeviceCatalog.currentOrNull() ?: return null
        val lightSpec = currentDevice.lightSpec ?: return null
        return DeviceSpec(
            profile = currentDevice.profile,
            deviceId = lightSpec.sdkDeviceId,
            channelCount = lightSpec.channelCount,
            cRange = lightSpec.cRange,
            aRange = lightSpec.aRange,
            bRange = lightSpec.bRange,
            cabRange = lightSpec.cabRange,
            d1Range = lightSpec.d1Range,
            centerSupported = lightSpec.centerSupported
        )
    }
}
