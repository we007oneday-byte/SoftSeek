package io.github.we007oneday_byte.softseek.core.logging

import java.time.LocalDate
import java.time.ZoneId

enum class LogTypeFilter(val label: String) {
    ALL("全部"),
    ERROR("ERROR"),
    CRASH("崩溃"),
    ANR("ANR"),
    INFO("INFO"),
}

enum class LogTimeRange(val label: String) {
    ALL("全部时间"),
    LAST_HOUR("近 1 小时"),
    TODAY("今天"),
    LAST_7_DAYS("近 7 天"),
}

object LogFilter {
    fun visible(
        entries: List<LogEntry>,
        showInfo: Boolean,
        moduleCode: String?,
        type: LogTypeFilter,
        range: LogTimeRange,
        now: Long = System.currentTimeMillis(),
    ): List<LogEntry> {
        val since = since(range, now)
        return entries.asSequence()
            .filter { showInfo || it.level == "ERROR" }
            .filter { moduleCode == null || it.module == moduleCode }
            .filter { entry ->
                when (type) {
                    LogTypeFilter.ALL -> true
                    LogTypeFilter.ERROR -> entry.level == "ERROR" && !entry.isCrashOrAnr
                    LogTypeFilter.CRASH -> entry.type == "CRASH"
                    LogTypeFilter.ANR -> entry.type == "ANR"
                    LogTypeFilter.INFO -> entry.level == "INFO"
                }
            }
            .filter { since == null || it.ts >= since }
            .sortedWith(
                compareByDescending<LogEntry> { it.isCrashOrAnr }
                    .thenByDescending { it.level == "ERROR" }
                    .thenByDescending { it.ts },
            )
            .toList()
    }

    fun export(entries: List<LogEntry>, mode: LogExportMode): List<LogEntry> =
        when (mode) {
            LogExportMode.ALL -> entries
            LogExportMode.ERROR_ONLY -> entries.filter { it.level == "ERROR" }
        }.sortedByDescending { it.ts }

    fun since(range: LogTimeRange, now: Long): Long? = when (range) {
        LogTimeRange.ALL -> null
        LogTimeRange.LAST_HOUR -> now - 60L * 60L * 1000L
        LogTimeRange.TODAY -> LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        LogTimeRange.LAST_7_DAYS -> now - 7L * 24L * 60L * 60L * 1000L
    }
}

enum class LogExportMode(val label: String) {
    ERROR_ONLY("仅 ERROR / 崩溃日志"),
    ALL("全量日志"),
}
