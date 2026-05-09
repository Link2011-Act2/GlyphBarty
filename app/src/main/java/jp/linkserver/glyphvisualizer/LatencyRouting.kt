package jp.linkserver.glyphvisualizer

fun CaptureUiState.resolvedLatencyMs(bluetoothOutputActive: Boolean): Float {
    return if (latencyAutoSwitchEnabled) {
        if (bluetoothOutputActive) {
            bluetoothLatencyMs
        } else {
            defaultOutputLatencyMs
        }
    } else {
        latencyMs
    }.coerceIn(0f, 500f)
}

fun CaptureUiState.withResolvedLatency(bluetoothOutputActive: Boolean): CaptureUiState {
    return copy(
        latencyMs = resolvedLatencyMs(bluetoothOutputActive),
        isBluetoothOutputActive = bluetoothOutputActive
    )
}

fun CaptureUiState.withLatencyEditedForCurrentRoute(
    newLatencyMs: Float,
    bluetoothOutputActive: Boolean
): CaptureUiState {
    val clamped = newLatencyMs.coerceIn(0f, 500f)
    val updated = if (latencyAutoSwitchEnabled) {
        if (bluetoothOutputActive) {
            copy(bluetoothLatencyMs = clamped)
        } else {
            copy(defaultOutputLatencyMs = clamped)
        }
    } else {
        copy(latencyMs = clamped)
    }
    return updated.withResolvedLatency(bluetoothOutputActive)
}
