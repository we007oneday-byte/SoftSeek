package io.github.we007oneday_byte.softseek.core.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTextSplitterTest {
    @Test
    fun `splits chinese sentences by terminal punctuation`() {
        val result = TtsTextSplitter.split("你好。今天天气不错！要出门吗？")
        assertEquals(listOf("你好。", "今天天气不错！", "要出门吗？"), result)
    }

    @Test
    fun `keeps newline as sentence boundary`() {
        val result = TtsTextSplitter.split("第一行\n第二行；第三行")
        assertEquals(listOf("第一行", "第二行；", "第三行"), result)
    }

    @Test
    fun `hard splits a long sentence without punctuation`() {
        val result = TtsTextSplitter.split("甲".repeat(170), maxChars = 80)
        assertEquals(3, result.size)
        assertTrue(result.all { it.length <= 80 })
        assertEquals(170, result.sumOf { it.length })
    }

    @Test
    fun `prefers a soft break near the limit`() {
        val text = "一二三四五六七八九十，".repeat(10)
        val result = TtsTextSplitter.split(text, maxChars = 30)
        assertTrue(result.all { it.length <= 30 })
        assertTrue(result.first().endsWith("，"))
    }

    @Test
    fun `blank text returns empty list`() {
        assertEquals(emptyList<String>(), TtsTextSplitter.split("  \n\t "))
    }
}
