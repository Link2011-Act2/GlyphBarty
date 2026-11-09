package jp.linkserver.glyphvisualizer.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.espresso.Espresso
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.MainActivity
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.SettingsPreferences
import jp.linkserver.glyphvisualizer.update.AppUpdates
import jp.linkserver.glyphvisualizer.update.ApkUpdateProfile
import jp.linkserver.glyphvisualizer.update.restorePreferences
import jp.linkserver.goldy.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Actual MainActivity/headers/dialog/navigation. No capture or hardware action is invoked. */
class GoldyHomeUiTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val app get()=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val settings get()=app.getSharedPreferences("glyph_visualizer_settings",0)
    private val host get()=app.getSharedPreferences("goldy_host_updates",0)
    private val legacy get()=app.getSharedPreferences("github_release_updates",0)
    private val store get()=UpdateStateStores.preferences(app,ApkUpdateProfile.repository)
    private val singleton=AppUpdates::class.java.getDeclaredField("instance").apply { isAccessible=true }
    private lateinit var originalSettings: Map<String,*>
    private lateinit var originalHost: Map<String,*>
    private lateinit var originalLegacy: Map<String,*>
    private lateinit var originalState: StoredUpdateState
    private var originalInstance: Any?=null
    private var scenario: ActivityScenario<MainActivity>?=null
    private val tag="v99.1.0-Release"
    @Before fun prepare()=runBlocking {
        originalSettings=settings.all.toMap();originalHost=host.all.toMap();originalLegacy=legacy.all.toMap()
        originalState=store.read();originalInstance=singleton.get(null)
        singleton.set(null,null)
        legacy.edit().clear().commit()
        host.edit().clear().putBoolean("migration_v1",true).putString("hidden_banner_tag",tag).commit()
        val info=app.packageManager.getPackageInfo(app.packageName,0)
        val installed=InstalledVersion(checkNotNull(info.versionName),info.longVersionCode)
        val release=GitHubRelease(910,tag,notes="Home update notes",
            url="https://github.com/Link2011-Act2/GlyphBarty/releases/tag/$tag")
        store.write(StoredUpdateState(cache=UpdateCache(ApkUpdateProfile.repository,ApkUpdateProfile.policy.id,installed,
            installed.name,false,ReleaseCatalog(listOf(release)),System.currentTimeMillis())))
    }
    @After fun restore()=runBlocking {
        scenario?.close()
        store.write(originalState)
        restorePreferences(settings,originalSettings);restorePreferences(host,originalHost);restorePreferences(legacy,originalLegacy)
        singleton.set(null,originalInstance)
    }
    private fun launch(experimental: Boolean,detailed: Boolean,nothing: Boolean) {
        SettingsPreferences.save(app,SettingsPreferences.load(app).copy(experimentalMainUiEnabled=experimental,
            detailedHomeEnabled=detailed,nothingStyleEnabled=nothing,automaticUpdateCheckEnabled=true))
        SettingsPreferences.markInitialSetupCompleted(app)
        SettingsPreferences.markNotificationPermissionPromptShown(app)
        scenario=ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15000) { compose.onAllNodesWithTag("goldy-notification-icon").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun assertOrder() {
        compose.onNodeWithTag("goldy-notification-icon").assertIsDisplayed()
        val update=compose.onNodeWithTag("goldy-notification-icon").fetchSemanticsNode().boundsInRoot
        val settings=compose.onNodeWithContentDescription(app.getString(R.string.cd_settings)).fetchSemanticsNode().boundsInRoot
        assertTrue("Update is immediately left of Settings",update.right<=settings.left+1f)
        assertEquals(settings.center.y,update.center.y,1f)
    }
    private fun openFromIcon() {
        compose.onNodeWithTag("goldy-notification-icon").performClick()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText(app.getString(R.string.goldy_update_view)).performClick()
        compose.onNodeWithTag("latest-release-notes").assertIsDisplayed()
    }
    private fun verifyHome(experimental: Boolean,detailed: Boolean,nothing: Boolean,secondary: Boolean=false) {
        launch(experimental,detailed,nothing)
        if(secondary) compose.onNodeWithText(app.getString(R.string.experimental_home_details)).performClick()
        assertOrder()
        openFromIcon()
        compose.onNodeWithContentDescription(app.getString(R.string.goldy_update_back)).performClick()
        assertOrder()
        if(secondary) compose.onNodeWithText(app.getString(R.string.experimental_home_details)).assertIsDisplayed()
        openFromIcon()
        scenario!!.recreate()
        compose.onNodeWithTag("latest-release-notes").assertIsDisplayed()
        Espresso.pressBack()
        assertOrder()
        if(secondary) compose.onNodeWithText(app.getString(R.string.experimental_home_details)).assertIsDisplayed()
    }
    @Test fun normalHomeHasUpdateBeforeSettingsAndRestoresHomeOrigin()=verifyHome(false,false,false)
    @Test fun experimentalHomeHasUpdateBeforeSettingsAndRestoresHomeOrigin()=verifyHome(true,false,true)
    @Test fun detailedHomeHasUpdateBeforeSettingsAndRestoresHomeOrigin()=verifyHome(true,true,true)
    @Test fun experimentalDetailsReturnsToSameDetailsScreen()=verifyHome(true,false,false,true)
    @Test fun aboutHasNoNotificationIconAndRetainsItsOwnReturnOrigin() {
        launch(false,false,false)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_settings)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_about_title)).performScrollTo().performClick()
        compose.onNodeWithTag("goldy-notification-icon").assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.goldy_update_view)).performScrollTo().performClick()
        compose.onNodeWithTag("latest-release-notes").assertIsDisplayed()
        scenario!!.recreate()
        Espresso.pressBack()
        compose.onNodeWithText(app.getString(R.string.about_updates_title)).assertIsDisplayed()
        compose.onNodeWithTag("goldy-notification-icon").assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.goldy_update_view)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.goldy_update_back)).performClick()
        compose.onNodeWithText(app.getString(R.string.about_updates_title)).assertIsDisplayed()
        compose.onNodeWithTag("goldy-notification-icon").assertDoesNotExist()
    }
}
