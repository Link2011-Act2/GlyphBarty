package jp.linkserver.glyphvisualizer.update

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.goldy.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UpdatePreferencesMigrationTest {
    @Test(timeout = 60_000) fun legacyIgnoreAndRealAutomaticSettingSurviveIdempotentMigration() = runBlocking<Unit> {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk", true))
        val legacy = app.getSharedPreferences("github_release_updates", 0)
        val host = app.getSharedPreferences("goldy_host_updates", 0)
        val settings = app.getSharedPreferences("glyph_visualizer_settings", 0)
        val oldLegacy = legacy.all.toMap(); val oldHost = host.all.toMap(); val oldSettings = settings.all.toMap()
        val store = UpdateStateStores.preferences(app, ApkUpdateProfile.repository)
        val original = store.read()
        fun fresh() = AppUpdates::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(app)
        try {
            legacy.edit().clear().putString("dismissed_update_tag", "v2.0.0-Beta2")
                .putBoolean("show_latest_release_for_testing", true)
                .putBoolean("ignore_check_interval_for_testing", true).putLong("last_check_ms", 1234567L).commit()
            settings.edit().putBoolean("automatic_update_check_enabled", false).commit()
            val savedLegacy = legacy.all.toMap(); val savedSettings = settings.all.toMap()
            host.edit().clear().commit()
            store.write(StoredUpdateState(ignoredTags = setOf("v3.0.0-release")))
            val first = fresh()
            first.restoreCachedState()
            assertEquals(setOf("v2.0.0-beta2", "v3.0.0-release"), store.read().ignoredTags)
            assertNull(store.read().cache) // Old cache was in memory; no Release/Asset is fabricated.
            assertEquals(savedLegacy, legacy.all); assertEquals(savedSettings, settings.all)
            assertEquals(1234567L, host.getLong("legacy_last_success_ms", 0))
            assertFalse(updateOptions(app).automaticChecksEnabled)
            assertFalse(updateOptions(app).notificationsEnabled)
            assertEquals(CheckOutcome.Disabled, first.check())
            store.write(store.read().copy(ignoredTags = store.read().ignoredTags + "v4.0.0-rc6"))
            fresh().restoreCachedState()
            assertEquals(3, store.read().ignoredTags.size)
            assertEquals(savedLegacy, legacy.all); assertEquals(savedSettings, settings.all)
            settings.edit().putBoolean("automatic_update_check_enabled", true).commit()
            assertTrue(updateOptions(app).automaticChecksEnabled)
            assertTrue(updateOptions(app).notificationsEnabled)
            val actualIntDev = VersionPolicies.Common.channel(installedVersionName(app)) == ReleaseChannel.IntDev
            assertEquals(actualIntDev, updateOptions(app).developer.ignoreVersionComparison)
            assertEquals(actualIntDev, updateOptions(app).developer.ignoreCheckInterval)
        } finally {
            store.write(original)
            restorePreferences(host, oldHost); restorePreferences(legacy, oldLegacy); restorePreferences(settings, oldSettings)
        }
    }
}
