package io.github.we007oneday_byte.softseek.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFilterTest {
    private val now = 1_763_630_000_000L
    private val entries = listOf(
        LogEntry(
            ts = now - 90_000,
            level = "INFO",
            type = "METRIC",
            module = "M01",
            metric = "sync_count",
            value = "3",
            unit = "次",
        ),
        LogEntry(ts = now - 60_000, level = "ERROR", type = "ERROR", module = "M05", msg = "请求失败"),
        LogEntry(ts = now - 30_000, level = "ERROR", type = "ANR", module = "M02", msg = "主线程卡顿"),
        LogEntry(ts = now - 10_000, level = "ERROR", type = "CRASH", module = "M02", msg = "崩溃"),
    )

    @Test
    fun defaultViewHidesInfoAndPinsCrashAndAnr() {
        val visible = LogFilter.visible(
            entries = entries,
            showInfo = false,
            moduleCode = null,
            type = LogTypeFilter.ALL,
            range = LogTimeRange.ALL,
            now = now,
        )

        assertEquals(3, visible.size)
        assertTrue(visible[0].isCrashOrAnr)
        assertTrue(visible[1].isCrashOrAnr)
        assertEquals("ERROR", visible[2].level)
    }

    @Test
    fun filtersByModuleTypeAndTime() {
        val moduleAndType = LogFilter.visible(
            entries = entries,
            showInfo = true,
            moduleCode = "M02",
            type = LogTypeFilter.ANR,
            range = LogTimeRange.LAST_HOUR,
            now = now,
        )

        assertEquals(1, moduleAndType.size)
        assertEquals("ANR", moduleAndType.first().type)
    }

    @Test
    fun errorTypeExcludesCrashAndAnr() {
        val onlyPlainError = LogFilter.visible(
            entries = entries,
            showInfo = true,
            moduleCode = null,
            type = LogTypeFilter.ERROR,
            range = LogTimeRange.ALL,
            now = now,
        )

        assertEquals(1, onlyPlainError.size)
        assertEquals("M05", onlyPlainError.first().module)
    }

    @Test
    fun errorExportDropsInfo() {
        val exported = LogFilter.export(entries, LogExportMode.ERROR_ONLY)
        assertEquals(3, exported.size)
        assertTrue(exported.all { it.level == "ERROR" })
    }
}
