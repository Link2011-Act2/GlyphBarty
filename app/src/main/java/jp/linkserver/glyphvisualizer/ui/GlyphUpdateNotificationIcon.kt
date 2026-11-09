package jp.linkserver.glyphvisualizer.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import jp.linkserver.goldy.GoldyState
import jp.linkserver.glyphvisualizer.ui.theme.NothingRed

/** Host badge palette; the shared Elief icon artwork and notification eligibility stay unchanged. */
@Composable
internal fun GlyphUpdateNotificationIcon(
    state: GoldyState,
    nothingStyle: Boolean,
    onOpenPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.notificationUpdate == null) return
    val strings = goldyNotificationStrings()
    IconButton(onClick = onOpenPrompt, modifier = modifier.testTag("goldy-notification-icon")
        .semantics { contentDescription = strings.iconDescription }) {
        BadgedBox(badge = {
            Badge(
                modifier = Modifier.testTag("glyph-update-badge"),
                containerColor = if (nothingStyle) NothingRed else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }) {
            Icon(painterResource(jp.linkserver.goldy.compose.R.drawable.elief_update), null)
        }
    }
}
