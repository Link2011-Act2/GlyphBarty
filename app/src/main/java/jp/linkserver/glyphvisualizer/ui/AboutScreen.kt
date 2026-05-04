package jp.linkserver.glyphvisualizer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jp.linkserver.glyphvisualizer.AppLogger
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOssLicenses: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionInfo = remember { resolveAppVersionInfo(context) }
    val versionName = versionInfo.first
    val versionCode = versionInfo.second
    val channelInfo = remember { splitVersionAndChannel(versionName) }
    val simpleVersion = channelInfo.first
    val channelName = channelInfo.second

    val channelLabel = when {
        channelName.equals("IntDev", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_intdev)
        channelName.equals("Beta", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_beta)
        channelName.equals("Stable", ignoreCase = true) -> stringResource(R.string.about_dev_channel_value_stable)
        else -> stringResource(R.string.about_dev_channel_value_unknown)
    }
    val channelDescResId = when {
        channelName.equals("IntDev", ignoreCase = true) -> R.string.about_dev_channel_desc_intdev
        channelName.equals("Beta", ignoreCase = true) -> R.string.about_dev_channel_desc_beta
        channelName.equals("Stable", ignoreCase = true) -> R.string.about_dev_channel_desc_stable
        else -> R.string.about_dev_channel_desc_unknown
    }

    var showChannelDialog by remember { mutableStateOf(false) }
    var showVersionDetailsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_screen_title)) },
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
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
                    color = MaterialTheme.colorScheme.surfaceContainer,
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
                    text = "デバッグログ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "問題が発生した際、ログをシェアして報告できます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                var logExists by remember { mutableStateOf(AppLogger.exists()) }
                OutlinedButton(
                    onClick = { AppLogger.share(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = logExists
                ) {
                    Text(if (logExists) "ログをシェア" else "ログなし")
                }
                OutlinedButton(
                    onClick = {
                        AppLogger.clear()
                        logExists = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = logExists
                ) {
                    Text("ログを消去")
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
                        versionCode
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
