package jp.linkserver.glyphvisualizer

import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatrixTrackTitleSettingsTest {
    @Test fun absentPreferencesAndLegacyJsonKeepFeatureOff() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("glyph_visualizer_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val state = SettingsPreferences.load(context)
        assertFalse(state.matrixTrackTitleEnabled)
        assertFalse(state.matrixTrackArtistEnabled)
        val imported = SettingsPreferences.importJson("{\"parameters\":{\"sensitivity\":2.0}}")
        assertFalse(imported.matrixTrackTitleEnabled)
        assertFalse(imported.matrixTrackArtistEnabled)
    }

    @Test fun preferencesJsonAndServiceIntentRoundTripBothValuesIncludingDisabledMaster() {
        val context = RuntimeEnvironment.getApplication()
        for (enabled in listOf(true, false)) {
            val state = CaptureUiState(matrixTrackTitleEnabled = enabled, matrixTrackArtistEnabled = true)
            SettingsPreferences.save(context, state)
            val loaded = SettingsPreferences.load(context)
            assertEquals(enabled, loaded.matrixTrackTitleEnabled)
            assertTrue(loaded.matrixTrackArtistEnabled)
            val imported = SettingsPreferences.importJson(SettingsPreferences.exportJson(loaded))
            assertEquals(enabled, imported.matrixTrackTitleEnabled)
            assertTrue(imported.matrixTrackArtistEnabled)
            val intent = CaptureIntentCommandCodec.encode(context, CaptureCommand.StartVisualizer(imported.toCaptureConfig()))
            val decoded = CaptureIntentCommandCodec.decode(intent, defaultServiceCaptureConfig()) as CaptureCommand.StartVisualizer
            assertEquals(enabled, decoded.config.matrixTrackTitleEnabled)
            assertTrue(decoded.config.matrixTrackArtistEnabled)
            assertTrue(decoded.config.applyToUiState(CaptureUiState()).matrixTrackArtistEnabled)
        }
    }

    @Test fun oldServiceCommandsPreserveFallbackSettings() {
        val fallback = defaultServiceCaptureConfig().copy(matrixTrackTitleEnabled = true, matrixTrackArtistEnabled = true)
        val decoded = CaptureIntentCommandCodec.decode(
            Intent().setAction(CaptureIntentCommandCodec.ACTION_UPDATE_CONFIG), fallback
        ) as CaptureCommand.UpdateConfig
        assertTrue(decoded.config.matrixTrackTitleEnabled)
        assertTrue(decoded.config.matrixTrackArtistEnabled)
    }
}
