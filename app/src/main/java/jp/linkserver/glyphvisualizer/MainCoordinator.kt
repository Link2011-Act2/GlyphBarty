package jp.linkserver.glyphvisualizer

import android.content.Context

/** Owns the repeated store -> service -> persistence update policies used by the UI. */
internal class MainCoordinator(
    private val context: Context,
    private val scheduleParameterSync: () -> Unit,
    private val syncCurrentParameters: (CaptureUiState) -> Unit,
    private val pushConfig: (CaptureUiState) -> Unit = { state ->
        CaptureCommandGateway.updateConfigPreservingRecordingLight(
            context = context,
            config = state.toCaptureConfig()
        )
    },
    private val persist: (CaptureUiState) -> Unit = { state ->
        SettingsPreferences.save(context, state)
    }
) {
    fun update(transform: (CaptureUiState) -> CaptureUiState): CaptureUiState {
        var updated = CaptureUiStore.state
        CaptureUiStore.update { current ->
            transform(current).also { updated = it }
        }
        return updated
    }

    fun updateDeferred(transform: (CaptureUiState) -> CaptureUiState) {
        update(transform)
        scheduleParameterSync()
    }

    fun updateAndSync(transform: (CaptureUiState) -> CaptureUiState) {
        syncCurrentParameters(update(transform))
    }

    fun updateAndPersist(transform: (CaptureUiState) -> CaptureUiState) {
        persist(update(transform))
    }

    fun updatePushAndPersist(transform: (CaptureUiState) -> CaptureUiState) {
        update(transform).also { updated ->
            pushConfig(updated)
            persist(updated)
        }
    }

    fun updatePersistAndSync(transform: (CaptureUiState) -> CaptureUiState) {
        update(transform).also { updated ->
            persist(updated)
            syncCurrentParameters(updated)
        }
    }
}
