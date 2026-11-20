package io.github.we007oneday_byte.softseek.data.remote.sse

import io.github.we007oneday_byte.softseek.data.remote.dto.AnthropicErrorEnvelopeDto
import io.github.we007oneday_byte.softseek.data.remote.dto.AnthropicStreamEventDto
import io.github.we007oneday_byte.softseek.data.remote.dto.AnthropicUsageDto
import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.ChatFinishReason
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.TokenUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject

/**
 * Anthropic Messages SSE 解析器。
 *
 * 与 OpenAI 的 choices/delta 不同，Anthropic 使用
 * message_start / content_block_delta / message_delta / message_stop 事件；
 * usage 也分散在 message_start（输入）与 message_delta（输出）中，需要累积后一次发出。
 */
class AnthropicSseParser
    @Inject
    constructor(
        private val json: Json,
    ) {
        fun parse(source: BufferedSource): Flow<ChatStreamEvent> = flow {
            val state = StreamState()
            val data = StringBuilder()
            var eventName: String? = null
            var terminal = false
            try {
                while (!terminal && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.isBlank() -> {
                            if (data.isNotEmpty()) {
                                terminal = emitPayload(data.toString(), eventName, state)
                                data.clear()
                                eventName = null
                            }
                        }
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> {
                            eventName = line.removePrefix("event:").trim()
                        }
                        line.startsWith("data:") -> {
                            val payload = line.removePrefix("data:").removePrefix(" ")
                            if (payload.isNotBlank()) {
                                if (data.isNotEmpty()) data.append('\n')
                                data.append(payload)
                            }
                        }
                    }
                }
                if (!terminal && data.isNotEmpty()) {
                    emitPayload(data.toString(), eventName, state)
                }
            } catch (error: IOException) {
                emit(ChatStreamEvent.Failed(ChatError.Network(error.message.orEmpty())))
            }
        }

        private suspend fun FlowCollector<ChatStreamEvent>.emitPayload(
            payload: String,
            eventName: String?,
            state: StreamState,
        ): Boolean {
            for (event in decodePayload(payload, eventName, state)) {
                emit(event)
                if (event is ChatStreamEvent.Failed) return true
            }
            return false
        }

        private fun decodePayload(
            payload: String,
            eventName: String?,
            state: StreamState,
        ): List<ChatStreamEvent> {
            val apiError = runCatching {
                json.decodeFromString<AnthropicErrorEnvelopeDto>(payload)
            }.getOrNull()
            if (!apiError?.error?.message.isNullOrBlank()) {
                return listOf(
                    ChatStreamEvent.Failed(ChatError.BadRequest(apiError?.error?.message.orEmpty())),
                )
            }
            return runCatching {
                val event = json.decodeFromString<AnthropicStreamEventDto>(payload)
                val type = event.type.ifBlank { eventName.orEmpty() }
                when (type) {
                    "message_start" -> {
                        event.message?.usage?.let { state.applyInputUsage(it) }
                        emptyList()
                    }
                    "content_block_delta" -> {
                        val delta = event.delta ?: return@runCatching emptyList()
                        when (delta.type) {
                            "text", "text_delta" -> delta.text?.takeIf { it.isNotEmpty() }
                                ?.let { listOf(ChatStreamEvent.ContentDelta(it)) }
                            "thinking", "thinking_delta" ->
                                delta.thinking
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let { listOf(ChatStreamEvent.ReasoningDelta(it)) }
                            else -> emptyList()
                        }.orEmpty()
                    }
                    "message_delta" -> {
                        event.usage?.let { state.applyOutputUsage(it) }
                        val events = mutableListOf<ChatStreamEvent>()
                        if (!state.usageEmitted) {
                            events += ChatStreamEvent.UsageReceived(state.toTokenUsage())
                            state.usageEmitted = true
                        }
                        events += ChatStreamEvent.Finished(event.delta?.stopReason.toFinishReason())
                        events
                    }
                    "message_stop" -> listOf(ChatStreamEvent.Completed)
                    "error" -> listOf(
                        ChatStreamEvent.Failed(
                            ChatError.BadRequest(
                                event.error?.message?.ifBlank { "Anthropic 流式错误" }
                                    ?: "Anthropic 流式错误",
                            ),
                        ),
                    )
                    "ping", "content_block_start", "content_block_stop" -> emptyList()
                    else -> emptyList()
                }
            }.getOrElse { error ->
                listOf(ChatStreamEvent.Failed(ChatError.Protocol(error.message.orEmpty())))
            }
        }

        private fun String?.toFinishReason(): ChatFinishReason = when (this) {
            "end_turn", "stop_sequence" -> ChatFinishReason.STOP
            "max_tokens" -> ChatFinishReason.LENGTH
            "tool_use" -> ChatFinishReason.TOOL_CALLS
            "refusal" -> ChatFinishReason.CONTENT_FILTER
            else -> ChatFinishReason.UNKNOWN
        }

        private class StreamState {
            var inputTokens = 0
            var outputTokens = 0
            var cacheReadTokens = 0
            var cacheCreationTokens = 0
            var usageEmitted = false

            fun applyInputUsage(usage: AnthropicUsageDto) {
                inputTokens = usage.inputTokens
                cacheReadTokens = usage.cacheReadInputTokens
                cacheCreationTokens = usage.cacheCreationInputTokens
            }

            fun applyOutputUsage(usage: AnthropicUsageDto) {
                outputTokens = usage.outputTokens
            }

            fun toTokenUsage(): TokenUsage = TokenUsage(
                promptTokens = inputTokens,
                completionTokens = outputTokens,
                totalTokens = inputTokens + outputTokens,
                cacheHitTokens = cacheReadTokens,
                cacheMissTokens = cacheCreationTokens,
            )
        }
    }
