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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.ui.components.common.SettingItem
import io.github.we007oneday_byte.softseek.ui.theme.ThemeManager
import io.github.we007oneday_byte.softseek.ui.theme.ThemeMode
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

private data class SettingsEntry(
    val title: String,
    val subtitle: String? = null,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
) {
    val themeLabel = stringResource(
        when (ThemeManager.mode) {
            ThemeMode.SYSTEM -> R.string.settings_theme_system
            ThemeMode.LIGHT -> R.string.settings_theme_light
            ThemeMode.DARK -> R.string.settings_theme_dark
        },
    )

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
                        SettingsEntry(
                            title = stringResource(R.string.settings_api_key),
                            subtitle = stringResource(R.string.settings_api_key_not_set),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_base_url),
                            subtitle = AppConstants.DEFAULT_BASE_URL,
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_guide),
                            onClick = onOpenGuide,
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_model),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_current_model),
                            subtitle = AppConstants.DEFAULT_MODEL,
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_thinking_effort),
                            subtitle = stringResource(R.string.settings_thinking_effort_value),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_max_output),
                            subtitle = stringResource(R.string.settings_max_output_value),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_chat),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_streaming),
                            subtitle = stringResource(R.string.settings_streaming_on),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_markdown),
                            subtitle = stringResource(R.string.settings_placeholder),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_call),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_asr),
                            subtitle = stringResource(R.string.settings_asr_value),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_tts),
                            subtitle = stringResource(R.string.settings_tts_value),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_memory),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_auto_cleanup),
                            subtitle = stringResource(R.string.settings_auto_cleanup_value),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_recycle_bin),
                            subtitle = stringResource(R.string.settings_recycle_bin_value),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_appearance),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_theme),
                            subtitle = themeLabel,
                            onClick = { ThemeManager.cycle() },
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_dynamic_color),
                            subtitle = stringResource(R.string.settings_dynamic_color_off),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_data),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_export_import),
                            subtitle = stringResource(R.string.settings_placeholder),
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_local_storage),
                            subtitle = stringResource(R.string.settings_local_storage_value),
                        ),
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_about),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_version),
                            subtitle = AppConstants.APP_VERSION,
                            onClick = onOpenAbout,
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_app_nature),
                            subtitle = stringResource(R.string.app_subtitle),
                        ),
                    ),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.disclaimer),
                    modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
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
    items: List<SettingsEntry>,
) {
    Column {
        Text(
            text = title,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.content_padding),
                top = 20.dp,
                end = dimensionResource(R.dimen.content_padding),
                bottom = dimensionResource(R.dimen.item_spacing),
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        items.forEach { entry ->
            SettingItem(
                title = entry.title,
                subtitle = entry.subtitle,
                onClick = entry.onClick,
            )
        }
        HorizontalDivider(color = MaterialTheme.softSeekColors.divider)
    }
}
