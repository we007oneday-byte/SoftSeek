package io.github.we007oneday_byte.softseek.core.logging

import kotlinx.serialization.Serializable

/**
 * 一行结构化日志。logback 写入的是 JSON Lines，字段名保持短且稳定，
 * 客户端日志页和 TXT 导出都复用同一套字段。
 */
@Serializable
data class LogEntry(
    val ts: Long = 0L,
    val time: String = "",
    val level: String = "INFO",
    val type: String = "INFO",
    val module: String = LogModule.APP.code,
    val metric: String = "",
    val value: String = "",
    val unit: String = "",
    val app: String = "",
    val device: String = "",
    val os: String = "",
    val thread: String = "",
    val msg: String = "",
    val throwable: String = "",
    val crashId: String = "",
) {
    val moduleLabel: String get() = LogModule.fromCode(module).label

    val isCrashOrAnr: Boolean get() = type == "CRASH" || type == "ANR"
}
