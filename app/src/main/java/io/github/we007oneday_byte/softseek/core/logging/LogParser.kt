package io.github.we007oneday_byte.softseek.core.logging

import kotlinx.serialization.json.Json

object LogParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(line: String): LogEntry? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching { json.decodeFromString<LogEntry>(trimmed) }.getOrNull()
    }
}
