package jp.linkserver.glyphvisualizer.audio

internal enum class MediaTrackChangeDirection {
    NEXT,
    PREVIOUS,
    UNKNOWN
}

internal enum class MediaTrackDirectionSource {
    RAW_SKIP_HINT,
    QUEUE_INDEX,
    TRACK_NUMBER,
    TRUSTED_HISTORY,
    UNKNOWN_FALLBACK
}
