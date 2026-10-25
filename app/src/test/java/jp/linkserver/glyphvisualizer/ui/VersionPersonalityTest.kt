package jp.linkserver.glyphvisualizer.ui

import android.content.res.Configuration
import jp.linkserver.glyphvisualizer.BuildConfig
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VersionPersonalityTest {
    @Test fun rollOrderAndBothLocalizedNamesAreFixed() {
        val english = listOf("Innocent", "Mad", "Composed", "Vivacious", "Depressed")
        val japanese = listOf("純粋", "狂気", "冷静", "活発", "憂鬱")
        assertEquals(listOf(1, 2, 3, 4, 5), VersionPersonality.entries.map { it.number })
        for ((language, expected) in listOf("en" to english, "ja" to japanese)) {
            val context = RuntimeEnvironment.getApplication()
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val resources = context.createConfigurationContext(config).resources
            for (number in 1..5) {
                assertEquals(expected[number - 1], resources.getString(VersionPersonality.fromNumber(number).nameResource))
            }
        }
    }

    @Test fun generatedBuildConfigContainsActualVersionSeedAndUnsignedShaRoll() {
        val core = BuildConfig.VERSION_NAME.substringBefore('-').substringBefore('+').split('.')
        val seed = "GlyphBarty:${core[0]}.${core[1]}"
        val unsignedByte = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))[0].toInt() and 0xff
        assertEquals(seed, BuildConfig.PERSONALITY_SEED)
        assertEquals((unsignedByte % 5) + 1, BuildConfig.PERSONALITY_NUMBER)
        if (seed == "GlyphBarty:2.2") assertEquals(VersionPersonality.VIVACIOUS, VersionPersonality.fromNumber(BuildConfig.PERSONALITY_NUMBER))
        assertTrue(BuildConfig.CODE_NAME.isNotBlank())
        assertTrue(BuildConfig.BUILD_NUMBER.startsWith("${BuildConfig.CODE_NAME}-"))
    }

    @Test fun invalidPersonalityNumbersDoNotSilentlySelectAnotherName() {
        for (number in listOf(Int.MIN_VALUE, 0, 6, Int.MAX_VALUE)) {
            val error = assertThrows(IllegalArgumentException::class.java) { VersionPersonality.fromNumber(number) }
            assertTrue(error.message.orEmpty().contains(number.toString()))
        }
    }
}
