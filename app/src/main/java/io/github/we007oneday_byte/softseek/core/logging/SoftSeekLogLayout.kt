package io.github.we007oneday_byte.softseek.core.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.LayoutBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 统一 JSON Lines 输出。字段名和顺序固定，日志页、TXT 导出、外部排查
 * 都按同一套结构解析；消息与堆栈会做 JSON 转义，避免换行破坏一行一条。
 */
class SoftSeekLogLayout : LayoutBase<ILoggingEvent>() {
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

    override fun doLayout(event: ILoggingEvent): String {
        val mdc = event.mdcPropertyMap ?: emptyMap()
        val type = mdc["type"] ?: if (event.level == Level.ERROR) "ERROR" else "INFO"
        return buildString(256) {
            append('{')
            append("\"ts\":").append(event.timeStamp)
            append(',')
            appendField(
                "time",
                timeFormatter.format(
                    Instant.ofEpochMilli(event.timeStamp).atZone(ZoneId.systemDefault()),
                ),
            )
            append(',')
            appendField("level", event.level.levelStr)
            append(',')
            appendField("type", type)
            append(',')
            appendField("module", mdc["module"] ?: LogModule.APP.code)
            append(',')
            appendField("metric", mdc["metric"].orEmpty())
            append(',')
            appendField("value", mdc["value"].orEmpty())
            append(',')
            appendField("unit", mdc["unit"].orEmpty())
            append(',')
            appendField("app", SoftSeekLogEnvironment.appVersion)
            append(',')
            appendField("device", SoftSeekLogEnvironment.deviceModel)
            append(',')
            appendField("os", SoftSeekLogEnvironment.osVersion)
            append(',')
            appendField("thread", event.threadName.orEmpty())
            append(',')
            appendField("msg", event.formattedMessage.orEmpty())
            append(',')
            appendField("throwable", event.throwableProxy?.let { ThrowableProxyUtil.asString(it) }.orEmpty())
            append(',')
            appendField("crashId", mdc["crashId"].orEmpty())
            append('}')
            append('\n')
        }
    }

    private fun StringBuilder.appendField(key: String, raw: String) {
        append('"')
        append(key)
        append("\":\"")
        append(escapeJson(raw))
        append('"')
    }

    private fun escapeJson(raw: String): String = buildString(raw.length) {
        raw.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
    }
}
