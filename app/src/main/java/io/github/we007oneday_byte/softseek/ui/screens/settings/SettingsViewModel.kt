package io.github.we007oneday_byte.softseek.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.we007oneday_byte.softseek.core.asr.VoskModelManager
import io.github.we007oneday_byte.softseek.core.asr.VoskModelState
import io.github.we007oneday_byte.softseek.core.image.ImageProcessor
import io.github.we007oneday_byte.softseek.core.network.BaseUrlValidator
import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.core.settings.ParameterPreset
import io.github.we007oneday_byte.softseek.core.tts.TtsEngine
import io.github.we007oneday_byte.softseek.core.tts.TtsEvent
import io.github.we007oneday_byte.softseek.data.model.ModelCatalog
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.auth.ApiKeyStore
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.CleanupResult
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import io.github.we007oneday_byte.softseek.domain.repository.ConversationRepository
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository
import io.github.we007oneday_byte.softseek.domain.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.Calendar
import javax.inject.Inject

data class BalanceUiState(
    val loading: Boolean = false,
    val balance: BalanceInfo? = null,
    val error: String? = null,
)

data class UsageUiState(
    val todayTokens: Long = 0,
    val monthTokens: Long = 0,
    val totalTokens: Long = 0,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val apiKeyStore: ApiKeyStore,
        private val provider: AiProvider,
        private val modelCatalog: ModelCatalog,
        private val conversationRepository: ConversationRepository,
        private val memoryPoolRepository: MemoryPoolRepository,
        private val usageRepository: UsageRepository,
        private val imageProcessor: ImageProcessor,
        private val voskModelManager: VoskModelManager,
        private val ttsEngine: TtsEngine,
        private val json: kotlinx.serialization.json.Json,
    ) : ViewModel() {
        val settings: StateFlow<AppSettings> = settingsRepository.settings
        val apiKeys = apiKeyStore.keySummaries
        val models: StateFlow<List<AiModelInfo>> = modelCatalog.models
        val activeKey: StateFlow<String?> = apiKeyStore.apiKey
        val asrModelState: StateFlow<VoskModelState> = voskModelManager.state
        val ttsReady: StateFlow<Boolean> = ttsEngine.ready

        private val ttsMessageState = MutableStateFlow<String?>(null)
        val ttsMessage: StateFlow<String?> = ttsMessageState.asStateFlow()

        private val balanceState = MutableStateFlow(BalanceUiState())
        val balance: StateFlow<BalanceUiState> = balanceState.asStateFlow()

        private val usageState = MutableStateFlow(UsageUiState())
        val usage: StateFlow<UsageUiState> = usageState.asStateFlow()

        private val cleanupState = MutableStateFlow<CleanupResult?>(null)
        val cleanupPreview: StateFlow<CleanupResult?> = cleanupState.asStateFlow()

        private val baseUrlErrorState = MutableStateFlow<String?>(null)
        val baseUrlError: StateFlow<String?> = baseUrlErrorState.asStateFlow()

        init {
            refreshUsage()
            voskModelManager.refresh()
            viewModelScope.launch {
                ttsEngine.events.collect { event ->
                    ttsMessageState.value = when (event) {
                        is TtsEvent.Started -> "正在播报…"
                        is TtsEvent.Done -> "播报完成"
                        is TtsEvent.Error -> event.message
                    }
                }
            }
        }

        fun update(transform: (AppSettings) -> AppSettings) {
            viewModelScope.launch { settingsRepository.update(transform) }
        }

        fun addApiKey(label: String, key: String, note: String?) {
            apiKeyStore.addKey(label, key, note)
            refreshBalance()
        }

        fun activateApiKey(id: String) {
            apiKeyStore.activateKey(id)
            refreshBalance()
        }

        fun deleteApiKey(id: String) {
            apiKeyStore.deleteKey(id)
            balanceState.value = BalanceUiState()
        }

        fun setBaseUrl(value: String): Boolean {
            return runCatching {
                val normalized = BaseUrlValidator.validate(value)
                baseUrlErrorState.value = null
                viewModelScope.launch {
                    settingsRepository.update { it.copy(baseUrl = normalized.trimEnd('/')) }
                }
                true
            }.getOrElse { error ->
                baseUrlErrorState.value = error.message ?: "Base URL 校验失败"
                false
            }
        }

        fun setProxyUrl(value: String) {
            update { it.copy(proxyUrl = value.trim()) }
        }

        fun setTimeouts(connect: Int, read: Int, write: Int) {
            update {
                it.copy(
                    connectTimeoutSeconds = connect.coerceIn(1, 300),
                    readTimeoutSeconds = read.coerceIn(1, 300),
                    writeTimeoutSeconds = write.coerceIn(1, 300),
                )
            }
        }

        fun refreshModels() {
            viewModelScope.launch {
                modelCatalog.refresh(apiKeyStore.apiKey.value)
            }
        }

        fun refreshBalance() {
            val key = apiKeyStore.apiKey.value
            if (key.isNullOrBlank()) {
                balanceState.value = BalanceUiState(error = "尚未配置 API Key")
                return
            }
            viewModelScope.launch {
                balanceState.value = BalanceUiState(loading = true)
                runCatching { provider.getBalance(key) }
                    .onSuccess { info ->
                        balanceState.value = BalanceUiState(balance = info)
                    }
                    .onFailure { error ->
                        balanceState.value = BalanceUiState(error = error.message ?: "余额查询失败")
                    }
            }
        }

        fun refreshUsage() {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val tomorrowStart = todayStart + DAY_MILLIS
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                usageState.value = UsageUiState(
                    todayTokens = usageRepository.tokensBetween(todayStart, tomorrowStart),
                    monthTokens = usageRepository.tokensBetween(monthStart, now + 1),
                    totalTokens = usageRepository.totalTokens(),
                )
            }
        }

        fun previewCleanup() {
            viewModelScope.launch {
                val conversation = conversationRepository.observeConversations().first().firstOrNull()
                if (conversation == null) {
                    cleanupState.value = CleanupResult(
                        poolId = "",
                        preview = true,
                        candidates = emptyList(),
                        deletedCount = 0,
                    )
                    return@launch
                }
                cleanupState.value = memoryPoolRepository.cleanupNow(conversation.id, preview = true)
            }
        }

        fun runCleanup() {
            viewModelScope.launch {
                val preview = cleanupState.value ?: return@launch
                if (preview.poolId.isBlank()) {
                    cleanupState.value = null
                    return@launch
                }
                memoryPoolRepository.cleanupNow(preview.poolId, preview = false)
                cleanupState.value = null
                refreshUsage()
            }
        }

        fun dismissCleanupPreview() {
            cleanupState.value = null
        }

        fun clearImageCache(): Int = imageProcessor.clearCache()

        fun installAsrModel() {
            voskModelManager.install()
        }

        fun cancelAsrModelInstall() {
            voskModelManager.cancelInstall()
        }

        fun testTts() {
            val result = ttsEngine.speak("你好，我是 SOFTSEEK 的语音助手。", "settings-test")
            ttsMessageState.value = result.exceptionOrNull()?.message ?: "正在播报…"
        }

        fun savePreset(name: String) {
            val clean = name.trim()
            if (clean.isBlank()) return
            val current = settings.value
            val preset = ParameterPreset(
                id = java.util.UUID.randomUUID().toString(),
                name = clean.take(24),
                model = current.defaultModel,
                effort = current.defaultReasoningEffort,
                maxOutputTokens = current.defaultMaxOutputTokens,
            )
            update { it.copy(presets = (it.presets + preset).takeLast(MAX_PRESETS)) }
        }

        fun applyPreset(id: String) {
            val preset = settings.value.presets.firstOrNull { it.id == id } ?: return
            update {
                it.copy(
                    defaultModel = preset.model,
                    defaultReasoningEffort = preset.effort,
                    defaultMaxOutputTokens = preset.maxOutputTokens,
                )
            }
        }

        fun deletePreset(id: String) {
            update { it.copy(presets = it.presets.filterNot { preset -> preset.id == id }) }
        }

        fun exportPresets(): String =
            runCatching { json.encodeToString(settings.value.presets) }.getOrDefault("[]")

        fun setAutoCleanup(enabled: Boolean) {
            update { it.copy(autoCleanup = enabled) }
            syncMemoryConfig { it.copy(enableAutoCleanup = enabled) }
        }

        fun setCleanupThreshold(count: Int) {
            val value = count.coerceIn(1, 50)
            update { it.copy(cleanupThresholdCount = value) }
            syncMemoryConfig { it.copy(cleanupThresholdCount = value) }
        }

        fun setProtectionHours(hours: Int) {
            val value = hours.coerceIn(0, 720)
            update { it.copy(cleanupProtectionHours = value) }
            syncMemoryConfig { it.copy(cleanupProtectionHours = value) }
        }

        fun setRetentionDays(days: Int) {
            val value = days.coerceIn(1, 365)
            update { it.copy(deletedRetentionDays = value) }
            syncMemoryConfig { it.copy(deletedRetentionDays = value) }
        }

        private fun syncMemoryConfig(
            transform: (MemoryPoolConfig) -> MemoryPoolConfig,
        ) {
            viewModelScope.launch {
                conversationRepository.observeConversations().first().forEach { conversation ->
                    val config = memoryPoolRepository.getConfig(conversation.id)
                    memoryPoolRepository.upsertConfig(transform(config))
                }
            }
        }

        private companion object {
            const val MAX_PRESETS = 12
            const val DAY_MILLIS = 24L * 60 * 60 * 1000
        }
    }
