package jp.linkserver.glyphvisualizer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class SettingsGroupPosition {
    Single,
    Top,
    Middle,
    Bottom
}

@Composable
fun SettingsNothingGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = settingsCardColor(nothingStyle = true),
        border = settingsCardBorder(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.outlineVariant
        ) {}
    }
}

@Composable
fun SettingsEntry(
    title: String,
    description: String,
    onClick: () -> Unit,
    nothingStyle: Boolean,
    position: SettingsGroupPosition
) {
    val containerColor = settingsCardColor(nothingStyle)
    val chevronTint = if (nothingStyle) {
        if (isSystemInDarkTheme()) Color(0xFFEDEDED) else Color(0xFF1A1A1A)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = itemShape(position, nothingStyle),
        color = containerColor,
        border = settingsCardBorder(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (nothingStyle) 22.dp else 16.dp,
                    vertical = if (nothingStyle) 18.dp else 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (nothingStyle) FontWeight.Normal else FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleEntry(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    nothingStyle: Boolean,
    position: SettingsGroupPosition
) {
    val containerColor = settingsCardColor(nothingStyle)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemShape(position, nothingStyle),
        color = containerColor,
        border = settingsCardBorder(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (nothingStyle) 22.dp else 16.dp,
                    vertical = if (nothingStyle) 18.dp else 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (nothingStyle) FontWeight.Normal else FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors()
            )
        }
    }
}

@Composable
fun SettingsRadioEntry(
    title: String,
    description: String,
    options: List<Pair<String, Boolean>>,
    onOptionSelected: (Int) -> Unit,
    nothingStyle: Boolean,
    position: SettingsGroupPosition
) {
    val containerColor = settingsCardColor(nothingStyle)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemShape(position, nothingStyle),
        color = containerColor,
        border = settingsCardBorder(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (nothingStyle) 22.dp else 16.dp,
                    vertical = if (nothingStyle) 18.dp else 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (nothingStyle) FontWeight.Normal else FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            options.forEachIndexed { index, (label, selected) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onOptionSelected(index) }
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun itemShape(position: SettingsGroupPosition, nothingStyle: Boolean): RoundedCornerShape {
    val cornerRadius = if (nothingStyle) 22.dp else 30.dp
    val innerRadius = if (nothingStyle) 6.dp else 10.dp
    return when (position) {
        SettingsGroupPosition.Single -> RoundedCornerShape(cornerRadius)
        SettingsGroupPosition.Top -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        SettingsGroupPosition.Middle -> RoundedCornerShape(innerRadius)
        SettingsGroupPosition.Bottom -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    }
}

@Composable
private fun settingsCardBorder(): BorderStroke? {
    return if (isSystemInDarkTheme()) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun settingsCardColor(nothingStyle: Boolean): Color {
    return if (nothingStyle) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    }
}
