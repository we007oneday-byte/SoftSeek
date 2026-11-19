package io.github.we007oneday_byte.softseek.data.remote.provider

import io.github.we007oneday_byte.softseek.data.remote.DeepSeekApi
import io.github.we007oneday_byte.softseek.data.remote.DeepSeekStreamApi
import io.github.we007oneday_byte.softseek.data.remote.dto.ApiErrorEnvelopeDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionRequestDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatMessageDto
import io.github.we007oneday_byte.softseek.data.remote.dto.StreamOptionsDto
import io.github.we007oneday_byte.softseek.data.remote.sse.SseParser
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceEntry
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import io.github.we007oneday_byte.softseek.domain.provider.AiProviderException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekProvider
    @Inject
    constructor(
        private val api: DeepSeekApi,
        private val streamApi: DeepSeekStreamApi,
        private val sseParser: SseParser,
        private val json: Json,
    ) : AiProvider {
        override val id: String = PROVIDER_ID

        override val displayName: String = "DeepSeek"

        override val capabilities: ProviderCapabilities = ProviderCapabilities(
            supportsStreaming = true,
            supportsReasoning = true,
            supportsVision = true,
            supportsBalance = true,
            supportsModelList = true,
        )

        override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> = flow {
            if (apiKey.isBlank()) {
                emit(ChatStreamEvent.Failed(ChatError.NoApiKey))
                return@flow
            }
            val response = try {
                streamApi.streamChat("Bearer $apiKey", request.toDto())
            } catch (error: IOException) {
                emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
                return@flow
            }
            if (!response.isSuccessful) {
                emit(ChatStreamEvent.Failed(mapHttpError(response.code(), response.errorBody()?.string())))
                return@flow
            }
            val body = response.body()
            if (body == null) {
                emit(ChatStreamEvent.Failed(ChatError.Protocol("响应体为空")))
                return@flow
            }
            try {
                emitAll(sseParser.parse(body.source()))
            } finally {
                body.close()
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun listModels(apiKey: String): List<AiModelInfo> {
            if (apiKey.isBlank()) throw AiProviderException(ChatError.NoApiKey)
            val response = api.listModels("Bearer $apiKey")
            if (!response.isSuccessful) {
                throw AiProviderException(
                    mapHttpError(response.code(), response.errorBody()?.string()),
                )
            }
            return response.body()?.data.orEmpty().map { model ->
                val effort = model.reasoningEffort ?: model.effort
                AiModelInfo(
                    id = model.id,
                    displayName = model.name ?: model.id,
                    contextWindow = model.contextWindow,
                    maxOutputTokens = model.maxOutputTokens,
                    inputModalities = model.inputModalities,
                    reasoningLevels = effort?.supportedLevels.orEmpty().mapNotNull { it.toReasoningEffort() },
                    defaultReasoningEffort = effort?.defaultLevel?.toReasoningEffort(),
                )
            }
        }

        override suspend fun getBalance(apiKey: String): BalanceInfo? {
            if (apiKey.isBlank()) throw AiProviderException(ChatError.NoApiKey)
            val response = api.balance("Bearer $apiKey")
            if (!response.isSuccessful) {
                throw AiProviderException(
                    mapHttpError(response.code(), response.errorBody()?.string()),
                )
            }
            val body = response.body() ?: return null
            return BalanceInfo(
                isAvailable = body.isAvailable,
                balances = body.balanceInfos.map {
                    BalanceEntry(
                        currency = it.currency,
                        totalBalance = it.totalBalance,
                        grantedBalance = it.grantedBalance,
                        toppedUpBalance = it.toppedUpBalance,
                    )
                },
            )
        }

        private fun AiChatRequest.toDto(): ChatCompletionRequestDto = ChatCompletionRequestDto(
            model = model,
            messages = messages.map { ChatMessageDto(role = it.role.name.lowercase(), content = it.content) },
            reasoningEffort = reasoningEffort.name.lowercase(),
            maxTokens = maxOutputTokens,
            stream = stream,
            streamOptions = if (stream) StreamOptionsDto(includeUsage = true) else null,
        )

        private fun mapHttpError(statusCode: Int, rawBody: String?): ChatError {
            val providerMessage = rawBody?.let { body ->
                runCatching { json.decodeFromString<ApiErrorEnvelopeDto>(body).error?.message }.getOrNull()
            }.orEmpty()
            return when (statusCode) {
                401, 403 -> ChatError.InvalidApiKey
                402 -> ChatError.InsufficientBalance
                429 -> ChatError.RateLimited
                in 500..599 -> ChatError.Server(statusCode, providerMessage)
                else -> ChatError.BadRequest(providerMessage.ifBlank { "HTTP $statusCode" })
            }
        }

        private fun String.toReasoningEffort(): ReasoningEffort? = when (lowercase()) {
            "none" -> ReasoningEffort.NONE
            "low" -> ReasoningEffort.LOW
            "high" -> ReasoningEffort.HIGH
            "max" -> ReasoningEffort.MAX
            else -> null
        }

        private companion object {
            const val PROVIDER_ID = "deepseek"
        }
    }
