package io.github.we007oneday_byte.softseek.data.model

import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelCatalog
    @Inject
    constructor(
        private val provider: AiProvider,
    ) {
        private val state = MutableStateFlow(initialModels())
        private val remoteLoaded = MutableStateFlow(false)
        private val observeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var catalogProviderType = provider.currentProviderType
        val models: StateFlow<List<AiModelInfo>> = state.asStateFlow()
        val hasRemoteModels: StateFlow<Boolean> = remoteLoaded.asStateFlow()

        init {
            observeScope.launch {
                provider.providerTypeChanges().collect { type ->
                    // 首次设置读取可能只是补上持久化值；只有协议真正切换才丢弃远程目录。
                    if (catalogProviderType != null && catalogProviderType != type) {
                        catalogProviderType = type
                        resetForProvider()
                    } else {
                        catalogProviderType = type
                    }
                }
            }
        }

        /** 切换接口协议后重置为该协议的内置模型，并丢弃上一协议的远程目录。 */
        fun resetForProvider() {
            state.value = initialModels()
            remoteLoaded.value = false
            catalogProviderType = provider.currentProviderType
        }

        suspend fun refresh(apiKey: String?): Result<List<AiModelInfo>> {
            if (apiKey.isNullOrBlank()) return Result.success(state.value)
            return runCatching { provider.listModels(apiKey) }
                .onSuccess { remote ->
                    if (remote.isNotEmpty()) {
                        state.value = remote
                        remoteLoaded.value = true
                    }
                }
        }

        fun contains(id: String): Boolean = state.value.any { it.id == id }

        fun model(id: String): AiModelInfo? = state.value.firstOrNull { it.id == id }

        fun ensureModel(id: String, maxOutputTokens: Int = 0): AiModelInfo = ensureModel(
            AiModelInfo(
                id = id,
                displayName = id,
                contextWindow = 0,
                maxOutputTokens = maxOutputTokens,
                inputModalities = listOf("text", "image"),
                reasoningLevels = emptyList(),
                defaultReasoningEffort = null,
            ),
        )

        fun ensureModel(model: AiModelInfo): AiModelInfo {
            val existing = state.value.firstOrNull { it.id == model.id }
            if (existing == null) {
                state.value = state.value + model
                return model
            }
            val merged = existing.copy(
                displayName = model.displayName.ifBlank { existing.displayName },
                contextWindow = model.contextWindow.takeIf { it > 0 } ?: existing.contextWindow,
                maxOutputTokens = model.maxOutputTokens.takeIf { it > 0 } ?: existing.maxOutputTokens,
                inputModalities = model.inputModalities.ifEmpty { existing.inputModalities },
                reasoningLevels = model.reasoningLevels.ifEmpty { existing.reasoningLevels },
                defaultReasoningEffort = model.defaultReasoningEffort ?: existing.defaultReasoningEffort,
            )
            state.value = state.value.map { if (it.id == model.id) merged else it }
            return merged
        }

        fun supportsVision(id: String): Boolean =
            model(id)?.inputModalities?.contains("image") == true

        private fun initialModels(): List<AiModelInfo> =
            provider.defaultModels().ifEmpty { defaultModels() }

        private fun defaultModels(): List<AiModelInfo> = listOf(
            AiModelInfo(
                id = "deepseek-chat",
                displayName = "DeepSeek Chat",
                contextWindow = 1_048_576,
                maxOutputTokens = 393_216,
                inputModalities = listOf("text", "image"),
                reasoningLevels = listOf(ReasoningEffort.LOW, ReasoningEffort.HIGH, ReasoningEffort.MAX),
                defaultReasoningEffort = ReasoningEffort.HIGH,
            ),
            AiModelInfo(
                id = "deepseek-reasoner",
                displayName = "DeepSeek Reasoner",
                contextWindow = 1_048_576,
                maxOutputTokens = 393_216,
                inputModalities = listOf("text"),
                reasoningLevels = listOf(ReasoningEffort.LOW, ReasoningEffort.HIGH, ReasoningEffort.MAX),
                defaultReasoningEffort = ReasoningEffort.HIGH,
            ),
        ).ifEmpty {
            listOf(
                AiModelInfo(
                    id = AppConstants.DEFAULT_MODEL,
                    displayName = AppConstants.DEFAULT_MODEL,
                    contextWindow = 1_048_576,
                    maxOutputTokens = 393_216,
                    inputModalities = listOf("text", "image"),
                    reasoningLevels = listOf(ReasoningEffort.HIGH),
                    defaultReasoningEffort = ReasoningEffort.HIGH,
                ),
            )
        }
    }
