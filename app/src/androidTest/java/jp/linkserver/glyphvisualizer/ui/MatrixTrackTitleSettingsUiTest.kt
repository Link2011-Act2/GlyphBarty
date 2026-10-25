package jp.linkserver.glyphvisualizer.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MatrixTrackTitleSettingsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun m3eLightHierarchy() = verify(false, false)
    @Test fun m3eDarkHierarchy() = verify(false, true)
    @Test fun nothingLightHierarchy() = verify(true, false)
    @Test fun nothingDarkHierarchy() = verify(true, true)

    private fun verify(nothing: Boolean, dark: Boolean) {
        var changes = 0
        var artistChanges = 0
        val restore = StateRestorationTester(compose)
        restore.setContent {
            var enabled by rememberSaveable { mutableStateOf(false) }
            var artist by rememberSaveable { mutableStateOf(true) }
            GlyphBartyTheme(nothingStyle = nothing, darkTheme = dark, dynamicColor = false) {
                FixtureSettings(nothing, false, {}, {},
                    matrixSupported = true,
                    matrixTrackTitleEnabled = enabled,
                    matrixTrackArtistEnabled = artist,
                    onMatrixTrackTitleEnabledChanged = { enabled = it; changes++ },
                    onMatrixTrackArtistEnabledChanged = { artist = it; artistChanges++ })
            }
        }
        val title = compose.activity.getString(R.string.settings_matrix_track_title_enabled)
        val row = compose.onNodeWithTag("matrix_track_title_entry")
        row.performScrollTo()
        // The right side changes state once and must not navigate.
        row.performTouchInput { click(Offset(width - 30f, height / 2f)) }
        compose.onNodeWithTag("matrix_track_title_master").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, changes) }
        val splitSwitch = isToggleable() and hasAnyAncestor(hasTestTag("matrix_track_title_entry"))
        compose.onNode(splitSwitch).assertIsOn()
        // The left side navigates and must not toggle.
        row.performTouchInput { click(Offset(30f, height / 2f)) }
        compose.onNodeWithTag("matrix_track_title_master").assertIsOn()
        compose.runOnIdle { assertEquals(1, changes) }
        compose.onNodeWithTag("matrix_track_title_artist").assertIsOn().assertIsEnabled()
        compose.onNodeWithTag("matrix_track_title_master").performClick().assertIsOff()
        compose.onNodeWithTag("matrix_track_title_artist").assertIsNotEnabled().assertIsOn()
        compose.runOnIdle { assertEquals(2, changes); assertEquals(0, artistChanges) }
        // Rotation restoration retains both the detail destination and the child's saved value.
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("matrix_track_title_master").assertIsOff()
        compose.onNodeWithTag("matrix_track_title_artist").assertIsOn().assertIsNotEnabled()
        compose.onNodeWithTag("matrix_track_title_master").performClick()
        compose.onNodeWithTag("matrix_track_title_artist").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(3, changes); assertEquals(1, artistChanges) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("matrix_track_title_master").assertDoesNotExist()
        row.performScrollTo()
        compose.onNode(splitSwitch).assertIsOn()
        // Navigate back using the toolbar too.
        compose.onNode(hasText(title) and hasClickAction()).performClick()
        compose.onNodeWithTag("matrix_track_title_artist").assertIsOff()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.cd_back)).performClick()
        row.performScrollTo().assertIsDisplayed()
    }

    @Test fun nonMatrixSettingsHideTheEntry() {
        compose.setContent {
            GlyphBartyTheme {
                FixtureSettings(false, false, {}, {}, matrixSupported = false)
            }
        }
        compose.onNodeWithTag("matrix_track_title_entry").assertDoesNotExist()
    }
}
