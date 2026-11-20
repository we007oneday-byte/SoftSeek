package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

/**
 * 设计预览：循环 VisualStyle.entries，逐一渲染缩略卡片与按钮。
 * 这样十个主题的布局/色板/按钮变化能在 Android Studio 预览里一次看完。
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun VisualStyleGalleryPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        VisualStyle.entries.forEach { style ->
            SoftSeekTheme(
                settings = AppSettings(visualStyle = style),
                isLowEndDeviceOverride = false,
            ) {
                StyleAwareCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            )
                        }
                        StyleAwareButton(
                            onClick = {},
                            primary = true,
                        ) {
                            Text("按钮")
                        }
                    }
                }
            }
        }
    }
}
