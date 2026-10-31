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
import android.os.Handler
import android.os.Looper
import android.media.AudioManager
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowMediaSessionManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixTrackTitleOutputTest {
    @Implements(MediaSessionManager::class)
    class CountingMediaSessionManager : ShadowMediaSessionManager() {
        var reads = 0
        @Implementation
        public override fun getActiveSessions(listener: ComponentName?): List<MediaController> {
            reads++
            return super.getActiveSessions(listener)
        }
    }

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
        fun tick(ms: Long, level: Float = 0f, refreshSnapshot: Boolean = true) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
            if (refreshSnapshot) MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
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
        override fun close() {
            session.release()
            // Robolectric's manager keeps controllers after MediaSession.release(). Do
            // not let a previous device fixture win active-session selection in the next.
            shadowOf(output.context.getSystemService(MediaSessionManager::class.java)).clearControllers()
        }
    }
    private class OutputFixture(val profile: GlyphDeviceProfile, val emulate: Boolean = false,
        mediaPlaybackOnlyProvider: (() -> Boolean)? = null,
        recoveryTestMode: MatrixRecoveryTestMode = MatrixRecoveryTestMode.STANDARD,
        diagnosticsEnabled: Boolean = true) : AutoCloseable {
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
        val sdkCalls = mutableListOf<String>()
        var framesToFail = 0
        var frameAttempts = 0
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
            MatrixRecoveryTestSettings.save(context, recoveryTestMode)
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
                        frameAttempts++
                        if (framesToFail > 0) {
                            framesToFail--
                            throw IllegalStateException("fake SDK frame failure")
                        }
                        sent += (requireNotNull(arguments)[0] as IntArray).copyOf()
                        sdkCalls += "frame:${(arguments[0] as IntArray).maxOrNull()}"
                        visible = true
                    }
                    "registerMatrixSDK" -> {
                        registrationTimes += SystemClock.elapsedRealtime()
                        if (registrationsToFail > 0) {
                            registrationsToFail--
                            sdkCalls += "register:false"
                            return@newProxyInstance false
                        }
                        sdkCalls += "register:true"
                    }
                    "closeAppMatrix" -> { sdkCalls += "closeAppMatrix"; closeCount++; visible = false }
                    "setMatrixColors" -> { sdkCalls += "turnOff"; blackoutCount++; visible = false }
                }
                when (method.returnType) {
                    java.lang.Boolean.TYPE -> true
                    java.lang.Integer.TYPE -> 0
                    else -> null
                }
            }
            field.set(manager, service)
            controller = GlyphMatrixController(context, {}, ownerHandler = Handler(Looper.getMainLooper()),
                mediaPlaybackOnlyProvider = mediaPlaybackOnlyProvider,
                matrixRecoveryDiagnosticsEnabled = diagnosticsEnabled)
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

        fun callback(name: String) = GlyphMatrixController::class.java.getDeclaredMethod(name)
            .apply { isAccessible = true }.invoke(controller)

        fun sessionOpen() = GlyphMatrixController::class.java.getDeclaredField("isSessionOpen")
            .apply { isAccessible = true }.getBoolean(controller)

        fun pendingPlaybackCheck() = GlyphMatrixController::class.java.getDeclaredField("normalPlaybackCheck")
            .apply { isAccessible = true }.get(controller) as Runnable?

        fun idleWithoutFrames(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

        fun assertFrameRegistrationOrder() {
            var registered = true // The fixture begins with an established session.
            sdkCalls.forEach { call ->
                when (call) {
                    "closeAppMatrix", "register:false" -> registered = false
                    "register:true" -> registered = true
                    else -> if (call.startsWith("frame:")) assertTrue("Frame after close without register: $sdkCalls", registered)
                }
            }
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
            controller.releaseSession()
            MatrixRecoveryTestSettings.save(context, MatrixRecoveryTestMode.STANDARD)
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

    @Test fun sdkRegistrationSurvivesInitialPlayingConfirmationWithBlackFramesOnBothDevices() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    playback.musicOnly = true
                    output.callback("handleServiceConnected")
                    assertEquals(1, output.registrationTimes.size)
                    playback.tick(1000, 0.5f)
                    playback.tick(999, 0.5f)
                    assertTrue(output.sessionOpen())
                    assertEquals(0, output.closeCount)
                    assertEquals(0, output.blackoutCount)
                    assertEquals(1, output.registrationTimes.size)
                    assertTrue(output.sent.isNotEmpty())
                    assertTrue(output.sent.all { frame -> frame.size == output.length * output.length && frame.all { it == 0 } })
                    playback.tick(1, 0.5f)
                    assertEquals(1, output.registrationTimes.size)
                    assertEquals(0, output.closeCount)
                    assertTrue(output.sent.last().any { it > 0 })
                    output.assertEncoding()
                }
            }
        }
    }

    @Test fun directControllerConfirmationAlsoSuppressesPixelsWithoutClosingSession() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.setControllerField("normalMediaPlaybackOnly", true)
                output.callback("handleServiceConnected")
                playback.tick(1000, 0.5f) // Call updateLevel directly, without the service gate.
                assertTrue(output.sessionOpen())
                assertTrue(output.sent.last().all { it == 0 })
                assertEquals(0, output.closeCount)
                assertEquals(0, output.blackoutCount)
                playback.tick(1000, 0.5f)
                assertEquals(1, output.registrationTimes.size)
                assertTrue(output.sent.last().any { it > 0 })
            }
        }
    }

    @Test fun briefPauseBufferingAndMissingSessionKeepTheSameSdkRegistration() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            OutputFixture(profile).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = true
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    output.callback("handleServiceConnected")
                    playback.tick(1000, 0.5f)
                    playback.tick(1000, 0.5f)
                    repeat(5) {
                        for ((state, duration) in listOf(PlaybackState.STATE_PAUSED to 399L,
                            PlaybackState.STATE_BUFFERING to 2999L, PlaybackState.STATE_NONE to 749L)) {
                            playback.state(state)
                            playback.tick(1)
                            playback.tick(duration)
                            playback.state(PlaybackState.STATE_PLAYING)
                            playback.tick(1, 0.5f)
                            assertEquals(1, output.registrationTimes.size)
                            assertEquals(0, output.closeCount)
                            assertEquals(0, output.blackoutCount)
                            assertTrue(output.sessionOpen())
                        }
                    }
                }
            }
        }
    }

    @Test fun confirmationCanBecomeConfirmedPauseWithoutAnotherServiceSuppressionEdge() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.callback("handleServiceConnected")
                playback.tick(1000, 0.5f)
                playback.state(PlaybackState.STATE_PAUSED)
                playback.tick(100, 0.5f)
                playback.tick(399, 0.5f)
                assertEquals(0, output.closeCount)
                assertTrue(output.sent.last().all { it == 0 })
                playback.tick(1, 0.5f)
                assertEquals(1, output.closeCount)
                assertFalse(output.sessionOpen())
                playback.tick(1000, 0.5f)
                assertEquals(1, output.closeCount)
                playback.state(PlaybackState.STATE_PLAYING)
                playback.tick(100, 0.5f)
                assertEquals(1, output.registrationTimes.size) // Closed sessions stay closed during confirmation.
                playback.tick(1000, 0.5f)
                assertEquals(2, output.registrationTimes.size)
                assertTrue(output.sessionOpen())
            }
        }
    }

    @Test fun missingControllerDuringConfirmationKeepsBlackSessionUntilPlayingIsConfirmedAgain() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.callback("handleServiceConnected")
                playback.tick(1000, 0.5f)
                val manager = shadowOf(output.context.getSystemService(MediaSessionManager::class.java))
                manager.clearControllers()
                playback.tick(100, 0.5f)
                playback.tick(749, 0.5f)
                assertTrue(output.sessionOpen())
                assertEquals(0, output.closeCount)
                assertTrue(output.sent.last().all { it == 0 })
                manager.addController(playback.session.controller)
                playback.tick(1, 0.5f)
                playback.tick(999, 0.5f)
                assertTrue(output.sent.last().all { it == 0 })
                playback.tick(1, 0.5f)
                assertEquals(1, output.registrationTimes.size)
                assertEquals(0, output.closeCount)
                assertEquals(0, output.blackoutCount)
                assertTrue(output.sent.last().any { it > 0 })
            }
        }
    }

    @Test fun explicitReleaseGlyphOffAndDisconnectStillInvalidateAConfirmingSession() {
        for (end in listOf("stop", "glyph-off", "disconnect")) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    playback.musicOnly = true
                    output.callback("handleServiceConnected")
                    playback.tick(1000, 0.5f)
                    assertTrue(output.sessionOpen())
                    when (end) {
                        "stop" -> output.controller.releaseSession()
                        "glyph-off" -> {
                            output.setControllerField("isBound", false) // No Android service was bound in this fixture.
                            output.setControllerField("glyphOutputAllowed", false)
                            output.callback("reconcileSdkBinding")
                        }
                        "disconnect" -> output.callback("handleServiceDisconnected")
                    }
                    assertFalse(output.sessionOpen())
                    assertEquals(if (end == "disconnect") 0 else 1, output.closeCount)
                    val frames = output.sent.size
                    playback.tick(2000, 0.5f)
                    playback.tick(60_000, 0.5f)
                    assertEquals(1, output.registrationTimes.size)
                    assertEquals(frames, output.sent.size)
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProIgnoresSavedDiagnosticsAndUsesTheExistingDTiming() {
        for (saved in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = saved, diagnosticsEnabled = false).use { output ->
                output.controller.bind()
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.sdkCalls.clear()
                MatrixRecoveryTestMode.entries.forEach { MatrixRecoveryTestSettings.save(output.context, it) }
                output.idleWithoutFrames(0)
                assertTrue(output.sdkCalls.isEmpty())
                output.updateAfter(1, 0f)
                output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS - 1, 0f)
                assertTrue(output.sdkCalls.isEmpty())
                output.updateAfter(1, 0.8f)
                assertEquals(0, output.closeCount)
                output.sdkCalls.clear()
                output.updateAfter(1, 0f)
                output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS, 0f)
                assertEquals(listOf("frame:0"), output.sdkCalls)
                assertTrue(output.sessionOpen())
                output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS - MatrixAudioSilencePolicy.BLACKOUT_MS - 1, 0f)
                assertEquals(0, output.closeCount)
                output.updateAfter(1, 0f)
                assertEquals(listOf("frame:0", "closeAppMatrix"), output.sdkCalls)
                output.updateAfter(1, 0.8f)
                assertEquals(1, output.registrationTimes.size)
                assertTrue(output.sent.last().any { it > 0 })
                assertEquals(0, output.blackoutCount)
                output.assertFrameRegistrationOrder()
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProCleanupUsesBlackAndCloseAndInvalidatesOldCallbacks() {
        for (end in listOf("stop", "unbind", "glyph-off", "normal-mode", "open-reel", "disconnect")) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, diagnosticsEnabled = false).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.updateAfter(1, 0f)
                output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS, 0f)
                val old = output.pendingPlaybackCheck()
                output.sdkCalls.clear()
                when (end) {
                    "stop" -> output.controller.releaseSession()
                    "unbind" -> output.controller.unbind()
                    "glyph-off" -> {
                        // The fixture has no real Android service binding to unbind.
                        output.setControllerField("isBound", false)
                        output.setControllerField("glyphOutputAllowed", false)
                        output.callback("reconcileSdkBinding")
                    }
                    "normal-mode" -> output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
                    "open-reel" -> output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                    "disconnect" -> output.callback("handleServiceDisconnected")
                }
                val expected = when (end) {
                    "disconnect" -> emptyList()
                    "glyph-off" -> listOf("closeAppMatrix")
                    else -> listOf("frame:0", "closeAppMatrix")
                }
                assertEquals("Cleanup calls: $end", expected, output.sdkCalls)
                assertFalse(output.sessionOpen())
                assertEquals(0, output.blackoutCount)
                assertNull(output.pendingPlaybackCheck())
                old?.run()
                output.idleWithoutFrames(60_000)
                assertEquals(expected, output.sdkCalls)
                assertTrue(output.registrationTimes.isEmpty())
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProSkipsCleanupFramesWhenTheSessionCannotSend() {
        for (guard in listOf("sdkConnected", "isSessionOpen", "glyphOutputAllowed", "isBound", "length", "matrixReleasedForSilence")) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, diagnosticsEnabled = false).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                when (guard) {
                    "length" -> {
                        output.setControllerField("physicalMatrixLength", 0)
                        output.setControllerField("physicalMatrixProfile", GlyphDeviceProfile.PHONE3_MATRIX)
                    }
                    "matrixReleasedForSilence" -> output.setControllerField(guard, true)
                    else -> output.setControllerField(guard, false)
                }
                val attempts = output.frameAttempts
                output.sdkCalls.clear()
                output.controller.releaseSession()
                assertEquals("Unsafe cleanup frame: $guard", attempts, output.frameAttempts)
                assertEquals(if (guard == "sdkConnected" || guard == "isSessionOpen") emptyList<String>()
                    else listOf("closeAppMatrix"), output.sdkCalls)
                assertFalse(output.sessionOpen())
                assertNull(output.pendingPlaybackCheck())
                assertEquals(0, output.blackoutCount)
                output.controller.releaseSession()
                assertEquals(if (guard == "sdkConnected" || guard == "isSessionOpen") 0 else 1, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProClosesOnceEvenIfItsCleanupBlackFrameFails() {
        for (end in listOf("stop", "unbind")) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, diagnosticsEnabled = false).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.framesToFail = 100
                val attempts = output.frameAttempts
                output.sdkCalls.clear()
                if (end == "stop") output.controller.releaseSession() else output.controller.unbind()
                assertEquals(attempts + 1, output.frameAttempts)
                assertEquals(listOf("closeAppMatrix"), output.sdkCalls)
                assertFalse(output.sessionOpen())
                output.controller.releaseSession()
                output.idleWithoutFrames(60_000)
                assertEquals(attempts + 1, output.frameAttempts)
                assertEquals(1, output.closeCount)
                assertTrue(output.registrationTimes.isEmpty())
                assertEquals(0, output.blackoutCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProFrameFailureKeepsBackoffWithoutRecursiveCleanupOrTurnOff() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, diagnosticsEnabled = false).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000, 0.8f)
            output.framesToFail = 100
            val attempts = output.frameAttempts
            repeat(3) { output.send(IntArray(169) { 128 }) }
            assertEquals(attempts + 4, output.frameAttempts) // Three failures and one best-effort clear.
            assertEquals(1, output.closeCount)
            assertFalse(output.sessionOpen())
            output.registrationsToFail = 1
            output.updateAfter(499, 0.8f)
            assertTrue(output.registrationTimes.isEmpty())
            output.updateAfter(1, 0.8f)
            assertEquals(1, output.registrationTimes.size)
            assertEquals(attempts + 4, output.frameAttempts)
            output.updateAfter(499, 0.8f)
            assertEquals(1, output.registrationTimes.size)
            output.framesToFail = 0
            output.updateAfter(1, 0.8f)
            assertEquals(2, output.registrationTimes.size)
            assertTrue(output.sessionOpen())
            assertTrue(output.sent.last().any { it > 0 })
            assertEquals(0, output.blackoutCount)
            output.assertFrameRegistrationOrder()
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun productionPhone4aProCaptureSuppressionAndRestartNeverUseTurnOff() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = MatrixRecoveryTestMode.TURN_OFF_ONLY,
            diagnosticsEnabled = false).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000, 0.8f)
            output.sdkCalls.clear()
            output.controller.turnOff() // Visualizer restart/back-down suppression entry point.
            assertEquals(listOf("frame:0"), output.sdkCalls)
            output.updateAfter(1, 0.8f)
            assertTrue(output.sessionOpen())
            assertTrue(output.registrationTimes.isEmpty())
            output.controller.releaseSession()
            val oldCalls = output.sdkCalls.size
            output.updateAfter(60_000, 0.8f)
            assertEquals(oldCalls, output.sdkCalls.size)
            output.controller.bind()
            output.updateAfter(1, 0.8f)
            assertEquals(1, output.registrationTimes.size)
            assertTrue(output.sent.last().any { it > 0 })
            output.callback("handleServiceDisconnected")
            output.updateAfter(60_000, 0.8f)
            assertEquals(1, output.registrationTimes.size)
            output.callback("handleServiceConnected")
            output.updateAfter(1, 0.8f)
            assertEquals(2, output.registrationTimes.size)
            assertEquals(0, output.blackoutCount)
            output.assertFrameRegistrationOrder()
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun disabledDiagnosticsPreservePhone3EmulationAndOpenReelTurnOff() {
        for (saved in MatrixRecoveryTestMode.entries) {
            for (emulate in listOf(false, true)) {
                OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX, emulate = emulate, recoveryTestMode = saved,
                    diagnosticsEnabled = false).use { output ->
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    output.updateAfter(1000, 0.8f)
                    output.updateAfter(1, 0f)
                    output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS, 0f)
                    assertEquals(2, output.blackoutCount)
                    assertEquals(1, output.closeCount)
                }
            }
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = saved, diagnosticsEnabled = false).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                output.updateAfter(1000, 0.8f)
                output.updateAfter(1, 0f)
                output.updateAfter(MatrixAudioSilencePolicy.OPEN_REEL_SILENCE_BLACKOUT_MS, 0f)
                assertEquals(1, output.blackoutCount)
                output.updateAfter(MatrixAudioSilencePolicy.OPEN_REEL_SILENCE_RELEASE_MS - MatrixAudioSilencePolicy.OPEN_REEL_SILENCE_BLACKOUT_MS, 0f)
                assertEquals(2, output.blackoutCount)
                assertEquals(1, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun recoveryMethodsHaveDistinctSdkSequencesAndResumeWithValidRegistration() {
        for (mode in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.sdkCalls.clear()
                output.updateAfter(1, 0f)
                output.updateAfter(119, 0f)
                assertTrue("Short silence changed SDK ownership: $mode", output.sdkCalls.isEmpty())
                output.updateAfter(1, 0.8f)
                assertEquals(0, output.closeCount)
                assertTrue(output.registrationTimes.isEmpty())
                output.sdkCalls.clear()
                output.updateAfter(1, 0f)
                output.updateAfter(120, 0f)
                output.updateAfter(330, 0f)
                val expected = when (mode) {
                    MatrixRecoveryTestMode.STANDARD -> listOf("turnOff", "turnOff", "closeAppMatrix")
                    MatrixRecoveryTestMode.KEEP_SESSION -> listOf("frame:0")
                    MatrixRecoveryTestMode.TURN_OFF_ONLY -> listOf("turnOff")
                    MatrixRecoveryTestMode.CLOSE_ONLY -> listOf("frame:0", "closeAppMatrix")
                }
                assertEquals("Unexpected silence SDK calls: $mode", expected, output.sdkCalls)
                val retained = mode == MatrixRecoveryTestMode.KEEP_SESSION || mode == MatrixRecoveryTestMode.TURN_OFF_ONLY
                assertEquals(retained, output.sessionOpen())
                output.updateAfter(1, 0.8f)
                assertEquals(if (retained) 0 else 1, output.registrationTimes.size)
                assertTrue(output.sent.last().any { it > 0 })
                assertTrue(output.sessionOpen())
                output.assertFrameRegistrationOrder()
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlySendsBlackAtBlackoutAndClosesOnlyAtTheExistingReleaseDeadline() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = MatrixRecoveryTestMode.CLOSE_ONLY).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000, 0.8f)
            output.sdkCalls.clear()
            output.updateAfter(1, 0f)
            output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS - 1, 0f)
            assertTrue(output.sdkCalls.isEmpty())
            output.updateAfter(1, 0f)
            assertEquals(listOf("frame:0"), output.sdkCalls)
            assertTrue(output.sessionOpen())
            assertTrue(output.sent.last().all { it == 0 })
            output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS - MatrixAudioSilencePolicy.BLACKOUT_MS - 1, 0f)
            assertEquals(listOf("frame:0"), output.sdkCalls)
            assertTrue(output.sessionOpen())
            output.updateAfter(1, 0f)
            assertEquals(listOf("frame:0", "closeAppMatrix"), output.sdkCalls)
            assertFalse(output.sessionOpen())
            output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS, 0f)
            assertEquals(1, output.closeCount)
            output.updateAfter(1, 0.8f)
            assertEquals(1, output.registrationTimes.size)
            assertEquals("register:true", output.sdkCalls[2])
            assertTrue(output.sdkCalls[3].startsWith("frame:"))
            assertTrue(output.sent.last().any { it > 0 })
            assertEquals(0, output.blackoutCount)
            output.assertFrameRegistrationOrder()
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlyResumesWithoutRegisteringWhenTheReleaseDeadlineHasNotArrived() {
        for (resumeAt in listOf(MatrixAudioSilencePolicy.BLACKOUT_MS + 1, MatrixAudioSilencePolicy.RELEASE_MS - 1)) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = MatrixRecoveryTestMode.CLOSE_ONLY).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.sdkCalls.clear()
                output.updateAfter(1, 0f)
                output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS, 0f)
                val old = output.pendingPlaybackCheck()
                output.updateAfter(resumeAt - MatrixAudioSilencePolicy.BLACKOUT_MS, 0.8f)
                old?.run()
                output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS, 0.8f)
                assertEquals(0, output.closeCount)
                assertEquals(0, output.blackoutCount)
                assertTrue(output.registrationTimes.isEmpty())
                assertTrue(output.sessionOpen())
                assertEquals("frame:0", output.sdkCalls.first())
                assertTrue(output.sent.last().any { it > 0 })
                output.assertFrameRegistrationOrder()
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlySelectionDuringBlackoutDoesNotBypassTheSilenceReleaseDeadline() {
        for (initial in listOf(MatrixRecoveryTestMode.KEEP_SESSION, MatrixRecoveryTestMode.TURN_OFF_ONLY)) {
            for (selectAt in listOf(MatrixAudioSilencePolicy.BLACKOUT_MS + 1, MatrixAudioSilencePolicy.RELEASE_MS - 1)) {
                OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = initial).use { output ->
                    output.controller.bind()
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    output.updateAfter(1000, 0.8f)
                    output.updateAfter(1, 0f)
                    output.updateAfter(MatrixAudioSilencePolicy.BLACKOUT_MS, 0f)
                    val off = output.blackoutCount
                    val old = output.pendingPlaybackCheck()
                    ShadowSystemClock.advanceBy(Duration.ofMillis(selectAt - MatrixAudioSilencePolicy.BLACKOUT_MS))
                    MatrixRecoveryTestSettings.save(output.context, MatrixRecoveryTestMode.CLOSE_ONLY)
                    output.idleWithoutFrames(0)
                    assertEquals(0, output.closeCount)
                    assertTrue(output.sessionOpen())
                    assertEquals("frame:0", output.sdkCalls.last())
                    output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS - selectAt - 1, 0f)
                    assertEquals(0, output.closeCount)
                    output.updateAfter(1, 0f)
                    assertEquals(1, output.closeCount)
                    assertEquals(off, output.blackoutCount)
                    old?.run()
                    assertEquals(1, output.closeCount)
                    output.updateAfter(1, 0.8f)
                    assertEquals(1, output.registrationTimes.size)
                    output.assertFrameRegistrationOrder()
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlyAndStandardUseIdenticalPlaybackPolicyReleaseDeadlines() {
        for (mode in listOf(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestMode.CLOSE_ONLY)) {
            for (musicOnly in listOf(false, true)) {
                for ((state, grace) in listOf(PlaybackState.STATE_PAUSED to MatrixPlaybackSessionPolicy.PAUSE_CONFIRM_MS,
                    PlaybackState.STATE_BUFFERING to MatrixPlaybackSessionPolicy.BUFFERING_MS,
                    PlaybackState.STATE_NONE to MatrixPlaybackSessionPolicy.MISSING_SESSION_MS)) {
                    OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                        PlaybackFixture(output).use { playback ->
                            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                            playback.musicOnly = musicOnly
                            playback.tick(1000, 0.8f)
                            playback.tick(1000, 0.8f)
                            output.sdkCalls.clear()
                            playback.state(state)
                            playback.tick(1)
                            assertTrue(output.sdkCalls.all { it.startsWith("frame:") })
                            output.idleWithoutFrames(grace - 1)
                            assertTrue("Premature release: $mode $state musicOnly=$musicOnly", output.sessionOpen())
                            assertEquals(0, output.closeCount)
                            assertEquals(0, output.blackoutCount)
                            output.idleWithoutFrames(1)
                            val expected = if (mode == MatrixRecoveryTestMode.STANDARD)
                                listOf("turnOff", "closeAppMatrix") else listOf("closeAppMatrix")
                            assertEquals("Policy release: $mode $state musicOnly=$musicOnly", expected,
                                output.sdkCalls.filterNot { it.startsWith("frame:") })
                            assertFalse(output.sessionOpen())
                            assertNull(output.pendingPlaybackCheck())
                            playback.state(PlaybackState.STATE_PLAYING)
                            playback.tick(1, 0.8f)
                            playback.tick(1000, 0.8f)
                            assertEquals(1, output.registrationTimes.size)
                            assertTrue(output.sent.last().any { it > 0 })
                            output.assertFrameRegistrationOrder()
                        }
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlyKeepsPlayingSilenceAndAudioFallbackForAsLongAsStandard() {
        for (mode in listOf(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestMode.CLOSE_ONLY)) {
            for (fallback in listOf(false, true)) {
                OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                        playback.musicOnly = !fallback
                        playback.tick(1000, 0.8f)
                        playback.tick(1000, 0.8f)
                        if (fallback) {
                            playback.state(PlaybackState.STATE_PAUSED)
                            shadowOf(output.context.getSystemService(AudioManager::class.java)).setIsMusicActive(true)
                        }
                        playback.tick(1, if (fallback) 0.8f else 0f)
                        output.idleWithoutFrames(10_000)
                        assertTrue(output.sessionOpen())
                        assertEquals(0, output.closeCount)
                        assertEquals(0, output.blackoutCount)
                        assertTrue(output.registrationTimes.isEmpty())
                        playback.state(PlaybackState.STATE_PLAYING)
                        playback.tick(1, 0.8f)
                        playback.tick(1000, 0.8f)
                        assertTrue(output.sent.last().any { it > 0 })
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun recoveryMethodsRespectPausedBufferingAndMissingDeadlinesWithoutFrames() {
        for (mode in MatrixRecoveryTestMode.entries) {
            for ((state, grace) in listOf(PlaybackState.STATE_PAUSED to 400L,
                PlaybackState.STATE_BUFFERING to 3000L, PlaybackState.STATE_NONE to 750L)) {
                OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                        playback.musicOnly = true
                        playback.tick(1000, 0.8f)
                        playback.tick(1000, 0.8f)
                        playback.state(state)
                        playback.tick(1)
                        output.idleWithoutFrames(grace + 2000)
                        val retained = mode == MatrixRecoveryTestMode.KEEP_SESSION || mode == MatrixRecoveryTestMode.TURN_OFF_ONLY
                        assertEquals("Deadline retention: $mode $state", retained, output.sessionOpen())
                        assertEquals(if (retained) 0 else 1, output.closeCount)
                        assertEquals(if (mode == MatrixRecoveryTestMode.KEEP_SESSION || mode == MatrixRecoveryTestMode.CLOSE_ONLY) 0 else 1, output.blackoutCount)
                        if (retained) assertNotNull(output.pendingPlaybackCheck()) else assertNull(output.pendingPlaybackCheck())
                        val sent = output.sent.size
                        output.idleWithoutFrames(1000)
                        if (mode == MatrixRecoveryTestMode.TURN_OFF_ONLY) assertEquals(sent, output.sent.size)
                        if (mode == MatrixRecoveryTestMode.KEEP_SESSION) assertTrue(output.sent.last().all { it == 0 })
                        playback.state(PlaybackState.STATE_PLAYING)
                        playback.tick(1, 0.8f)
                        playback.tick(1000, 0.8f)
                        assertEquals(if (retained) 0 else 1, output.registrationTimes.size)
                        assertTrue(output.sent.last().any { it > 0 })
                        output.assertFrameRegistrationOrder()
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun recoveryMethodChangesDoNotReinitializeHealthySessionsAndReconcileHeldSessions() {
        for (initial in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = initial).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.bind() // Observe the real diagnostic preference on the owner looper.
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                    playback.musicOnly = true
                    playback.tick(1000, 0.8f)
                    playback.tick(1000, 0.8f)
                    val closes = output.closeCount
                    val off = output.blackoutCount
                    val registrations = output.registrationTimes.size
                    MatrixRecoveryTestMode.entries.forEach { mode ->
                        MatrixRecoveryTestSettings.save(output.context, mode)
                        output.idleWithoutFrames(0)
                    }
                    assertEquals(closes, output.closeCount)
                    assertEquals(off, output.blackoutCount)
                    assertEquals(registrations, output.registrationTimes.size)
                    MatrixRecoveryTestSettings.save(output.context, MatrixRecoveryTestMode.KEEP_SESSION)
                    output.idleWithoutFrames(0)
                    playback.state(PlaybackState.STATE_PAUSED)
                    playback.tick(1)
                    output.idleWithoutFrames(400)
                    assertTrue(output.sessionOpen())
                    MatrixRecoveryTestSettings.save(output.context, MatrixRecoveryTestMode.TURN_OFF_ONLY)
                    output.idleWithoutFrames(0)
                    assertEquals(off + 1, output.blackoutCount)
                    assertEquals(closes, output.closeCount)
                    // A fresh resume at the setting change must not be closed using old PAUSED state.
                    playback.state(PlaybackState.STATE_PLAYING)
                    MatrixRecoveryTestSettings.save(output.context, MatrixRecoveryTestMode.CLOSE_ONLY)
                    output.idleWithoutFrames(0)
                    assertEquals(closes, output.closeCount)
                    playback.tick(1000, 0.8f)
                    playback.state(PlaybackState.STATE_PAUSED)
                    playback.tick(1)
                    output.idleWithoutFrames(400)
                    assertEquals(closes + 1, output.closeCount)
                    assertEquals(off + 1, output.blackoutCount) // D never called turnOff.
                    output.assertFrameRegistrationOrder()
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun recoveryMethodsNeverOverrideExplicitStopGlyphOffOrSdkDisconnection() {
        for (mode in MatrixRecoveryTestMode.entries) {
            for (end in listOf("stop", "service-end", "glyph-off", "disconnect")) {
                OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                        playback.musicOnly = true
                        playback.tick(1000)
                        playback.state(PlaybackState.STATE_PAUSED)
                        playback.tick(1)
                        output.idleWithoutFrames(400)
                        val old = output.pendingPlaybackCheck()
                        val closes = output.closeCount
                        val off = output.blackoutCount
                        val openBeforeEnd = output.sessionOpen()
                        when (end) {
                            "stop", "service-end" -> output.controller.releaseSession()
                            "glyph-off" -> {
                                output.setControllerField("isBound", false)
                                output.setControllerField("glyphOutputAllowed", false)
                                output.callback("reconcileSdkBinding")
                            }
                            "disconnect" -> output.callback("handleServiceDisconnected")
                        }
                        assertFalse(output.sessionOpen())
                        assertNull(output.pendingPlaybackCheck())
                        if (end != "disconnect") {
                            val withoutTurnOff = mode == MatrixRecoveryTestMode.KEEP_SESSION || mode == MatrixRecoveryTestMode.CLOSE_ONLY
                            val releases = if ((withoutTurnOff || end == "glyph-off") && !openBeforeEnd) 0 else 1
                            assertEquals(closes + releases, output.closeCount)
                            assertEquals(off + if (withoutTurnOff) 0 else releases, output.blackoutCount)
                        }
                        val calls = output.sdkCalls.size
                        old?.run()
                        output.idleWithoutFrames(60_000)
                        assertEquals(calls, output.sdkCalls.size)
                        assertTrue(output.registrationTimes.isEmpty())
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun diagnosticHoldIsFullyReleasedOnModeSwitchAndSdkReconnectCreatesANewSession() {
        for (mode in listOf(MatrixRecoveryTestMode.KEEP_SESSION, MatrixRecoveryTestMode.TURN_OFF_ONLY)) {
            for (end in listOf("normal-mode", "open-reel", "reconnect")) {
                OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                        playback.musicOnly = true
                        playback.tick(1000)
                        playback.state(PlaybackState.STATE_PAUSED)
                        playback.tick(1)
                        output.idleWithoutFrames(400)
                        val old = requireNotNull(output.pendingPlaybackCheck())
                        val closes = output.closeCount
                        if (end == "reconnect") {
                            output.callback("handleServiceDisconnected")
                            output.callback("handleServiceConnected")
                        } else {
                            output.controller.setGlyphMode(if (end == "open-reel") GlyphPatternRegistry.P3_MATRIX_OPEN_REEL else GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
                            assertEquals(closes + 1, output.closeCount)
                            assertFalse(output.sessionOpen())
                        }
                        val calls = output.sdkCalls.size
                        old.run()
                        assertEquals(calls, output.sdkCalls.size)
                        playback.state(PlaybackState.STATE_PLAYING)
                        if (end == "open-reel") output.updateAfter(1000, 0.8f) else {
                            playback.tick(1, 0.8f)
                            playback.tick(1000, 0.8f)
                        }
                        assertTrue(output.sessionOpen())
                        assertEquals(1, output.registrationTimes.size)
                        output.assertFrameRegistrationOrder()
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun modeSwitchClosesAnyStillOpenDiagnosticBlackoutAndInvalidatesItsCallback() {
        for (mode in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.updateAfter(1, 0f)
                output.updateAfter(120, 0f)
                val old = output.pendingPlaybackCheck()
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                assertFalse(output.sessionOpen())
                assertEquals(1, output.closeCount) // Every mode is still open at blackout and closes on the switch.
                assertNull(output.pendingPlaybackCheck())
                val calls = output.sdkCalls.size
                old?.run()
                assertEquals(calls, output.sdkCalls.size)
                output.updateAfter(1, 0.8f)
                assertEquals(1, output.registrationTimes.size)
                output.assertFrameRegistrationOrder()
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun closeOnlyNeverSendsFramesUntilAResumeRegistrationSucceeds() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = MatrixRecoveryTestMode.CLOSE_ONLY).use { output ->
            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
            output.updateAfter(1000, 0.8f)
            output.updateAfter(1, 0f)
            output.updateAfter(MatrixAudioSilencePolicy.RELEASE_MS, 0f)
            assertEquals(1, output.closeCount)
            assertEquals(0, output.blackoutCount)
            val frames = output.sent.size
            output.registrationsToFail = 1
            output.updateAfter(1, 0.8f)
            assertFalse(output.sessionOpen())
            assertEquals(frames, output.sent.size)
            output.updateAfter(499, 0.8f)
            assertEquals(frames, output.sent.size)
            output.updateAfter(1, 0.8f)
            assertTrue(output.sessionOpen())
            assertEquals(2, output.registrationTimes.size)
            assertTrue(output.sent.size > frames)
            assertEquals(0, output.blackoutCount)
            output.assertFrameRegistrationOrder()
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun nonFinalCaptureSuppressionUsesTheExperimentButFinalStopAlwaysFullyReleases() {
        for (mode in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.sdkCalls.clear()
                output.controller.turnOff() // Entry point used during capture restart/back-down suppression.
                when (mode) {
                    MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestMode.TURN_OFF_ONLY -> assertEquals(listOf("turnOff"), output.sdkCalls)
                    MatrixRecoveryTestMode.KEEP_SESSION -> assertEquals(listOf("frame:0"), output.sdkCalls)
                    MatrixRecoveryTestMode.CLOSE_ONLY -> assertEquals(listOf("frame:0"), output.sdkCalls)
                }
                output.sdkCalls.clear()
                output.controller.releaseSession()
                assertEquals(if (mode == MatrixRecoveryTestMode.STANDARD || mode == MatrixRecoveryTestMode.TURN_OFF_ONLY)
                    listOf("turnOff", "closeAppMatrix") else listOf("frame:0", "closeAppMatrix"), output.sdkCalls)
                assertFalse(output.sessionOpen())
                assertNull(output.pendingPlaybackCheck())
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun recoveryDiagnosticIsIgnoredOnPhone3EmulationAndOpenReel() {
        for (mode in MatrixRecoveryTestMode.entries) {
            OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX, emulate = true, recoveryTestMode = mode).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                output.updateAfter(1000, 0.8f)
                output.updateAfter(1, 0f)
                output.updateAfter(450, 0f)
                assertEquals(1, output.closeCount)
                assertEquals(2, output.blackoutCount)
            }
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, recoveryTestMode = mode).use { output ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                output.updateAfter(1000, 0.8f)
                output.updateAfter(1, 0f)
                output.updateAfter(1999, 0f)
                assertEquals(0, output.blackoutCount)
                output.updateAfter(1, 0f)
                assertEquals(1, output.blackoutCount)
                output.updateAfter(1000, 0f)
                assertEquals(1, output.closeCount)
                assertEquals(2, output.blackoutCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun staleReleaseCandidatesKeepReturningPlaybackInEitherFrameTimerOrder() {
        for (confirming in listOf(false, true)) {
            for ((state, grace, removeController) in listOf(
                Triple(PlaybackState.STATE_PAUSED, 400L, false),
                Triple(PlaybackState.STATE_BUFFERING, 3000L, false),
                Triple(PlaybackState.STATE_NONE, 750L, false),
                Triple(PlaybackState.STATE_NONE, 750L, true))) {
                for (first in listOf("service-frame", "controller-frame", "timer")) {
                    OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                        PlaybackFixture(output).use { playback ->
                            playback.musicOnly = true
                            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                            playback.tick(1000)
                            if (!confirming) playback.tick(1000)
                            val manager = shadowOf(output.context.getSystemService(MediaSessionManager::class.java))
                            if (removeController) manager.clearControllers() else playback.state(state)
                            playback.tick(1)
                            output.idleWithoutFrames(grace - 1)
                            // Preserve a cache younger than 250ms while the real session resumes.
                            MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
                            playback.state(PlaybackState.STATE_PLAYING)
                            if (removeController) manager.addController(playback.session.controller)
                            val queued = requireNotNull(output.pendingPlaybackCheck())
                            ShadowSystemClock.advanceBy(Duration.ofMillis(1))
                            when (first) {
                                "timer" -> {
                                    queued.run()
                                    playback.tick(0, 0.5f, refreshSnapshot = false)
                                }
                                "service-frame" -> {
                                    playback.tick(0, 0.5f, refreshSnapshot = false)
                                    queued.run()
                                }
                                "controller-frame" -> {
                                    output.controller.updateLevel(0.5f)
                                    queued.run()
                                }
                            }
                            assertEquals("Unexpected close: $state $first confirming=$confirming", 0, output.closeCount)
                            assertTrue(output.registrationTimes.isEmpty())
                            assertTrue(output.sessionOpen())
                            // Discarding the expired proposal must retain confirmed history, or
                            // require a fresh whole second if the original resume was unconfirmed.
                            assertEquals(!confirming, output.controller.isNormalMatrixPlaybackOutputAllowed(true, 0.5f))
                            output.idleWithoutFrames(1000)
                            assertEquals(0, output.closeCount)
                            assertTrue(output.controller.isNormalMatrixPlaybackOutputAllowed(true, 0.5f))
                        }
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    @Config(shadows = [CountingMediaSessionManager::class])
    fun healthyFramesAvoidDuplicateReadsAndIdleMonitoringStillObservesPause() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                val manager = shadowOf(output.context.getSystemService(MediaSessionManager::class.java)) as CountingMediaSessionManager
                playback.musicOnly = false
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                playback.tick(1000, 0.5f, refreshSnapshot = false)
                val start = manager.reads
                repeat(100) {
                    playback.tick(20, 0.5f, refreshSnapshot = false)
                    val beforeTimer = manager.reads
                    output.idleWithoutFrames(0)
                    assertEquals("Timer duplicated a fresh frame's snapshot read", beforeTimer, manager.reads)
                }
                assertTrue("Normal 250ms cache cadence was exceeded", manager.reads - start <= 8)
                val reads = manager.reads
                playback.state(PlaybackState.STATE_PAUSED) // Stop frames; the monitor must take over.
                output.idleWithoutFrames(249)
                assertEquals(reads, manager.reads)
                output.idleWithoutFrames(1)
                assertTrue(manager.reads > reads)
                output.idleWithoutFrames(399)
                assertEquals(0, output.closeCount)
                output.idleWithoutFrames(1)
                assertEquals(1, output.closeCount)
                assertNull(output.pendingPlaybackCheck())
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    @Config(shadows = [CountingMediaSessionManager::class])
    fun activeAudioFallbackDoesNotForceEveryFrameToReadTheSession() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                val manager = shadowOf(output.context.getSystemService(MediaSessionManager::class.java)) as CountingMediaSessionManager
                playback.musicOnly = false
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                playback.state(PlaybackState.STATE_STOPPED)
                playback.tick(1000, 0.8f, refreshSnapshot = false)
                val start = manager.reads
                repeat(100) {
                    playback.tick(20, 0.8f, refreshSnapshot = false)
                    val beforeTimer = manager.reads
                    output.idleWithoutFrames(0)
                    assertEquals(beforeTimer, manager.reads)
                }
                assertTrue(manager.reads - start <= 8)
                assertEquals(0, output.closeCount)
                assertTrue(output.registrationTimes.isEmpty())
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun silentAudioFallbackRefreshesBeforeItsSilenceRelease() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = false
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                playback.tick(1000, 0.8f)
                val manager = shadowOf(output.context.getSystemService(MediaSessionManager::class.java))
                manager.clearControllers()
                playback.tick(1)
                output.idleWithoutFrames(749)
                MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
                manager.addController(playback.session.controller)
                // Music Only is off: NONE can allow output while relinquishing retention.
                // updateLevel's audio-silence release must also see the returning PLAYING state.
                playback.tick(1, refreshSnapshot = false)
                assertEquals(0, output.closeCount)
                assertTrue(output.sessionOpen())
                assertTrue(output.registrationTimes.isEmpty())
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun pausedBufferingAndMissingDeadlinesReleaseWithoutAudioFramesOnBothDevices() {
        for (profile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)) {
            for (confirming in listOf(false, true)) {
                for ((state, grace) in listOf(PlaybackState.STATE_PAUSED to 400L,
                    PlaybackState.STATE_BUFFERING to 3000L, PlaybackState.STATE_NONE to 750L)) {
                    OutputFixture(profile).use { output ->
                        PlaybackFixture(output).use { playback ->
                            output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_BAR)
                            playback.musicOnly = true
                            playback.tick(1000)
                            if (!confirming) playback.tick(1000)
                            playback.state(state)
                            playback.tick(1)
                            output.idleWithoutFrames(grace - 1)
                            assertTrue("Released early: $state confirming=$confirming", output.sessionOpen())
                            output.idleWithoutFrames(1)
                            assertFalse("Held past deadline: $state confirming=$confirming", output.sessionOpen())
                            assertEquals(1, output.closeCount)
                            assertNull(output.pendingPlaybackCheck())
                            output.idleWithoutFrames(60_000)
                            assertEquals(1, output.closeCount)
                        }
                    }
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun timerObservesPauseThatBeginsAfterTheLastAudioFrame() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                playback.tick(1000)
                playback.tick(1000)
                output.idleWithoutFrames(250)
                playback.state(PlaybackState.STATE_PAUSED) // No audio frame observes this change.
                output.idleWithoutFrames(250 + 399)
                assertTrue(output.sessionOpen())
                output.idleWithoutFrames(1)
                assertEquals(1, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun deadlineRefreshesCachedMediaStateImmediatelyBeforeRelease() {
        for (confirming in listOf(false, true)) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = true
                    playback.tick(1000)
                    if (!confirming) playback.tick(1000)
                    playback.state(PlaybackState.STATE_PAUSED)
                    playback.tick(1)
                    output.idleWithoutFrames(399)
                    // Cache the old PAUSED state just before dispatch; the callback must force a read.
                    MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
                    playback.state(PlaybackState.STATE_PLAYING)
                    output.idleWithoutFrames(1)
                    assertTrue(output.sessionOpen())
                    assertEquals(0, output.closeCount)
                    output.idleWithoutFrames(1000)
                    assertEquals(0, output.closeCount)
                    assertTrue(output.registrationTimes.isEmpty())
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun overdueCallbackUsesStateAtDispatchInsteadOfTheExpiredDecision() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                playback.tick(1000)
                playback.state(PlaybackState.STATE_PAUSED)
                playback.tick(1)
                val queued = requireNotNull(output.pendingPlaybackCheck())
                ShadowSystemClock.advanceBy(Duration.ofSeconds(10)) // Ready but not dispatched.
                MediaSessionPlaybackGate.currentPlaybackSnapshot(output.context, forceRefresh = true)
                playback.state(PlaybackState.STATE_PLAYING)
                queued.run()
                assertEquals(0, output.closeCount)
                assertTrue(output.sessionOpen())
                assertTrue(output.sent.all { frame -> frame.all { it == 0 } })
                output.idleWithoutFrames(1000)
                assertEquals(0, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun stoppedDisconnectedDisabledAndReplacedGenerationsRejectOldCallbacks() {
        for (end in listOf("stop-restart", "glyph-off", "disconnect", "mode")) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = true
                    playback.tick(1000)
                    playback.state(PlaybackState.STATE_PAUSED)
                    playback.tick(1)
                    val old = requireNotNull(output.pendingPlaybackCheck())
                    when (end) {
                        "stop-restart" -> {
                            output.controller.releaseSession()
                            output.controller.bind()
                            playback.state(PlaybackState.STATE_PLAYING)
                            playback.tick(1)
                            playback.tick(1000)
                        }
                        "glyph-off" -> {
                            output.setControllerField("isBound", false)
                            output.setControllerField("glyphOutputAllowed", false)
                            output.callback("reconcileSdkBinding")
                        }
                        "disconnect" -> output.callback("handleServiceDisconnected")
                        "mode" -> output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                    }
                    val closes = output.closeCount
                    val registrations = output.registrationTimes.size
                    val frames = output.sent.size
                    val replacement = output.pendingPlaybackCheck()
                    ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
                    old.run() // Simulate a removed callback already dequeued before invalidation.
                    assertEquals(closes, output.closeCount)
                    assertEquals(registrations, output.registrationTimes.size)
                    assertEquals(frames, output.sent.size)
                    assertSame(replacement, output.pendingPlaybackCheck())
                    if (end != "stop-restart") assertNull(replacement)
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun fallbackRechecksCurrentAudioAndNeverUsesAStaleLoudFrameForever() {
        for (state in listOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_NONE)) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = false
                    playback.tick(1000, 0.8f)
                    playback.state(state)
                    playback.tick(1, 0.8f)
                    val audio = shadowOf(output.context.getSystemService(AudioManager::class.java))
                    audio.setIsMusicActive(true)
                    output.idleWithoutFrames(4000)
                    assertTrue(output.sessionOpen())
                    assertEquals(0, output.closeCount)
                    audio.setIsMusicActive(false)
                    output.idleWithoutFrames(250)
                    assertEquals(1, output.closeCount)
                    assertNull(output.pendingPlaybackCheck())
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun deadlineUsesUpdatedMusicOnlySettingWithoutAnotherFrame() {
        var musicOnly = true
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX, mediaPlaybackOnlyProvider = { musicOnly }).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                playback.tick(1000)
                playback.state(PlaybackState.STATE_PAUSED)
                playback.tick(1)
                shadowOf(output.context.getSystemService(AudioManager::class.java)).setIsMusicActive(true)
                musicOnly = false
                output.idleWithoutFrames(4000)
                assertEquals(0, output.closeCount)
                assertTrue(output.sessionOpen())
                musicOnly = true
                output.idleWithoutFrames(250)
                assertEquals(1, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun playingSilentVideoKeepsItsSessionWithoutFramesAndNeverReplaysOldPixels() {
        for (musicOnly in listOf(false, true)) {
            OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
                PlaybackFixture(output).use { playback ->
                    playback.musicOnly = musicOnly
                    playback.tick(1000)
                    val frames = output.sent.size
                    output.idleWithoutFrames(600_000)
                    assertEquals(0, output.closeCount)
                    assertTrue(output.registrationTimes.isEmpty())
                    assertTrue(output.sessionOpen())
                    assertEquals(frames, output.sent.size)
                    assertNotNull(output.pendingPlaybackCheck())
                }
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun mixedTransientStatesDoNotExtendTheIndependentBufferingDeadline() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                playback.musicOnly = true
                playback.tick(1000)
                playback.state(PlaybackState.STATE_BUFFERING)
                playback.tick(1)
                output.idleWithoutFrames(2250)
                playback.state(PlaybackState.STATE_NONE)
                output.idleWithoutFrames(250)
                playback.state(PlaybackState.STATE_BUFFERING)
                output.idleWithoutFrames(499)
                assertEquals(0, output.closeCount)
                output.idleWithoutFrames(1)
                assertEquals(1, output.closeCount)
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun openReelNeverStartsTheNormalMatrixDeadlineMonitor() {
        OutputFixture(GlyphDeviceProfile.PHONE4A_PRO_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_OPEN_REEL)
                playback.tick(1000)
                assertNull(output.pendingPlaybackCheck())
                output.idleWithoutFrames(60_000)
                assertEquals(0, output.closeCount)
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
                    preserveTextBrightness = true)
                output.send(frame)
                output.assertEncoding()
                assertEquals(225, output.sent.last()[frame.size / 2 + 1])
                assertEquals(2056, output.sent.last()[frame.size / 2])
            }
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun canvasTitleReachesSdkInFourLevelsWhilePlayingSilenceRetainsAllThreeProfiles() {
        for ((profile, emulate) in listOf(GlyphDeviceProfile.PHONE3_MATRIX to false,
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX to false, GlyphDeviceProfile.PHONE3_MATRIX to true)) {
            OutputFixture(profile, emulate).use { output ->
                PlaybackFixture(output).use { playback ->
                    output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
                    output.controller.setMatrixTrackTitleSettings(true, false)
                    playback.tick(1000)
                    shadowOf(playback.session.controller).setMetadata(MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "new-song")
                        .putString(MediaMetadata.METADATA_KEY_TITLE, "日本語の長い曲名 ABC 123 Artist").build())
                    playback.tick(100)
                    // Give the first glyph time to enter the circular physical mask; a
                    // valid frame can still be black when only its leading blank is visible.
                    for (elapsed in listOf(700L, 500L, 1000L)) {
                        playback.tick(elapsed)
                        output.assertEncoding()
                        assertEquals(0, output.closeCount)
                        assertEquals(0, output.blackoutCount)
                        assertTrue("No title pixels: $profile emulate=$emulate elapsed=$elapsed", output.sent.last().any { it > 0 })
                        assertTrue(output.internalFrame().all { it in listOf(0, 42, 85, 127) })
                        assertTrue(output.sent.last().all { it in listOf(0, 674, 1365, 2039) })
                    }
                    if (emulate) {
                        val physical = output.internalFrame()
                        for (y in 0 until 25) for (x in 0 until 25) {
                            if (y !in 6..18 || x !in 6..18) assertEquals(0, physical[y * 25 + x])
                        }
                    }
                    output.controller.turnOff()
                }
            }
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun reconnectSwitchesEmulationSizeAndRecyclesThePreviousTitleBitmap() {
        OutputFixture(GlyphDeviceProfile.PHONE3_MATRIX).use { output ->
            PlaybackFixture(output).use { playback ->
                output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
                output.controller.setMatrixTrackTitleSettings(true, false)
                val reconnect = GlyphMatrixController::class.java.getDeclaredMethod("handleServiceConnected")
                    .apply { isAccessible = true }
                val rendererField = GlyphMatrixController::class.java.getDeclaredField("trackTitleRenderer")
                    .apply { isAccessible = true }
                var previousBitmap: android.graphics.Bitmap? = null
                for ((index, emulate) in listOf(false, true, false).withIndex()) {
                    output.setControllerField("initialPhone4aProEmulationEnabled", emulate)
                    reconnect.invoke(output.controller)
                    previousBitmap?.let { assertTrue(it.isRecycled) }
                    playback.tick(1000) // Reconnect seeds the current track without replaying it.
                    assertNull(rendererField.get(output.controller))
                    shadowOf(playback.session.controller).setMetadata(MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "switch-$index")
                        .putString(MediaMetadata.METADATA_KEY_TITLE, "日本語の曲名 ABC 123").build())
                    playback.tick(100)
                    val renderer = rendererField.get(output.controller) as MatrixCanvasTrackTitleRenderer
                    assertEquals(if (emulate) 13 else 25, renderer.matrixLength)
                    previousBitmap = requireNotNull(renderer.cachedBitmap)
                    assertEquals(renderer.matrixLength, previousBitmap.height)
                    playback.tick(700)
                    output.assertEncoding()
                    assertTrue(output.sent.last().any { it > 0 })
                }
                output.controller.turnOff()
                assertTrue(requireNotNull(previousBitmap).isRecycled)
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
                preview.unbind()
                assertTrue(output.sent.isEmpty())
                assertTrue(output.registrationTimes.isEmpty())
                assertEquals(0, output.closeCount)
                assertEquals(0, output.blackoutCount)
            }
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun liveMirrorAlreadyReceivesFinalTitleCompositionAndCenteredEmulationInEightBits() {
        for ((profile, emulate) in listOf(GlyphDeviceProfile.PHONE3_MATRIX to false,
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX to false, GlyphDeviceProfile.PHONE3_MATRIX to true)) {
            val frames = mutableListOf<GlyphPreviewFrame.Matrix>()
            val listener: (GlyphPreviewFrame?) -> Unit = { if (it is GlyphPreviewFrame.Matrix) frames += it }
            GlyphPreviewFrameStore.register(listener)
            try {
                OutputFixture(profile, emulate).use { output ->
                    PlaybackFixture(output).use { playback ->
                        output.controller.setGlyphMode(GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS)
                        output.controller.setMatrixTrackTitleSettings(true, false)
                        playback.tick(1000, 0.5f)
                        shadowOf(playback.session.controller).setMetadata(MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, "mirror-next")
                            .putString(MediaMetadata.METADATA_KEY_TITLE, "日本語の長い曲名 ABC 123").build())
                        playback.tick(100, 0.5f)
                        playback.tick(700, 0.5f)
                        val frozen = frames.last()
                        assertEquals(output.logicalProfile, frozen.deviceProfile)
                        assertEquals(profile, frozen.physicalDeviceProfile)
                        assertEquals(output.length, frozen.matrixSize)
                        assertArrayEquals(output.internalFrame(), frozen.pixels)
                        assertTrue(frozen.pixels.all { it in 0..255 })
                        assertTrue(frozen.pixels.any { it == 127 })
                        output.assertEncoding()
                        val frozenPixels = frozen.pixels.copyOf()
                        playback.tick(1000, 0.5f)
                        assertArrayEquals(frozenPixels, frozen.pixels)
                        assertFalse(frozen.pixels.contentEquals(frames.last().pixels))
                        assertArrayEquals(output.internalFrame(), frames.last().pixels)
                        if (emulate) {
                            for (y in 0 until 25) for (x in 0 until 25) {
                                if (y !in 6..18 || x !in 6..18) assertEquals(0, frames.last().pixels[y * 25 + x])
                            }
                        }
                        playback.tick(21_000, 0.5f)
                        assertArrayEquals(output.internalFrame(), frames.last().pixels)
                        output.controller.turnOff()
                        assertTrue(frames.last().pixels.all { it == 0 })
                    }
                }
            } finally { GlyphPreviewFrameStore.unregister(listener) }
        }
    }
}
