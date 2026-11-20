package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Compose MaterialTheme 未覆盖的业务色。所有值都由当前 VisualStyleSpec 派生，
 * 不再保留固定的黑白灰常量。
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

val LocalSoftSeekColors = staticCompositionLocalOf {
    val colors = MinimalSpec.light
    SoftSeekExtendedColors(
        userBubble = colors.userBubble,
        aiBubble = colors.aiBubble,
        thinkingBackground = colors.thinking,
        textPrimary = colors.onSurface,
        textSecondary = colors.onSurface.copy(alpha = 0.62f),
        divider = colors.outline,
        codeBackground = colors.code,
    )
}
