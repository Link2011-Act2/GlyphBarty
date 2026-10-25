package jp.linkserver.glyphvisualizer.ui

import androidx.annotation.StringRes
import jp.linkserver.glyphvisualizer.R

/** Stable roll order; independent of the manually selected app codename. */
internal enum class VersionPersonality(val number: Int, @param:StringRes val nameResource: Int) {
    INNOCENT(1, R.string.about_personality_innocent),
    MAD(2, R.string.about_personality_mad),
    COMPOSED(3, R.string.about_personality_composed),
    VIVACIOUS(4, R.string.about_personality_vivacious),
    DEPRESSED(5, R.string.about_personality_depressed);

    companion object {
        fun fromNumber(number: Int): VersionPersonality = requireNotNull(entries.firstOrNull { it.number == number }) {
            "Personality roll must be in 1..5, got $number"
        }
    }
}
