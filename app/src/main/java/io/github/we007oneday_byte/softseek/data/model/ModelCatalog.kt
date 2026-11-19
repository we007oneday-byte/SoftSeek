package io.github.we007oneday_byte.softseek.data.model

import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelCatalog
    @Inject
    constructor(
        private val provider: AiProvider,
    ) {
        private val state = MutableStateFlow(defaultModels())
        val models: StateFlow<List<AiModelInfo>> = state.asStateFlow()

        suspend fun refresh(apiKey: String?): Result<List<AiModelInfo>> {
            if (apiKey.isNullOrBlank()) return Result.success(state.value)
            return runCatching { provider.listModels(apiKey) }
                .onSuccess { remote ->
                    if (remote.isNotEmpty()) state.value = remote
                }
        }

        fun model(id: String): AiModelInfo? = state.value.firstOrNull { it.id == id }

        fun supportsVision(id: String): Boolean =
            model(id)?.inputModalities?.contains("image") == true

        private fun defaultModels(): List<AiModelInfo> = listOf(
            AiModelInfo(
                id = "deepseek-flash",
                displayName = "DeepSeek V4.1 Flash",
                contextWindow = 1_048_576,
                maxOutputTokens = 393_216,
                inputModalities = listOf("text", "image"),
                reasoningLevels = listOf(ReasoningEffort.LOW, ReasoningEffort.HIGH, ReasoningEffort.MAX),
                defaultReasoningEffort = ReasoningEffort.HIGH,
            ),
            AiModelInfo(
                id = "deepseek-v4-pro",
                displayName = "DeepSeek V4 Pro",
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
