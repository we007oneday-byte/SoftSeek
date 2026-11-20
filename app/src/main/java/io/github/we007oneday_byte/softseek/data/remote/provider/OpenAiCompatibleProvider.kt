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
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import io.github.we007oneday_byte.softseek.domain.provider.AiProviderException
import io.github.we007oneday_byte.softseek.domain.provider.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通用 OpenAI Chat Completions 兼容 Provider。
 *
 * 不绑定任何单一服务商：只要求服务实现
 * `POST {baseUrl}/chat/completions`、`GET {baseUrl}/models`，并使用
 * `Authorization: Bearer <API Key>`。
 *
 * 对非标准扩展字段（`reasoning_effort`、`stream_options`、`max_tokens`）
 * 做一次降级重试：若服务端返回 400/422 并明确指出这些字段不受支持，
 * 自动改用仅含 `model/messages/stream` 的严格 OpenAI 请求，避免第三方
 * 兼容服务直接失败。
 */
@Singleton
class OpenAiCompatibleProvider
    @Inject
    constructor(
        private val apiFactory: DynamicApiFactory,
        private val sseParser: SseParser,
        private val json: Json,
        private val settingsRepository: SettingsRepository,
    ) : AiProvider {
        override val id: String = PROVIDER_ID

        override val displayName: String = "OpenAI 兼容服务"

        override val currentProviderType: ProviderType = ProviderType.OPENAI_COMPATIBLE

        override val capabilities: ProviderCapabilities = ProviderCapabilities(
            supportsStreaming = true,
            supportsReasoning = true,
            supportsVision = true,
            supportsBalance = false,
            supportsModelList = true,
        )

        override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> = flow {
            if (apiKey.isBlank()) {
                emit(ChatStreamEvent.Failed(ChatError.NoApiKey))
                return@flow
            }
            val first = try {
                sendStream(apiKey, request, compatibilityOnly = false)
            } catch (error: IOException) {
                emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
                return@flow
            }
            if (!first.isSuccessful) {
                val errorBody = first.errorBody()?.string()
                if (shouldRetryStrict(first.code(), errorBody)) {
                    val retry = try {
                        sendStream(apiKey, request, compatibilityOnly = true)
                    } catch (error: IOException) {
                        emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
                        return@flow
                    }
                    if (!retry.isSuccessful) {
                        emit(
                            ChatStreamEvent.Failed(
                                mapHttpError(retry.code(), retry.errorBody()?.string()),
                            ),
                        )
                        return@flow
                    }
                    emitBody(retry)
                    return@flow
                }
                emit(ChatStreamEvent.Failed(mapHttpError(first.code(), errorBody)))
                return@flow
            }
            emitBody(first)
        }.flowOn(Dispatchers.IO)

        private suspend fun sendStream(
            apiKey: String,
            request: AiChatRequest,
            compatibilityOnly: Boolean,
        ): Response<ResponseBody> = apiFactory.streamApi().streamChat(
            endpoint("chat/completions"),
            "Bearer $apiKey",
            request.toDto(compatibilityOnly),
        )

        private suspend fun FlowCollector<ChatStreamEvent>.emitBody(
            response: Response<ResponseBody>,
        ) {
            val body = response.body()
            if (body == null) {
                emit(ChatStreamEvent.Failed(ChatError.Protocol("响应体为空")))
                return
            }
            try {
                emitAll(sseParser.parse(body.source()))
            } finally {
                body.close()
            }
        }

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
                val modalities = model.inputModalities.ifEmpty { DEFAULT_MODALITIES }
                AiModelInfo(
                    id = model.id,
                    displayName = model.name ?: model.id,
                    contextWindow = model.contextWindow,
                    maxOutputTokens = model.maxOutputTokens,
                    inputModalities = modalities,
                    reasoningLevels = effort?.supportedLevels.orEmpty().mapNotNull { it.toReasoningEffort() },
                    defaultReasoningEffort = effort?.defaultLevel?.toReasoningEffort(),
                )
            }
        }

        /**
         * 通用 OpenAI 兼容服务没有统一的余额接口，余额能力在 UI 中隐藏。
         * 保留该方法是为了满足 [AiProvider] 的通用契约。
         */
        override suspend fun getBalance(apiKey: String): BalanceInfo? = null

        private fun AiChatRequest.toDto(compatibilityOnly: Boolean): ChatCompletionRequestDto =
            ChatCompletionRequestDto(
                model = model,
                messages = messages.map { message -> message.toDto() },
                reasoningEffort = if (compatibilityOnly || reasoningEffort == ReasoningEffort.NONE) {
                    null
                } else {
                    reasoningEffort.name.lowercase()
                },
                maxTokens = if (compatibilityOnly) null else maxOutputTokens.takeIf { it > 0 },
                stream = stream,
                streamOptions = if (!compatibilityOnly && stream) {
                    StreamOptionsDto(includeUsage = true)
                } else {
                    null
                },
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
            var base = settingsRepository.settings.value.baseUrl.trim().trimEnd('/')
            // 容忍用户把完整端点误填进 Base URL。
            base = base.removeSuffix("/chat/completions")
            base = base.removeSuffix("/models").trimEnd('/')
            return "$base/$path"
        }

        private fun shouldRetryStrict(statusCode: Int, rawBody: String?): Boolean {
            if (statusCode != 400 && statusCode != 422) return false
            val body = rawBody.orEmpty().lowercase()
            if (body.isBlank()) return false
            val mentionsOptionalParam = listOf(
                "reasoning_effort",
                "stream_options",
                "max_tokens",
                "max_completion_tokens",
            ).any(body::contains)
            return mentionsOptionalParam
        }

        private fun mapHttpError(statusCode: Int, rawBody: String?): ChatError {
            val providerMessage = rawBody?.let { body ->
                runCatching { json.decodeFromString<ApiErrorEnvelopeDto>(body).error?.message }.getOrNull()
            }.orEmpty()
            return when (statusCode) {
                401, 403 -> ChatError.InvalidApiKey
                402 -> ChatError.InsufficientBalance
                404 -> ChatError.BadRequest(
                    providerMessage.ifBlank { "模型或接口不存在，请检查 Base URL 与模型 ID" },
                )
                429 -> ChatError.RateLimited
                in 500..599 -> ChatError.Server(statusCode, providerMessage)
                else -> ChatError.BadRequest(providerMessage.ifBlank { "HTTP $statusCode" })
            }
        }

        private fun String.toReasoningEffort(): ReasoningEffort? = when (lowercase()) {
            "none", "off", "disabled" -> ReasoningEffort.NONE
            "low", "minimal" -> ReasoningEffort.LOW
            "medium", "high" -> ReasoningEffort.HIGH
            "max", "maximum", "xhigh" -> ReasoningEffort.MAX
            else -> null
        }

        private companion object {
            const val PROVIDER_ID = "openai-compatible"
            const val MAX_IMAGES_PER_MESSAGE = 8
            val DEFAULT_MODALITIES = listOf("text", "image")
        }
    }
