package io.github.we007oneday_byte.softseek.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorEnvelopeDto(
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val message: String? = null,
    val type: String? = null,
    val code: JsonElement? = null,
    val param: String? = null,
)
