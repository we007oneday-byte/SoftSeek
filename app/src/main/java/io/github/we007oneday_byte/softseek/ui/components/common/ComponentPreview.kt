package io.github.we007oneday_byte.softseek.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekTheme

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SettingItemPreview() {
    SoftSeekTheme {
        Column(Modifier.padding(16.dp)) {
            SettingItem(
                title = stringResource(R.string.settings_api),
                subtitle = stringResource(R.string.not_configured),
                icon = Icons.Outlined.Settings,
                onClick = { },
            )
            SettingSwitchItem(
                title = stringResource(R.string.settings_streaming),
                subtitle = stringResource(R.string.settings_streaming_on),
                checked = true,
                onCheckedChange = { },
            )
            SettingValueItem(
                title = stringResource(R.string.settings_theme),
                value = stringResource(R.string.settings_theme_system),
                onClick = { },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CommonComponentsPreview() {
    SoftSeekTheme {
        Column(Modifier.padding(16.dp)) {
            CommonButton(text = stringResource(R.string.guide_start), onClick = { })
            MarkdownText(
                text = stringResource(R.string.app_name) + "\n" + stringResource(R.string.app_subtitle),
            )
        }
    }
}
