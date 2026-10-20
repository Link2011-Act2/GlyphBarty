package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.pyrite.screen.PyriteBackButton
import jp.linkserver.pyrite.screen.PyriteScreenScaffold

@Composable
internal fun GlyphScreenScaffold(title: String, onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit) {
    PyriteScreenScaffold(title, navigationIcon = {
        PyriteBackButton(onBack, stringResource(R.string.cd_back))
    }, content = content)
}
