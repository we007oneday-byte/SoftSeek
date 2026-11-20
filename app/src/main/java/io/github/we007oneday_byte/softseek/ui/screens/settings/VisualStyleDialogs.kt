package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import io.github.we007oneday_byte.softseek.core.settings.VoiceAnimationStyle
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

@Composable
fun VisualStyleDialog(
    current: VisualStyle,
    onSelect: (VisualStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("视觉风格") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(VisualStyle.entries, key = { it.name }) { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = style == current,
                            onClick = { onSelect(style) },
                        )
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(text = style.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.softSeekColors.textSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun VoiceAnimationStyleDialog(
    current: VoiceAnimationStyle,
    onSelect: (VoiceAnimationStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通话收音动画") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(VoiceAnimationStyle.entries, key = { it.name }) { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = style == current,
                            onClick = { onSelect(style) },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = style.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
