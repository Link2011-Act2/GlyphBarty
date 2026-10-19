package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.pyrite.screen.PyriteBackButton
import jp.linkserver.pyrite.screen.PyriteScreenScaffold

/** Nothing uses the shared chrome; the existing Material option retains its title treatment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlyphScreenScaffold(title: String, onBack: () -> Unit, nothingStyle: Boolean,
    settingsHeader: Boolean = false, content: @Composable (PaddingValues) -> Unit) {
    val back: @Composable () -> Unit = { PyriteBackButton(onBack, stringResource(R.string.cd_back)) }
    if (nothingStyle) PyriteScreenScaffold(title, navigationIcon = back, content = content)
    else Scaffold(containerColor = MaterialTheme.colorScheme.surface, topBar = {
        TopAppBar(title = {
            if (settingsHeader) Text(title, Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.titleLarge.copy(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                fontWeight = FontWeight.Bold)
            else Text(title)
        }, navigationIcon = back, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ))
    }, content = content)
}
