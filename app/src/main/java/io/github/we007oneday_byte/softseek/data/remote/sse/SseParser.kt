package io.github.we007oneday_byte.softseek.data.remote.sse

import io.github.we007oneday_byte.softseek.data.remote.dto.ApiErrorEnvelopeDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionChunkDto
import io.github.we007oneday_byte.softseek.data.remote.dto.UsageDto
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatFinishReason
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.TokenUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject

class SseParser
    @Inject
    constructor(
        private val json: Json,
    ) {
        fun parse(source: BufferedSource): Flow<ChatStreamEvent> = flow {
            val data = StringBuilder()
            var terminal = false
            try {
                while (!terminal && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.isBlank() -> {
                            if (data.isNotEmpty()) {
                                terminal = emitPayload(data.toString())
                                data.clear()
                            }
                        }
                        line.startsWith(":") -> Unit
                        line.startsWith("data:") -> {
                            val payload = line.removePrefix("data:").trimStart()
                            if (payload == DONE) {
                                if (data.isNotEmpty()) {
                                    terminal = emitPayload(data.toString())
                                    data.clear()
                                }
                                if (!terminal) {
                                    emit(ChatStreamEvent.Completed)
                                }
                                terminal = true
                            } else if (payload.isNotBlank()) {
                                if (data.isNotEmpty()) data.append('\n')
                                data.append(payload)
                            }
                        }
                        line.startsWith("event:") || line.startsWith("id:") || line.startsWith("retry:") -> Unit
                    }
                }
                if (!terminal && data.isNotEmpty()) {
                    emitPayload(data.toString())
                }
            } catch (error: IOException) {
                emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
            }
        }

        private suspend fun kotlinx.coroutines.flow.FlowCollector<ChatStreamEvent>.emitPayload(
            payload: String,
        ): Boolean {
            val events = decodePayload(payload)
            for (event in events) {
                emit(event)
                if (event is ChatStreamEvent.Failed) return true
            }
            return false
        }

        private fun decodePayload(payload: String): List<ChatStreamEvent> {
            val apiErrorMessage = runCatching {
                json.decodeFromString<ApiErrorEnvelopeDto>(payload).error?.message
            }.getOrNull()
            if (!apiErrorMessage.isNullOrBlank()) {
                return listOf(ChatStreamEvent.Failed(ChatError.BadRequest(apiErrorMessage)))
            }
            return runCatching {
                val chunk = json.decodeFromString<ChatCompletionChunkDto>(payload)
                buildList {
                    for (choice in chunk.choices) {
                        choice.delta.reasoningContent?.takeIf { it.isNotEmpty() }?.let {
                            add(ChatStreamEvent.ReasoningDelta(it))
                        }
                        choice.delta.content?.takeIf { it.isNotEmpty() }?.let {
                            add(ChatStreamEvent.ContentDelta(it))
                        }
                        choice.finishReason?.let {
                            add(ChatStreamEvent.Finished(it.toFinishReason()))
                        }
                    }
                    chunk.usage?.let { add(ChatStreamEvent.UsageReceived(it.toTokenUsage())) }
                }
            }.getOrElse { error ->
                listOf(ChatStreamEvent.Failed(ChatError.Protocol(error.message.orEmpty())))
            }
        }

        private fun UsageDto.toTokenUsage(): TokenUsage = TokenUsage(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            reasoningTokens = completionTokensDetails?.reasoningTokens ?: 0,
            cacheHitTokens = promptCacheHitTokens,
            cacheMissTokens = promptCacheMissTokens,
        )

        private fun String.toFinishReason(): ChatFinishReason = when (this) {
            "stop" -> ChatFinishReason.STOP
            "length" -> ChatFinishReason.LENGTH
            "content_filter" -> ChatFinishReason.CONTENT_FILTER
            "tool_calls" -> ChatFinishReason.TOOL_CALLS
            "insufficient_system_resource" -> ChatFinishReason.INSUFFICIENT_SYSTEM_RESOURCE
            "aborted" -> ChatFinishReason.ABORTED
            else -> ChatFinishReason.UNKNOWN
        }

        private companion object {
            const val DONE = "[DONE]"
        }
    }
