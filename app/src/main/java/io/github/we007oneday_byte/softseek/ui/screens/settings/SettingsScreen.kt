package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.core.asr.VoskModelState
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.core.logging.LogModule
import io.github.we007oneday_byte.softseek.core.settings.AppThemeMode
import io.github.we007oneday_byte.softseek.core.settings.CodeTheme
import io.github.we007oneday_byte.softseek.core.settings.ParameterPreset
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeySummary
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.ProviderType
import io.github.we007oneday_byte.softseek.ui.components.common.SettingItem
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareCard
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareDivider
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareSettingItem
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

private data class SettingsEntry(
    val title: String,
    val subtitle: String? = null,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    section: SettingsSection,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val keys by viewModel.apiKeys.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val asrModelState by viewModel.asrModelState.collectAsStateWithLifecycle()
    val ttsReady by viewModel.ttsReady.collectAsStateWithLifecycle()
    val ttsMessage by viewModel.ttsMessage.collectAsStateWithLifecycle()
    val cleanupPreview by viewModel.cleanupPreview.collectAsStateWithLifecycle()
    val baseUrlError by viewModel.baseUrlError.collectAsStateWithLifecycle()
    var showKeyManager by remember { mutableStateOf(false) }
    var showProvider by remember { mutableStateOf(false) }
    var showBaseUrl by remember { mutableStateOf(false) }
    val importAsrModelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importAsrModel(uri)
    }
    LaunchedEffect(Unit) {
        viewModel.refreshAsrModel()
    }
    var showProxy by remember { mutableStateOf(false) }
    var showConnectTimeout by remember { mutableStateOf(false) }
    var showReadTimeout by remember { mutableStateOf(false) }
    var showWriteTimeout by remember { mutableStateOf(false) }
    var showModel by remember { mutableStateOf(false) }
    var showMaxOutput by remember { mutableStateOf(false) }
    var showCleanupThreshold by remember { mutableStateOf(false) }
    var showProtection by remember { mutableStateOf(false) }
    var showRetention by remember { mutableStateOf(false) }
    var showUsageWarning by remember { mutableStateOf(false) }
    var showPreset by remember { mutableStateOf(false) }
    var showVisualStyle by remember { mutableStateOf(false) }
    var showVoiceAnimation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var cacheMessage by remember { mutableStateOf<String?>(null) }

    val activeKey = keys.firstOrNull { it.isActive }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(section.titleRes)) },
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
                if (section != SettingsSection.API) return@item
                SettingsGroup(
                    title = stringResource(R.string.settings_api),
                    items = buildList {
                        add(
                            SettingsEntry(
                                title = "接口协议",
                                subtitle = settings.providerType.displayName,
                                onClick = { showProvider = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = stringResource(R.string.settings_api_key),
                                subtitle = activeKey?.let { "${it.label} ${it.hint}" }
                                    ?: stringResource(R.string.settings_api_key_not_set),
                                onClick = { showKeyManager = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = stringResource(R.string.settings_base_url),
                                subtitle = settings.baseUrl,
                                onClick = { showBaseUrl = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = "代理地址",
                                subtitle = settings.proxyUrl.ifBlank { "不使用代理" },
                                onClick = { showProxy = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = "连接超时",
                                subtitle = "${settings.connectTimeoutSeconds}s",
                                onClick = { showConnectTimeout = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = "读取超时",
                                subtitle = "${settings.readTimeoutSeconds}s",
                                onClick = { showReadTimeout = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = "写入超时",
                                subtitle = "${settings.writeTimeoutSeconds}s",
                                onClick = { showWriteTimeout = true },
                            ),
                        )
                        if (viewModel.capabilities.supportsBalance) {
                            add(
                                SettingsEntry(
                                    title = stringResource(R.string.settings_balance),
                                    subtitle = when {
                                        balance.loading -> "查询中…"
                                        balance.error != null -> balance.error
                                        balance.balance != null -> balance.balance!!.balances.joinToString(" / ") {
                                            "${it.currency} ${it.totalBalance}"
                                        }
                                        else -> "点击查询余额"
                                    },
                                    onClick = viewModel::refreshBalance,
                                ),
                            )
                        }
                    },
                )
            }
            item {
                if (section != SettingsSection.MODEL) return@item
                SettingsGroup(
                    title = stringResource(R.string.settings_model),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_current_model),
                            subtitle = models.firstOrNull { it.id == settings.defaultModel }?.displayName
                                ?: settings.defaultModel,
                            onClick = { showModel = true },
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_thinking_effort),
                            subtitle = effortLabel(settings.defaultReasoningEffort),
                            onClick = { showModel = true },
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_max_output),
                            subtitle = settings.defaultMaxOutputTokens.toString(),
                            onClick = { showMaxOutput = true },
                        ),
                        SettingsEntry(
                            title = "刷新模型列表",
                            subtitle = "从 /models 拉取能力与档位",
                            onClick = viewModel::refreshModels,
                        ),
                        SettingsEntry(
                            title = "参数预设",
                            subtitle = "${settings.presets.size} 个 · 重启保留 · 可导出",
                            onClick = { showPreset = true },
                        ),
                    ),
                )
            }
            item {
                if (section != SettingsSection.CHAT) return@item
                SettingsGroup(title = stringResource(R.string.settings_chat)) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_streaming),
                        checked = settings.streamingEnabled,
                        onCheckedChange = { enabled -> viewModel.update { it.copy(streamingEnabled = enabled) } },
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_markdown),
                        checked = settings.markdownEnabled,
                        onCheckedChange = { enabled -> viewModel.update { it.copy(markdownEnabled = enabled) } },
                    )
                }
            }
            item {
                if (section != SettingsSection.CALL) return@item
                SettingsGroup(
                    title = stringResource(R.string.settings_call),
                    items = buildList {
                        add(
                            SettingsEntry(
                                title = stringResource(R.string.settings_asr_model),
                                subtitle = asrModelSubtitle(asrModelState),
                                onClick = if (asrModelState is VoskModelState.Installed) {
                                    null
                                } else {
                                    viewModel::installAsrModel
                                },
                            ),
                        )
                        if (asrModelState !is VoskModelState.Installed) {
                            add(
                                SettingsEntry(
                                    title = stringResource(R.string.settings_asr_model_import),
                                    subtitle = stringResource(R.string.settings_asr_model_import_value),
                                    onClick = {
                                        importAsrModelLauncher.launch(
                                            arrayOf(
                                                "application/zip",
                                                "application/x-zip-compressed",
                                                "application/octet-stream",
                                            ),
                                        )
                                    },
                                ),
                            )
                        }
                        if (
                            asrModelState is VoskModelState.Downloading ||
                            asrModelState is VoskModelState.Verifying ||
                            asrModelState is VoskModelState.Extracting
                        ) {
                            add(
                                SettingsEntry(
                                    title = stringResource(R.string.settings_asr_model_cancel),
                                    subtitle = stringResource(R.string.settings_asr_model_cancel_value),
                                    onClick = viewModel::cancelAsrModelInstall,
                                ),
                            )
                        }
                        add(
                            SettingsEntry(
                                title = "收音动画",
                                subtitle = settings.voiceAnimationStyle.label,
                                onClick = { showVoiceAnimation = true },
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = stringResource(R.string.settings_tts),
                                subtitle = stringResource(R.string.settings_tts_value),
                            ),
                        )
                        add(
                            SettingsEntry(
                                title = "试听系统 TTS",
                                subtitle = ttsMessage ?: if (ttsReady) {
                                    "点击播报"
                                } else {
                                    "系统 TTS 未就绪"
                                },
                                onClick = viewModel::testTts,
                            ),
                        )
                    },
                )
            }
            item {
                if (section != SettingsSection.MEMORY) return@item
                SettingsGroup(title = stringResource(R.string.settings_memory)) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_cleanup),
                        subtitle = stringResource(R.string.settings_auto_cleanup_value),
                        checked = settings.autoCleanup,
                        onCheckedChange = viewModel::setAutoCleanup,
                    )
                    SettingItem(
                        title = "保留最近消息数",
                        subtitle = settings.cleanupThresholdCount.toString(),
                        onClick = { showCleanupThreshold = true },
                    )
                    SettingItem(
                        title = "保护期（小时）",
                        subtitle = settings.cleanupProtectionHours.toString(),
                        onClick = { showProtection = true },
                    )
                    SettingItem(
                        title = stringResource(R.string.settings_recycle_bin),
                        subtitle = "${settings.deletedRetentionDays} 天",
                        onClick = { showRetention = true },
                    )
                    SettingItem(
                        title = "立即清理（先预览，再确认）",
                        subtitle = "默认不自动删除；手动清理可取消",
                        onClick = viewModel::previewCleanup,
                    )
                }
            }
            item {
                if (section != SettingsSection.APPEARANCE) return@item
                SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                    SettingItem(
                        title = stringResource(R.string.settings_theme),
                        subtitle = themeLabel(settings.themeMode),
                        onClick = {
                            val next = when (settings.themeMode) {
                                AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.DARK
                                AppThemeMode.DARK -> AppThemeMode.SYSTEM
                            }
                            viewModel.update { it.copy(themeMode = next) }
                        },
                    )
                    SettingItem(
                        title = "视觉风格",
                        subtitle = settings.visualStyle.label,
                        onClick = { showVisualStyle = true },
                    )
                    SliderSetting(
                        title = "字号 ${"%.2f".format(settings.fontScale)}x",
                        value = settings.fontScale,
                        range = 0.85f..1.3f,
                        steps = 8,
                        onValueChange = { value -> viewModel.update { it.copy(fontScale = value) } },
                    )
                    SliderSetting(
                        title = "气泡圆角 ${settings.bubbleRadiusDp}dp",
                        value = settings.bubbleRadiusDp.toFloat(),
                        range = 4f..24f,
                        steps = 9,
                        onValueChange = { value -> viewModel.update { it.copy(bubbleRadiusDp = value.toInt()) } },
                    )
                    SettingItem(
                        title = "代码块主题",
                        subtitle = codeThemeLabel(settings.codeTheme),
                        onClick = {
                            val next = when (settings.codeTheme) {
                                CodeTheme.FOLLOW_THEME -> CodeTheme.LIGHT
                                CodeTheme.LIGHT -> CodeTheme.DARK
                                CodeTheme.DARK -> CodeTheme.FOLLOW_THEME
                            }
                            viewModel.update { it.copy(codeTheme = next) }
                        },
                    )
                }
            }
            item {
                if (section != SettingsSection.DATA) return@item
                SettingsGroup(
                    title = stringResource(R.string.settings_data),
                    items = listOf(
                        SettingsEntry(
                            title = stringResource(R.string.settings_export_import),
                            subtitle = stringResource(R.string.settings_placeholder),
                        ),
                        SettingsEntry(
                            title = "今日 / 本月 / 累计用量",
                            subtitle = "${usage.todayTokens} / ${usage.monthTokens} / ${usage.totalTokens} tokens",
                            onClick = viewModel::refreshUsage,
                        ),
                        SettingsEntry(
                            title = "用量预警阈值",
                            subtitle = if (settings.usageWarningTokens <= 0) {
                                "未设置"
                            } else {
                                "本月 ${usage.monthTokens} / ${settings.usageWarningTokens} tokens"
                            },
                            onClick = { showUsageWarning = true },
                        ),
                        SettingsEntry(
                            title = "清理图片缓存",
                            subtitle = cacheMessage ?: "长边 1568、质量 85、上限 100MB",
                            onClick = {
                                val count = viewModel.clearImageCache()
                                cacheMessage = "已清理 $count 个缓存文件"
                            },
                        ),
                        SettingsEntry(
                            title = stringResource(R.string.settings_local_storage),
                            subtitle = stringResource(R.string.settings_local_storage_value),
                        ),
                    ),
                )
            }
            item {
                if (section != SettingsSection.ABOUT) return@item
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
                if (section != SettingsSection.ABOUT) return@item
                Text(
                    text = stringResource(R.string.disclaimer),
                    modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        }
    }

    if (showKeyManager) {
        ApiKeyManagerDialog(
            keys = keys,
            onAdd = viewModel::addApiKey,
            onUpdate = viewModel::updateApiKey,
            onActivate = viewModel::activateApiKey,
            onDelete = viewModel::deleteApiKey,
            onDismiss = { showKeyManager = false },
        )
    }
    if (showProvider) {
        ProviderTypeDialog(
            current = settings.providerType,
            onSelect = { type ->
                viewModel.setProviderType(type)
                showProvider = false
            },
            onDismiss = { showProvider = false },
        )
    }
    if (showBaseUrl) {
        BaseUrlDialog(
            initial = settings.baseUrl,
            error = baseUrlError,
            onSave = { value ->
                val ok = viewModel.setBaseUrl(value)
                if (ok) showBaseUrl = false
            },
            onDismiss = { showBaseUrl = false },
        )
    }
    if (showProxy) {
        TextSettingDialog(
            title = "代理地址",
            initial = settings.proxyUrl,
            hint = "http://127.0.0.1:7890，留空表示不使用",
            onSave = { value ->
                viewModel.setProxyUrl(value)
                showProxy = false
            },
            onDismiss = { showProxy = false },
        )
    }
    if (showConnectTimeout) {
        NumberDialog(
            title = "连接超时（秒）",
            initial = settings.connectTimeoutSeconds,
            range = 1..300,
            onConfirm = { value ->
                viewModel.setTimeouts(value, settings.readTimeoutSeconds, settings.writeTimeoutSeconds)
                showConnectTimeout = false
            },
            onDismiss = { showConnectTimeout = false },
        )
    }
    if (showReadTimeout) {
        NumberDialog(
            title = "读取超时（秒）",
            initial = settings.readTimeoutSeconds,
            range = 1..300,
            onConfirm = { value ->
                viewModel.setTimeouts(settings.connectTimeoutSeconds, value, settings.writeTimeoutSeconds)
                showReadTimeout = false
            },
            onDismiss = { showReadTimeout = false },
        )
    }
    if (showWriteTimeout) {
        NumberDialog(
            title = "写入超时（秒）",
            initial = settings.writeTimeoutSeconds,
            range = 1..300,
            onConfirm = { value ->
                viewModel.setTimeouts(settings.connectTimeoutSeconds, settings.readTimeoutSeconds, value)
                showWriteTimeout = false
            },
            onDismiss = { showWriteTimeout = false },
        )
    }
    if (showModel) {
        ModelSettingsDialog(
            models = models,
            currentModel = settings.defaultModel,
            currentEffort = settings.defaultReasoningEffort,
            currentMaxTokens = settings.defaultMaxOutputTokens,
            onRefresh = viewModel::refreshModels,
            onApply = { model, effort ->
                viewModel.ensureModel(model)
                viewModel.update {
                    it.copy(
                        defaultModel = model.id,
                        defaultReasoningEffort = effort,
                    )
                }
                showModel = false
            },
            onDismiss = { showModel = false },
        )
    }
    if (showMaxOutput) {
        NumberDialog(
            title = stringResource(R.string.settings_max_output),
            initial = settings.defaultMaxOutputTokens,
            range = 256..393_216,
            onConfirm = { value ->
                viewModel.update { it.copy(defaultMaxOutputTokens = value) }
                showMaxOutput = false
            },
            onDismiss = { showMaxOutput = false },
        )
    }
    if (showCleanupThreshold) {
        NumberDialog(
            title = "保留最近消息数",
            initial = settings.cleanupThresholdCount,
            range = 1..50,
            onConfirm = { value ->
                viewModel.setCleanupThreshold(value)
                showCleanupThreshold = false
            },
            onDismiss = { showCleanupThreshold = false },
        )
    }
    if (showProtection) {
        NumberDialog(
            title = "保护期（小时）",
            initial = settings.cleanupProtectionHours,
            range = 0..720,
            onConfirm = { value ->
                viewModel.setProtectionHours(value)
                showProtection = false
            },
            onDismiss = { showProtection = false },
        )
    }
    if (showRetention) {
        NumberDialog(
            title = stringResource(R.string.settings_recycle_bin),
            initial = settings.deletedRetentionDays,
            range = 1..365,
            onConfirm = { value ->
                viewModel.setRetentionDays(value)
                showRetention = false
            },
            onDismiss = { showRetention = false },
        )
    }
    if (showUsageWarning) {
        NumberDialog(
            title = "用量预警阈值（tokens，0 表示关闭）",
            initial = settings.usageWarningTokens.toInt(),
            range = 0..9_999_999,
            onConfirm = { value ->
                viewModel.update { it.copy(usageWarningTokens = value.toLong()) }
                showUsageWarning = false
            },
            onDismiss = { showUsageWarning = false },
        )
    }
    if (showPreset) {
        PresetDialog(
            presets = settings.presets,
            onSave = viewModel::savePreset,
            onApply = viewModel::applyPreset,
            onDelete = viewModel::deletePreset,
            onExport = {
                val payload = viewModel.exportPresets()
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("SOFTSEEK 参数预设", payload))
                android.widget.Toast.makeText(context, "预设 JSON 已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showPreset = false },
        )
    }
    if (showVisualStyle) {
        VisualStyleDialog(
            current = settings.visualStyle,
            onSelect = { style ->
                AppLog.info(
                    module = LogModule.THEME,
                    message = "切换视觉风格: ${style.label}",
                    metric = "theme_switch",
                    value = 1,
                    unit = "次",
                )
                viewModel.update { it.copy(visualStyle = style) }
                showVisualStyle = false
            },
            onDismiss = { showVisualStyle = false },
        )
    }
    if (showVoiceAnimation) {
        VoiceAnimationStyleDialog(
            current = settings.voiceAnimationStyle,
            onSelect = { style ->
                viewModel.update { it.copy(voiceAnimationStyle = style) }
                showVoiceAnimation = false
            },
            onDismiss = { showVoiceAnimation = false },
        )
    }
    cleanupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCleanupPreview,
            title = { Text("清理预览") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp)) {
                    if (preview.candidates.isEmpty()) {
                        Text("没有可清理的消息（置顶会话、保护期内消息会跳过）")
                    } else {
                        Text("将软删除 ${preview.candidates.size} 条旧消息。")
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(preview.candidates) { candidate ->
                                Text(
                                    text = "${candidate.role.name} · ${candidate.preview.ifBlank { "[无正文]" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (preview.candidates.isNotEmpty()) {
                    TextButton(onClick = viewModel::runCleanup) {
                        Text("清理")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCleanupPreview) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsEntry> = emptyList(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val uiSettings = LocalSoftSeekUiSettings.current
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "settings-chevron",
    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        horizontal = uiSettings.sectionHeaderHorizontalPaddingDp.dp,
                        vertical = uiSettings.sectionHeaderVerticalPaddingDp.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (items.isNotEmpty()) {
                        Text(
                            text = "${items.size} 项设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.softSeekColors.textSecondary,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier
                        .size(uiSettings.iconSizeDp.dp)
                        .rotate(chevronRotation),
                    tint = MaterialTheme.softSeekColors.textSecondary,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    StyleAwareDivider(
                        color = MaterialTheme.softSeekColors.divider.copy(alpha = 0.6f),
                    )
                    items.forEach { entry ->
                        SettingItem(
                            title = entry.title,
                            subtitle = entry.subtitle,
                            onClick = entry.onClick,
                        )
                    }
                    content?.invoke(this)
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    StyleAwareSettingItem(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
    StyleAwareDivider(color = MaterialTheme.softSeekColors.divider)
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.content_padding))) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.softSeekColors.textPrimary,
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun ProviderTypeDialog(
    current: ProviderType,
    onSelect: (ProviderType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("接口协议") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "协议决定请求路径、鉴权头与流式事件格式。切换后会自动使用该协议的默认 Base URL（自定义地址会保留）和内置模型。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
                ProviderType.entries.forEach { type ->
                    FilterChip(
                        selected = current == type,
                        onClick = { onSelect(type) },
                        label = { Text(type.displayName) },
                    )
                    Text(
                        text = "默认地址：${type.defaultBaseUrl}\n默认模型：${type.defaultModelId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun ApiKeyManagerDialog(
    keys: List<ApiKeySummary>,
    onAdd: (String, String, String?) -> Unit,
    onUpdate: (String, String, String?, String?) -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var label by remember { mutableStateOf("API Key") }
    var key by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    fun resetForm() {
        editingId = null
        label = "API Key"
        key = ""
        note = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_api_key)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (keys.isEmpty()) {
                    Text(stringResource(R.string.settings_api_key_not_set))
                } else {
                    Text(
                        text = "点击已有 Key 可修改名称、备注或替换密钥；点击“切换”启用另一把 Key。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Column {
                        keys.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingId = item.id
                                        label = item.label
                                        note = item.note.orEmpty()
                                        key = ""
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "${item.label} ${if (item.isActive) "· 当前" else ""}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = "${item.hint}${item.note?.let { " · $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.softSeekColors.textSecondary,
                                    )
                                }
                                if (!item.isActive) {
                                    TextButton(onClick = { onActivate(item.id) }) {
                                        Text("切换")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onDelete(item.id)
                                        if (editingId == item.id) resetForm()
                                    },
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "删除")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (editingId != null) {
                    Text(
                        text = "正在编辑已保存的 Key；Key 留空时只修改名称和备注。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("备注名") },
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (editingId == null) "API Key" else "替换 API Key（可留空）") },
                    placeholder = { Text(stringResource(R.string.settings_api_key_dialog_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("备注（可选）") },
                )
                Text(
                    text = "Key 使用 Android Keystore AES/GCM 加密保存，明文不落盘。",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editingId != null) {
                        onUpdate(
                            editingId!!,
                            label,
                            key.ifBlank { null },
                            note.ifBlank { null },
                        )
                        resetForm()
                    } else if (key.isNotBlank()) {
                        onAdd(label, key, note.ifBlank { null })
                        key = ""
                        note = ""
                    }
                },
                enabled = editingId != null || key.isNotBlank(),
            ) {
                Text(if (editingId == null) "添加 Key" else "保存修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun TextSettingDialog(
    title: String,
    initial: String,
    hint: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(hint) },
                )
                Text(
                    text = "保存后对新请求生效。",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.settings_api_key_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun BaseUrlDialog(
    initial: String,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_base_url)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_base_url)) },
                )
                error?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "填写 API 根路径，例如 https://api.openai.com/v1 或 https://api.deepseek.com。" +
                        "应用会自动拼接 /chat/completions 与 /models；仅允许 HTTPS。",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.settings_api_key_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun ModelSettingsDialog(
    models: List<AiModelInfo>,
    currentModel: String,
    currentEffort: ReasoningEffort,
    currentMaxTokens: Int,
    onRefresh: () -> Unit,
    onApply: (AiModelInfo, ReasoningEffort) -> Unit,
    onDismiss: () -> Unit,
) {
    val available = models
    var selected by remember(currentModel, available) {
        mutableStateOf(available.firstOrNull { it.id == currentModel })
    }
    var effort by remember(currentEffort) { mutableStateOf(currentEffort) }
    var manualModelId by remember(currentModel) { mutableStateOf("") }
    LaunchedEffect(Unit) { onRefresh() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_model_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (available.isEmpty()) {
                    Text(
                        text = "模型列表为空。可在下方手动填写 OpenAI 兼容模型 ID。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                } else {
                    available.forEach { model ->
                        FilterChip(
                            selected = model.id == selected?.id,
                            onClick = { selected = model },
                            label = {
                                Text(
                                    "${model.displayName} · ${if (model.inputModalities.contains("image")) {
                                        "图片+文本"
                                    } else {
                                        "纯文本"
                                    }}",
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_thinking_effort),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReasoningEffort.entries.forEach { level ->
                        val supported = selected == null ||
                            selected?.reasoningLevels?.isEmpty() == true ||
                            level == ReasoningEffort.NONE ||
                            selected?.reasoningLevels?.contains(level) == true
                        if (supported) {
                            FilterChip(
                                selected = effort == level,
                                onClick = { effort = level },
                                label = { Text(effortLabel(level)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualModelId,
                    onValueChange = { manualModelId = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("手动模型 ID（可选）") },
                    placeholder = { Text("例如 gpt-4o、deepseek-chat") },
                    supportingText = {
                        Text("填写后优先使用此 ID；不会影响 /models 的结果")
                    },
                )
                Text(
                    text = "当前 max_tokens：$currentMaxTokens",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
                TextButton(onClick = onRefresh) { Text("刷新模型列表") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val manual = manualModelId.trim()
                    val model = if (manual.isNotBlank()) {
                        AiModelInfo(
                            id = manual,
                            displayName = manual,
                            contextWindow = 0,
                            maxOutputTokens = currentMaxTokens,
                            inputModalities = listOf("text", "image"),
                            reasoningLevels = emptyList(),
                            defaultReasoningEffort = null,
                        )
                    } else {
                        selected
                    }
                    if (model != null) onApply(model, effort)
                },
            ) {
                Text(stringResource(R.string.chat_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun PresetDialog(
    presets: List<ParameterPreset>,
    onSave: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("参数预设") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp)) {
                if (presets.isEmpty()) {
                    Text("暂无预设。保存后可在不同会话间复用。")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(presets, key = { it.id }) { preset ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp),
                                ) {
                                    Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${preset.model} · ${effortLabel(
                                            preset.effort,
                                        )} · ${preset.maxOutputTokens}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.softSeekColors.textSecondary,
                                    )
                                }
                                TextButton(onClick = { onApply(preset.id) }) { Text("应用") }
                                IconButton(onClick = { onDelete(preset.id) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "删除预设")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("预设名称") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onSave(name)
                            name = ""
                        },
                        enabled = name.isNotBlank(),
                    ) {
                        Text("保存当前参数")
                    }
                    TextButton(onClick = onExport) { Text("导出 JSON") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_confirm)) }
        },
    )
}

@Composable
private fun NumberDialog(
    title: String,
    initial: Int,
    range: IntRange,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial.toString()) }
    val parsed = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                Text(
                    text = "范围 ${range.first} - ${range.last}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it.coerceIn(range.first, range.last)) } },
                enabled = parsed != null,
            ) {
                Text(stringResource(R.string.chat_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

private fun themeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.SYSTEM -> "跟随系统"
    AppThemeMode.LIGHT -> "浅色"
    AppThemeMode.DARK -> "深色"
}

@Composable
private fun asrModelSubtitle(state: VoskModelState): String = when (state) {
    VoskModelState.NotInstalled -> stringResource(R.string.settings_asr_model_not_installed)
    is VoskModelState.Downloading -> {
        val percent = if (state.totalBytes > 0) {
            ((state.downloadedBytes * 100) / state.totalBytes).toInt()
        } else {
            0
        }
        stringResource(R.string.settings_asr_model_downloading, percent)
    }
    VoskModelState.Verifying -> stringResource(R.string.settings_asr_model_verifying)
    VoskModelState.Extracting -> stringResource(R.string.settings_asr_model_extracting)
    VoskModelState.ArchiveReady -> stringResource(R.string.settings_asr_model_archive_ready)
    is VoskModelState.Installed -> stringResource(R.string.settings_asr_model_installed)
    is VoskModelState.Failed -> stringResource(R.string.settings_asr_model_failed, state.message)
}

private fun codeThemeLabel(theme: CodeTheme): String = when (theme) {
    CodeTheme.FOLLOW_THEME -> "跟随主题"
    CodeTheme.LIGHT -> "浅色代码"
    CodeTheme.DARK -> "深色代码"
}

private fun effortLabel(value: ReasoningEffort): String = when (value) {
    ReasoningEffort.NONE -> "关闭思考"
    ReasoningEffort.LOW -> "低"
    ReasoningEffort.HIGH -> "高"
    ReasoningEffort.MAX -> "最大"
}
