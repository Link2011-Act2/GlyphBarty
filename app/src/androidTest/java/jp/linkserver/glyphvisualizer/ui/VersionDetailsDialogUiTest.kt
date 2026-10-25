package jp.linkserver.glyphvisualizer.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.BuildConfig
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VersionDetailsDialogUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun initLogger() { AppLogger.init(InstrumentationRegistry.getInstrumentation().targetContext) }

    @Test fun materialLightEnglish() = verifyDialog(false, false, "en")
    @Test fun materialDarkJapanese() = verifyDialog(false, true, "ja")
    @Test fun nothingLightJapanese() = verifyDialog(true, false, "ja")
    @Test fun nothingDarkEnglish() = verifyDialog(true, true, "en")
    @Test fun materialLightJapanese() = verifyDialog(false, false, "ja")
    @Test fun materialDarkEnglish() = verifyDialog(false, true, "en")
    @Test fun nothingLightEnglish() = verifyDialog(true, false, "en")
    @Test fun nothingDarkJapanese() = verifyDialog(true, true, "ja")

    private fun localizedText(language: String, id: Int): String {
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        return compose.activity.createConfigurationContext(config).getString(id)
    }

    private fun personalityText(language: String) = "#${BuildConfig.PERSONALITY_NUMBER} · " +
        localizedText(language, VersionPersonality.fromNumber(BuildConfig.PERSONALITY_NUMBER).nameResource)

    @Composable private fun Localized(language: String, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val config = remember(context, language) {
            Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        }
        val localized = remember(context, config) { context.createConfigurationContext(config) }
        CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config,
            LocalResources provides localized.resources, content = content)
    }

    private fun verifyDialog(nothing: Boolean, dark: Boolean, language: String) {
        compose.setContent {
            Localized(language) {
                GlyphBartyTheme(nothingStyle = nothing, darkTheme = dark, dynamicColor = false) {
                    var visible by remember { mutableStateOf(true) }
                    if (visible) VersionDetailsDialog(onDismissRequest = { visible = false })
                }
            }
        }
        compose.onNodeWithTag("version_details_version").assertTextEquals(BuildConfig.VERSION_NAME)
        compose.onNodeWithTag("version_details_code").assertTextEquals(BuildConfig.VERSION_CODE.toString())
        compose.onNodeWithTag("version_details_build").performScrollTo().assertTextEquals(BuildConfig.BUILD_NUMBER)
        compose.onNodeWithTag("version_details_codename").performScrollTo().assertTextEquals(BuildConfig.CODE_NAME)
        compose.onNodeWithTag("version_details_roll").assertDoesNotExist()
        compose.onNodeWithTag("version_details_personality").performScrollTo().assertTextEquals(personalityText(language))
        compose.onNodeWithTag("version_details_seed").performScrollTo().assertTextContains(BuildConfig.PERSONALITY_SEED, substring = true)
        assertVisualHierarchy()
        compose.onNodeWithTag("version_details_version").performScrollTo()
        saveScreenshot("${if (nothing) "nothing" else "material"}-${if (dark) "dark" else "light"}-$language")
        compose.onNodeWithText(localizedText(language, android.R.string.ok)).assertIsDisplayed().performClick()
        compose.onNodeWithTag("version_details_dialog").assertDoesNotExist()
    }

    @Test fun smallDialogWithLargeFontsWrapsLongValuesAndKeepsCloseAccessible() = verifySmallDialog(true, false, "en")
    @Test fun smallMaterialLightEnglish() = verifySmallDialog(false, false, "en")
    @Test fun smallMaterialDarkJapanese() = verifySmallDialog(false, true, "ja")
    @Test fun smallNothingDarkJapanese() = verifySmallDialog(true, true, "ja")

    private fun verifySmallDialog(nothing: Boolean, dark: Boolean, language: String) {
        val longBuild = "${BuildConfig.CODE_NAME}-d2.2.0-" + "0123456789abcdef".repeat(12)
        compose.setContent {
            val parentComposition = rememberCompositionContext()
            // Dialog derives its separate window from LocalView, not LocalContext.
            // Give that actual host View scaled resources without changing device settings.
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                val scaledContext = ContextThemeWrapper(compose.activity, 0).apply {
                    applyOverrideConfiguration(Configuration(compose.activity.resources.configuration).apply {
                        setLocale(Locale.forLanguageTag(language))
                        fontScale = 2f
                    })
                }
                ComposeView(scaledContext).apply {
                    setParentCompositionContext(parentComposition)
                    setContent {
                        GlyphBartyTheme(nothingStyle = nothing, darkTheme = dark, dynamicColor = false) {
                            var visible by remember { mutableStateOf(true) }
                            if (visible) VersionDetailsDialog(onDismissRequest = { visible = false },
                                modifier = Modifier.width(280.dp).heightIn(max = 420.dp), buildNumber = longBuild)
                        }
                    }
                }
            })
        }
        val prefix = "small-${if (nothing) "nothing" else "material"}-${if (dark) "dark" else "light"}-$language"
        saveScreenshot("$prefix-top")
        assertFullWrappedText("version_details_version", BuildConfig.VERSION_NAME)
        assertFullWrappedText("version_details_build", longBuild)
        compose.onNodeWithTag("version_details_codename").performScrollTo().assertTextEquals(BuildConfig.CODE_NAME)
        compose.onNodeWithTag("version_details_personality").performScrollTo().assertTextEquals(personalityText(language))
        assertVisualHierarchy()
        compose.onNodeWithTag("version_details_seed").performScrollTo().assertIsDisplayed()
        saveScreenshot("$prefix-bottom")
        compose.onNodeWithText(localizedText(language, android.R.string.ok)).assertIsDisplayed().performClick()
        compose.onNodeWithTag("version_details_dialog").assertDoesNotExist()
    }

    private fun textLayout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun assertVisualHierarchy() {
        val codename = textLayout("version_details_codename")
        val personality = textLayout("version_details_personality")
        val seed = textLayout("version_details_seed")
        val version = textLayout("version_details_version")
        assertTrue(codename.layoutInput.style.fontSize.value > personality.layoutInput.style.fontSize.value)
        assertTrue(version.layoutInput.style.fontSize.value > personality.layoutInput.style.fontSize.value)
        assertTrue(personality.layoutInput.style.fontSize.value > seed.layoutInput.style.fontSize.value)
        assertEquals("Personality number and name should share one line", 1, personality.lineCount)
        assertFalse("Personality line must remain complete", personality.hasVisualOverflow)
        assertFalse("Codename must remain complete", codename.hasVisualOverflow)
    }

    private fun assertFullWrappedText(tag: String, expected: String) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).performScrollTo().assertTextEquals(expected)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals("Dialog must use the enlarged Android font configuration", 2f, results.single().layoutInput.density.fontScale, 0.001f)
        assertTrue("Text should wrap: $tag, style=${results.single().layoutInput.style}, size=${results.single().size}", results.single().lineCount > 1)
        assertFalse("Text must not be truncated: $tag", results.single().hasVisualOverflow)
    }

    @Test fun existingAboutVersionTapOpensFullDetailsAndBackDismisses() {
        compose.setContent {
            Localized("en") {
                GlyphBartyTheme(dynamicColor = false) { AboutScreen(onBack = {}) }
            }
        }
        compose.onNodeWithText(localizedText("en", R.string.about_overview_version_title)).performScrollTo().performClick()
        compose.onNodeWithTag("version_details_version").assertTextEquals(BuildConfig.VERSION_NAME)
        compose.onNodeWithTag("version_details_code").assertTextEquals(BuildConfig.VERSION_CODE.toString())
        pressBack()
        compose.onNodeWithTag("version_details_dialog").assertDoesNotExist()
        compose.onNodeWithText(localizedText("en", R.string.about_overview_version_title)).assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "version-details-epica").apply { mkdirs() }
        val image = compose.onNodeWithTag("version_details_dialog").captureToImage().asAndroidBitmap()
        try { File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { image.recycle() }
    }
}
