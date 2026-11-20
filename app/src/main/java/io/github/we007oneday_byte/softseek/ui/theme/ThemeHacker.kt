package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val HackerSpec = VisualStyleSpec(
    style = VisualStyle.Hacker,
    forceDarkMode = true,
    light = styleColors(
        primary = 0xFF39FF14,
        onPrimary = 0xFF041104,
        background = 0xFF0C0C0C,
        onBackground = 0xFFE6FFE6,
        surface = 0xFF111411,
        onSurface = 0xFFE6FFE6,
        secondary = 0xFF1E3322,
        tertiary = 0xFF39FF14,
        surfaceVariant = 0xFF17221A,
        onSurfaceVariant = 0xFF9BBFA2,
        outline = 0xFF1E4A2B,
        userBubble = 0xFF12331E,
        aiBubble = 0xFF101810,
        thinking = 0xFF142016,
        code = 0xFF050805,
    ),
    dark = styleColors(
        primary = 0xFF39FF14,
        onPrimary = 0xFF041104,
        background = 0xFF0C0C0C,
        onBackground = 0xFFE6FFE6,
        surface = 0xFF111411,
        onSurface = 0xFFE6FFE6,
        secondary = 0xFF1E3322,
        tertiary = 0xFF39FF14,
        surfaceVariant = 0xFF17221A,
        onSurfaceVariant = 0xFF9BBFA2,
        outline = 0xFF1E4A2B,
        userBubble = 0xFF12331E,
        aiBubble = 0xFF101810,
        thinking = 0xFF142016,
        code = 0xFF050805,
    ),
    layout = layout(card = 2, bubble = 4, spacing = 10, border = 1f, elevation = 2),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.Glow,
    borderPaint = StyleBorderPaint.Glow,
    fontFamily = FontFamily.Monospace,
)

fun buildHackerLightScheme() = HackerSpec.light.toColorScheme(dark = true)

fun buildHackerDarkScheme() = HackerSpec.dark.toColorScheme(dark = true)

@Composable
fun HackerBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val phase = rememberBackdropPhase(animate = !isLowEndDevice, durationMillis = 7000)
    Canvas(modifier.fillMaxSize()) {
        drawRect(color = colors.background)
        if (!isLowEndDevice && size.width > 0f && size.height > 0f) {
            // 固定 56 条扫描线，不用 while 递增：即使画布高度异常为 0 也不会死循环。
            repeat(56) { index ->
                val y = size.height * index / 55f
                drawLine(
                    color = colors.primary.copy(alpha = 0.035f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
            val scanY = phase * size.height
            drawLine(
                color = colors.primary.copy(alpha = 0.28f),
                start = Offset(0f, scanY),
                end = Offset(size.width, scanY),
                strokeWidth = 2f,
            )
            drawLine(
                color = colors.primary.copy(alpha = 0.08f),
                start = Offset(0f, scanY - 18f),
                end = Offset(size.width, scanY - 18f),
                strokeWidth = 10f,
            )
        }
    }
}
