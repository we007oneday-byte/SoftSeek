package io.github.we007oneday_byte.softseek.core.settings

import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `default settings keep auto cleanup disabled`() {
        val settings = AppSettings()
        assertFalse(settings.autoCleanup)
        assertEquals(MemoryPoolConfig.DEFAULT_CLEANUP_THRESHOLD_COUNT, settings.cleanupThresholdCount)
        assertEquals(16, settings.bubbleRadiusDp)
        assertEquals(CodeTheme.FOLLOW_THEME, settings.codeTheme)
    }

    @Test
    fun `settings round trip keeps presets and cleanup policy`() {
        val preset = ParameterPreset(
            id = "p1",
            name = "长文",
            model = "deepseek-v4-pro",
            effort = ReasoningEffort.MAX,
            maxOutputTokens = 65536,
        )
        val original = AppSettings(
            themeMode = AppThemeMode.DARK,
            dynamicColor = true,
            fontScale = 1.1f,
            bubbleRadiusDp = 22,
            codeTheme = CodeTheme.DARK,
            streamingEnabled = false,
            markdownEnabled = false,
            defaultModel = "deepseek-v4-pro",
            defaultReasoningEffort = ReasoningEffort.LOW,
            defaultMaxOutputTokens = 4096,
            presets = listOf(preset),
            autoCleanup = true,
            cleanupThresholdCount = 6,
            cleanupProtectionHours = 48,
            deletedRetentionDays = 30,
            baseUrl = "https://proxy.example.com/v1",
            proxyUrl = "http://127.0.0.1:7890",
        )
        val raw = json.encodeToString(original)
        val decoded = json.decodeFromString<AppSettings>(raw)
        assertEquals(original, decoded)
        assertTrue(decoded.presets.single().name == "长文")
    }

    @Test
    fun `legacy json without presets falls back to empty list`() {
        val decoded = json.decodeFromString<AppSettings>("""{"autoCleanup":true}""")
        assertTrue(decoded.autoCleanup)
        assertTrue(decoded.presets.isEmpty())
    }
}
