package io.github.we007oneday_byte.softseek.core.logging

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 只读取应用私有目录下的 logback 日志文件，不访问网络。
 * 日志页默认展示 ERROR，开启开关后才合并 INFO/METRIC。
 */
class LogRepository(context: Context) {
    data class ExportResult(val file: File, val count: Int)

    private val logDir = File(context.filesDir, "logs")
    private val exportDir = File(context.filesDir, "log_exports")

    suspend fun load(): List<LogEntry> = withContext(Dispatchers.IO) {
        readEntries().take(MAX_ENTRIES)
    }

    suspend fun export(
        mode: LogExportMode,
        now: Long = System.currentTimeMillis(),
    ): ExportResult = withContext(Dispatchers.IO) {
        exportDir.mkdirs()
        val selected = LogFilter.export(readEntries(), mode)
        val file = File(exportDir, "softseek-log-${fileStamp(now)}.txt")
        file.writeText(buildExportText(selected, mode, now), Charsets.UTF_8)
        ExportResult(file, selected.size)
    }

    private fun readEntries(): List<LogEntry> {
        val files = logDir.listFiles { file -> file.isFile && file.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        val entries = mutableListOf<LogEntry>()
        files.forEach { file ->
            runCatching {
                file.useLines { lines ->
                    lines.forEach { line ->
                        LogParser.parse(line)?.let { entries += it }
                    }
                }
            }
        }
        return entries
    }

    fun exportDirectory(): File = exportDir

    private fun buildExportText(
        entries: List<LogEntry>,
        mode: LogExportMode,
        now: Long,
    ): String = buildString {
        appendLine("SOFTSEEK 运行日志")
        appendLine("exported_at=${formatTime(now)} (${now}ms)")
        appendLine("app_version=${SoftSeekLogEnvironment.appVersion}")
        appendLine("device_model=${SoftSeekLogEnvironment.deviceModel}")
        appendLine("os_version=${SoftSeekLogEnvironment.osVersion}")
        appendLine("export_mode=${mode.label}")
        appendLine("entry_count=${entries.size}")
        appendLine("----")
        entries.forEach { entry ->
            appendLine(entry.toExportLine())
        }
    }

    private fun LogEntry.toExportLine(): String = buildString {
        append(time.ifBlank { formatTime(ts) })
        append(" | level=")
        append(level)
        append(" | type=")
        append(type)
        append(" | module=")
        append(module)
        if (metric.isNotBlank()) {
            append(" | metric=")
            append(metric)
            append(" | value=")
            append(value)
            append(" | unit=")
            append(unit)
        }
        append(" | app=")
        append(app.ifBlank { SoftSeekLogEnvironment.appVersion })
        append(" | device=")
        append(device.ifBlank { SoftSeekLogEnvironment.deviceModel })
        append(" | os=")
        append(os.ifBlank { SoftSeekLogEnvironment.osVersion })
        append(" | crash_id=")
        append(crashId)
        append(" | msg=")
        append(msg)
        if (throwable.isNotBlank()) {
            append(" | throwable=")
            append(throwable)
        }
    }

    private fun fileStamp(millis: Long): String = FILE_STAMP.format(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()),
    )

    private fun formatTime(millis: Long): String = TIME_STAMP.format(
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()),
    )

    private companion object {
        const val MAX_ENTRIES = 10_000
        val FILE_STAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
        val TIME_STAMP: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }
}
