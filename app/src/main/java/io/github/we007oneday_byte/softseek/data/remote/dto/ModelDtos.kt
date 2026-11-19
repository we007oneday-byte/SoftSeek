package io.github.we007oneday_byte.softseek.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponseDto(
    val data: List<ModelDto> = emptyList(),
)

@Serializable
data class ModelDto(
    val id: String,
    val name: String? = null,
    @SerialName("context_window") val contextWindow: Int = 0,
    @SerialName("max_output_tokens") val maxOutputTokens: Int = 0,
    @SerialName("input_modalities") val inputModalities: List<String> = emptyList(),
    @SerialName("reasoning_effort") val reasoningEffort: EffortDto? = null,
    val effort: EffortDto? = null,
)

@Serializable
data class EffortDto(
    @SerialName("supported_levels") val supportedLevels: List<String> = emptyList(),
    @SerialName("default") val defaultLevel: String? = null,
)
