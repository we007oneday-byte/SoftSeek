package io.github.we007oneday_byte.softseek.domain.provider

import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.data.remote.provider.AnthropicProvider
import io.github.we007oneday_byte.softseek.data.remote.provider.OpenAiCompatibleProvider
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按当前设置的接口协议路由到具体 Provider。
 * 协议切换后无需重建 ViewModel，下一次调用会自动使用新协议。
 */
@Singleton
class RoutingAiProvider
    @Inject
    constructor(
        private val openAiCompatible: OpenAiCompatibleProvider,
        private val anthropic: AnthropicProvider,
        private val settingsRepository: SettingsRepository,
    ) : AiProvider {
        override val id: String
            get() = active().id

        override val displayName: String
            get() = active().displayName

        override val capabilities: ProviderCapabilities
            get() = active().capabilities

        override val currentProviderType: ProviderType
            get() = settingsRepository.settings.value.providerType

        override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> =
            active().streamChat(apiKey, request)

        override suspend fun listModels(apiKey: String): List<AiModelInfo> =
            active().listModels(apiKey)

        override suspend fun getBalance(apiKey: String): BalanceInfo? =
            active().getBalance(apiKey)

        override fun defaultModels(): List<AiModelInfo> = active().defaultModels()

        override fun providerTypeChanges(): Flow<ProviderType> =
            settingsRepository.settings.map { it.providerType }.distinctUntilChanged()

        private fun active(
            settings: AppSettings = settingsRepository.settings.value,
        ): AiProvider = when (settings.providerType) {
            ProviderType.OPENAI_COMPATIBLE -> openAiCompatible
            ProviderType.ANTHROPIC -> anthropic
        }
    }
