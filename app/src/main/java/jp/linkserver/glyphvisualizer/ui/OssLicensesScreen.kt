package jp.linkserver.glyphvisualizer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import org.json.JSONArray
import org.json.JSONObject

private data class OssEntry(
    val title: String,
    val coordinate: String,
    val license: String,
    val url: String?,
    val body: String?
)

private fun JSONObject.optMeaningfulString(key: String): String? {
    val value = optString(key).trim()
    return value.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}

private fun loadOssEntries(context: android.content.Context): List<OssEntry> {
    return try {
        val json = context.assets.open("oss_licenses/oss_licenses_auto.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val items = root.optJSONArray("entries") ?: JSONArray()
        val results = ArrayList<OssEntry>(items.length())
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val title = obj.optString("title").ifBlank { obj.optString("coordinate") }
            if (title.isBlank()) continue
            results.add(
                OssEntry(
                    title = title,
                    coordinate = obj.optString("coordinate"),
                    license = obj.optMeaningfulString("license")
                        ?: context.getString(R.string.oss_unknown_license),
                    url = obj.optMeaningfulString("url"),
                    body = obj.optMeaningfulString("body")
                )
            )
        }
        results
    } catch (_: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssLicensesScreen(
    nothingStyleEnabled: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries = remember { loadOssEntries(context) }
    var selectedEntry by remember { mutableStateOf<OssEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.oss_screen_title),
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
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.oss_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(entries, key = { it.coordinate.ifBlank { it.title } }) { entry ->
                    OssEntryCard(
                        entry = entry,
                        onShowText = { selectedEntry = entry },
                        onOpenUrl = {
                            entry.url?.let { url ->
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                } catch (_: Exception) { }
                            }
                        }
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        val bodyText = resolveBodyText(entry, LocalContext.current)
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.title) },
            text = {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun OssEntryCard(
    entry: OssEntry,
    onShowText: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val bodyText = resolveBodyText(entry, LocalContext.current)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.coordinate.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.coordinate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenUrl,
                    enabled = !entry.url.isNullOrBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.oss_button_open_url))
                }
                if (bodyText.isNotBlank()) {
                    OutlinedButton(
                        onClick = onShowText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.oss_button_show_text))
                    }
                }
            }
        }
    }
}

private fun resolveBodyText(entry: OssEntry, context: android.content.Context): String {
    if (!entry.body.isNullOrBlank()) return entry.body
    val normalized = entry.license.trim().lowercase()
    return when {
        normalized.contains("apache") && normalized.contains("2.0") ->
            context.getString(R.string.license_apache_body)
        normalized.contains("glyph sdk end user license agreement") ->
            context.getString(R.string.license_glyph_sdk_eula_body)
        else -> entry.url ?: ""
    }
}
