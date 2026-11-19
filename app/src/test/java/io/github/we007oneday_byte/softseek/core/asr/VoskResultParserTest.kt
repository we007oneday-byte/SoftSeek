package io.github.we007oneday_byte.softseek.core.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoskResultParserTest {
    @Test
    fun parsesFinalText() {
        assertEquals("你好世界", VoskResultParser.text("""{"text":"你好世界"}"""))
    }

    @Test
    fun parsesPartialText() {
        assertEquals("你好", VoskResultParser.text("""{"partial":"你好"}"""))
    }

    @Test
    fun ignoresEmptyAndMalformed() {
        assertNull(VoskResultParser.text("""{"text":""}"""))
        assertNull(VoskResultParser.text("""{"text":"   "}"""))
        assertNull(VoskResultParser.text("not-json"))
        assertNull(VoskResultParser.text(null))
    }
}
