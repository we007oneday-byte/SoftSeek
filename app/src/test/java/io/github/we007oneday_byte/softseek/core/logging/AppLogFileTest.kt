package io.github.we007oneday_byte.softseek.core.logging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.slf4j.LoggerFactory
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppLogFileTest {
    @Test
    fun writesStructuredInfoAndSeparateCrashFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "logs").deleteRecursively()
        SoftSeekLogEnvironment.init("test-version")
        LogbackConfigurator.configure(context)
        val root = (LoggerFactory.getILoggerFactory() as LoggerContext)
            .getLogger(Logger.ROOT_LOGGER_NAME)
        assertTrue(root.iteratorForAppenders().asSequence().toList().isNotEmpty())
        assertEquals(null, AppLog.initFailure())

        AppLog.metric(LogModule.CHAT, "login_duration", 1200, "ms")
        AppLog.error(LogModule.CHAT, "请求失败")
        AppLog.crash(LogModule.CALL, IllegalStateException("boom"))

        val logDir = File(context.filesDir, "logs")
        val infoFile = File(logDir, "softseek-info.log")
        val errorFile = File(logDir, "softseek-error.log")
        val crashFile = File(logDir, "softseek-crash.log")
        assertTrue("info log missing", infoFile.exists())
        assertTrue("error log missing", errorFile.exists())
        assertTrue("crash log missing", crashFile.exists())

        val lines = infoFile.readLines().filter { it.startsWith("{") }
        assertTrue(lines.none { it.contains("\"level\":\"ERROR\"") })
        val metric = LogParser.parse(lines.first { it.contains("login_duration") })
        requireNotNull(metric)
        assertEquals("M01", metric.module)
        assertEquals("1200", metric.value)
        assertEquals("ms", metric.unit)
        assertTrue(metric.app.isNotBlank())

        val errorText = errorFile.readText()
        assertTrue(errorText.contains("请求失败"))
        assertTrue(!errorText.contains("\"type\":\"CRASH\""))
        val crashText = crashFile.readText()
        assertTrue(crashText.contains("\"type\":\"CRASH\""))
        assertTrue(crashText.contains("\"module\":\"M02\""))
    }
}
