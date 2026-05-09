package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry as Patterns
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
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

        private val MODE_C1_LINEAR = Patterns.P2_C1_LINEAR
        private val MODE_C1_CENTER = Patterns.P2_C1_CENTER
        private val MODE_D1 = Patterns.P2_D1_LINEAR
        private val MODE_D1_CENTER = Patterns.P2_D1_CENTER
        private val MODE_C1_SPECTRUM = Patterns.P2_C1_SPECTRUM
        private val MODE_D1_SPECTRUM = Patterns.P2_D1_SPECTRUM
        private val MODE_ALL_BRIGHTNESS = Patterns.P2_ALL_BRIGHTNESS

        private val MODE_P3A_C_LINEAR = Patterns.P3A_C_LINEAR
        private val MODE_P3A_C_CENTER = Patterns.P3A_C_CENTER
        private val MODE_P3A_C_SPECTRUM = Patterns.P3A_C_SPECTRUM
        private val MODE_P3A_CAB_LINEAR = Patterns.P3A_CAB_LINEAR
        private val MODE_P3A_CAB_CENTER = Patterns.P3A_CAB_CENTER
        private val MODE_P3A_CAB_SPECTRUM = Patterns.P3A_CAB_SPECTRUM
        private val MODE_P3A_ALL_BRIGHTNESS = Patterns.P3A_ALL_BRIGHTNESS

        private val MODE_P2A_C_LINEAR = Patterns.P2A_C_LINEAR
        private val MODE_P2A_C_CENTER = Patterns.P2A_C_CENTER
        private val MODE_P2A_C_SPECTRUM = Patterns.P2A_C_SPECTRUM
        private val MODE_P2A_ALL_BRIGHTNESS = Patterns.P2A_ALL_BRIGHTNESS

        private val MODE_P4A_LINEAR = Patterns.P4A_LINEAR
        private val MODE_P4A_CENTER = Patterns.P4A_CENTER
        private val MODE_P4A_SPECTRUM = Patterns.P4A_SPECTRUM
        private val MODE_P4A_ALL_BRIGHTNESS = Patterns.P4A_ALL_BRIGHTNESS
    }

    private enum class DeviceProfile {
        PHONE2,
        PHONE2A,
        PHONE3A,
        PHONE4A
    }

    private data class DeviceSpec(
        val profile: DeviceProfile,
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
    private var glyphMode = MODE_C1_LINEAR
    private var binaryMode = false
    private var outputGamma = ALL_BRIGHTNESS_RESPONSE_GAMMA
    private var levelAutoScaleEnabled = false
    private var spectrumAutoScaleEnabled = false
    private var allBrightnessAutoScaleEnabled = false
    private var autoScaleWindowMs = DEFAULT_AUTO_SCALE_WINDOW_MS
    private var levelMin = 0f
    private var levelMax = 1f
    private var lastLevelUpdateMs = 0L
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
        }
    }

    override fun setBinaryMode(binary: Boolean) {
        binaryMode = binary
    }

    override fun setOutputGamma(gamma: Float) {
        outputGamma = gamma.coerceIn(0.6f, 2.6f)
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
        val nextWindowMs = seconds.coerceIn(10f, 60f) * 1_000f
        if (autoScaleWindowMs != nextWindowMs) {
            autoScaleWindowMs = nextWindowMs
        }
    }

    override fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?
    ) {
        this.lowEnergy = lowEnergy.coerceIn(0f, 1f)
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

        val spec = deviceSpec ?: return
        val cFrame = cLinearFrame ?: return

        try {
            when (spec.profile) {
                DeviceProfile.PHONE2 -> updatePhone2Level(spec, cFrame, renderLevel)
                DeviceProfile.PHONE2A -> updatePhone2aLevel(spec, cFrame, renderLevel)
                DeviceProfile.PHONE3A -> updatePhone3aLevel(spec, cFrame, renderLevel)
                DeviceProfile.PHONE4A -> updatePhone4aLevel(spec, cFrame, renderLevel)
            }
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
        return Patterns.isLevelAutoScale(glyphMode)
    }

    private fun updatePhone2aLevel(spec: DeviceSpec, cFrame: GlyphFrame, level: Float) {
        when (glyphMode) {
            MODE_P2A_C_CENTER, MODE_C1_CENTER -> updateCenterRange(level, spec.cRange)
            MODE_P2A_C_SPECTRUM, MODE_C1_SPECTRUM -> updateSpectrumRanges(level, listOf(spec.cRange))
            MODE_P2A_ALL_BRIGHTNESS, MODE_ALL_BRIGHTNESS -> updateAllBrightness(level)
            MODE_P2A_C_LINEAR, MODE_C1_LINEAR -> displayLinear(level, cFrame, spec.cRange.count())
            else -> displayLinear(level, cFrame, spec.cRange.count())
        }
    }

    private fun updatePhone2Level(spec: DeviceSpec, cFrame: GlyphFrame, level: Float) {
        when (glyphMode) {
            MODE_ALL_BRIGHTNESS -> updateAllBrightness(level)
            MODE_C1_CENTER -> updateCenterRange(level, spec.cRange)
            MODE_D1_CENTER -> updateCenterRange(level, spec.d1Range ?: spec.cRange)
            MODE_C1_SPECTRUM -> updateSpectrumRanges(level, listOf(spec.cRange))
            MODE_D1 -> {
                val d1Range = spec.d1Range
                if (d1Range != null) {
                    updateLinearRanges(level, listOf(d1Range))
                } else {
                    displayLinear(level, cFrame, spec.cRange.count())
                }
            }
            MODE_D1_SPECTRUM -> {
                val range = spec.d1Range ?: spec.cRange
                updateSpectrumRanges(level, listOf(range))
            }
            else -> displayLinear(level, cFrame, spec.cRange.count())
        }
    }

    private fun updatePhone3aLevel(spec: DeviceSpec, cFrame: GlyphFrame, level: Float) {
        val cabRange = spec.cabRange ?: spec.cRange
        val cabFrame = cabLinearFrame ?: cFrame
        val abcRanges = listOfNotNull(spec.cRange, spec.aRange, spec.bRange)

        when (glyphMode) {
            MODE_P3A_C_CENTER -> updateCenterRange(level, spec.cRange)
            MODE_P3A_C_SPECTRUM -> updateSpectrumRanges(level, listOf(spec.cRange))
            MODE_P3A_CAB_LINEAR -> updateLinearRanges(level, abcRanges)
            MODE_P3A_CAB_CENTER -> updateCenterRanges(level, abcRanges)
            MODE_P3A_CAB_SPECTRUM -> updateSpectrumRanges(level, abcRanges)
            MODE_P3A_ALL_BRIGHTNESS, MODE_ALL_BRIGHTNESS -> updateAllBrightness(level)

            // Backward compatibility for old saved modes.
            MODE_C1_CENTER -> updateCenterRange(level, spec.cRange)
            MODE_D1 -> displayLinear(level, cabFrame, cabRange.count())

            MODE_P3A_C_LINEAR, MODE_C1_LINEAR -> displayLinear(level, cFrame, spec.cRange.count())
            else -> displayLinear(level, cFrame, spec.cRange.count())
        }
    }

    private fun updatePhone4aLevel(spec: DeviceSpec, cFrame: GlyphFrame, level: Float) {
        when (glyphMode) {
            MODE_P4A_CENTER -> updateCenterRange(level, spec.cRange)
            MODE_P4A_SPECTRUM -> updateSpectrumRanges(level, listOf(spec.cRange))
            MODE_P4A_ALL_BRIGHTNESS -> updateAllBrightness(level)
            MODE_P4A_LINEAR -> displayLinear(level, cFrame, spec.cRange.count())
            else -> displayLinear(level, cFrame, spec.cRange.count())
        }
    }

    private fun displayLinear(level: Float, frame: GlyphFrame, segments: Int) {
        val progress = (level.coerceIn(0f, 1f) * 100).toInt()
        glyphManager.displayProgress(
            frame,
            if (binaryMode) binarySnap(level, segments) else progress,
            reverseDirection
        )
    }

    private fun binarySnap(level: Float, segments: Int): Int {
        if (segments <= 0) return 0
        val fullSegs = (level.coerceIn(0f, 1f) * segments).toInt()
        return (fullSegs * 100f / segments).toInt()
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

        val channels = if (reverseDirection) range.reversed().toList() else range.toList()
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
        val channels = if (reverseDirection) range.reversed().toList() else range.toList()
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
            cLinearFrame?.let { displayLinear(level, it, spec.cRange.count()) }
            return
        }

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            turnOff()
            return
        }

        if (spec.profile == DeviceProfile.PHONE2 && glyphMode == MODE_D1_CENTER && 29 in channelRange) {
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
        silenceStartedAt = 0L
        sessionReleasedForSilence = false
        try {
            glyphManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff ignored because session is not ready", error)
        }
    }

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
        return when {
            Common.is23111() || Common.is23113() -> DeviceSpec(
                profile = DeviceProfile.PHONE2A,
                deviceId = if (Common.is23113()) Glyph.DEVICE_23113 else Glyph.DEVICE_23111,
                channelCount = 26,
                cRange = 0..23,
                aRange = 24..24,
                bRange = 25..25,
                centerSupported = true
            )
            Common.is22111() -> DeviceSpec(
                profile = DeviceProfile.PHONE2,
                deviceId = Glyph.DEVICE_22111,
                channelCount = 33,
                cRange = 3..18,
                d1Range = 25..32,
                centerSupported = true
            )
            Common.is24111() -> DeviceSpec(
                profile = DeviceProfile.PHONE3A,
                deviceId = Glyph.DEVICE_24111,
                channelCount = 36,
                cRange = 0..19,
                aRange = 20..30,
                bRange = 31..35,
                cabRange = 0..35,
                centerSupported = true
            )
            Common.is25111() -> DeviceSpec(
                profile = DeviceProfile.PHONE4A,
                deviceId = Glyph.DEVICE_25111,
                channelCount = 6,
                cRange = 0..5,
                centerSupported = true
            )
            else -> null
        }
    }
}
