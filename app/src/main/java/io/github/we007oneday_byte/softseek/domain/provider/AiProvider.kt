package io.github.we007oneday_byte.softseek.domain.provider

import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import kotlinx.coroutines.flow.Flow

interface AiChatService {
    fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent>
}

interface AiProvider : AiChatService {
    val id: String

    val displayName: String

    val capabilities: ProviderCapabilities

    suspend fun listModels(apiKey: String): List<AiModelInfo>

    suspend fun getBalance(apiKey: String): BalanceInfo?
}
