package jp.linkserver.glyphvisualizer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.BuildConfig
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import jp.linkserver.glyphvisualizer.update.checkGitHubReleaseUpdate
import jp.linkserver.glyphvisualizer.update.detectReleaseChannel
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.isUpdateCheckIntervalIgnoredForTesting
import jp.linkserver.glyphvisualizer.update.markUpdateCheckFinished
import jp.linkserver.glyphvisualizer.update.setShowLatestReleaseForTestingEnabled
import jp.linkserver.glyphvisualizer.update.setUpdateCheckIntervalIgnoredForTesting
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
    val scope = rememberCoroutineScope()
    val versionInfo = remember { resolveAppVersionInfo(context) }
    val versionName = versionInfo.first
    val versionCode = versionInfo.second
    val channelInfo = remember { splitVersionAndChannel(versionName) }
    val simpleVersion = channelInfo.first
    val channelName = remember(versionName) { detectReleaseChannel(versionName) }

    val channelLabel = when {
        channelName.equals("IntDev", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_intdev)
        channelName.equals("Beta", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_beta)
        channelName.equals("Stable", ignoreCase = true) ||
            channelName.equals("Release", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_stable)
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
    val isIntDevBuild = remember { isIntDevBuild() }
    var showLatestForTesting by remember { mutableStateOf(isShowLatestReleaseForTestingEnabled(context)) }
    var ignoreCheckIntervalForTesting by remember { mutableStateOf(isUpdateCheckIntervalIgnoredForTesting(context)) }
    val repositoryUrl = stringResource(R.string.about_support_site_url)

    fun startUpdateCheck(manual: Boolean) {
        if (checkingUpdates) return
        checkingUpdates = true
        updateStatus = if (manual) context.getString(R.string.about_update_checking) else null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                checkGitHubReleaseUpdate(
                    repositoryUrl = repositoryUrl,
                    showLatestForTesting = isShowLatestReleaseForTestingEnabled(context)
                )
            }
            markUpdateCheckFinished(context)
            checkingUpdates = false
            result
                .onSuccess { updateInfo ->
                    if (updateInfo != null) {
                        updateStatus = context.getString(R.string.about_update_available, updateInfo.tagName)
                        onUpdateAvailable(updateInfo)
                    } else if (manual) {
                        updateStatus = context.getString(R.string.about_update_latest)
                    }
                }
                .onFailure { error ->
                    updateStatus = context.getString(
                        R.string.about_update_check_failed,
                        error.localizedMessage ?: error.javaClass.simpleName
                    )
                }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = stringResource(R.string.about_screen_title),
                        fontFamily = NTypeFontFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        lineHeight = 44.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = true)
                    ),
                    fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_version_label, channelLabel, versionName, versionCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_version_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVersionDetailsDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.about_simple_version_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.about_simple_version_value, simpleVersion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChannelDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.about_dev_channel_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = channelLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_updates_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.about_updates_body),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { startUpdateCheck(manual = true) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !checkingUpdates
                ) {
                    if (checkingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(stringResource(R.string.about_update_check_button))
                }
                updateStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isIntDevBuild) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.about_update_test_mode_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.about_update_test_mode_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = showLatestForTesting,
                                    onCheckedChange = { enabled ->
                                        showLatestForTesting = enabled
                                        setShowLatestReleaseForTestingEnabled(context, enabled)
                                    }
                                )
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.about_update_ignore_interval_test_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.about_update_ignore_interval_test_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = ignoreCheckIntervalForTesting,
                                    onCheckedChange = { enabled ->
                                        ignoreCheckIntervalForTesting = enabled
                                        setUpdateCheckIntervalIgnoredForTesting(context, enabled)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_known_issues_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.about_known_issues_body),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_support_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.about_support_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        openUrl(context, context.getString(R.string.about_support_site_url))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_support_open_site))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_debug_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.about_debug_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                var logExists by remember { mutableStateOf(AppLogger.exists()) }
                OutlinedButton(
                    onClick = { AppLogger.share(context) },
                    modifier = Modifier.fillMaxWidth(),
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
                    onClick = {
                        AppLogger.clear()
                        logExists = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = logExists
                ) {
                    Text(stringResource(R.string.about_debug_clear))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_oss_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.about_oss_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onOssLicenses,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_oss_open))
                }
            }
        }
    }

    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            title = {
                Text(stringResource(R.string.about_dev_channel_dialog_title, channelName))
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
        AlertDialog(
            onDismissRequest = { showVersionDetailsDialog = false },
            title = { Text(stringResource(R.string.about_version_details_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.about_version_details_message,
                        versionName,
                        versionCode,
                        BuildConfig.BUILD_NUMBER
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showVersionDetailsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
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
