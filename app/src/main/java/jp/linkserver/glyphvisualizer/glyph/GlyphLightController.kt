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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class GlyphLightController(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit,
    initialPhone4bEmulationEnabled: Boolean = false,
    private val previewDeviceProfile: GlyphDeviceProfile? = null,
    private val previewFrameListener: ((GlyphPreviewFrame.Lights) -> Unit)? = null
) : GlyphOutputController {
    companion object {
        private const val TAG = "GlyphLightController"
        private const val MIN_LIGHT = 800
        private const val MAX_LIGHT = 4095
        private const val SILENCE_RELEASE_MS = 3_000L
        private const val SILENCE_ACTIVITY_THRESHOLD = 0.003f
        private const val DEFAULT_AUTO_SCALE_WINDOW_MS = 30_000f
        private const val ALL_BRIGHTNESS_ON_THRESHOLD = 0.075f
        private const val ALL_BRIGHTNESS_OFF_THRESHOLD = 0.045f
        private const val ALL_BRIGHTNESS_CURVE_FLOOR = 0.06f
        private const val ALL_BRIGHTNESS_MIN_LIGHT = 240
        private const val ALL_BRIGHTNESS_RESPONSE_GAMMA = 1.8f
        private const val LINEAR_PEAK_BASE_BRIGHTNESS_RATIO = 0.34f
        private const val LINEAR_PEAK_FALLOFF_PER_SECOND = 1.25f
        private const val PULSE_TRAIN_BASE_BRIGHTNESS_RATIO = 0.28f
        private const val PULSE_TRAIN_TAIL_LENGTH = 0.24f
        private const val PULSE_TRAIN_MIN_TRIGGER_LEVEL = 0.18f
        private const val PULSE_TRAIN_TRIGGER_DELTA = 0.08f
        private const val PULSE_TRAIN_SPEED_PER_SECOND = 1.8f
        private const val PULSE_TRAIN_BRIGHTNESS_FALLOFF = 0.42f
        private const val SPECTRUM_MARKER_RESPONSE_PER_SECOND = 9f
        private const val SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS = 0.8f
        private const val SPECTRUM_MARKER_PHONE4A_MAX_RADIUS_SEGMENTS = 2.5f
        private const val SPECTRUM_MARKER_PHONE4B_MAX_RADIUS_SEGMENTS = 2f
        private const val SPECTRUM_MARKER_RADIUS_ATTACK_PER_SECOND = 8f
        private const val SPECTRUM_MARKER_RADIUS_RELEASE_PER_SECOND = 3.5f
        private const val SPECTRUM_MARKER_RADIUS_LEVEL_EXPONENT = 0.65f
        private const val SPECTRUM_MARKER_EDGE_PADDING_SEGMENTS = 2f
        private const val SPECTRUM_MARKER_MAX_STEP_SEGMENTS = 2f
        private const val SPECTRUM_MARKER_PEAK_WINDOW = 2
        private const val SPECTRUM_MARKER_MIN_PEAK = 0.001f
        private const val PHONE4A_BASE_INDICATOR_EPSILON = 0.000001f
        private const val PHONE4A_BASE_INDICATOR_GAMMA = 2.0f
        private const val PHONE4A_BASE_INDICATOR_DECAY = 0.90f
        private const val PHONE4A_BASE_INDICATOR_PEAK_FALLOFF = 0.9995f
        private const val PHONE4A_RECORDING_LIGHT_CHANNEL = 6
        private const val PHONE4B_RECORDING_LIGHT_CHANNEL = 4
    }

    private data class TravelingPulse(
        var position: Float,
        var brightness: Float
    )

    private interface BaseIndicatorRenderer {
        fun accepts(profile: GlyphDeviceProfile): Boolean
        fun setSmoothing(smoothing: Float, smoothingBalance: Float)
        fun updateAnalysis(level: Float)
        fun reset()
        fun apply(colors: IntArray, binaryMode: Boolean, outputGamma: Float)
    }

    private class Phone4SeriesBaseIndicatorRenderer : BaseIndicatorRenderer {
        private var bandLevel = 0f
        private var decayedBandLevel = 0f
        private var zonePeak = PHONE4A_BASE_INDICATOR_EPSILON

        override fun accepts(profile: GlyphDeviceProfile): Boolean {
            return profile == GlyphDeviceProfile.PHONE4A ||
                profile == GlyphDeviceProfile.PHONE4B
        }

        override fun setSmoothing(smoothing: Float, smoothingBalance: Float) = Unit

        override fun updateAnalysis(level: Float) {
            bandLevel = level.coerceIn(0f, 1f)
        }

        override fun reset() {
            bandLevel = 0f
            decayedBandLevel = 0f
            zonePeak = PHONE4A_BASE_INDICATOR_EPSILON
        }

        override fun apply(colors: IntArray, binaryMode: Boolean, outputGamma: Float) {
            if (colors.isEmpty()) return

            val current = bandLevel.coerceIn(0f, 1f)
            val risen = max(decayedBandLevel, current)
            decayedBandLevel =
                (PHONE4A_BASE_INDICATOR_DECAY * risen) +
                    ((1f - PHONE4A_BASE_INDICATOR_DECAY) * current)
            if (decayedBandLevel < PHONE4A_BASE_INDICATOR_EPSILON) {
                decayedBandLevel = 0f
            }

            zonePeak = max(
                decayedBandLevel,
                zonePeak * PHONE4A_BASE_INDICATOR_PEAK_FALLOFF
            ).coerceAtLeast(PHONE4A_BASE_INDICATOR_EPSILON)
            val normalized = (decayedBandLevel / zonePeak).coerceIn(0f, 1f)
            val shaped = normalized * normalized
            val gammaMapped = shaped.pow(PHONE4A_BASE_INDICATOR_GAMMA)
            val brightness = if (binaryMode) {
                if (gammaMapped >= 0.5f) MAX_LIGHT else 0
            } else {
                if (gammaMapped <= PHONE4A_BASE_INDICATOR_EPSILON) {
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
        val physicalProfile: GlyphDeviceProfile,
        val deviceId: String,
        val channelCount: Int,
        val cRange: IntRange,
        val recordingLightChannel: Int? = null,
        val aRange: IntRange? = null,
        val bRange: IntRange? = null,
        val cabRange: IntRange? = null,
        val d1Range: IntRange? = null,
        val d1CenterChannel: Int? = null,
        val centerSupported: Boolean = false
    )

    private val glyphManager = GlyphManager.getInstance(context.applicationContext)
    private var isBound = false
    private var isSessionOpen = false
    private var reverseDirection = false
    private var glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice()
    private var fillOtherGlyphLightsEnabled = false
    private var binaryMode = false
    private var baseIndicatorEnabled = false
    private var recordingLightIncluded = false
    private var phone4bEmulationEnabled = initialPhone4bEmulationEnabled &&
        GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE4A
    private var outputGamma = ALL_BRIGHTNESS_RESPONSE_GAMMA
    private var levelAutoScaleEnabled = false
    private var spectrumAutoScaleEnabled = false
    private var allBrightnessAutoScaleEnabled = false
    private var smoothing = 0.45f
    private var smoothingBalance = 0f
    private var autoScaleWindowMs = DEFAULT_AUTO_SCALE_WINDOW_MS
    private var autoScaleOffset = 0f
    private var autoScaleStrategy = GlyphAutoScaleStrategy.LEGACY
    private var visualTuningOverride: GlyphVisualTuning? = null
    private val legacyLevelAutoScale = LegacyAutoScaleController()
    private val legacySpectrumAutoScale = LegacySpectrumAutoScaleController()
    private val legacyAllBrightnessAutoScale = LegacyAutoScaleController()
    private val levelAutoGain = AutoGainController()
    private val spectrumAutoGain = AutoGainController()
    private val allBrightnessAutoGain = AutoGainController()
    private var allBrightnessGateOn = false
    private var linearPeakLevel = 0f
    private var lastLinearPeakUpdateMs = 0L
    private val pulseTrainPulses = mutableListOf<TravelingPulse>()
    private var lastPulseTrainUpdateMs = 0L
    private var lastPulseTrainTriggerLevel = 0f
    private var lastPreviewLevel = 0f
    private var baseIndicatorMin = 0f
    private var baseIndicatorMax = 1f
    private var lastBaseIndicatorUpdateMs = 0L
    private var lowEnergy = 0f
    private var highEnergy = 0f
    private var spectrumBands = FloatArray(0)
    private var rawSpectrumPeak = 0f
    private var smoothedSpectrumBands = FloatArray(0)
    private var spectrumMarkerPosition: Float? = null
    private var lastSpectrumMarkerUpdateMs = 0L
    private var spectrumMarkerRadiusSegments = SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS
    private var lastSpectrumMarkerRadiusUpdateMs = 0L
    private var silenceStartedAt = 0L
    private var sessionReleasedForSilence = false

    private var deviceSpec: DeviceSpec? = null
    private var cLinearFrame: GlyphFrame? = null
    private var cabLinearFrame: GlyphFrame? = null
    private var aLinearFrame: GlyphFrame? = null
    private var bLinearFrame: GlyphFrame? = null
    private var d1Frame: GlyphFrame? = null
    private var fullGlyphBrightness = IntArray(0)
    private var lastSentFrame = IntArray(0)
    private var blankFrame = IntArray(0)
    private val baseIndicatorRenderers: List<BaseIndicatorRenderer> = listOf(
        Phone4SeriesBaseIndicatorRenderer()
    )
    // SDK 接続待ちの間に届いたレベルを保持し、接続後に再送する
    @Volatile private var pendingLevel: Float = -1f

    init {
        previewDeviceProfile?.let { profile ->
            val spec = resolvePreviewDeviceSpec(profile)
                ?: error("Lights preview is unavailable for $profile")
            deviceSpec = spec
            fullGlyphBrightness = IntArray(spec.channelCount)
            isSessionOpen = true
            isBound = true
        }
    }

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            invalidateLastSentFrame()
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
            invalidateLastSentFrame()
            isSessionOpen = false
            silenceStartedAt = 0L
            sessionReleasedForSilence = false
            onStatusChanged(context.getString(R.string.status_glyph_service_disconnected))
        }
    }

    override fun bind() {
        if (previewDeviceProfile != null) return
        if (isBound) return
        isBound = true
        glyphManager.init(callback)
        onStatusChanged(context.getString(R.string.status_glyph_service_connecting))
    }

    override fun unbind() {
        if (previewDeviceProfile != null) {
            turnOff()
            return
        }
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
            resetLinearPeakTracking()
            resetPulseTrainTracking()
            resetBaseIndicatorTracking()
        }
    }

    override fun setFillOtherGlyphLightsEnabled(enabled: Boolean) {
        fillOtherGlyphLightsEnabled = enabled
    }

    override fun setBinaryMode(binary: Boolean) {
        binaryMode = binary
    }

    override fun setBaseIndicatorEnabled(enabled: Boolean) {
        baseIndicatorEnabled = enabled
        clearPhone4bRecordingLightIfUnused()
    }

    override fun setRecordingLightIncluded(enabled: Boolean) {
        if (recordingLightIncluded != enabled) {
            resetSpectrumMarkerTracking()
        }
        recordingLightIncluded = enabled
        clearPhone4bRecordingLightIfUnused()
    }

    override fun setPhone4bEmulationEnabled(enabled: Boolean) {
        val nextEnabled = enabled &&
            GlyphDeviceCatalog.currentProfile() == GlyphDeviceProfile.PHONE4A
        if (phone4bEmulationEnabled == nextEnabled) return

        val shouldRebind = isBound
        if (shouldRebind) {
            turnOff()
            closeSession()
            runCatching { glyphManager.unInit() }
            isBound = false
        }
        invalidateLastSentFrame()
        phone4bEmulationEnabled = nextEnabled
        resetSpectrumMarkerTracking()
        deviceSpec = null
        cLinearFrame = null
        cabLinearFrame = null
        aLinearFrame = null
        bLinearFrame = null
        d1Frame = null
        fullGlyphBrightness = IntArray(0)
        if (shouldRebind) bind()
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

    override fun setAutoScaleStrategy(strategy: GlyphAutoScaleStrategy) {
        if (autoScaleStrategy != strategy) {
            autoScaleStrategy = strategy
            resetLevelScaleTracking()
            resetSpectrumScaleTracking()
            resetAllBrightnessScaleTracking()
        }
    }

    override fun setVisualTuningOverride(tuning: GlyphVisualTuning?) {
        visualTuningOverride = tuning
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
        val nextWindowMs = seconds.coerceIn(5f, 60f) * 1_000f
        if (autoScaleWindowMs != nextWindowMs) {
            autoScaleWindowMs = nextWindowMs
            resetBaseIndicatorTracking()
        }
    }

    override fun setAutoScaleOffset(offset: Float) {
        val nextOffset = offset.coerceIn(0f, 0.4f)
        if (autoScaleOffset != nextOffset) {
            autoScaleOffset = nextOffset
            resetLevelScaleTracking()
            resetSpectrumScaleTracking()
            resetAllBrightnessScaleTracking()
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
        phone4aBaseBandLevel: Float,
        waveformSamples: FloatArray?,
        leftWaveformSamples: FloatArray?,
        rightWaveformSamples: FloatArray?
    ) {
        this.lowEnergy = lowEnergy.coerceIn(0f, 1f)
        updateBaseIndicatorAnalysis(phone4aBaseBandLevel)
        this.highEnergy = highEnergy.coerceIn(0f, 1f)
        val raw = spectrumBands ?: FloatArray(0)
        rawSpectrumPeak = raw.maxOrNull()?.coerceIn(0f, 1f) ?: 0f
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
        val framePeak = input.maxOrNull()?.coerceIn(0f, 1f) ?: 0f
        if (!spectrumAutoScaleEnabled) return input

        val now = SystemClock.elapsedRealtime()
        if (autoScaleStrategy == GlyphAutoScaleStrategy.LEGACY) {
            return legacySpectrumAutoScale.update(
                input = input,
                nowMs = now,
                windowMs = autoScaleWindowMs,
                offset = autoScaleOffset
            )
        }

        val gain = spectrumAutoGain.update(
            referenceRaw = framePeak,
            nowMs = now,
            targetLevel = effectiveAutoScaleTargetLevel(autoScaleOffset),
            gainUpTauSeconds = autoScaleWindowMs / 1_000f,
            holdGainIncrease = framePeak < SILENCE_ACTIVITY_THRESHOLD
        )

        val profile = currentTuningProfile()
        val patternKind = GlyphPatternRegistry.kindOf(glyphMode)
        return FloatArray(input.size) { index ->
            applyAutoScaleVisualTuning(
                value = input[index].coerceIn(0f, 1f) * gain,
                autoScaleEnabled = spectrumAutoScaleEnabled,
                strategy = autoScaleStrategy,
                profile = profile,
                patternKind = patternKind,
                override = visualTuningOverride
            )
        }
    }

    private fun resetSpectrumScaleTracking() {
        legacySpectrumAutoScale.reset()
        spectrumAutoGain.reset()
        rawSpectrumPeak = 0f
        smoothedSpectrumBands = FloatArray(0)
        resetSpectrumMarkerTracking()
        resetAllBrightnessScaleTracking()
    }

    private fun resetSpectrumMarkerTracking() {
        spectrumMarkerPosition = null
        lastSpectrumMarkerUpdateMs = 0L
        spectrumMarkerRadiusSegments = SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS
        lastSpectrumMarkerRadiusUpdateMs = 0L
    }

    override fun updateLevel(level: Float) {
        val now = SystemClock.elapsedRealtime()
        val clamped = level.coerceIn(0f, 1f)
        val maxBand = rawSpectrumPeak
        val renderMode = GlyphPatternRegistry.recipeFor(glyphMode)?.renderMode
        val renderLevel = if (renderMode == GlyphPatternRenderMode.ALL_BRIGHTNESS) {
            normalizeAllBrightnessLevel(clamped, now)
        } else {
            normalizeLevelForMode(clamped, now)
        }
        val pulseTrainActivity = if (renderMode == GlyphPatternRenderMode.PULSE_TRAIN) {
            updatePulseTrainState(clamped, maxBand)
        } else {
            0f
        }
        val activity = when (renderMode) {
            GlyphPatternRenderMode.LINEAR_PEAK -> max(max(clamped, maxBand), linearPeakLevel)
            GlyphPatternRenderMode.PULSE_TRAIN -> max(max(clamped, maxBand), pulseTrainActivity)
            else -> max(clamped, maxBand)
        }

        if (activity < SILENCE_ACTIVITY_THRESHOLD) {
            if (previewDeviceProfile != null) {
                deviceSpec?.let(::submitBlankFrame)
                return
            }
            if (silenceStartedAt <= 0L) silenceStartedAt = now
            if (!isSessionOpen) {
                pendingLevel = level
            }
            if (isSessionOpen && !sessionReleasedForSilence) {
                deviceSpec?.let { spec ->
                    try {
                        submitBlankFrame(spec)
                    } catch (_: GlyphException) {
                    }
                }
            }
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
        lastPreviewLevel = renderLevel

        val spec = deviceSpec ?: return

        try {
            renderLightPattern(spec, renderLevel)
        } catch (_: GlyphException) {
        }
    }

    private fun normalizeLevelForMode(level: Float, nowMs: Long): Float {
        if (!levelAutoScaleEnabled || !isLevelAutoScaleMode()) return level
        if (autoScaleStrategy == GlyphAutoScaleStrategy.LEGACY) {
            return legacyLevelAutoScale.update(
                value = level,
                nowMs = nowMs,
                windowMs = autoScaleWindowMs,
                offset = autoScaleOffset
            )
        }
        val gain = levelAutoGain.update(
            referenceRaw = level,
            nowMs = nowMs,
            targetLevel = effectiveAutoScaleTargetLevel(autoScaleOffset),
            gainUpTauSeconds = autoScaleWindowMs / 1_000f,
            holdGainIncrease = level < SILENCE_ACTIVITY_THRESHOLD
        )
        return applyAutoScaleVisualTuning(
            value = level * gain,
            autoScaleEnabled = levelAutoScaleEnabled,
            strategy = autoScaleStrategy,
            profile = currentTuningProfile(),
            patternKind = GlyphPatternRegistry.kindOf(glyphMode),
            override = visualTuningOverride
        )
    }

    private fun resetLevelScaleTracking() {
        legacyLevelAutoScale.reset()
        levelAutoGain.reset()
    }

    private fun updateLinearPeakLevel(level: Float): Float {
        val now = SystemClock.elapsedRealtime()
        val elapsedSeconds = if (lastLinearPeakUpdateMs <= 0L) {
            0f
        } else {
            ((now - lastLinearPeakUpdateMs).coerceAtLeast(0L) / 1000f)
        }
        lastLinearPeakUpdateMs = now
        val decayed = (linearPeakLevel - (elapsedSeconds * LINEAR_PEAK_FALLOFF_PER_SECOND)).coerceAtLeast(0f)
        linearPeakLevel = max(level.coerceIn(0f, 1f), decayed).coerceIn(0f, 1f)
        return linearPeakLevel
    }

    private fun resetLinearPeakTracking() {
        linearPeakLevel = 0f
        lastLinearPeakUpdateMs = 0L
    }

    private fun updatePulseTrainState(level: Float, maxBand: Float): Float {
        val now = SystemClock.elapsedRealtime()
        val elapsedSeconds = if (lastPulseTrainUpdateMs <= 0L) {
            0f
        } else {
            ((now - lastPulseTrainUpdateMs).coerceAtLeast(0L) / 1000f)
        }
        lastPulseTrainUpdateMs = now

        pulseTrainPulses.removeAll { pulse ->
            pulse.position += elapsedSeconds * PULSE_TRAIN_SPEED_PER_SECOND
            pulse.brightness = (pulse.brightness - (elapsedSeconds * PULSE_TRAIN_BRIGHTNESS_FALLOFF)).coerceAtLeast(0f)
            pulse.position > (1f + PULSE_TRAIN_TAIL_LENGTH) || pulse.brightness <= 0.01f
        }

        val triggerLevel = max(level.coerceIn(0f, 1f), maxBand.coerceIn(0f, 1f))
        val triggerRise = triggerLevel - lastPulseTrainTriggerLevel
        if (triggerLevel >= PULSE_TRAIN_MIN_TRIGGER_LEVEL &&
            (triggerRise >= PULSE_TRAIN_TRIGGER_DELTA || triggerLevel >= 0.72f)
        ) {
            pulseTrainPulses += TravelingPulse(
                position = 0f,
                brightness = (0.55f + triggerLevel * 0.45f).coerceIn(0f, 1f)
            )
            if (pulseTrainPulses.size > 4) {
                pulseTrainPulses.removeAt(0)
            }
        }
        lastPulseTrainTriggerLevel = triggerLevel
        return pulseTrainPulses.maxOfOrNull { it.brightness } ?: 0f
    }

    private fun resetPulseTrainTracking() {
        pulseTrainPulses.clear()
        lastPulseTrainUpdateMs = 0L
        lastPulseTrainTriggerLevel = 0f
    }

    private fun isLevelAutoScaleMode(): Boolean {
        return GlyphPatternRegistry.isLevelAutoScale(glyphMode)
    }

    private fun renderLightPattern(spec: DeviceSpec, level: Float) {
        val recipe = GlyphPatternRegistry.recipeFor(glyphMode)
            ?: GlyphPatternRegistry.recipeFor(GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice())
            ?: return
        val linearPeakLevelForFrame = if (recipe.renderMode == GlyphPatternRenderMode.LINEAR_PEAK) {
            updateLinearPeakLevel(level)
        } else {
            0f
        }
        val pulseTrainBrightnessForFrame = if (recipe.renderMode == GlyphPatternRenderMode.PULSE_TRAIN) {
            pulseTrainPulses.maxOfOrNull { it.brightness } ?: 0f
        } else {
            0f
        }
        val ranges = resolveLightRanges(spec, recipe.lightZones)

        if (
            spec.profile == GlyphDeviceProfile.PHONE4A ||
            spec.profile == GlyphDeviceProfile.PHONE4B
        ) {
            updatePhone4SeriesFrame(spec) { colors ->
                applyRecipeToColors(
                    colors = colors,
                    ranges = ranges,
                    renderMode = recipe.renderMode,
                    level = level,
                    linearPeakLevel = linearPeakLevelForFrame,
                    pulseTrainBrightness = pulseTrainBrightnessForFrame
                )
            }
            return
        }

        when (recipe.renderMode) {
            GlyphPatternRenderMode.LINEAR -> updateLinearRanges(level, ranges)
            GlyphPatternRenderMode.LINEAR_PEAK -> updateLinearPeakRanges(level, linearPeakLevelForFrame, ranges)
            GlyphPatternRenderMode.PULSE_TRAIN -> updatePulseTrainRanges(level, ranges)
            GlyphPatternRenderMode.CENTER -> {
                if (ranges.size <= 1) {
                    updateCenterRange(level, ranges.firstOrNull() ?: spec.cRange)
                } else {
                    updateCenterRanges(level, ranges)
                }
            }
            GlyphPatternRenderMode.SPECTRUM -> updateSpectrumRanges(level, ranges)
            GlyphPatternRenderMode.CLASSIC -> updateClassicSpectrum(level, spec)
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> updateAllBrightness(level)
            else -> updateLinearRanges(level, ranges.ifEmpty { listOf(spec.cRange) })
        }
    }

    private fun resolveLightRanges(spec: DeviceSpec, zones: List<GlyphLightZone>): List<IntRange> {
        val resolved = zones.mapNotNull { zone ->
            when (zone) {
                GlyphLightZone.C -> effectiveMainRange(spec)
                GlyphLightZone.A -> spec.aRange
                GlyphLightZone.B -> spec.bRange
                GlyphLightZone.CAB -> spec.cabRange
                GlyphLightZone.D1 -> spec.d1Range
            }
        }
        return if (resolved.isEmpty()) listOf(effectiveMainRange(spec)) else resolved
    }

    private fun applyRecipeToColors(
        colors: IntArray,
        ranges: List<IntRange>,
        renderMode: GlyphPatternRenderMode,
        level: Float,
        linearPeakLevel: Float = 0f,
        pulseTrainBrightness: Float = 0f
    ) {
        when (renderMode) {
            GlyphPatternRenderMode.LINEAR -> ranges.forEach { applyLinearRange(colors, it, level) }
            GlyphPatternRenderMode.LINEAR_PEAK -> ranges.forEach { applyLinearPeakRange(colors, it, level, linearPeakLevel) }
            GlyphPatternRenderMode.PULSE_TRAIN -> ranges.forEach { applyPulseTrainRange(colors, it, level, pulseTrainBrightness) }
            GlyphPatternRenderMode.CENTER -> ranges.forEach { applyCenterRange(colors, it, level) }
            GlyphPatternRenderMode.SPECTRUM -> ranges.forEach { applySpectrumRange(colors, it, level) }
            GlyphPatternRenderMode.SPECTRUM_MARKER -> ranges.forEach { applySpectrumMarkerRange(colors, it, level) }
            GlyphPatternRenderMode.CLASSIC -> applyClassicSpectrum(colors, deviceSpec ?: return, level)
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> {
                val primaryRange = ranges.firstOrNull() ?: return
                applyAllBrightnessRange(colors, primaryRange, level)
            }
            else -> ranges.forEach { applyLinearRange(colors, it, level) }
        }
    }

    private fun applyFillOtherGlyphLights(
        colors: IntArray,
        spec: DeviceSpec,
        usedRanges: List<IntRange>,
        level: Float
    ) {
        if (!fillOtherGlyphLightsEnabled) return
        if (GlyphPatternRegistry.isAllBrightness(glyphMode)) return
        if (spec.profile !in setOf(GlyphDeviceProfile.PHONE1, GlyphDeviceProfile.PHONE2, GlyphDeviceProfile.PHONE2A)) {
            return
        }
        applyClassicSpectrum(
            colors = colors,
            spec = spec,
            level = level,
            excludedRanges = usedRanges
        )
    }

    private fun updateLinearRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyLinearRange(colors, range, clamped)
        }
        applyFillOtherGlyphLights(colors, spec, ranges, clamped)
        submitFrame(colors)
    }

    private fun updateLinearPeakRanges(level: Float, peakLevel: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        val peak = peakLevel.coerceIn(0f, 1f)
        if (clamped <= 0.001f && peak <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyLinearPeakRange(colors, range, clamped, peak)
        }
        applyFillOtherGlyphLights(colors, spec, ranges, clamped)
        submitFrame(colors)
    }

    private fun updatePulseTrainRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        val pulseBrightness = pulseTrainPulses.maxOfOrNull { it.brightness } ?: 0f
        if (clamped <= 0.001f && pulseBrightness <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyPulseTrainRange(colors, range, clamped, pulseBrightness)
        }
        applyFillOtherGlyphLights(colors, spec, ranges, clamped)
        submitFrame(colors)
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

    private fun applyPulseTrainRange(colors: IntArray, range: IntRange, level: Float, pulseTrainBrightness: Float) {
        val count = range.count()
        if (count <= 0) return

        val channels = range.toList()
        val slots = centerPairSlots(channels, isCenterDirectionReversed())
        if (slots.isEmpty()) return

        val ambientMax = (MAX_LIGHT * PULSE_TRAIN_BASE_BRIGHTNESS_RATIO).roundToInt()
            .coerceIn(MIN_LIGHT / 3, MAX_LIGHT)
        val ambientLevel = (0.08f + level.coerceIn(0f, 1f) * 0.22f).coerceIn(0f, 0.3f)
        slots.forEachIndexed { index, slotChannels ->
            val slotRatio = if (slots.size <= 1) 0f else index / (slots.size - 1f)
            val centerBias = (1f - slotRatio * 0.9f).coerceIn(0f, 1f)
            val brightness = if (binaryMode) {
                if (index == 0 && level >= 0.2f) ambientMax else 0
            } else {
                (ambientMax * ambientLevel * centerBias).roundToInt().coerceIn(0, ambientMax)
            }
            if (brightness > 0) {
                slotChannels.forEach { channel ->
                    if (channel in colors.indices) {
                        colors[channel] = max(colors[channel], brightness)
                    }
                }
            }
        }

        if (pulseTrainBrightness <= 0.001f) return
        pulseTrainPulses.forEach { pulse ->
            val centerIndex = (pulse.position.coerceIn(0f, 1.15f) * slots.size).toInt()
            val tailRadius = max(1, (slots.size * (PULSE_TRAIN_TAIL_LENGTH * 0.65f)).roundToInt())
            for (offset in -tailRadius..tailRadius) {
                val slotIndex = centerIndex + offset
                val slotChannels = slots.getOrNull(slotIndex) ?: continue
                val normalizedDistance = kotlin.math.abs(offset) / (tailRadius + 0.001f)
                val rippleBand = (1f - normalizedDistance).coerceAtLeast(0f).pow(1.6f)
                val forwardBias = if (offset < 0) 0.72f else 1f
                val brightnessFloat = (pulse.brightness * rippleBand * forwardBias).coerceIn(0f, 1f)
                val brightness = if (binaryMode) {
                    if (brightnessFloat >= 0.35f) MAX_LIGHT else 0
                } else {
                    (brightnessFloat.coerceIn(0f, 1f).pow(outputGamma) * MAX_LIGHT).roundToInt()
                        .coerceIn(0, MAX_LIGHT)
                }
                if (brightness > 0) {
                    slotChannels.forEach { channel ->
                        if (channel in colors.indices) {
                            colors[channel] = max(colors[channel], brightness)
                        }
                    }
                }
            }
        }
    }

    private fun updateSpectrumRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applySpectrumRange(colors, range, clamped)
        }
        applyFillOtherGlyphLights(colors, spec, ranges, clamped)
        submitFrame(colors)
    }

    private fun updateClassicSpectrum(level: Float, spec: DeviceSpec) {
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        applyClassicSpectrum(colors, spec, clamped)
        submitFrame(colors)
    }

    private fun applyClassicSpectrum(
        colors: IntArray,
        spec: DeviceSpec,
        level: Float,
        excludedRanges: List<IntRange> = emptyList()
    ) {
        val groups = classicSpectrumGroupsFor(spec)
            .map { group ->
                group.filter { channel ->
                    excludedRanges.none { channel in it }
                }
            }
            .filter { it.isNotEmpty() }
        if (groups.isEmpty()) return

        val orderedGroups = if (shouldReverseLightOrder()) groups.reversed() else groups
        val lastIndex = (orderedGroups.size - 1).coerceAtLeast(1)
        orderedGroups.forEachIndexed { index, channels ->
            val position = index / lastIndex.toFloat()
            val bandValue = sampleSpectrumAt(position)
            val weighted = (bandValue * level).coerceIn(0f, 1f)
            val shaped = weighted.pow(outputGamma)
            val brightness = if (binaryMode) {
                if (shaped >= 0.5f) MAX_LIGHT else 0
            } else {
                (shaped * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
            }
            if (brightness <= 0) return@forEachIndexed
            channels.forEach { channel ->
                if (channel in colors.indices && brightness > colors[channel]) {
                    colors[channel] = brightness
                }
            }
        }
    }

    private fun applyLinearPeakRange(colors: IntArray, range: IntRange, level: Float, peakLevel: Float) {
        val count = range.count()
        if (count <= 0) return

        val baseMax = (MAX_LIGHT * LINEAR_PEAK_BASE_BRIGHTNESS_RATIO).roundToInt()
            .coerceIn(MIN_LIGHT, MAX_LIGHT)
        val virtualLit = level.coerceIn(0f, 1f) * count
        val fullLit = virtualLit.toInt().coerceIn(0, count)
        val edgeBrightness = if (binaryMode) 0 else {
            ((virtualLit - fullLit) * baseMax).roundToInt().coerceIn(0, baseMax)
        }
        val channels = if (shouldReverseLightOrder()) range.reversed().toList() else range.toList()
        channels.forEachIndexed { index, channel ->
            val brightness = when {
                index < fullLit -> baseMax
                index == fullLit && fullLit < count -> edgeBrightness
                else -> 0
            }
            if (brightness > 0 && channel in colors.indices) {
                colors[channel] = max(colors[channel], brightness)
            }
        }

        val peakIndex = ((peakLevel.coerceIn(0f, 1f) * count).roundToInt() - 1)
            .coerceIn(0, count - 1)
        val peakChannel = channels.getOrNull(peakIndex) ?: return
        if (peakChannel in colors.indices) {
            colors[peakChannel] = MAX_LIGHT
        }
    }

    private fun classicSpectrumGroupsFor(spec: DeviceSpec): List<List<Int>> {
        return when (spec.profile) {
            GlyphDeviceProfile.PHONE1 -> listOf(
                listOf(0),
                listOf(1),
                (2..5).toList(),
                (7..14).toList(),
                listOf(6)
            )
            GlyphDeviceProfile.PHONE2 -> listOf(
                listOf(0),
                listOf(1),
                listOf(2),
                (3..18).toList(),
                listOf(19),
                listOf(20),
                listOf(21),
                listOf(22),
                listOf(23),
                (25..32).toList(),
                listOf(24)
            )
            GlyphDeviceProfile.PHONE2A -> listOf(
                listOf(25),
                listOf(24),
                (0..23).toList()
            )
            GlyphDeviceProfile.PHONE3A -> listOf(
                (20..30).toList(),
                (31..35).toList(),
                (0..19).toList()
            )
            else -> emptyList()
        }
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

    private fun applySpectrumMarkerRange(colors: IntArray, range: IntRange, level: Float) {
        val channels = if (shouldReverseLightOrder()) range.reversed().toList() else range.toList()
        if (channels.isEmpty()) return

        val targetPosition = dominantSpectrumPosition() ?: return
        val markerPosition = smoothSpectrumMarkerPosition(targetPosition, channels.size)
        val markerSpan = (channels.size - 1).coerceAtLeast(0) +
            (SPECTRUM_MARKER_EDGE_PADDING_SEGMENTS * 2f)
        val markerCoordinate = -SPECTRUM_MARKER_EDGE_PADDING_SEGMENTS +
            (markerPosition * markerSpan)
        val markerRadius = smoothSpectrumMarkerRadius(level)

        channels.forEachIndexed { index, channel ->
            if (channel !in colors.indices) return@forEachIndexed

            val distance = abs(index - markerCoordinate)
            val remaining = (1f - (distance / markerRadius)).coerceIn(0f, 1f)
            val falloff = remaining * remaining * (3f - (2f * remaining))
            val brightnessRatio = falloff.coerceIn(0f, 1f)
            val brightness = if (binaryMode) {
                if (brightnessRatio >= 0.5f) MAX_LIGHT else 0
            } else {
                (brightnessRatio * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
            }
            if (brightness > colors[channel]) {
                colors[channel] = brightness
            }
        }
    }

    private fun smoothSpectrumMarkerRadius(level: Float): Float {
        val maxRadius = if (deviceSpec?.profile == GlyphDeviceProfile.PHONE4B) {
            SPECTRUM_MARKER_PHONE4B_MAX_RADIUS_SEGMENTS
        } else {
            SPECTRUM_MARKER_PHONE4A_MAX_RADIUS_SEGMENTS
        }
        val normalizedLevel = level.coerceIn(0f, 1f).pow(SPECTRUM_MARKER_RADIUS_LEVEL_EXPONENT)
        val target = SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS +
            ((maxRadius - SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS) * normalizedLevel)
        val now = SystemClock.elapsedRealtime()
        if (lastSpectrumMarkerRadiusUpdateMs <= 0L) {
            spectrumMarkerRadiusSegments = target
            lastSpectrumMarkerRadiusUpdateMs = now
            return target
        }

        val elapsedSeconds = (now - lastSpectrumMarkerRadiusUpdateMs).coerceIn(0L, 100L) / 1_000f
        lastSpectrumMarkerRadiusUpdateMs = now
        val response = if (target > spectrumMarkerRadiusSegments) {
            SPECTRUM_MARKER_RADIUS_ATTACK_PER_SECOND
        } else {
            SPECTRUM_MARKER_RADIUS_RELEASE_PER_SECOND
        }
        val blend = (1f - exp(-response * elapsedSeconds)).coerceIn(0f, 1f)
        spectrumMarkerRadiusSegments += (target - spectrumMarkerRadiusSegments) * blend
        return spectrumMarkerRadiusSegments.coerceIn(
            SPECTRUM_MARKER_MIN_RADIUS_SEGMENTS,
            maxRadius
        )
    }

    private fun dominantSpectrumPosition(): Float? {
        val bands = spectrumBands
        if (bands.isEmpty()) return null

        val peakIndex = bands.indices.maxByOrNull { bands[it] } ?: return null
        val peak = bands[peakIndex].coerceIn(0f, 1f)
        if (peak <= SPECTRUM_MARKER_MIN_PEAK) return null
        if (bands.size == 1) return 0f

        val first = (peakIndex - SPECTRUM_MARKER_PEAK_WINDOW).coerceAtLeast(0)
        val last = (peakIndex + SPECTRUM_MARKER_PEAK_WINDOW).coerceAtMost(bands.lastIndex)
        var weightedIndex = 0f
        var totalWeight = 0f
        for (index in first..last) {
            val relative = (bands[index].coerceIn(0f, 1f) / peak).coerceIn(0f, 1f)
            val weight = relative * relative * relative
            weightedIndex += index * weight
            totalWeight += weight
        }
        val continuousIndex = if (totalWeight > 0f) weightedIndex / totalWeight else peakIndex.toFloat()
        return (continuousIndex / bands.lastIndex).coerceIn(0f, 1f)
    }

    private fun smoothSpectrumMarkerPosition(target: Float, segmentCount: Int): Float {
        val now = SystemClock.elapsedRealtime()
        val current = spectrumMarkerPosition
        if (current == null || lastSpectrumMarkerUpdateMs <= 0L) {
            spectrumMarkerPosition = target
            lastSpectrumMarkerUpdateMs = now
            return target
        }

        val elapsedSeconds = (now - lastSpectrumMarkerUpdateMs).coerceIn(0L, 100L) / 1_000f
        lastSpectrumMarkerUpdateMs = now
        val blend = (1f - exp(-SPECTRUM_MARKER_RESPONSE_PER_SECOND * elapsedSeconds)).coerceIn(0f, 1f)
        val markerSpan = (segmentCount - 1).coerceAtLeast(0) +
            (SPECTRUM_MARKER_EDGE_PADDING_SEGMENTS * 2f)
        val maxStep = (SPECTRUM_MARKER_MAX_STEP_SEGMENTS / markerSpan).coerceIn(0f, 1f)
        val step = ((target - current) * blend).coerceIn(-maxStep, maxStep)
        return (current + step).coerceIn(0f, 1f).also {
            spectrumMarkerPosition = it
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
            GlyphDeviceProfile.PHONE4A,
            GlyphDeviceProfile.PHONE4B -> !reverseDirection
            else -> reverseDirection
        }
    }

    private fun updateAllBrightness(level: Float) {
        val spec = deviceSpec ?: return
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0f) {
            submitBlankFrame(spec)
            return
        }
        val normalized = ((clamped - ALL_BRIGHTNESS_CURVE_FLOOR) / (1f - ALL_BRIGHTNESS_CURVE_FLOOR))
            .coerceIn(0f, 1f)
        val shaped = normalized.pow(outputGamma)
        val brightness = if (binaryMode) {
            MAX_LIGHT
        } else {
            (ALL_BRIGHTNESS_MIN_LIGHT + ((MAX_LIGHT - ALL_BRIGHTNESS_MIN_LIGHT) * shaped)).roundToInt()
        }
        fullGlyphBrightness.fill(brightness)
        submitFrame(fullGlyphBrightness)
    }

    private fun applyAllBrightnessRange(colors: IntArray, range: IntRange, level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0f) return
        val normalized = ((clamped - ALL_BRIGHTNESS_CURVE_FLOOR) / (1f - ALL_BRIGHTNESS_CURVE_FLOOR))
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

    private fun normalizeAllBrightnessLevel(level: Float, nowMs: Long): Float {
        if (!allBrightnessAutoScaleEnabled) return level
        if (autoScaleStrategy == GlyphAutoScaleStrategy.LEGACY) {
            return legacyAllBrightnessAutoScale.update(
                value = level,
                nowMs = nowMs,
                windowMs = autoScaleWindowMs,
                offset = autoScaleOffset
            )
        }

        allBrightnessGateOn = if (allBrightnessGateOn) {
            level > ALL_BRIGHTNESS_OFF_THRESHOLD
        } else {
            level >= ALL_BRIGHTNESS_ON_THRESHOLD
        }

        val gain = allBrightnessAutoGain.update(
            referenceRaw = level,
            nowMs = nowMs,
            targetLevel = effectiveAutoScaleTargetLevel(autoScaleOffset),
            gainUpTauSeconds = autoScaleWindowMs / 1_000f,
            holdGainIncrease = !allBrightnessGateOn
        )
        return if (allBrightnessGateOn) {
            applyAutoScaleVisualTuning(
                value = level * gain,
                autoScaleEnabled = allBrightnessAutoScaleEnabled,
                strategy = autoScaleStrategy,
                profile = currentTuningProfile(),
                patternKind = GlyphPatternRegistry.kindOf(glyphMode),
                override = visualTuningOverride
            )
        } else {
            0f
        }
    }

    private fun resetAllBrightnessScaleTracking() {
        legacyAllBrightnessAutoScale.reset()
        allBrightnessAutoGain.reset()
        allBrightnessGateOn = false
    }

    private fun currentTuningProfile(): GlyphDeviceProfile {
        return deviceSpec?.profile ?: previewDeviceProfile ?: GlyphDeviceCatalog.currentProfile()
    }

    private fun updateCenterRange(level: Float, channelRange: IntRange) {
        val spec = deviceSpec ?: return
        if (!spec.centerSupported || channelRange.count() < 2) {
            updateLinearRanges(level, listOf(channelRange))
            return
        }

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val d1CenterChannel = spec.d1CenterChannel
        if (d1CenterChannel != null && channelRange == spec.d1Range && channelRange.count() == 8) {
            val centerChannel = d1CenterChannel
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
            val slots = if (isCenterDirectionReversed()) {
                (pairCount downTo 1).map { pair ->
                    listOf(leftChannels[pair - 1], rightChannels[pair - 1])
                } + listOf(listOf(centerChannel))
            } else {
                listOf(listOf(centerChannel)) + (1..pairCount).map { pair ->
                    listOf(leftChannels[pair - 1], rightChannels[pair - 1])
                }
            }
            slots.forEachIndexed { index, channels ->
                val brightness = brightnessForCenterSlot(index, fullSlots, edgeBrightness)
                if (brightness > 0) {
                    channels.forEach { channel ->
                        if (channel in colors.indices) colors[channel] = brightness
                    }
                }
            }

            applyFillOtherGlyphLights(colors, spec, listOf(channelRange), clamped)
            submitFrame(colors)
            return
        }

        val pairCount = channelRange.count() / 2
        val virtualPairs = clamped * pairCount
        val fullPairs = virtualPairs.toInt()
        val edgeBrightness = if (binaryMode) {
            0
        } else {
            ((virtualPairs - fullPairs) * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
        }

        val colors = IntArray(spec.channelCount)
        val slots = centerPairSlots(channelRange.toList(), isCenterDirectionReversed())
        slots.forEachIndexed { index, channels ->
            val brightness = brightnessForCenterSlot(index, fullPairs, edgeBrightness)
            channels.forEach { channel ->
                if (brightness > 0 && channel in channelRange && channel in colors.indices) {
                    colors[channel] = brightness
                }
            }
        }
        applyFillOtherGlyphLights(colors, spec, listOf(channelRange), clamped)
        submitFrame(colors)
    }

    private fun updateCenterRanges(level: Float, ranges: List<IntRange>) {
        val spec = deviceSpec ?: return
        if (ranges.isEmpty()) return

        val clamped = level.coerceIn(0f, 1f)
        if (clamped <= 0.001f) {
            submitBlankFrame(spec)
            return
        }

        val colors = IntArray(spec.channelCount)
        for (range in ranges) {
            applyCenterRange(colors, range, clamped)
        }
        applyFillOtherGlyphLights(colors, spec, ranges, clamped)
        submitFrame(colors)
    }

    private fun applyCenterRange(colors: IntArray, channelRange: IntRange, level: Float) {
        val count = channelRange.count()
        if (count < 1) return

        val slots = centerPairSlots(channelRange.toList(), isCenterDirectionReversed())
        val virtualSlots = level * slots.size
        val fullSlots = virtualSlots.toInt().coerceIn(0, slots.size)
        val edgeBrightness = if (binaryMode) {
            0
        } else {
            ((virtualSlots - fullSlots) * MAX_LIGHT).roundToInt().coerceIn(0, MAX_LIGHT)
        }

        slots.forEachIndexed { index, slotChannels ->
            val brightness = brightnessForCenterSlot(index, fullSlots, edgeBrightness)
            if (brightness > 0) {
                slotChannels.forEach { channel ->
                    if (channel in channelRange && channel in colors.indices) {
                        colors[channel] = brightness
                    }
                }
            }
        }
    }

    private fun centerPairSlots(channels: List<Int>, reversed: Boolean): List<List<Int>> {
        if (channels.isEmpty()) return emptyList()
        val count = channels.size
        val slots = if (count % 2 == 1) {
            val centerIdx = count / 2
            val pairCount = count / 2
            listOf(listOf(channels[centerIdx])) + (1..pairCount).map { pair ->
                listOf(channels[centerIdx - pair], channels[centerIdx + pair])
            }
        } else {
            val pairCount = count / 2
            val centerLeft = pairCount - 1
            val centerRight = pairCount
            (0 until pairCount).map { offset ->
                listOf(channels[centerLeft - offset], channels[centerRight + offset])
            }
        }
        return if (reversed) slots.asReversed() else slots
    }

    private fun brightnessForCenterSlot(index: Int, fullSlots: Int, edgeBrightness: Int): Int {
        return when {
            index < fullSlots -> MAX_LIGHT
            index == fullSlots -> edgeBrightness
            else -> 0
        }
    }

    private fun isCenterDirectionReversed(): Boolean = reverseDirection

    private fun submitFrame(colors: IntArray) {
        if (previewDeviceProfile != null) {
            updateLastSentFrame(colors)
            val spec = deviceSpec ?: return
            previewFrameListener?.invoke(
                GlyphPreviewFrame.Lights(
                    deviceProfile = spec.profile,
                    physicalDeviceProfile = spec.physicalProfile,
                    glyphMode = glyphMode,
                    timestampMs = SystemClock.elapsedRealtime(),
                    brightness = colors.copyOf()
                )
            )
            return
        }
        try {
            glyphManager.setFrameColors(colors)
        } catch (error: Throwable) {
            invalidateLastSentFrame()
            throw error
        }

        updateLastSentFrame(colors)
        mirrorPreviewFrame(colors)
    }

    private fun updateLastSentFrame(colors: IntArray) {
        if (lastSentFrame.size != colors.size) {
            lastSentFrame = colors.copyOf()
        } else {
            colors.copyInto(lastSentFrame)
        }
    }

    private fun submitBlankFrame(spec: DeviceSpec) {
        lastPreviewLevel = 0f
        val requiredSize = frameChannelCount(spec)
        if (blankFrame.size != requiredSize) {
            blankFrame = IntArray(requiredSize)
        }
        submitFrame(blankFrame)
    }

    private fun invalidateLastSentFrame() {
        if (lastSentFrame.isNotEmpty()) {
            lastSentFrame = IntArray(0)
        }
    }

    override fun turnOff() {
        invalidateLastSentFrame()
        lastPreviewLevel = 0f
        resetLinearPeakTracking()
        resetPulseTrainTracking()
        resetSpectrumMarkerTracking()
        resetBaseIndicators()
        silenceStartedAt = 0L
        // Keep sessionReleasedForSilence intact. A capture-only restart calls turnOff()
        // without reopening a session, so clearing it here would prevent updateLevel()
        // from entering reopenSessionAfterSilence() when audio returns.
        if (previewDeviceProfile != null) {
            mirrorOffPreviewFrameToListener()
            return
        }
        try {
            glyphManager.turnOff()
            mirrorOffPreviewFrame()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff ignored because session is not ready", error)
        }
    }

    override fun releaseSession() {
        turnOff()
        closeSession()
    }

    override fun suspendSession() {
        releaseSessionForSilence()
    }

    private fun updatePhone4SeriesFrame(
        spec: DeviceSpec,
        populateMain: (IntArray) -> Unit
    ) {
        val colors = IntArray(frameChannelCount(spec))
        populateMain(colors)
        if (baseIndicatorEnabled) {
            applyBaseIndicator(spec.profile, colors)
        }
        if (colors.none { it > 0 }) {
            lastPreviewLevel = 0f
        }
        submitFrame(colors)
    }

    private fun effectiveMainRange(spec: DeviceSpec): IntRange {
        if (!recordingLightIncluded) return spec.cRange
        val recordingLightChannel = spec.recordingLightChannel ?: return spec.cRange
        return spec.cRange.first..recordingLightChannel
    }

    private fun frameChannelCount(spec: DeviceSpec): Int {
        val recordingLightChannel = spec.recordingLightChannel
        return if ((recordingLightIncluded || baseIndicatorEnabled) && recordingLightChannel != null) {
            maxOf(spec.channelCount, recordingLightChannel + 1)
        } else spec.channelCount
    }

    private fun clearPhone4bRecordingLightIfUnused() {
        val spec = deviceSpec ?: return
        if (
            spec.profile != GlyphDeviceProfile.PHONE4B ||
            recordingLightIncluded ||
            baseIndicatorEnabled ||
            !isSessionOpen
        ) {
            return
        }
        val recordingLightChannel = spec.recordingLightChannel ?: return
        runCatching {
            submitFrame(IntArray(maxOf(spec.channelCount, recordingLightChannel + 1)))
        }.onFailure { error ->
            AppLogger.w(TAG, "Failed to clear Phone (4b) recording light", error)
        }
    }

    private fun updateBaseIndicatorAnalysis(level: Float) {
        val profile = deviceSpec?.profile ?: return
        val rawLevel = level.coerceIn(0f, 1f)
        baseIndicatorRenderers
            .firstOrNull { it.accepts(profile) }
            ?.updateAnalysis(normalizeBaseIndicatorLevel(rawLevel))
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

        return normalizeWithAutoScaleOffset(level, baseIndicatorMin, baseIndicatorMax)
    }

    private fun normalizeWithAutoScaleOffset(value: Float, minTrack: Float, maxTrack: Float): Float {
        val range = (maxTrack - minTrack).coerceAtLeast(0.05f)
        val adjustedMin = minTrack - (range * autoScaleOffset)
        val adjustedMax = maxTrack + (range * autoScaleOffset)
        val adjustedRange = (adjustedMax - adjustedMin).coerceAtLeast(0.05f)
        return ((value - adjustedMin) / adjustedRange).coerceIn(0f, 1f)
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
        invalidateLastSentFrame()
        try {
            glyphManager.turnOff()
            mirrorOffPreviewFrame()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff during silence release failed", error)
        }
        closeSession()
        sessionReleasedForSilence = true
    }

    private fun mirrorPreviewFrame(colors: IntArray, force: Boolean = false) {
        val spec = deviceSpec ?: return
        GlyphPreviewFrameStore.publishLights(
            deviceProfile = spec.profile,
            physicalDeviceProfile = spec.physicalProfile,
            glyphMode = glyphMode,
            brightness = colors,
            force = force
        )
    }

    private fun mirrorOffPreviewFrame() {
        val spec = deviceSpec ?: return
        mirrorPreviewFrame(IntArray(frameChannelCount(spec)), force = true)
    }

    private fun mirrorOffPreviewFrameToListener() {
        val spec = deviceSpec ?: return
        previewFrameListener?.invoke(
            GlyphPreviewFrame.Lights(
                deviceProfile = spec.profile,
                physicalDeviceProfile = spec.physicalProfile,
                glyphMode = glyphMode,
                timestampMs = SystemClock.elapsedRealtime(),
                brightness = IntArray(frameChannelCount(spec))
            )
        )
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
                invalidateLastSentFrame()
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
        invalidateLastSentFrame()
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
        val emulatePhone4b = phone4bEmulationEnabled &&
            currentDevice.profile == GlyphDeviceProfile.PHONE4A
        val effectiveProfile = if (emulatePhone4b) {
            GlyphDeviceProfile.PHONE4B
        } else {
            currentDevice.profile
        }
        val cRange = if (emulatePhone4b) 2..5 else lightSpec.cRange
        val recordingLightChannel = when {
            emulatePhone4b -> PHONE4A_RECORDING_LIGHT_CHANNEL
            currentDevice.profile == GlyphDeviceProfile.PHONE4A -> PHONE4A_RECORDING_LIGHT_CHANNEL
            currentDevice.profile == GlyphDeviceProfile.PHONE4B -> PHONE4B_RECORDING_LIGHT_CHANNEL
            else -> null
        }
        return DeviceSpec(
            profile = effectiveProfile,
            physicalProfile = currentDevice.profile,
            deviceId = lightSpec.sdkDeviceId,
            channelCount = lightSpec.channelCount,
            cRange = cRange,
            recordingLightChannel = recordingLightChannel,
            aRange = lightSpec.aRange,
            bRange = lightSpec.bRange,
            cabRange = lightSpec.cabRange,
            d1Range = lightSpec.d1Range,
            d1CenterChannel = lightSpec.d1CenterChannel,
            centerSupported = lightSpec.centerSupported
        )
    }

    private fun resolvePreviewDeviceSpec(profile: GlyphDeviceProfile): DeviceSpec? {
        val lightSpec = GlyphDeviceCatalog.definitionForProfile(profile)?.lightSpec ?: return null
        val recordingLightChannel = when (profile) {
            GlyphDeviceProfile.PHONE4A -> PHONE4A_RECORDING_LIGHT_CHANNEL
            GlyphDeviceProfile.PHONE4B -> PHONE4B_RECORDING_LIGHT_CHANNEL
            else -> null
        }
        return DeviceSpec(
            profile = profile,
            physicalProfile = profile,
            deviceId = lightSpec.sdkDeviceId,
            channelCount = lightSpec.channelCount,
            cRange = lightSpec.cRange,
            recordingLightChannel = recordingLightChannel,
            aRange = lightSpec.aRange,
            bRange = lightSpec.bRange,
            cabRange = lightSpec.cabRange,
            d1Range = lightSpec.d1Range,
            d1CenterChannel = lightSpec.d1CenterChannel,
            centerSupported = lightSpec.centerSupported
        )
    }
}
