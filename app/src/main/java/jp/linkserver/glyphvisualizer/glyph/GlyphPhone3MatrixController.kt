package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import jp.linkserver.glyphvisualizer.AppLogger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.pow

class GlyphPhone3MatrixController(
    context: Context,
    private val onStatusChanged: (String) -> Unit
) : GlyphOutputController {

    private enum class MatrixDevice {
        PHONE3,
        PHONE4A_PRO
    }

    companion object {
        private const val TAG = "GlyphPhone3Matrix"
        private const val COLOR_ON = 255
        private const val COLOR_OFF = 0
        private const val SILENCE_RELEASE_MS = 3_000L
        private const val SILENCE_ACTIVITY_THRESHOLD = 0.003f
        private const val SPECTRUM_HISTORY_WINDOW_MS = 120_000L
        private const val ALL_BRIGHTNESS_OFF_THRESHOLD = 0.06f
        private const val ALL_BRIGHTNESS_MIN_LIGHT = 240
        private const val ALL_BRIGHTNESS_MAX_LIGHT = 4095
        private const val ALL_BRIGHTNESS_RESPONSE_GAMMA = 1.8f
        private const val ALL_BRIGHTNESS_MIN_LIGHT_MATRIX = 60
        private const val ALL_BRIGHTNESS_MAX_LIGHT_MATRIX = 255
        private const val MODE_P3_MATRIX_BAR = "P3_MATRIX_BAR"
        private const val MODE_P3_MATRIX_FIELD = "P3_MATRIX_FIELD"
        private const val MODE_P3_MATRIX_CIRCLE = "P3_MATRIX_CIRCLE"
        private const val MODE_P3_MATRIX_SPECTRUM = "P3_MATRIX_SPECTRUM"
        private const val MODE_P3_MATRIX_SPECTRUM_CENTER = "P3_MATRIX_SPECTRUM_CENTER"
        private const val MODE_P3_MATRIX_ALL_BRIGHTNESS = "P3_MATRIX_ALL_BRIGHTNESS"
        private const val FRAME_INTERVAL_MS = 16L // ~60fps
    }

    private val glyphMatrixManager = GlyphMatrixManager.getInstance(context.applicationContext)
    private var isBound = false
    private var isSessionOpen = false
    private var reverseDirection = true
    private var glyphMode = MODE_P3_MATRIX_BAR
    private var spectrumAutoScaleEnabled = false
    private var allBrightnessAutoScaleEnabled = false
    private var allBrightnessMin = 0f
    private var allBrightnessMax = 1f
    private var lastAllBrightnessUpdateMs = 0L
    private var matrixLength = 0
    private var pendingLevel = -1f
    private var lastRenderAt = 0L
    private var lastLitRows = -1
    private var failureCount = 0
    private var frameBuffer = IntArray(0)
    private var lowEnergy = 0f
    private var highEnergy = 0f
    private var leftLevel = 0f
    private var rightLevel = 0f
    private var spectrumBands = FloatArray(0)
    private var smoothedSpectrumBands = FloatArray(0)
    private var spectrumBandMins = FloatArray(0)
    private var spectrumBandMaxs = FloatArray(0)
    private var lastSpectrumUpdateMs = 0L
    private var silenceStartedAt = 0L
    private var matrixReleasedForSilence = false
    private var matrixDevice = MatrixDevice.PHONE3

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            matrixDevice = when {
                Common.is25111p() -> MatrixDevice.PHONE4A_PRO
                Common.is23112() -> MatrixDevice.PHONE3
                else -> {
                    onStatusChanged("Glyph Matrix output is currently available on Phone (3)/(4a Pro). model=${Build.MODEL}")
                    return
                }
            }

            matrixLength = Common.getDeviceMatrixLength()
            if (matrixLength <= 0) {
                onStatusChanged("Glyph Matrix length is unavailable on this device.")
                return
            }
            frameBuffer = IntArray(matrixLength * matrixLength)

            val targetDeviceCode = when (matrixDevice) {
                MatrixDevice.PHONE3 -> Glyph.DEVICE_23112
                MatrixDevice.PHONE4A_PRO -> Glyph.DEVICE_25111p
            }
            val registered = glyphMatrixManager.register(targetDeviceCode)
            if (!registered) {
                onStatusChanged("Glyph Matrix SDK registration failed.")
                return
            }

            isSessionOpen = true
            failureCount = 0
            lastLitRows = -1
            silenceStartedAt = 0L
            matrixReleasedForSilence = false
            try {
                glyphMatrixManager.setGlyphMatrixTimeout(true)
            } catch (error: Throwable) {
                AppLogger.w(TAG, "setGlyphMatrixTimeout(true) failed", error)
            }
            onStatusChanged("Glyph Matrix session ready on ${Build.MODEL}.")

            val pending = pendingLevel
            if (pending >= 0f) {
                pendingLevel = -1f
                updateLevel(pending)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            isSessionOpen = false
            onStatusChanged("Glyph Matrix service disconnected.")
        }
    }

    override fun bind() {
        if (isBound) return
        isBound = true
        glyphMatrixManager.init(callback)
        onStatusChanged("Connecting to the Glyph Matrix service...")
    }

    override fun unbind() {
        turnOff()
        if (isBound) {
            glyphMatrixManager.unInit()
            isBound = false
            isSessionOpen = false
        }
    }

    override fun setReverseDirection(reverse: Boolean) {
        reverseDirection = reverse
    }

    override fun setGlyphMode(mode: String) {
        if (glyphMode != mode) {
            glyphMode = mode
            lastLitRows = -1
            resetSpectrumScaleTracking()
            resetAllBrightnessScaleTracking()
        }
    }

    override fun setBinaryMode(binary: Boolean) {
        // Matrix minimum implementation does not use binary mode yet.
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

    override fun updateAnalysis(
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray?
    ) {
        this.lowEnergy = lowEnergy.coerceIn(0f, 1f)
        this.highEnergy = highEnergy.coerceIn(0f, 1f)
        this.leftLevel = leftLevel.coerceIn(0f, 1f)
        this.rightLevel = rightLevel.coerceIn(0f, 1f)
        val raw = spectrumBands ?: FloatArray(0)
        val resampled = if (matrixDevice == MatrixDevice.PHONE4A_PRO && raw.size > 13) {
            downsampleBands(raw, 13)
        } else {
            raw
        }
        this.spectrumBands = normalizeSpectrumBands(applySpectrumSmoothing(resampled))
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
        val drift = (elapsedMs.toFloat() / SPECTRUM_HISTORY_WINDOW_MS).coerceIn(0f, 1f)

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
    }

    private fun resetAllBrightnessScaleTracking() {
        allBrightnessMin = 0f
        allBrightnessMax = 1f
        lastAllBrightnessUpdateMs = 0L
    }

    private fun normalizeAllBrightnessLevel(level: Float): Float {
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastAllBrightnessUpdateMs <= 0L) 0L else (now - lastAllBrightnessUpdateMs).coerceAtLeast(0L)
        lastAllBrightnessUpdateMs = now
        val drift = (elapsed.toFloat() / SPECTRUM_HISTORY_WINDOW_MS).coerceIn(0f, 1f)

        allBrightnessMin = min(level, (allBrightnessMin + drift).coerceIn(0f, 1f))
        allBrightnessMax = max(level, (allBrightnessMax - drift).coerceIn(0f, 1f))

        val range = (allBrightnessMax - allBrightnessMin).coerceAtLeast(0.05f)
        return ((level - allBrightnessMin) / range).coerceIn(0f, 1f)
    }

    override fun updateLevel(level: Float) {
        if (!isSessionOpen || matrixLength <= 0) {
            pendingLevel = level
            return
        }
        pendingLevel = -1f

        val now = SystemClock.elapsedRealtime()

        val clamped = level.coerceIn(0f, 1f)
        val maxBand = if (spectrumBands.isNotEmpty()) spectrumBands.maxOrNull() ?: 0f else 0f
        val activity = max(max(clamped, max(leftLevel, rightLevel)), maxBand)
        if (activity < SILENCE_ACTIVITY_THRESHOLD) {
            if (silenceStartedAt <= 0L) silenceStartedAt = now
            if (!matrixReleasedForSilence && now - silenceStartedAt >= SILENCE_RELEASE_MS) {
                releaseMatrixForSilence()
            }
            return
        }
        silenceStartedAt = 0L

        if (matrixReleasedForSilence) {
            val registered = glyphMatrixManager.register(currentDeviceCode())
            if (!registered) {
                onStatusChanged("Glyph Matrix resume failed. Restart capture to retry.")
                return
            }
            matrixReleasedForSilence = false
        }

        if (now - lastRenderAt < FRAME_INTERVAL_MS) return
        lastRenderAt = now

        val litRows = (clamped * matrixLength).roundToInt().coerceIn(0, matrixLength)
        if (glyphMode == MODE_P3_MATRIX_BAR && litRows == lastLitRows) return
        if (glyphMode == MODE_P3_MATRIX_ALL_BRIGHTNESS && clamped <= ALL_BRIGHTNESS_OFF_THRESHOLD) {
            turnOff()
            return
        }
        lastLitRows = litRows

        if (frameBuffer.size != matrixLength * matrixLength) {
            frameBuffer = IntArray(matrixLength * matrixLength)
        }
        frameBuffer.fill(COLOR_OFF)

        val barWidth = (matrixLength / 8).coerceAtLeast(1)
        val leftCenter = (matrixLength * 0.35f).roundToInt().coerceIn(0, matrixLength - 1)
        val rightCenter = (matrixLength * 0.65f).roundToInt().coerceIn(0, matrixLength - 1)

        fun drawBar(centerX: Int, rows: Int) {
            val startX = (centerX - barWidth / 2).coerceAtLeast(0)
            val endXExclusive = (startX + barWidth).coerceAtMost(matrixLength)

            for (row in 0 until rows.coerceIn(0, matrixLength)) {
                val y = if (reverseDirection) row else (matrixLength - 1 - row)
                val rowOffset = y * matrixLength
                for (x in startX until endXExclusive) {
                    frameBuffer[rowOffset + x] = COLOR_ON
                }
            }
        }

        fun drawSpectrum(centerLowToHigh: Boolean) {
            val centerY = matrixLength / 2
            val maxPixelsByColumn = buildColumnMaxPixels(matrixLength, matrixDevice)

            fun sampleBandForColumn(x: Int): Float {
                val sampledX = if (!centerLowToHigh && reverseDirection) {
                    matrixLength - 1 - x
                } else {
                    x
                }
                if (spectrumBands.isNotEmpty()) {
                    val rawRatio = if (centerLowToHigh) {
                        val center = (matrixLength - 1f) / 2f
                        if (center <= 0f) 0f else (kotlin.math.abs(sampledX - center) / center).coerceIn(0f, 1f)
                    } else {
                        if (matrixLength <= 1) 0f else (sampledX / (matrixLength - 1f)).coerceIn(0f, 1f)
                    }
                    // Spectrum Center では方向トグルで中心/端の周波数割り当てを反転する
                    val ratio = if (centerLowToHigh && reverseDirection) 1f - rawRatio else rawRatio
                    val idx = (ratio * (spectrumBands.size - 1)).roundToInt().coerceIn(0, spectrumBands.lastIndex)
                    return spectrumBands[idx].coerceIn(0f, 1f)
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
                val activePx = (maxPx * clamped * band).roundToInt().coerceIn(0, maxPx)
                if (activePx <= 0) continue

                // 奇数画素で中央軸対称にする
                val oddPx = if (activePx % 2 == 0) (activePx - 1).coerceAtLeast(1) else activePx
                val half = oddPx / 2

                // 中央を軸に上下へ広がる（1列=1マス、列間ギャップなし）
                for (dy in 0..half) {
                    val yTop = (centerY - dy).coerceIn(0, matrixLength - 1)
                    val yBottom = (centerY + dy).coerceIn(0, matrixLength - 1)
                    frameBuffer[yTop * matrixLength + x] = COLOR_ON
                    frameBuffer[yBottom * matrixLength + x] = COLOR_ON
                }
            }
        }

        when (glyphMode) {
            MODE_P3_MATRIX_FIELD -> {
                // 端っこまで広げたフィールド：全幅を使用
                for (row in 0 until litRows.coerceIn(0, matrixLength)) {
                    val y = if (reverseDirection) row else (matrixLength - 1 - row)
                    val rowOffset = y * matrixLength
                    for (x in 0 until matrixLength) {
                        frameBuffer[rowOffset + x] = COLOR_ON
                    }
                }
            }
            MODE_P3_MATRIX_CIRCLE -> {
                // 中心からの距離で円を描画
                val center = (matrixLength - 1) / 2f
                val maxRadius = (matrixLength - 1) / 2f
                    val radius = (litRows / matrixLength.toFloat()) * maxRadius
                
                if (reverseDirection) {
                    // 反転時は背景を点灯させ、音が小さいほど内側を大きく削る
                    if (litRows <= 0) {
                        // 無音時は完全に削く（全て黒）
                        frameBuffer.fill(COLOR_OFF)
                    } else {
                        val cutoutRows = (matrixLength - litRows).coerceIn(0, matrixLength)
                        val cutoutRadius = (cutoutRows / matrixLength.toFloat()) * maxRadius
                        frameBuffer.fill(COLOR_ON)
                        for (y in 0 until matrixLength) {
                            val rowOffset = y * matrixLength
                            for (x in 0 until matrixLength) {
                                val dx = x - center
                                val dy = y - center
                                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                                if (distance <= cutoutRadius) {
                                    frameBuffer[rowOffset + x] = COLOR_OFF
                                }
                            }
                        }
                    }
                } else {
                    // 通常時：中心から円を拡大
                    for (y in 0 until matrixLength) {
                        val rowOffset = y * matrixLength
                        for (x in 0 until matrixLength) {
                            val dx = x - center
                            val dy = y - center
                            val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            
                            if (distance <= radius) {
                                frameBuffer[rowOffset + x] = COLOR_ON
                            }
                        }
                    }
                }
                
                // 無音時は中心の点を消灯
                if (litRows <= 0) {
                    val cx = center.roundToInt()
                    val cy = center.roundToInt()
                    val centerOffset = cy * matrixLength + cx
                    if (centerOffset >= 0 && centerOffset < frameBuffer.size) {
                        frameBuffer[centerOffset] = COLOR_OFF
                    }
                }
            }
            MODE_P3_MATRIX_SPECTRUM -> drawSpectrum(centerLowToHigh = false)
            MODE_P3_MATRIX_SPECTRUM_CENTER -> drawSpectrum(centerLowToHigh = true)
            MODE_P3_MATRIX_ALL_BRIGHTNESS -> {
                val normalizedRaw = if (allBrightnessAutoScaleEnabled) {
                    normalizeAllBrightnessLevel(clamped)
                } else {
                    clamped
                }
                if (normalizedRaw > ALL_BRIGHTNESS_OFF_THRESHOLD) {
                    val normalized = ((normalizedRaw - ALL_BRIGHTNESS_OFF_THRESHOLD) / (1f - ALL_BRIGHTNESS_OFF_THRESHOLD))
                        .coerceIn(0f, 1f)
                    val shaped = normalized.pow(ALL_BRIGHTNESS_RESPONSE_GAMMA)
                    val brightness = (ALL_BRIGHTNESS_MIN_LIGHT_MATRIX + ((ALL_BRIGHTNESS_MAX_LIGHT_MATRIX - ALL_BRIGHTNESS_MIN_LIGHT_MATRIX) * shaped)).roundToInt()
                        .coerceIn(0, 255)
                    frameBuffer.fill(brightness)
                } else {
                    frameBuffer.fill(COLOR_OFF)
                }
            }
            else -> drawBar(matrixLength / 2, litRows)
        }

        try {
            glyphMatrixManager.setAppMatrixFrame(frameBuffer)
            failureCount = 0
        } catch (error: GlyphException) {
            failureCount += 1
            if (failureCount >= 3) {
                AppLogger.e(TAG, "setAppMatrixFrame repeatedly failed. disabling matrix output", error)
                isSessionOpen = false
                onStatusChanged("Glyph Matrix update failed. Restart capture to retry.")
            }
        } catch (error: Throwable) {
            failureCount += 1
            if (failureCount >= 3) {
                AppLogger.e(TAG, "setAppMatrixFrame crashed repeatedly. disabling matrix output", error)
                isSessionOpen = false
                onStatusChanged("Glyph Matrix update crashed. Restart capture to retry.")
            }
        }
    }

    override fun turnOff() {
        silenceStartedAt = 0L
        matrixReleasedForSilence = false
        try {
            glyphMatrixManager.turnOff()
        } catch (error: Throwable) {
            AppLogger.w(TAG, "turnOff ignored", error)
        }
        try {
            glyphMatrixManager.closeAppMatrix()
        } catch (_: Throwable) {
        }
    }

    private fun releaseMatrixForSilence() {
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
        matrixReleasedForSilence = true
    }

    private fun currentDeviceCode(): String {
        return when (matrixDevice) {
            MatrixDevice.PHONE3 -> Glyph.DEVICE_23112
            MatrixDevice.PHONE4A_PRO -> Glyph.DEVICE_25111p
        }
    }

    private fun buildColumnMaxPixels(length: Int, device: MatrixDevice): IntArray {
        val profile = when (device) {
            // Phone (3)
            MatrixDevice.PHONE3 -> intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25,
                25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7
            )
            // Phone (4a) Pro: 端=5, 2番目=9, 3-4番目=11, 5番目〜中央=13
            MatrixDevice.PHONE4A_PRO -> intArrayOf(
                5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5
            )
        }
        if (length == profile.size) return profile

        val out = IntArray(length)
        for (i in 0 until length) {
            val src = if (length <= 1) (profile.lastIndex / 2f) else i * (profile.lastIndex.toFloat() / (length - 1f))
            val srcIdx = src.roundToInt().coerceIn(0, profile.lastIndex)
            val scaled = (profile[srcIdx] * (length / profile.size.toFloat())).roundToInt().coerceAtLeast(1)
            out[i] = if (scaled % 2 == 0) (scaled - 1).coerceAtLeast(1) else scaled
        }
        return out
    }
}
