package jp.linkserver.glyphvisualizer.glyph

import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.thirdparty.IGlyphService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemClock
import java.lang.reflect.Proxy
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixTrackTitleOutputTest {
    private class OutputFixture(val profile: GlyphDeviceProfile, val emulate: Boolean = false) : AutoCloseable {
        val context = RuntimeEnvironment.getApplication()
        val length = if (profile == GlyphDeviceProfile.PHONE3_MATRIX) 25 else 13
        val logicalProfile = if (emulate) GlyphDeviceProfile.PHONE4A_PRO_MATRIX else profile
        val manager: GlyphMatrixManager
        val field = GlyphMatrixManager::class.java.getDeclaredField("mService").apply { isAccessible = true }
        val original: Any?
        val sent = mutableListOf<IntArray>()
        val controller: GlyphMatrixController
        private val submit = GlyphMatrixController::class.java.getDeclaredMethod("submitMatrixFrame",
            IntArray::class.java).apply { isAccessible = true }

        init {
            ShadowBuild.setModel(if (length == 25) Glyph.DEVICE_23112 else Glyph.DEVICE_25111p)
            manager = GlyphMatrixManager.getInstance(context)
            original = field.get(manager)
            val service = Proxy.newProxyInstance(IGlyphService::class.java.classLoader,
                arrayOf(IGlyphService::class.java)) { _, method, arguments ->
                if (method.name == "setAppMatrixColors") sent += (requireNotNull(arguments)[0] as IntArray).copyOf()
                when (method.returnType) {
                    java.lang.Boolean.TYPE -> true
                    java.lang.Integer.TYPE -> 0
                    else -> null
                }
            }
            field.set(manager, service)
            controller = GlyphMatrixController(context, {})
            // Simulate an established SDK session; use the real render and SDK send methods.
            setControllerField("physicalMatrixProfile", profile)
            setControllerField("matrixProfile", logicalProfile)
            setControllerField("physicalMatrixLength", length)
            setControllerField("matrixLength", if (emulate) 13 else length)
            setControllerField("isSessionOpen", true)
            setControllerField("phone4aProEmulatedOnPhone3", emulate)
        }

        private fun setControllerField(name: String, value: Any) {
            GlyphMatrixController::class.java.getDeclaredField(name).apply { isAccessible = true }.set(controller, value)
        }

        fun send(frame: IntArray) { submit.invoke(controller, frame) }
        fun internalFrame(): IntArray = (GlyphMatrixController::class.java.getDeclaredField("lastSentFrameBuffer")
            .apply { isAccessible = true }.get(controller) as IntArray).copyOf()

        fun render(mode: String, level: Float = 0.5f) {
            controller.setGlyphMode(mode)
            repeat(3) {
                ShadowSystemClock.advanceBy(Duration.ofMillis(100))
                controller.updateAnalysis(0.5f, 0.5f, 0.5f, 0.5f, FloatArray(32) { 0.5f }, 0.5f,
                    0f, FloatArray(64) { if (it % 2 == 0) 0.5f else -0.5f }, null, null)
                controller.updateLevel(level)
            }
        }

        fun assertEncoding() {
            val internal = internalFrame()
            assertEquals(length * length, internal.size)
            assertTrue(internal.all { it in 0..255 })
            val heights = GlyphMatrixPreviewGeometry.columnHeights(profile, length)
            for (y in 0 until length) for (x in 0 until length) {
                val top = (length - heights[x]) / 2
                val expected = if (y in top until top + heights[x]) MatrixFrameBrightness.toSdk(internal[y * length + x]) else 0
                assertEquals(expected, sent.last()[y * length + x])
            }
        }

        override fun close() { field.set(manager, original) }
    }

    @Test fun sharedBoundaryEncodesNormalFramesOnceAndDeduplicatesInEightBitScale() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                val first = IntArray(output.length * output.length) { 40 }.apply { this[size / 2] = 128 }
                val original = first.copyOf()
                output.send(first)
                output.assertEncoding()
                output.send(first)
                assertEquals(1, output.sent.size)
                val next = first.copyOf().apply { this[size / 2] = 129 }
                output.send(next)
                output.assertEncoding()
                assertEquals(2, output.sent.size)
                assertEquals(2056, output.sent[0][first.size / 2])
                assertEquals(2072, output.sent[1][first.size / 2])
                assertArrayEquals(original, first)
                assertArrayEquals(next, output.internalFrame())
            }
        }
    }

    @Test fun everyMatrixModeUsesTheSameSdkScaleOnBothDevicesAndEmulation() {
        for ((profile, emulate) in listOf(GlyphDeviceProfile.PHONE3_MATRIX to false,
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX to false, GlyphDeviceProfile.PHONE3_MATRIX to true)) {
            OutputFixture(profile, emulate).use { output ->
                output.controller.setOutputGamma(2.2f)
                output.controller.setLevelAutoScaleEnabled(true)
                output.controller.setSpectrumAutoScaleEnabled(true)
                output.controller.setAllBrightnessAutoScaleEnabled(true)
                output.controller.setExperimentalPerformanceOptimizationsEnabled(true)
                for (pattern in GlyphPatternRegistry.patternsFor(output.logicalProfile)) {
                    output.render(pattern.id)
                    output.assertEncoding()
                }
                assertTrue(output.sent.any { frame -> frame.any { it > 255 } })
                if (emulate) assertTrue(output.sent.last().take(6 * 25).all { it == 0 })
            }
        }
    }

    @Test fun binaryModeRemainsOffOrFullBrightness() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                output.controller.setBinaryMode(true)
                output.render(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.assertEncoding()
                assertTrue(output.internalFrame().all { it == 0 || it == 255 })
                assertTrue(output.sent.last().all { it == 0 || it == 4095 })
                assertTrue(output.sent.last().any { it == 4095 })
            }
        }
    }

    @Test fun titleAndBackgroundAreComposedBeforeTheSingleOutputConversion() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                val frame = IntArray(output.length * output.length)
                val background = IntArray(frame.size) { 200 }
                val text = IntArray(frame.size).apply { this[size / 2] = 128 }
                MatrixTrackTitleCompositor.compose(background, text, 0.2f, output.length,
                    GlyphMatrixPreviewGeometry.columnHeights(profile, output.length), frame,
                    preserveTextBrightness = profile == GlyphDeviceProfile.PHONE3_MATRIX)
                output.send(frame)
                output.assertEncoding()
                assertEquals(642, output.sent.last()[frame.size / 2 + 1])
                assertEquals(if (profile == GlyphDeviceProfile.PHONE3_MATRIX) 2056 else 4095,
                    output.sent.last()[frame.size / 2])
            }
        }
    }

    @Test fun previewOnlyControllerStillPublishesEightBitFramesWithoutSdkCalls() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                val frames = mutableListOf<GlyphPreviewFrame.Matrix>()
                val preview = GlyphMatrixController(output.context, {}, previewDeviceProfile = profile,
                    previewFrameListener = { frames += it })
                preview.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                preview.setBinaryMode(true)
                ShadowSystemClock.advanceBy(Duration.ofMillis(100))
                preview.updateLevel(0.5f)
                assertTrue(frames.last().pixels.all { it == 0 || it == 255 })
                assertTrue(frames.last().pixels.any { it == 255 })
                assertTrue(output.sent.isEmpty())
            }
        }
    }
}
