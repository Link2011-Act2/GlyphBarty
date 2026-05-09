package jp.linkserver.glyphvisualizer

data class GlyphDevicePresentation(
    val deviceLabel: String,
    val glyphLabel: String
) {
    val heroTitle: String
        get() = if (deviceLabel.isBlank()) glyphLabel else "$deviceLabel\n$glyphLabel"
}
