package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.ui.components.common.SettingItem
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_api),
                    items = listOf(
                        stringResource(R.string.settings_api_key) to stringResource(R.string.not_configured),
                        stringResource(R.string.settings_base_url) to AppConstants.DEFAULT_BASE_URL,
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_model),
                    items = listOf(
                        "当前模型" to AppConstants.DEFAULT_MODEL,
                        "思考强度" to "high（默认）",
                        "最大输出" to "8K / 64K / 128K",
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_chat),
                    items = listOf(
                        "流式输出" to "开启",
                        "Markdown 渲染" to stringResource(R.string.settings_placeholder),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_call),
                    items = listOf(
                        "语音识别" to "Vosk small-cn（42MB）",
                        "语音合成" to "系统 TTS",
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_memory),
                    items = listOf(
                        "自动清理" to "默认关闭，按需开启",
                        "回收站" to "7 天",
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_appearance),
                    items = listOf(
                        "主题" to "跟随系统",
                        "动态取色" to "关闭",
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_data),
                    items = listOf(
                        "导出 / 导入" to stringResource(R.string.settings_placeholder),
                        "本地存储" to "仅本机，不上传",
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_about),
                    items = listOf(
                        "版本" to AppConstants.APP_VERSION,
                        "应用性质" to stringResource(R.string.app_subtitle),
                    ),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.disclaimer),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<Pair<String, String?>>,
) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        items.forEach { (itemTitle, subtitle) ->
            SettingItem(
                title = itemTitle,
                subtitle = subtitle,
                onClick = { },
            )
        }
        HorizontalDivider(color = MaterialTheme.softSeekColors.divider)
    }
}
