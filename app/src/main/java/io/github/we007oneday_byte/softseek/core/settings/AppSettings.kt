package io.github.we007oneday_byte.softseek.core.settings

import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.domain.model.ImageDetail
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.domain.provider.ProviderType
import kotlinx.serialization.Serializable

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class CodeTheme {
    FOLLOW_THEME,
    LIGHT,
    DARK,
}

@Serializable
data class ParameterPreset(
    val id: String,
    val name: String,
    val model: String,
    val effort: ReasoningEffort,
    val maxOutputTokens: Int,
)

@Serializable
data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val visualStyle: VisualStyle = VisualStyle.Minimal,
    val voiceAnimationStyle: VoiceAnimationStyle = VoiceAnimationStyle.WAVE,
    val dynamicColor: Boolean = false,
    val fontScale: Float = 1.0f,
    val bubbleRadiusDp: Int = 16,
    val codeTheme: CodeTheme = CodeTheme.FOLLOW_THEME,
    val streamingEnabled: Boolean = true,
    val markdownEnabled: Boolean = true,
    val defaultModel: String = AppConstants.DEFAULT_MODEL,
    /** 用户手动填写过的模型 ID，用于 /models 不可用或未返回该模型时仍视为有效。 */
    val manualModelIds: List<String> = emptyList(),
    val defaultReasoningEffort: ReasoningEffort = ReasoningEffort.HIGH,
    val defaultMaxInputTokens: Int = MemoryPoolConfig.DEFAULT_MAX_INPUT_TOKENS,
    val defaultMaxOutputTokens: Int = 8192,
    val presets: List<ParameterPreset> = emptyList(),
    val autoCleanup: Boolean = false,
    val cleanupThresholdCount: Int = MemoryPoolConfig.DEFAULT_CLEANUP_THRESHOLD_COUNT,
    val cleanupProtectionHours: Int = MemoryPoolConfig.DEFAULT_CLEANUP_PROTECTION_HOURS,
    val deletedRetentionDays: Int = MemoryPoolConfig.DEFAULT_DELETED_RETENTION_DAYS,
    val imageDetail: ImageDetail = ImageDetail.AUTO,
    val providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String = AppConstants.DEFAULT_BASE_URL,
    val proxyUrl: String = "",
    val connectTimeoutSeconds: Int = 15,
    val readTimeoutSeconds: Int = 60,
    val writeTimeoutSeconds: Int = 60,
    val usageWarningTokens: Long = 0L,
)
