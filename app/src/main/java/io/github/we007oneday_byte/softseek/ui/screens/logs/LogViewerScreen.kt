package io.github.we007oneday_byte.softseek.ui.screens.logs

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.core.logging.LogEntry
import io.github.we007oneday_byte.softseek.core.logging.LogExportMode
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.core.logging.LogTimeRange
import io.github.we007oneday_byte.softseek.core.logging.LogTypeFilter
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareCard
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBack: () -> Unit,
    viewModel: LogViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uiSettings = LocalSoftSeekUiSettings.current
    var showExportDialog by remember { mutableStateOf(false) }
    val visibleEntries = remember(
        state.entries,
        state.showInfo,
        state.moduleCode,
        state.type,
        state.range,
    ) {
        state.visibleEntries()
    }
    val modules = remember(state.entries) {
        state.entries
            .asSequence()
            .map { it.module }
            .distinct()
            .sorted()
            .map { LogModule.fromCode(it) }
            .toList()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_logs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::refresh,
                        enabled = !state.loading,
                    ) {
                        Text("刷新")
                    }
                    TextButton(
                        onClick = { showExportDialog = true },
                        enabled = state.entries.isNotEmpty() && !state.exporting,
                    ) {
                        Text("导出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.loading || state.exporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = uiSettings.screenHorizontalPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示 INFO 常规日志",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "默认只展示 ERROR、崩溃和 ANR；开启后显示全部运行日志。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                }
                Switch(
                    checked = state.showInfo,
                    onCheckedChange = viewModel::setShowInfo,
                )
            }

            Text(
                text = "按模块筛选",
                modifier = Modifier.padding(
                    start = uiSettings.screenHorizontalPaddingDp.dp,
                    top = 12.dp,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = uiSettings.screenHorizontalPaddingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.moduleCode == null,
                        onClick = { viewModel.setModule(null) },
                        label = { Text("全部模块") },
                    )
                }
                items(modules, key = { it.code }) { module ->
                    FilterChip(
                        selected = state.moduleCode == module.code,
                        onClick = { viewModel.setModule(module.code) },
                        label = { Text("${module.code} ${module.label}") },
                    )
                }
            }

            Text(
                text = "按日志类型 / 时间范围筛选",
                modifier = Modifier.padding(
                    start = uiSettings.screenHorizontalPaddingDp.dp,
                    top = 8.dp,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = uiSettings.screenHorizontalPaddingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LogTypeFilter.entries, key = { "type-${it.name}" }) { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.label) },
                    )
                }
                item { Spacer(Modifier.width(4.dp)) }
                items(LogTimeRange.entries, key = { "range-${it.name}" }) { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = uiSettings.screenHorizontalPaddingDp.dp,
                    vertical = 6.dp,
                ),
                color = MaterialTheme.softSeekColors.divider.copy(alpha = 0.6f),
            )
            Text(
                text = "共 ${state.entries.size} 条落盘日志 · 当前显示 ${visibleEntries.size} 条",
                modifier = Modifier.padding(horizontal = uiSettings.screenHorizontalPaddingDp.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.softSeekColors.textSecondary,
            )

            if (visibleEntries.isEmpty() && !state.loading) {
                Text(
                    text = if (state.showInfo) {
                        "当前筛选条件下没有日志。"
                    } else {
                        "暂无 ERROR 日志。默认隐藏 INFO；可打开上方开关查看常规运行日志。"
                    },
                    modifier = Modifier.padding(
                        horizontal = uiSettings.screenHorizontalPaddingDp.dp,
                        vertical = 24.dp,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = uiSettings.screenHorizontalPaddingDp.dp,
                        end = uiSettings.screenHorizontalPaddingDp.dp,
                        top = 6.dp,
                        bottom = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = visibleEntries,
                        key = { index, entry -> "${entry.ts}-${entry.crashId}-$index" },
                    ) { _, entry ->
                        LogEntryCard(entry = entry)
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出日志") },
            text = {
                Text(
                    "文件保存在应用私有目录 log_exports。选择“仅错误”只导出 ERROR、崩溃和 ANR；" +
                        "选择“全量”导出 INFO 与指标日志。",
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.export(LogExportMode.ERROR_ONLY)
                        },
                    ) {
                        Text("仅错误")
                    }
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.export(LogExportMode.ALL)
                        },
                    ) {
                        Text("全量")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    state.exportedFile?.let { file ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExportedFile,
            title = { Text("导出完成") },
            text = {
                Column {
                    Text(file.name, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        file.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "文件位于应用私有目录，不会自动上传网络。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { shareLog(context, file) }) {
                    Text("分享 / 打开")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportedFile) {
                    Text("关闭")
                }
            },
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("日志操作失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    val uiSettings = LocalSoftSeekUiSettings.current
    val accent = when {
        entry.type == "CRASH" -> MaterialTheme.colorScheme.error
        entry.type == "ANR" -> MaterialTheme.colorScheme.tertiary
        entry.level == "ERROR" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.softSeekColors.textSecondary
    }
    StyleAwareCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = false,
    ) {
        Column(
            modifier = Modifier.padding(uiSettings.cardPaddingDp.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.type,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${entry.module} · ${entry.moduleLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = entry.time.replace('T', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (entry.metric.isNotBlank()) {
                Text(
                    text = "指标 ${entry.metric} = ${entry.value}${entry.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = entry.msg.ifBlank { "(无消息)" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (entry.throwable.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.throwable,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = MaterialTheme.softSeekColors.textSecondary,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.crashId.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "crash_id=${entry.crashId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

private fun shareLog(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "导出 SOFTSEEK 日志").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        Toast.makeText(context, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
    }
}
