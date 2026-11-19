package io.github.we007oneday_byte.softseek.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaseUrlValidatorTest {
    @Test
    fun addsTrailingSlashAndKeepsHttps() {
        assertEquals("https://api.deepseek.com/", BaseUrlValidator.validate("https://api.deepseek.com"))
    }

    @Test
    fun rejectsPlainHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            BaseUrlValidator.validate("http://api.deepseek.com")
        }
    }

    @Test
    fun rejectsMalformedUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            BaseUrlValidator.validate("not a url")
        }
    }
}
