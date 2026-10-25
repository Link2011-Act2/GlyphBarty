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
import android.os.SystemClock
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixTrackTitleOutputTest {
    private class PlaybackFixture(private val output: OutputFixture) : AutoCloseable {
        val session = MediaSession(output.context, "normal-matrix-test")
        var musicOnly: Boolean? = null
        private var suppressed = false
        init {
            Settings.Secure.putString(output.context.contentResolver, "enabled_notification_listeners",
                ComponentName(output.context, MediaSessionNotificationListenerService::class.java).flattenToString())
            shadowOf(session.controller).setPackageName("music")
            shadowOf(session.controller).setMetadata(MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "same-song")
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Same song").build())
            state(PlaybackState.STATE_PLAYING)
            shadowOf(output.context.getSystemService(MediaSessionManager::class.java)).addController(session.controller)
        }
        fun state(state: Int) {
            shadowOf(session.controller).setPlaybackState(PlaybackState.Builder().setState(state, 0, 1f).build())
        }
        fun tick(ms: Long, level: Float = 0f) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
            MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
            val allowed = musicOnly?.let { output.controller.isNormalMatrixPlaybackOutputAllowed(it, level) } ?: true
            if (!allowed) {
                if (!suppressed) output.controller.suspendSession()
            } else output.controller.updateLevel(level)
            suppressed = !allowed
        }
        fun analysis(rawPeak: Float, pcmAmplitude: Float, level: Float) {
            output.controller.updateAnalysis(pcmAmplitude, pcmAmplitude, level, level,
                FloatArray(25) { if (rawPeak > 0f) 1f else 0f }, rawPeak, 0f,
                FloatArray(64) { if (it % 2 == 0) pcmAmplitude else -pcmAmplitude }, null, null)
        }
        override fun close() { session.release() }
    }
    private class OutputFixture(val profile: GlyphDeviceProfile, val emulate: Boolean = false) : AutoCloseable {
        val context = RuntimeEnvironment.getApplication()
        val length = if (profile == GlyphDeviceProfile.PHONE3_MATRIX) 25 else 13
        val logicalProfile = if (emulate) GlyphDeviceProfile.PHONE4A_PRO_MATRIX else profile
        val manager: GlyphMatrixManager
        val field = GlyphMatrixManager::class.java.getDeclaredField("mService").apply { isAccessible = true }
        val original: Any?
        private val executorField = GlyphMatrixManager::class.java.getDeclaredField("mExecutor").apply { isAccessible = true }
        private val taskField = GlyphMatrixManager::class.java.getDeclaredField("mTask").apply { isAccessible = true }
        private val originalExecutor: Any?
        private val originalTask: Any?
        val sent = mutableListOf<IntArray>()
        var framesToFail = 0
        var registrationsToFail = 0
        val registrationTimes = mutableListOf<Long>()
        var closeCount = 0
        var blackoutCount = 0
        var visible = false
        val controller: GlyphMatrixController
        private val submit = GlyphMatrixController::class.java.getDeclaredMethod("submitMatrixFrame",
            IntArray::class.java).apply { isAccessible = true }

        init {
            ShadowBuild.setModel(if (length == 25) Glyph.DEVICE_23112 else Glyph.DEVICE_25111p)
            // Default fixture explicitly exercises audio fallback. Media tests opt into a session
            // below; do not inherit the singleton snapshot left by a different Robolectric test.
            Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", "")
            MediaSessionPlaybackGate.currentPlaybackSnapshot(context, forceRefresh = true)
            manager = GlyphMatrixManager.getInstance(context)
            original = field.get(manager)
            originalExecutor = executorField.get(manager)
            originalTask = taskField.get(manager)
            taskField.set(manager, null)
            // SDK turnOff normally queues a global clear. Execute it synchronously in this fake
            // so lifecycle assertions cannot race an unrelated executor thread.
            executorField.set(manager, object : AbstractExecutorService() {
                override fun execute(command: Runnable) = command.run()
                override fun shutdown() = Unit
                override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
                override fun isShutdown() = false
                override fun isTerminated() = false
                override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
            })
            val service = Proxy.newProxyInstance(IGlyphService::class.java.classLoader,
                arrayOf(IGlyphService::class.java)) { _, method, arguments ->
                when (method.name) {
                    "setAppMatrixColors" -> {
                        if (framesToFail > 0) {
                            framesToFail--
                            throw IllegalStateException("fake SDK frame failure")
                        }
                        sent += (requireNotNull(arguments)[0] as IntArray).copyOf()
                        visible = true
                    }
                    "registerMatrixSDK" -> {
                        registrationTimes += SystemClock.elapsedRealtime()
                        if (registrationsToFail > 0) {
                            registrationsToFail--
                            return@newProxyInstance false
                        }
                    }
                    "closeAppMatrix" -> { closeCount++; visible = false }
                    "setMatrixColors" -> { blackoutCount++; visible = false }
                }
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
            setControllerField("isBound", true)
            setControllerField("bindRequested", true)
            setControllerField("sdkConnected", true)
            setControllerField("glyphOutputAllowed", true)
            setControllerField("phone4aProEmulatedOnPhone3", emulate)
        }

        fun setControllerField(name: String, value: Any) {
            GlyphMatrixController::class.java.getDeclaredField(name).apply { isAccessible = true }.set(controller, value)
        }

        fun send(frame: IntArray) { submit.invoke(controller, frame) }
        fun updateAfter(ms: Long, level: Float = 0.5f) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
            controller.updateLevel(level)
        }
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

        override fun close() {
            field.set(manager, original)
            executorField.set(manager, originalExecutor)
            taskField.set(manager, originalTask)
            val monitor = GlyphMatrixController::class.java.getDeclaredField("nothingOsGlyphSettingsMonitor")
                .apply { isAccessible = true }.get(controller) as NothingOsGlyphSettingsMonitor
            monitor.stop()
        }
    }

    @Test fun playingSilenceKeepsEveryNormalMatrixModeRegisteredAndConvergesToZero() {
        for ((profile, emulate) in listOf(GlyphDeviceProfile.PHONE3_MATRIX to false,
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX to false, GlyphDeviceProfile.PHONE3_MATRIX to true)) {
            OutputFixture(profile, emulate).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.setExperimentalPerformanceOptimizationsEnabled(true)
                    for (pattern in GlyphPatternRegistry.patternsFor(output.logicalProfile)
                        .filter { it.recipe.renderMode != GlyphPatternRenderMode.MATRIX_OPEN_REEL }) {
                        output.controller.setGlyphMode(pattern.id)
                        repeat(4) { playback.analysis(0.2f, 0.1f, 0.5f); playback.tick(100, 0.5f) }
                        repeat(100) { playback.analysis(0f, 0f, 0f); playback.tick(50) }
                        assertEquals("Unexpected release in ${pattern.id}", 0, output.closeCount)
                        assertTrue("Residual light in ${pattern.id} $profile emulate=$emulate", output.sent.last().all { it == 0 })
                        assertEquals("Stale meter in ${pattern.id}", 0f, output.controller.previewLevel(), 0f)
                        output.assertEncoding()
                    }
                }
            }
        }
    }

    @Test fun spectrumNoiseCannotBecomeFullScaleOrKeepAgcAliveAndAudioReturnsPromptly() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            for (strategy in GlyphAutoScaleStrategy.entries) {
                OutputFixture(profile).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setSpectrumAutoScaleEnabled(true)
                        output.controller.setLevelAutoScaleEnabled(true)
                        output.controller.setAutoScaleStrategy(strategy)
                        output.controller.setAutoScaleOffset(0.2f)
                        for (pattern in GlyphPatternRegistry.patternsFor(profile).filter { it.kind == GlyphPatternKind.SPECTRUM }) {
                            output.controller.setGlyphMode(pattern.id)
                            repeat(4) { playback.analysis(0.2f, 0.1f, 0.5f); playback.tick(100, 0.5f) }
                            // A normalized shape can still have a peak of one at this noise floor.
                            repeat(80) { playback.analysis(0.0002f, 0.00002f, 0.01f); playback.tick(50, 0.01f) }
                            assertTrue("Noise remained in ${pattern.id} $profile $strategy", output.sent.last().all { it == 0 })
                            val gain = GlyphMatrixController::class.java.getDeclaredField("spectrumAutoGain")
                                .apply { isAccessible = true }.get(output.controller)
                            assertEquals(1f, gain.javaClass.getDeclaredField("gain").apply { isAccessible = true }.getFloat(gain), 0f)
                            val levelGain = GlyphMatrixController::class.java.getDeclaredField("levelAutoGain")
                                .apply { isAccessible = true }.get(output.controller)
                            assertEquals(1f, levelGain.javaClass.getDeclaredField("gain").apply { isAccessible = true }.getFloat(levelGain), 0f)
                            playback.analysis(0.2f, 0.1f, 0.5f)
                            playback.tick(50, 0.5f)
                            // Spectrogram shifts once per interval; all modes must resume without registration.
                            assertTrue("Audio did not return in ${pattern.id}", output.sent.last().any { it > 0 })
                            assertEquals(0, output.closeCount)
                        }
                    }
                }
            }
        }
    }

    @Test fun verySoftMusicSurvivesTheRawSignalGateOnBothDevices() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_SPECTRUM_BOTTOM)
                    output.controller.setSpectrumAutoScaleEnabled(true)
                    output.controller.setLevelAutoScaleEnabled(true)
                    repeat(10) { playback.analysis(0.004f, 0.0001f, 0.04f); playback.tick(50, 0.04f) }
                    assertTrue(output.sent.last().any { it > 0 })
                    assertEquals(0, output.closeCount)
                }
            }
        }
    }

    @Test fun normalizedCaptureNoiseCannotLightSpectrumWhenTheSourceEnvelopeIsZero() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            for (strategy in GlyphAutoScaleStrategy.entries) {
                OutputFixture(profile).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setSpectrumAutoScaleEnabled(true)
                        output.controller.setLevelAutoScaleEnabled(true)
                        output.controller.setAutoScaleStrategy(strategy)
                        output.controller.setAutoScaleOffset(0.2f)
                        for (pattern in GlyphPatternRegistry.patternsFor(profile).filter { it.kind == GlyphPatternKind.SPECTRUM }) {
                            output.controller.setGlyphMode(pattern.id)
                            repeat(4) { playback.analysis(0.2f, 0.1f, 0.5f); playback.tick(100, 0.5f) }
                            // OutputMix uses normalized 8-bit waveforms. One LSB is ~0.0078,
                            // so this case must also respect the zero capture envelope.
                            repeat(80) { playback.analysis(0.02f, 1f / 128f, 0f); playback.tick(50) }
                            assertTrue("Floor remained in ${pattern.id} $profile $strategy", output.sent.last().all { it == 0 })
                            assertEquals(0, output.closeCount)
                        }
                    }
                }
            }
        }
    }

    @Test fun confirmedPauseReturnsControlAndPlayingSilenceReregistersWithoutAudio() {
        for (musicOnly in listOf(false, true)) {
            OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = musicOnly
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    playback.tick(1000, 0.5f)
                    playback.tick(1000, 0.5f) // Music-only initial confirmation.
                    val closes = output.closeCount
                    playback.state(PlaybackState.STATE_PAUSED)
                    playback.tick(100)
                    playback.tick(399)
                    assertEquals(closes, output.closeCount)
                    playback.tick(1)
                    assertEquals(closes + 1, output.closeCount)
                    playback.state(PlaybackState.STATE_PLAYING)
                    playback.tick(100)
                    playback.tick(1000)
                    assertEquals(closes + 1, output.closeCount)
                    assertTrue(output.registrationTimes.isNotEmpty())
                    playback.state(PlaybackState.STATE_STOPPED)
                    playback.tick(50)
                    assertEquals(closes + 2, output.closeCount)
                }
            }
        }
    }

    @Test fun openReelStillUsesTwoSecondBlackoutThreeSecondReleaseAndPausedHold() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                playback.tick(1000, 0.5f)
                playback.tick(1)
                playback.tick(1999)
                assertEquals(0, output.blackoutCount)
                playback.tick(1)
                assertEquals(1, output.blackoutCount)
                playback.tick(999)
                assertEquals(0, output.closeCount)
                playback.tick(1)
                assertEquals(1, output.closeCount)
                playback.state(PlaybackState.STATE_PAUSED)
                playback.tick(100)
                playback.tick(29_999)
                assertEquals(1, output.closeCount) // The service watchdog, not the new policy, owns 30s expiry.
                assertTrue(output.visible)
            }
        }
    }

    @Test fun smoothedLevelTailDoesNotPreventPauseReleaseWhenTheRawSignalHasStopped() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_SPECTRUM)
                playback.analysis(0.2f, 0.1f, 0.5f)
                playback.tick(1000, 0.5f)
                playback.state(PlaybackState.STATE_PAUSED)
                playback.analysis(0f, 0f, 0.5f) // Envelope still high, absolute audio has stopped.
                playback.tick(100, 0.5f)
                playback.tick(400, 0.5f)
                assertEquals(1, output.closeCount)
            }
        }
    }

    @Test fun silenceThresholdsAndAudioResumePreserveTheSessionLifecycle() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000)
                output.updateAfter(1, 0f)
                output.updateAfter(119, 0f)
                assertEquals(0, output.blackoutCount)
                output.updateAfter(1, 0f)
                assertEquals(1, output.blackoutCount)
                assertEquals(0, output.closeCount)
                output.updateAfter(329, 0f)
                assertEquals(0, output.closeCount)
                output.updateAfter(1, 0f)
                assertEquals(1, output.closeCount)
                output.updateAfter(500, 0f)
                assertTrue(output.registrationTimes.isEmpty())
                output.updateAfter(50)
                assertEquals(1, output.registrationTimes.size)
                assertTrue(output.visible)
                output.assertEncoding()
                output.updateAfter(1, 0f)
                output.updateAfter(100)
                assertEquals(1, output.closeCount) // A short track gap never releases.
                assertTrue(output.visible)
            }
        }
    }

    @Test fun threeSendFailuresRecoverAfterBackoffInsteadOfPermanentlyReturningEarly() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000)
            output.framesToFail = 3
            repeat(3) { output.send(IntArray(625) { 128 }) }
            assertEquals(1, output.closeCount)
            val count = output.sent.size
            output.updateAfter(499)
            assertEquals(count, output.sent.size)
            assertTrue(output.registrationTimes.isEmpty())
            output.updateAfter(1)
            assertEquals(1, output.registrationTimes.size)
            assertTrue(output.sent.size > count)
            output.assertEncoding()
        }
    }

    @Test fun failedResumeRegistrationsBackOffAndStopAfterFiveAttempts() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000)
            output.controller.suspendSession()
            output.registrationsToFail = 99
            output.updateAfter(1)
            repeat(499) { output.updateAfter(1) }
            assertEquals(1, output.registrationTimes.size)
            output.updateAfter(1)
            assertEquals(2, output.registrationTimes.size)
            output.updateAfter(1000)
            output.updateAfter(2000)
            output.updateAfter(4000)
            assertEquals(5, output.registrationTimes.size)
            output.updateAfter(60_000)
            assertEquals(5, output.registrationTimes.size)
            assertFalse(output.visible)
        }
    }

    @Test fun registrationFailureThenSuccessRestoresOutputAndInvalidatesTheOldFrame() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000)
            val original = output.sent.last()
            output.controller.suspendSession()
            output.registrationsToFail = 1
            output.updateAfter(1)
            assertFalse(output.visible)
            output.updateAfter(500)
            assertEquals(2, output.registrationTimes.size)
            assertTrue(output.visible)
            assertArrayEquals(original, output.sent.last())
        }
    }

    @Test fun reusedFramesRefreshAfterOneSecondAndCanRedrawAfterAnExternalBlackout() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.controller.setBinaryMode(true)
            output.controller.setExperimentalPerformanceOptimizationsEnabled(true)
            output.updateAfter(1000)
            val count = output.sent.size
            output.visible = false // Simulate OS-side loss of the displayed frame; app cache remains.
            repeat(9) { output.updateAfter(100) }
            assertEquals(count, output.sent.size)
            assertFalse(output.visible)
            output.updateAfter(100)
            assertEquals(count + 1, output.sent.size)
            assertTrue(output.visible)
            output.assertEncoding()
        }
    }

    @Test fun intentionalStopOsDisableAndDisconnectionNeverRunRegistrationRecovery() {
        for (stop in listOf("release", "unbind", "os", "disconnect")) {
            OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
                output.framesToFail = 3
                repeat(3) { output.send(IntArray(625) { 128 }) }
                when (stop) {
                    "release" -> output.controller.releaseSession()
                    "unbind" -> output.setControllerField("bindRequested", false)
                    "os" -> output.setControllerField("glyphOutputAllowed", false)
                    "disconnect" -> output.setControllerField("sdkConnected", false)
                }
                output.updateAfter(60_000)
                assertTrue("Unexpected recovery after $stop", output.registrationTimes.isEmpty())
                assertTrue(output.sent.isEmpty())
            }
        }
    }

    @Test fun sdkReconnectionAndExplicitRestartReopenButLateCallbacksDoNotUndoAStop() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            fun callback(name: String) = GlyphMatrixController::class.java.getDeclaredMethod(name)
                .apply { isAccessible = true }.invoke(output.controller)
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000)
            callback("handleServiceDisconnected")
            output.updateAfter(1000)
            assertTrue(output.registrationTimes.isEmpty())
            callback("handleServiceConnected")
            output.updateAfter(100)
            assertEquals(1, output.registrationTimes.size)
            assertTrue(output.visible)
            output.controller.releaseSession()
            callback("handleServiceConnected")
            output.updateAfter(1000)
            assertEquals(1, output.registrationTimes.size)
            assertFalse(output.visible)
            output.controller.bind()
            output.updateAfter(1000)
            assertEquals(2, output.registrationTimes.size)
            assertTrue(output.visible)
        }
    }

    @Test fun osDisablePreservesResumeIntentButNeverOverridesAnExplicitStop() {
        for (stopped in listOf(false, true)) {
            OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
                fun callback(name: String) = GlyphMatrixController::class.java.getDeclaredMethod(name)
                    .apply { isAccessible = true }.invoke(output.controller)
                if (stopped) output.controller.releaseSession()
                // The fixture has no Android service binding. Exercise the OS release transition
                // without calling Context.unbindService on a fabricated connection.
                output.setControllerField("isBound", false)
                output.setControllerField("glyphOutputAllowed", false)
                callback("unbindSdk")
                output.setControllerField("isBound", true)
                output.setControllerField("glyphOutputAllowed", true)
                callback("handleServiceConnected")
                output.updateAfter(1000)
                assertEquals(if (stopped) 0 else 1, output.registrationTimes.size)
                assertEquals(!stopped, output.visible)
            }
        }
    }

    @Test fun failedInitialRegistrationIsRetriedFromActiveAudioOnly() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.registrationsToFail = 1
            GlyphMatrixController::class.java.getDeclaredMethod("handleServiceConnected")
                .apply { isAccessible = true }.invoke(output.controller)
            assertEquals(1, output.registrationTimes.size)
            output.updateAfter(2000, 0f)
            assertEquals(1, output.registrationTimes.size)
            output.updateAfter(100)
            assertEquals(2, output.registrationTimes.size)
            assertTrue(output.visible)
            output.assertEncoding()
        }
    }

    @Test fun healthyRapidTrackGapsDoNotExhaustTheFailureRecoveryBudget() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000)
            repeat(12) {
                output.updateAfter(1, 0f)
                output.updateAfter(450, 0f)
                output.updateAfter(50)
                assertTrue("Failed after rapid track gap $it", output.visible)
            }
            assertEquals(12, output.registrationTimes.size)
            output.assertEncoding()
        }
    }

    @Test fun successfulRegistrationWithContinuouslyFailingFramesCannotRetryForever() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            output.framesToFail = 99
            repeat(3) { output.send(IntArray(625) { 128 }) }
            repeat(5) { attempt ->
                output.updateAfter(500L shl attempt)
                output.updateAfter(33)
                output.updateAfter(33)
                assertEquals(attempt + 1, output.registrationTimes.size)
            }
            output.updateAfter(60_000)
            assertEquals(5, output.registrationTimes.size)
            assertEquals(6, output.closeCount)
            assertTrue(output.sent.isEmpty())
        }
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
                MatrixTrackTitleCompositor.compose(background, text, MatrixTrackTitleAnimation.BACKGROUND_LEVEL, output.length,
                    GlyphMatrixPreviewGeometry.columnHeights(profile, output.length), frame,
                    preserveTextBrightness = profile == GlyphDeviceProfile.PHONE3_MATRIX)
                output.send(frame)
                output.assertEncoding()
                assertEquals(321, output.sent.last()[frame.size / 2 + 1])
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
