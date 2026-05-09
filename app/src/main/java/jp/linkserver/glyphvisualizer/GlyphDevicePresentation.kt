package jp.linkserver.glyphvisualizer

import com.nothing.ketchum.Common

data class GlyphDevicePresentation(
    val deviceLabel: String,
    val glyphLabel: String
) {
    val heroTitle: String
        get() = if (deviceLabel.isBlank()) glyphLabel else "$deviceLabel\n$glyphLabel"
}

object GlyphDevicePresentationRegistry {
    private data class Entry(
        val matcher: () -> Boolean,
        val presentation: GlyphDevicePresentation
    )

    private val entries: List<Entry> = listOf(
        Entry(
            matcher = { Common.is25111p() },
            presentation = GlyphDevicePresentation("Phone (4a) Pro", "Glyph Matrix")
        ),
        Entry(
            matcher = { Common.is23112() },
            presentation = GlyphDevicePresentation("Phone (3)", "Glyph Matrix")
        ),
        Entry(
            matcher = { Common.is25111() },
            presentation = GlyphDevicePresentation("Phone (4a)", "Glyph Bar")
        ),
        Entry(
            matcher = { Common.is24111() },
            presentation = GlyphDevicePresentation("Phone (3a) Series", "Glyph Lights")
        ),
        Entry(
            matcher = { Common.is23113() },
            presentation = GlyphDevicePresentation("Phone (2a) Plus", "Glyph Lights")
        ),
        Entry(
            matcher = { Common.is23111() },
            presentation = GlyphDevicePresentation("Phone (2a)", "Glyph Lights")
        ),
        Entry(
            matcher = { Common.is22111() },
            presentation = GlyphDevicePresentation("Phone (2)", "Glyph Lights")
        )
    )

    private val fallback = GlyphDevicePresentation("Nothing Phone", "Glyph Interface")

    fun current(): GlyphDevicePresentation {
        return entries.firstOrNull { it.matcher() }?.presentation ?: fallback
    }
}
