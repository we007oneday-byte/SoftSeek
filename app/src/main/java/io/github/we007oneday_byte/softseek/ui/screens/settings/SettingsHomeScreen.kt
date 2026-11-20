package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareCard
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareDivider
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareSettingItem
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

/**
 * 设置一级页：保留分类入口，具体开关在二级页。
 * 这一层不读取具体设置状态，避免把整份 SettingsScreen 的状态提前加载。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
) {
    val uiSettings = LocalSoftSeekUiSettings.current
    Scaffold(
        containerColor = Color.Transparent,
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
                StyleAwareCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = uiSettings.screenHorizontalPaddingDp.dp,
                            vertical = uiSettings.sectionVerticalPaddingDp.dp,
                        ),
                    contentPadding = false,
                ) {
                    Column {
                        SettingsSection.entries.forEachIndexed { index, section ->
                            StyleAwareSettingItem(
                                title = stringResource(section.titleRes),
                                subtitle = stringResource(section.subtitleRes),
                                onClick = { onOpenSection(section) },
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.softSeekColors.textSecondary,
                                    )
                                },
                            )
                            if (index != SettingsSection.entries.lastIndex) {
                                StyleAwareDivider(
                                    color = MaterialTheme.softSeekColors.divider.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(uiSettings.spacingDp.dp))
                Text(
                    text = stringResource(R.string.disclaimer),
                    modifier = Modifier.padding(horizontal = uiSettings.screenHorizontalPaddingDp.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        }
    }
}
