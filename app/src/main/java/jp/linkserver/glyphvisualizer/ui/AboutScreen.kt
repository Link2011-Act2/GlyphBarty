package jp.linkserver.glyphvisualizer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.BuildConfig
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import jp.linkserver.glyphvisualizer.update.AppUpdateRepository
import jp.linkserver.glyphvisualizer.update.detectReleaseChannel
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.pyrite.about.AboutOverviewMosaic as PyriteAboutOverviewMosaic
import jp.linkserver.pyrite.about.AboutInfo
import jp.linkserver.pyrite.about.AboutDetail
import jp.linkserver.pyrite.about.AboutOverviewContent
import jp.linkserver.pyrite.settings.SettingsContentCard
import jp.linkserver.pyrite.settings.SettingsGroup
import jp.linkserver.pyrite.about.AboutUpdateCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOssLicenses: () -> Unit = {},
    onUpdateAvailable: (AppUpdateInfo) -> Unit = {},
    nothingStyleEnabled: Boolean = false
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val versionInfo = remember { resolveAppVersionInfo(context) }
    val versionName = versionInfo.first
    val versionCode = versionInfo.second
    val channelInfo = remember { splitVersionAndChannel(versionName) }
    val simpleVersion = channelInfo.first
    val channelName = remember(versionName) { detectReleaseChannel(versionName) }

    val channelLabel = when {
        channelName.equals("IntDev", ignoreCase = true) ->
            stringResource(R.string.about_dev_channel_value_intdev)
        channelName.equals("Beta", ignoreCase = true) ->
            stringResource(R.string.about_dev_channel_value_beta)
        channelName.equals("Stable", ignoreCase = true) ||
            channelName.equals("Release", ignoreCase = true) ->
            stringResource(R.string.about_dev_channel_value_stable)
        else -> stringResource(R.string.about_dev_channel_value_unknown)
    }

    val channelDescResId = when {
        channelName.equals("IntDev", ignoreCase = true) -> R.string.about_dev_channel_desc_intdev
        channelName.equals("Beta", ignoreCase = true) -> R.string.about_dev_channel_desc_beta
        channelName.equals("Stable", ignoreCase = true) ||
            channelName.equals("Release", ignoreCase = true) -> R.string.about_dev_channel_desc_stable
        else -> R.string.about_dev_channel_desc_unknown
    }

    var showChannelDialog by remember { mutableStateOf(false) }
    var showVersionDetailsDialog by remember { mutableStateOf(false) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var logExists by remember { mutableStateOf(AppLogger.exists()) }
    var showClearLogConfirmation by remember { mutableStateOf(false) }
    val repositoryUrl = stringResource(R.string.about_support_site_url)

    fun startUpdateCheck(manual: Boolean) {
        if (checkingUpdates) return
        checkingUpdates = true
        updateStatus = if (manual) resources.getString(R.string.about_update_checking) else null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                AppUpdateRepository.checkAndRecord(
                    context = context,
                    repositoryUrl = repositoryUrl,
                    showLatestForTesting = isShowLatestReleaseForTestingEnabled(context)
                )
            }
            checkingUpdates = false
            result
                .onSuccess { updateInfo ->
                    if (updateInfo != null) {
                        updateStatus = resources.getString(R.string.about_update_available, updateInfo.tagName)
                        onUpdateAvailable(updateInfo)
                    } else if (manual) {
                        updateStatus = resources.getString(R.string.about_update_latest)
                    }
                }
                .onFailure { error ->
                    updateStatus = resources.getString(
                        R.string.about_update_check_failed,
                        error.localizedMessage ?: error.javaClass.simpleName
                    )
                }
        }
    }

    GlyphScreenScaffold(stringResource(R.string.about_screen_title), onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            PyriteAboutOverviewMosaic(
                info = AboutInfo(stringResource(R.string.app_name),
                    "$channelLabel ${stringResource(R.string.about_overview_release_line, versionName, versionCode).replace(' ', '\u00A0')}"),
                overview = AboutOverviewContent(
                    channel = AboutDetail(stringResource(R.string.about_overview_channel_title), channelName.ifBlank { channelLabel }),
                    build = AboutDetail(stringResource(R.string.about_overview_build_title), BuildConfig.BUILD_NUMBER),
                    version = AboutDetail(stringResource(R.string.about_overview_version_title),
                        stringResource(R.string.about_overview_version_value, simpleVersion))
                ),
                icon = { Image(painterResource(R.drawable.app_icon_foreground), null, Modifier.fillMaxSize()) },
                onVersionClick = { showVersionDetailsDialog = true },
                onChannelClick = { showChannelDialog = true }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionTitle(stringResource(R.string.about_updates_title))
                AboutUpdateCard(
                    description = stringResource(R.string.about_updates_body),
                    status = updateStatus ?: stringResource(R.string.about_update_not_checked),
                    actionLabel = stringResource(R.string.about_update_check_button),
                    onCheck = { startUpdateCheck(manual = true) },
                    checking = checkingUpdates, hasResult = updateStatus != null
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionTitle(stringResource(R.string.about_known_issues_title))
                SettingsContentCard() {
                    Text(
                        text = stringResource(R.string.about_known_issues_body),
                        modifier = Modifier.padding(
                            horizontal = 22.dp,
                            vertical = 18.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionTitle(stringResource(R.string.settings_category_information))
                SettingsGroup {
                    item("about-action-0") {
                        SettingsContentCard() {
                            AboutSettingsActionCardContent(
                                title = stringResource(R.string.about_support_title),
                                description = stringResource(R.string.about_support_help),
                                actionLabel = stringResource(R.string.about_support_open_site),
                                nothingStyleEnabled = nothingStyleEnabled,
                                onAction = {
                                    openUrl(context, resources.getString(R.string.about_support_site_url))
                                }
                            )
                        }
                    }

                    item("about-action-1") {
                        SettingsContentCard() {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                    horizontal = 22.dp,
                                    vertical = 18.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = stringResource(R.string.about_debug_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (nothingStyleEnabled) {
                                            FontWeight.Normal
                                        } else {
                                            FontWeight.Bold
                                        }
                                    )
                                    Text(
                                        text = stringResource(R.string.about_debug_help),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { AppLogger.share(context) },
                                        modifier = Modifier.weight(1f),
                                        enabled = logExists
                                    ) {
                                        Text(
                                            if (logExists) {
                                                stringResource(R.string.about_debug_share)
                                            } else {
                                                stringResource(R.string.about_debug_empty)
                                            }
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { showClearLogConfirmation = true },
                                        modifier = Modifier.weight(1f),
                                        enabled = logExists
                                    ) {
                                        Text(stringResource(R.string.about_debug_clear))
                                    }
                                }
                            }
                        }
                    }

                    item("about-action-2") {
                        SettingsContentCard() {
                            AboutSettingsActionCardContent(
                                title = stringResource(R.string.about_oss_title),
                                description = stringResource(R.string.about_oss_help),
                                actionLabel = stringResource(R.string.about_oss_open),
                                nothingStyleEnabled = nothingStyleEnabled,
                                onAction = onOssLicenses
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = {
                Text(stringResource(R.string.about_dev_channel_dialog_title, channelLabel))
            },
            text = { Text(stringResource(channelDescResId)) },
            confirmButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showVersionDetailsDialog) {
        VersionDetailsDialog(
            onDismissRequest = { showVersionDetailsDialog = false },
            versionName = versionName,
            versionCode = versionCode,
        )
    }

    if (showClearLogConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearLogConfirmation = false },
            title = { Text(stringResource(R.string.about_debug_clear_dialog_title)) },
            text = { Text(stringResource(R.string.about_debug_clear_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppLogger.clear()
                        logExists = false
                        showClearLogConfirmation = false
                    }
                ) {
                    Text(stringResource(R.string.about_debug_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogConfirmation = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun AboutSettingsActionCardContent(
    title: String,
    description: String,
    actionLabel: String,
    nothingStyleEnabled: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 22.dp,
                vertical = 18.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(actionLabel)
        }
    }
}



private fun splitVersionAndChannel(versionName: String): Pair<String, String> {
    val hyphenPos = versionName.lastIndexOf('-')
    if (hyphenPos > 0 && hyphenPos < versionName.length - 1) {
        val core = versionName.substring(0, hyphenPos).trim()
        val channel = versionName.substring(hyphenPos + 1).trim().trim('(', ')')
        if (core.isNotBlank() && channel.isNotBlank()) return Pair(core, channel)
    }
    return Pair(versionName, "unknown")
}

private fun resolveAppVersionInfo(context: Context): Pair<String, Int> {
    return try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = info.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
        Pair(versionName, versionCode)
    } catch (_: Exception) {
        Pair("unknown", 0)
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) { }
}
