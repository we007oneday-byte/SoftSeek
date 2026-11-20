package io.github.we007oneday_byte.softseek.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogParserTest {
    @Test
    fun parsesStructuredJsonLine() {
        val line = """
            {"ts":1763630000123,"time":"2026-11-20T18:13:20.123+08:00","level":"ERROR",
            "type":"CRASH","module":"M02","metric":"call_end","value":"1200","unit":"ms",
            "app":"0.1.0","device":"Xiaomi 14","os":"Android 15 (API 35)","thread":"main",
            "msg":"未捕获异常","throwable":"java.lang.IlllegalStateException: boom","crashId":"crash-1"}
        """.trimIndent()

        val entry = LogParser.parse(line)

        requireNotNull(entry)
        assertEquals(1763630000123L, entry.ts)
        assertEquals("ERROR", entry.level)
        assertEquals("CRASH", entry.type)
        assertEquals("M02", entry.module)
        assertEquals("AI 通话", entry.moduleLabel)
        assertEquals("call_end", entry.metric)
        assertEquals("1200", entry.value)
        assertEquals("ms", entry.unit)
        assertEquals("crash-1", entry.crashId)
        assertTrue(entry.isCrashOrAnr)
    }

    @Test
    fun ignoresNonJsonAndBrokenLines() {
        assertNull(LogParser.parse("普通文本日志"))
        assertNull(LogParser.parse("""{"ts":"not-a-number"}"""))
    }
}
