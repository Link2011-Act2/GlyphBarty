package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.BuildConfig
import jp.linkserver.glyphvisualizer.R

@Composable
internal fun VersionDetailsDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
    buildNumber: String = BuildConfig.BUILD_NUMBER,
    codename: String = BuildConfig.CODE_NAME,
    personalityNumber: Int = BuildConfig.PERSONALITY_NUMBER,
    personalitySeed: String = BuildConfig.PERSONALITY_SEED,
) {
    val personality = VersionPersonality.fromNumber(personalityNumber)
    AlertDialog(
        modifier = modifier.testTag("version_details_dialog"),
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.about_version_details_title), fontFamily = FontFamily.SansSerif) },
        text = {
            SelectionContainer {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("version_details_content"),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailLabel(stringResource(R.string.about_version_details_version))
                        Text(versionName, Modifier.fillMaxWidth().testTag("version_details_version"),
                            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    DetailField(stringResource(R.string.about_version_details_internal_code), versionCode.toString(), "version_details_code")
                    DetailField(stringResource(R.string.about_version_details_build_number), buildNumber, "version_details_build")
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DetailLabel(stringResource(R.string.about_version_details_codename))
                                Text(codename, Modifier.fillMaxWidth().testTag("version_details_codename"),
                                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailLabel(stringResource(R.string.about_version_details_personality))
                                Text(stringResource(R.string.about_personality_result, personality.number, stringResource(personality.nameResource)),
                                    Modifier.fillMaxWidth().testTag("version_details_personality"),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1)
                                Text(stringResource(R.string.about_version_details_seed, personalitySeed),
                                    Modifier.fillMaxWidth().testTag("version_details_seed"),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

@Composable
private fun DetailLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DetailField(label: String, value: String, tag: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailLabel(label)
        Text(value, Modifier.fillMaxWidth().testTag(tag), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
