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
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixTrackTitleOutputTest {
    @Test fun sdkCoverageChangesAreSentEvenWhenDimBackgroundHidesPreviewDifference() {
        val context = RuntimeEnvironment.getApplication()
        ShadowBuild.setModel(Glyph.DEVICE_23112)
        val manager = GlyphMatrixManager.getInstance(context)
        val field = GlyphMatrixManager::class.java.getDeclaredField("mService").apply { isAccessible = true }
        val original = field.get(manager)
        val sent = mutableListOf<IntArray>()
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
        try {
            val controller = GlyphMatrixController(context, {})
            val submit = GlyphMatrixController::class.java.getDeclaredMethod("submitMatrixFrame",
                IntArray::class.java, IntArray::class.java).apply { isAccessible = true }
            val preview = IntArray(625) { 40 }
            val firstSdk = IntArray(625) { 40 }.apply { this[312] = 48 }
            val nextSdk = firstSdk.copyOf().apply { this[312] = 64 }
            submit.invoke(controller, preview, firstSdk)
            submit.invoke(controller, preview, nextSdk)
            submit.invoke(controller, preview, nextSdk)
            assertEquals(2, sent.size)
            assertArrayEquals(firstSdk, sent[0])
            assertArrayEquals(nextSdk, sent[1])
        } finally { field.set(manager, original) }
    }
}
