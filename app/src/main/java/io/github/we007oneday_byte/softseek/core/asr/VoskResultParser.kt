package io.github.we007oneday_byte.softseek.core.asr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Vosk 的 result/partialResult 都是 JSON；只取 text/partial，不让 JSON 细节泄漏到状态机。 */
object VoskResultParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun text(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val value = obj["text"]?.jsonPrimitive?.contentOrNull
                ?: obj["partial"]?.jsonPrimitive?.contentOrNull
            value?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
