package jp.linkserver.glyphvisualizer.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.linkserver.goldy.*
import jp.linkserver.goldy.compose.*
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.update.*
import jp.linkserver.goldy.compose.UpdateOverviewScreen as EliefOverviewScreen

@Composable
fun UpdateOverviewScreen(updateInfo: UpdateCandidate?, onBack: () -> Unit) {
    val model: UpdateDownloadViewModel = viewModel()
    UpdateOverviewScreen(updateInfo, onBack, model)
}

@Composable
internal fun UpdateOverviewScreen(updateInfo: UpdateCandidate?, onBack: () -> Unit,
    model: UpdateDownloadViewModel) {
    val state by model.state.collectAsState()
    val pinned by model.selection.collectAsState()
    val candidate = pinned ?: updateInfo
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(updateInfo) { model.selectCandidate(updateInfo) }
    DisposableEffect(owner,model) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_RESUME) model.onForegroundChanged(true)
            if(event==Lifecycle.Event.ON_PAUSE) model.onForegroundChanged(false)
        }
        owner.lifecycle.addObserver(observer)
        model.onForegroundChanged(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { owner.lifecycle.removeObserver(observer); model.onForegroundChanged(false) }
    }
    val active=state.releaseIdentity==transferIdentity(candidate)
    val strings=goldyUpdateStrings().let { if(active && state.waiting) it.copy(download=stringResource(R.string.goldy_transfer_restart)) else it }
    val prepared=state.preparedRequest.takeIf { candidate==null }
    if(prepared!=null) {
        EliefOverviewScreen(preparedApk=prepared.metadata,
            transfer=if(state.restoring) UpdateTransferState.PreparingInstall else state.transfer,
            onBack=onBack,onAction={model.action(null,it)},strings=strings,
            message=state.message.takeUnless { state.transfer is UpdateTransferState.Failed })
        return
    }
    EliefOverviewScreen(UpdateUiState(candidate,apkAvailable=candidate?.release?.let(ApkUpdateProfile.selection::select)!=null,
        transfer=if(state.restoring) UpdateTransferState.PreparingInstall else if(active) state.transfer else UpdateTransferState.Idle,
        message=state.message.takeUnless { state.transfer is UpdateTransferState.Failed }),onBack,
        { openUpdateUrl(context,it) },{ model.action(candidate,it) },strings=strings,header={
            if(active && state.waiting) {
                state.retainedProgress?.let { Text(DownloadProgressFormatter.format(it,locale)) }
                TextButton(onClick=model::cancel) { Text(stringResource(R.string.goldy_update_cancel_download)) }
            }
        })
}
internal fun openUpdateUrl(context: android.content.Context, url: String) {
    val uri=android.net.Uri.parse(url)
    if(uri.scheme in listOf("https","http")) runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,uri)) }
}
