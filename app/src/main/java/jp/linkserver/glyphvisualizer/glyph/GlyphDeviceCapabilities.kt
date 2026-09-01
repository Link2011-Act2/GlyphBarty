package jp.linkserver.glyphvisualizer.glyph

internal fun GlyphDeviceProfile.supportsFillOtherGlyphLights(): Boolean = this in setOf(
    GlyphDeviceProfile.PHONE1,
    GlyphDeviceProfile.PHONE2,
    GlyphDeviceProfile.PHONE2A,
    GlyphDeviceProfile.PHONE3A
)
