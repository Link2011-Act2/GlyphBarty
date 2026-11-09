package jp.linkserver.glyphvisualizer.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed
import jp.linkserver.goldy.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GoldyUpdatePresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun candidate(tag: String, prerelease: Boolean = false) = UpdateCandidate(GitHubRelease(900, tag,
        url="https://github.com/Link2011-Act2/GlyphBarty/releases/tag/$tag", prerelease=prerelease))

    @Test fun notificationBadgeUsesHostPaletteAndEligibilityWithoutChangingTheme() {
        var nothing by mutableStateOf(true)
        var dark by mutableStateOf(false)
        var state by mutableStateOf(GoldyState(availableUpdate=candidate("v99.0.0-Release"),notificationUpdate=candidate("v99.0.0-Release")))
        var clicks=0
        compose.setContent {
            GlyphBartyTheme(darkTheme=dark,dynamicColor=false,nothingStyle=nothing) {
                val color=if(nothing) NothingRed else MaterialTheme.colorScheme.secondaryContainer
                Column {
                    Box(Modifier.size(16.dp).background(color).testTag("badge-reference"))
                    Surface { GlyphUpdateNotificationIcon(state,nothing,{clicks++}) }
                }
            }
        }
        for(style in listOf(true,false)) for(night in listOf(false,true)) {
            compose.runOnIdle { nothing=style;dark=night }
            val bitmap=compose.onNodeWithTag("glyph-update-badge",useUnmergedTree=true).captureToImage().toPixelMap()
            val reference=compose.onNodeWithTag("badge-reference").captureToImage().toPixelMap()
            assertEquals("nothing=$style dark=$night",
                reference[reference.width/2,reference.height/2].toArgb(),bitmap[bitmap.width/2,bitmap.height/2].toArgb())
        }
        // Capture resting palette roles before exercising clicks, which produce a press ripple.
        compose.onNodeWithTag("goldy-notification-icon").performClick()
        compose.runOnIdle { assertEquals(1,clicks);state=GoldyState(availableUpdate=candidate("v99.0.0-Release")) }
        compose.onNodeWithTag("goldy-notification-icon").assertDoesNotExist() // Ignored/non-notifying candidate.
        compose.runOnIdle { state=GoldyState() }
        compose.onNodeWithTag("goldy-notification-icon").assertDoesNotExist()
    }

    @Test fun overviewGetsChannelFromAppPolicyForAllPublishedTracks() {
        var update by mutableStateOf(candidate("v2.1.0-Release"))
        compose.setContent { GlyphBartyTheme { UpdateOverviewScreen(update,{}) } }
        for((tag,label) in listOf("v2.1.0-Release" to "Release", "v2.1.0-Stable" to "Stable",
            "v2.1.0-RC6" to "RC", "v2.1.0-Beta2" to "Beta", "v2.1.0-IntDev_rev10" to "IntDev")) {
            compose.runOnIdle { update=candidate(tag,tag.contains("Beta")||tag.contains("IntDev")) }
            compose.onNodeWithText(tag).assertIsDisplayed()
            compose.onNodeWithText(label).assertIsDisplayed()
            compose.onNodeWithText("Unknown").assertDoesNotExist()
        }
    }
}
