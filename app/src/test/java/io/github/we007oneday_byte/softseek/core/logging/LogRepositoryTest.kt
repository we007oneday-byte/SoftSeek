package io.github.we007oneday_byte.softseek.core.logging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LogRepositoryTest {
    @Test
    fun exportsTxtIntoPrivateDirectoryWithEnvironmentHeader() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logDir = File(context.filesDir, "logs")
        logDir.deleteRecursively()
        logDir.mkdirs()
        File(context.filesDir, "log_exports").deleteRecursively()

        val info = LogEntry(
            ts = 1_763_630_000_000L,
            time = "2025-11-20T18:00:00.000+08:00",
            level = "INFO",
            type = "METRIC",
            module = "M01",
            metric = "login_duration",
            value = "1200",
            unit = "ms",
            app = "test-version",
            device = "test-device",
            os = "Android 15 (API 35)",
            msg = "登录耗时",
        )
        val error = LogEntry(
            ts = 1_763_630_010_000L,
            time = "2025-11-20T18:00:10.000+08:00",
            level = "ERROR",
            type = "ERROR",
            module = "M05",
            app = "test-version",
            device = "test-device",
            os = "Android 15 (API 35)",
            msg = "请求失败",
        )
        val json = Json { encodeDefaults = true }
        File(logDir, "softseek-info.log").writeText(
            json.encodeToString(info) + "\n" + json.encodeToString(error) + "\n",
        )
        SoftSeekLogEnvironment.init("test-version")

        val repository = LogRepository(context)
        val all = repository.export(LogExportMode.ALL, now = 1_763_630_020_000L)
        assertTrue(all.file.path.startsWith(context.filesDir.absolutePath))
        assertTrue(all.file.path.contains("log_exports"))
        assertEquals(2, all.count)
        val text = all.file.readText()
        assertTrue(text.contains("SOFTSEEK 运行日志"))
        assertTrue(text.contains("app_version=test-version"))
        assertTrue(text.contains("export_mode=全量日志"))
        assertTrue(text.contains("login_duration"))

        val errorOnly = repository.export(LogExportMode.ERROR_ONLY, now = 1_763_630_030_000L)
        assertEquals(1, errorOnly.count)
        assertEquals(false, errorOnly.file.readText().contains("login_duration"))
    }
}
