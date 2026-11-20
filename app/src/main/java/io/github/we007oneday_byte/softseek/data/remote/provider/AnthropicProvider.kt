package io.github.we007oneday_byte.softseek.data.remote.provider

import io.github.we007oneday_byte.softseek.core.network.DynamicApiFactory
import io.github.we007oneday_byte.softseek.data.remote.dto.AnthropicErrorEnvelopeDto
import io.github.we007oneday_byte.softseek.data.remote.sse.AnthropicSseParser
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiMessage
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anthropic Messages API（/v1/messages）。
 *
 * 鉴权使用 x-api-key + anthropic-version，请求体使用 system/messages 结构；
 * 流式事件由 [AnthropicSseParser] 转换为统一 ChatStreamEvent。
 * Anthropic 不提供 /models，因此模型列表由内置预设 + 用户手动模型 ID 组成。
 */
@Singleton
class AnthropicProvider
    @Inject
    constructor(
        private val apiFactory: DynamicApiFactory,
        private val parser: AnthropicSseParser,
        private val json: Json,
        private val settingsRepository: SettingsRepository,
    ) : AiProvider {
        override val id: String = PROVIDER_ID

        override val displayName: String = "Anthropic Messages"

        override val currentProviderType: ProviderType = ProviderType.ANTHROPIC

        override val capabilities: ProviderCapabilities = ProviderCapabilities(
            supportsStreaming = true,
            supportsReasoning = true,
            supportsVision = true,
            supportsBalance = false,
            supportsModelList = false,
        )

        override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> = flow {
            if (apiKey.isBlank()) {
                emit(ChatStreamEvent.Failed(ChatError.NoApiKey))
                return@flow
            }
            val first = try {
                send(apiKey, request, includeThinking = true)
            } catch (error: IOException) {
                emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
                return@flow
            }
            if (!first.isSuccessful) {
                val errorBody = first.errorBody()?.string()
                if (shouldRetryWithoutThinking(first.code(), errorBody)) {
                    val retry = try {
                        send(apiKey, request, includeThinking = false)
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

        private suspend fun send(
            apiKey: String,
            request: AiChatRequest,
            includeThinking: Boolean,
        ): Response<ResponseBody> = apiFactory.anthropicApi().messages(
            endpoint(),
            apiKey,
            ANTHROPIC_VERSION,
            request.toRequestBody(includeThinking),
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
                emitAll(parser.parse(body.source()))
            } finally {
                body.close()
            }
        }

        override suspend fun listModels(apiKey: String): List<AiModelInfo> {
            if (apiKey.isBlank()) throw AiProviderException(ChatError.NoApiKey)
            throw AiProviderException(
                ChatError.BadRequest("Anthropic Messages 不提供模型列表，请手动填写模型 ID"),
            )
        }

        override suspend fun getBalance(apiKey: String): BalanceInfo? = null

        override fun defaultModels(): List<AiModelInfo> = listOf(
            AiModelInfo(
                id = "claude-sonnet-4-5",
                displayName = "Claude Sonnet 4.5",
                contextWindow = 0,
                maxOutputTokens = 8192,
                inputModalities = listOf("text", "image"),
                reasoningLevels = emptyList(),
                defaultReasoningEffort = null,
            ),
            AiModelInfo(
                id = "claude-haiku-4-5",
                displayName = "Claude Haiku 4.5",
                contextWindow = 0,
                maxOutputTokens = 8192,
                inputModalities = listOf("text", "image"),
                reasoningLevels = emptyList(),
                defaultReasoningEffort = null,
            ),
        )

        private fun AiChatRequest.toRequestBody(includeThinking: Boolean): RequestBody {
            val system = messages
                .filter { it.role == MessageRole.SYSTEM }
                .joinToString("\n\n") { it.content }
                .trim()
            val anthropicMessages = messages
                .filter { it.role != MessageRole.SYSTEM }
                .map { it.toAnthropicMessage() }
            val maxTokens = maxOutputTokens.takeIf { it > 0 } ?: DEFAULT_MAX_TOKENS
            val payload = buildJsonObject {
                put("model", model)
                put("max_tokens", maxTokens)
                if (system.isNotBlank()) put("system", system)
                put("messages", buildJsonArray { anthropicMessages.forEach { add(it) } })
                put("stream", true)
                if (includeThinking) {
                    thinkingBudget(maxTokens, reasoningEffort)?.let { budget ->
                        put(
                            "thinking",
                            buildJsonObject {
                                put("type", "enabled")
                                put("budget_tokens", budget)
                            },
                        )
                    }
                }
            }
            return json.encodeToString(JsonObject.serializer(), payload)
                .toRequestBody(JSON_MEDIA_TYPE)
        }

        private fun AiMessage.toAnthropicMessage(): JsonObject = buildJsonObject {
            put(
                "role",
                if (role == MessageRole.ASSISTANT) "assistant" else "user",
            )
            if (images.isEmpty()) {
                put("content", content.ifBlank { "…" })
            } else {
                put(
                    "content",
                    buildJsonArray {
                        addJsonObject {
                            put("type", "text")
                            put("text", content.ifBlank { "请查看图片" })
                        }
                        images.take(MAX_IMAGES_PER_MESSAGE).forEach { image ->
                            addJsonObject {
                                put("type", "image")
                                put(
                                    "source",
                                    buildJsonObject {
                                        put("type", "base64")
                                        put("media_type", image.mime.ifBlank { "image/jpeg" })
                                        put("data", image.base64)
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }

        private fun thinkingBudget(maxTokens: Int, effort: ReasoningEffort): Int? {
            val desired = when (effort) {
                ReasoningEffort.NONE -> return null
                ReasoningEffort.LOW -> 1024
                ReasoningEffort.HIGH -> 4096
                ReasoningEffort.MAX -> 8192
            }
            return minOf(desired, maxTokens - 1).takeIf { it >= MIN_THINKING_BUDGET }
        }

        private fun endpoint(): String {
            var base = settingsRepository.settings.value.baseUrl.trim().trimEnd('/')
            // 容忍用户把完整端点或 /v1 误填进 Base URL。
            base = base.removeSuffix("/v1/messages")
            base = base.removeSuffix("/messages")
            base = base.removeSuffix("/v1").trimEnd('/')
            return "$base/v1/messages"
        }

        private fun shouldRetryWithoutThinking(statusCode: Int, rawBody: String?): Boolean {
            if (statusCode != 400 && statusCode != 422) return false
            val body = rawBody.orEmpty().lowercase()
            return body.contains("thinking") || body.contains("budget_tokens")
        }

        private fun mapHttpError(statusCode: Int, rawBody: String?): ChatError {
            val providerMessage = rawBody?.let { body ->
                runCatching {
                    json.decodeFromString<AnthropicErrorEnvelopeDto>(body).error?.message
                }.getOrNull()
            }.orEmpty()
            return when (statusCode) {
                401, 403 -> ChatError.InvalidApiKey
                429 -> ChatError.RateLimited
                404 -> ChatError.BadRequest(
                    providerMessage.ifBlank { "接口或模型不存在，请检查 Base URL 与模型 ID" },
                )
                in 500..599 -> ChatError.Server(statusCode, providerMessage)
                else -> ChatError.BadRequest(providerMessage.ifBlank { "HTTP $statusCode" })
            }
        }

        private companion object {
            const val PROVIDER_ID = "anthropic"
            const val ANTHROPIC_VERSION = "2023-06-01"
            const val DEFAULT_MAX_TOKENS = 8192
            const val MIN_THINKING_BUDGET = 1024
            const val MAX_IMAGES_PER_MESSAGE = 5
            val JSON_MEDIA_TYPE = "application/json".toMediaType()
        }
    }
