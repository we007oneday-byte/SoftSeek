package io.github.we007oneday_byte.softseek.domain.provider

import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface AiChatService {
    fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent>
}

interface AiProvider : AiChatService {
    val id: String

    val displayName: String

    val capabilities: ProviderCapabilities

    suspend fun listModels(apiKey: String): List<AiModelInfo>

    suspend fun getBalance(apiKey: String): BalanceInfo?

    /** 当前协议在未拉取远程模型前可用的内置模型；没有则返回空。 */
    fun defaultModels(): List<AiModelInfo> = emptyList()

    /** 当前协议；用于区分“首次读取设置”与真正的协议切换。 */
    val currentProviderType: ProviderType?
        get() = null

    /** 协议切换通知；ModelCatalog 用它重置内置模型。单一协议实现无需覆盖。 */
    fun providerTypeChanges(): Flow<ProviderType> = emptyFlow()
}
