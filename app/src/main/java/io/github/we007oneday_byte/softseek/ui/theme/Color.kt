package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 主色刻意偏离 DeepSeek 官方蓝，采用低饱和紫罗兰，避免品牌混淆。
val SoftSeekPrimaryLight = Color(0xFF6C5CE7)
val SoftSeekPrimaryVariantLight = Color(0xFF5A4BD1)
val SoftSeekPrimaryDark = Color(0xFF8B7CFF)
val SoftSeekPrimaryVariantDark = Color(0xFF7566E8)

val SoftSeekBackgroundLight = Color(0xFFF8F9FA)
val SoftSeekSurfaceLight = Color(0xFFFFFFFF)
val SoftSeekBackgroundDark = Color(0xFF121212)
val SoftSeekSurfaceDark = Color(0xFF1E1E1E)

val SoftSeekTextPrimaryLight = Color(0xFF1A1A1A)
val SoftSeekTextSecondaryLight = Color(0xFF666666)
val SoftSeekTextPrimaryDark = Color(0xFFFFFFFF)
val SoftSeekTextSecondaryDark = Color(0xFFAAAAAA)

val SoftSeekDividerLight = Color(0xFFE5E7EB)
val SoftSeekDividerDark = Color(0xFF333333)

val SoftSeekAiBubbleLight = Color(0xFFF0F2F5)
val SoftSeekAiBubbleDark = Color(0xFF2A2A2A)

val SoftSeekThinkingLight = Color(0xFFF5F3FF)
val SoftSeekThinkingDark = Color(0xFF252336)

val SoftSeekCodeBackgroundLight = Color(0xFFF3F4F6)
val SoftSeekCodeBackgroundDark = Color(0xFF151515)

val SoftSeekErrorLight = Color(0xFFF44336)
val SoftSeekErrorDark = Color(0xFFEF5350)

/**
 * Compose MaterialTheme 未覆盖的业务色：气泡、思考块、代码块。
 */
data class SoftSeekExtendedColors(
    val userBubble: Color,
    val aiBubble: Color,
    val thinkingBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val codeBackground: Color,
)

val LightSoftSeekExtendedColors = SoftSeekExtendedColors(
    userBubble = SoftSeekPrimaryLight,
    aiBubble = SoftSeekAiBubbleLight,
    thinkingBackground = SoftSeekThinkingLight,
    textPrimary = SoftSeekTextPrimaryLight,
    textSecondary = SoftSeekTextSecondaryLight,
    divider = SoftSeekDividerLight,
    codeBackground = SoftSeekCodeBackgroundLight,
)

val DarkSoftSeekExtendedColors = SoftSeekExtendedColors(
    userBubble = SoftSeekPrimaryDark,
    aiBubble = SoftSeekAiBubbleDark,
    thinkingBackground = SoftSeekThinkingDark,
    textPrimary = SoftSeekTextPrimaryDark,
    textSecondary = SoftSeekTextSecondaryDark,
    divider = SoftSeekDividerDark,
    codeBackground = SoftSeekCodeBackgroundDark,
)

val LocalSoftSeekColors = staticCompositionLocalOf { LightSoftSeekExtendedColors }
