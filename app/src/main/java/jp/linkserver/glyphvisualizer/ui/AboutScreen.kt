package jp.linkserver.glyphvisualizer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingDotFontFamily
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import jp.linkserver.glyphvisualizer.update.AppUpdateRepository
import jp.linkserver.glyphvisualizer.update.detectReleaseChannel
import jp.linkserver.glyphvisualizer.update.isShowLatestReleaseForTestingEnabled
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
        updateStatus = if (manual) context.getString(R.string.about_update_checking) else null
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
                        fontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
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
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AboutOverviewMosaic(
                channelName = channelName,
                versionName = versionName,
                simpleVersion = simpleVersion,
                versionCode = versionCode,
                buildNumber = BuildConfig.BUILD_NUMBER,
                nothingStyleEnabled = nothingStyleEnabled,
                onVersionClick = { showVersionDetailsDialog = true },
                onChannelClick = { showChannelDialog = true }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionTitle(stringResource(R.string.about_updates_title))
                SettingsItemSurface(
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.about_updates_body),
                            modifier = Modifier.padding(
                                horizontal = 22.dp,
                                vertical = 18.dp
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 22.dp
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 22.dp,
                                    vertical = 14.dp
                                ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = updateStatus
                                    ?: stringResource(R.string.about_update_not_checked),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (updateStatus == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { startUpdateCheck(manual = true) },
                                enabled = !checkingUpdates
                            ) {
                                Text(stringResource(R.string.about_update_check_button))
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AboutSectionTitle(stringResource(R.string.about_known_issues_title))
                SettingsItemSurface(
                    nothingStyle = nothingStyleEnabled,
                    position = SettingsGroupPosition.Single
                ) {
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
                Column {
                    SettingsItemSurface(
                        nothingStyle = nothingStyleEnabled,
                        position = SettingsGroupPosition.Top
                    ) {
                        AboutSettingsActionCardContent(
                            title = stringResource(R.string.about_support_title),
                            description = stringResource(R.string.about_support_help),
                            actionLabel = stringResource(R.string.about_support_open_site),
                            nothingStyleEnabled = nothingStyleEnabled,
                            onAction = {
                                openUrl(context, context.getString(R.string.about_support_site_url))
                            }
                        )
                    }
                    SettingsDividerGap()
                    SettingsItemSurface(
                        nothingStyle = nothingStyleEnabled,
                        position = SettingsGroupPosition.Middle
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
                    SettingsDividerGap()
                    SettingsItemSurface(
                        nothingStyle = nothingStyleEnabled,
                        position = SettingsGroupPosition.Bottom
                    ) {
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

@Composable
private fun AboutOverviewMosaic(
    channelName: String,
    versionName: String,
    simpleVersion: String,
    versionCode: Int,
    buildNumber: String,
    nothingStyleEnabled: Boolean,
    onVersionClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardBorder: BorderStroke? = null
    val titleFontFamily = if (nothingStyleEnabled) NTypeFontFamily else null
    val gap = 3.dp
    val cardRadius = 20.dp
    val heroHeight = 176.dp
    val bridgeLength = 48.dp
    val bridgeOverlap = 1.dp
    val gutterColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val squareSize = (maxWidth - gap) * 0.5f

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Surface(
                onClick = onVersionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight),
                shape = RoundedCornerShape(cardRadius),
                color = cardColor,
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Image(
                        painter = painterResource(R.drawable.app_icon_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontFamily = titleFontFamily,
                            fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold,
                            fontSize = 27.sp,
                            lineHeight = 32.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.about_overview_release_line,
                                versionName,
                                versionCode
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(squareSize)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(squareSize),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Surface(
                        onClick = onChannelClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(squareSize),
                        shape = RoundedCornerShape(cardRadius),
                        color = cardColor,
                        border = cardBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.about_overview_channel_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = channelName,
                                fontFamily = titleFontFamily,
                                fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold,
                                fontSize = 25.sp,
                                lineHeight = 29.sp
                            )
                        }
                    }

                    Surface(
                        onClick = onVersionClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(squareSize),
                        shape = RoundedCornerShape(cardRadius),
                        color = cardColor,
                        border = cardBorder
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.about_overview_build_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = buildNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 16.sp
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.about_overview_version_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.about_overview_version_value,
                                        simpleVersion
                                    ),
                                    fontFamily = if (nothingStyleEnabled) {
                                        NothingDotFontFamily
                                    } else {
                                        null
                                    },
                                    fontWeight = if (nothingStyleEnabled) FontWeight.Normal else FontWeight.Bold,
                                    fontSize = 25.sp,
                                    lineHeight = 29.sp
                                )
                            }
                        }
                    }
                }

                val horizontalBridgeStart = (squareSize - bridgeLength) * 0.5f
                val verticalBridgeStart = (squareSize - bridgeLength) * 0.5f

                Box(
                    modifier = Modifier
                        .offset(
                            x = horizontalBridgeStart,
                            y = -gap - bridgeOverlap
                        )
                        .width(bridgeLength)
                        .height(gap + bridgeOverlap * 2f)
                        .background(cardColor)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = horizontalBridgeStart - gap * 0.5f,
                            y = -gap
                        )
                        .size(gap)
                        .background(gutterColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = horizontalBridgeStart + bridgeLength - gap * 0.5f,
                            y = -gap
                        )
                        .size(gap)
                        .background(gutterColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = squareSize - bridgeOverlap,
                            y = verticalBridgeStart
                        )
                        .width(gap + bridgeOverlap * 2f)
                        .height(bridgeLength)
                        .background(cardColor)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = squareSize,
                            y = verticalBridgeStart - gap * 0.5f
                        )
                        .size(gap)
                        .background(gutterColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = squareSize,
                            y = verticalBridgeStart + bridgeLength - gap * 0.5f
                        )
                        .size(gap)
                        .background(gutterColor, CircleShape)
                )
            }
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
