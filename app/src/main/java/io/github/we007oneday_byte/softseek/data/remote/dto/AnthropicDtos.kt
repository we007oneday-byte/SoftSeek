package io.github.we007oneday_byte.softseek.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicStreamEventDto(
    val type: String = "",
    val message: AnthropicMessageDto? = null,
    val delta: AnthropicDeltaDto? = null,
    val usage: AnthropicUsageDto? = null,
    val error: AnthropicErrorDto? = null,
)

@Serializable
data class AnthropicMessageDto(
    val id: String = "",
    val model: String = "",
    val usage: AnthropicUsageDto? = null,
)

@Serializable
data class AnthropicDeltaDto(
    val type: String = "",
    val text: String? = null,
    val thinking: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
)

@Serializable
data class AnthropicUsageDto(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
    @SerialName("cache_read_input_tokens") val cacheReadInputTokens: Int = 0,
    @SerialName("cache_creation_input_tokens") val cacheCreationInputTokens: Int = 0,
)

@Serializable
data class AnthropicErrorEnvelopeDto(
    val type: String = "",
    val error: AnthropicErrorDto? = null,
)

@Serializable
data class AnthropicErrorDto(
    val type: String = "",
    val message: String = "",
)
