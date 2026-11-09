package jp.linkserver.glyphvisualizer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jp.linkserver.goldy.compose.UpdateNotificationStrings
import jp.linkserver.goldy.compose.UpdateStrings
import jp.linkserver.glyphvisualizer.R

@Composable
internal fun goldyNotificationStrings() = UpdateNotificationStrings(
    title=stringResource(R.string.goldy_update_title), available=stringResource(R.string.goldy_update_available),
    openDetails=stringResource(R.string.goldy_update_view), hideBanner=stringResource(R.string.goldy_update_hide),
    ignoreVersion=stringResource(R.string.goldy_update_ignore), close=stringResource(android.R.string.cancel),
    iconDescription=stringResource(R.string.goldy_update_icon), check=stringResource(R.string.about_update_check_button),
    retry=stringResource(R.string.goldy_update_retry), checking=stringResource(R.string.about_update_checking),
    idle=stringResource(R.string.goldy_update_idle), upToDate=stringResource(R.string.about_update_latest),
    noReleases=stringResource(R.string.about_update_latest), noEligible=stringResource(R.string.about_update_latest),
    unsupported=stringResource(R.string.goldy_update_failed), disabled=stringResource(R.string.goldy_update_idle),
    failure=stringResource(R.string.goldy_update_failed), throttled=stringResource(R.string.goldy_update_failed),
    incomplete=stringResource(R.string.goldy_update_incomplete), cached=stringResource(R.string.goldy_update_cached))

@Composable
internal fun goldyUpdateStrings() = UpdateStrings(
    title=stringResource(R.string.goldy_update_title), back=stringResource(R.string.goldy_update_back),
    releaseNotes=stringResource(R.string.update_release_notes_title), noNotes=stringResource(R.string.update_release_notes_empty),
    history=stringResource(R.string.goldy_update_history), expand=stringResource(R.string.goldy_update_expand),
    collapse=stringResource(R.string.goldy_update_collapse), openRelease=stringResource(R.string.goldy_update_open_release),
    download=stringResource(R.string.goldy_transfer_download), cancel=stringResource(R.string.goldy_update_cancel_download),
    retry=stringResource(R.string.goldy_update_retry), install=stringResource(R.string.goldy_transfer_install),
    allowInstall=stringResource(R.string.goldy_transfer_allow_install), permissionRequired=stringResource(R.string.goldy_transfer_permission_guidance),
    checking=stringResource(R.string.about_update_checking), noUpdate=stringResource(R.string.about_update_latest),
    noApk=stringResource(R.string.goldy_update_no_apk), downloading=stringResource(R.string.goldy_update_downloading),
    verifying=stringResource(R.string.goldy_update_verifying), preparing=stringResource(R.string.goldy_update_preparing),
    ready=stringResource(R.string.goldy_update_ready), handedOff=stringResource(R.string.goldy_update_handed_off),
    cancelled=stringResource(R.string.goldy_update_cancelled), incompleteHistory=stringResource(R.string.goldy_update_incomplete))
