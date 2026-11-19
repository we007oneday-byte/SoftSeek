package io.github.we007oneday_byte.softseek.data.remote.provider

import io.github.we007oneday_byte.softseek.core.network.DynamicApiFactory
import io.github.we007oneday_byte.softseek.data.remote.dto.ApiErrorEnvelopeDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionRequestDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatMessageDto
import io.github.we007oneday_byte.softseek.data.remote.dto.StreamOptionsDto
import io.github.we007oneday_byte.softseek.data.remote.sse.SseParser
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiMessage
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekProvider
    @Inject
    constructor(
        private val apiFactory: DynamicApiFactory,
        private val sseParser: SseParser,
        private val json: Json,
        private val settingsRepository: SettingsRepository,
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
                apiFactory.streamApi().streamChat(
                    endpoint("chat/completions"),
                    "Bearer $apiKey",
                    request.toDto(),
                )
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
            val response = apiFactory.api().listModels(endpoint("models"), "Bearer $apiKey")
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
            val response = apiFactory.api().balance(endpoint("user/balance"), "Bearer $apiKey")
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
            messages = messages.map { message -> message.toDto() },
            reasoningEffort = reasoningEffort.name.lowercase(),
            maxTokens = maxOutputTokens,
            stream = stream,
            streamOptions = if (stream) StreamOptionsDto(includeUsage = true) else null,
        )

        private fun AiMessage.toDto(): ChatMessageDto {
            val text = content.ifBlank { if (images.isNotEmpty()) "请查看图片" else "" }
            val payload = if (images.isEmpty()) {
                JsonPrimitive(text)
            } else {
                buildJsonArray {
                    addJsonObject {
                        put("type", "text")
                        put("text", text)
                    }
                    images.take(MAX_IMAGES_PER_MESSAGE).forEach { image ->
                        addJsonObject {
                            put("type", "image_url")
                            put(
                                "image_url",
                                buildJsonObject {
                                    put("url", "data:${image.mime};base64,${image.base64}")
                                    put("detail", image.detail.name.lowercase())
                                },
                            )
                        }
                    }
                }
            }
            return ChatMessageDto(role = role.name.lowercase(), content = payload)
        }

        private fun endpoint(path: String): String {
            val base = settingsRepository.settings.value.baseUrl.trim().trimEnd('/')
            return "$base/$path"
        }

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
            const val MAX_IMAGES_PER_MESSAGE = 8
        }
    }
