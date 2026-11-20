package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val NeonSpec = VisualStyleSpec(
    style = VisualStyle.Neon,
    forceDarkMode = true,
    light = styleColors(
        primary = 0xFF00E5FF,
        onPrimary = 0xFF001014,
        background = 0xFF08030F,
        onBackground = 0xFFF3E9FF,
        surface = 0xFF140A24,
        onSurface = 0xFFF3E9FF,
        secondary = 0xFFFF5CCF,
        tertiary = 0xFF9D7CFF,
        surfaceVariant = 0xFF1B1029,
        onSurfaceVariant = 0xFFB99BE0,
        outline = 0xFF4B2A6B,
        userBubble = 0xFF3A1450,
        aiBubble = 0xFF160B23,
        thinking = 0xFF1B1029,
        code = 0xFF05020A,
    ),
    dark = styleColors(
        primary = 0xFF00E5FF,
        onPrimary = 0xFF001014,
        background = 0xFF08030F,
        onBackground = 0xFFF3E9FF,
        surface = 0xFF140A24,
        onSurface = 0xFFF3E9FF,
        secondary = 0xFFFF5CCF,
        tertiary = 0xFF9D7CFF,
        surfaceVariant = 0xFF1B1029,
        onSurfaceVariant = 0xFFB99BE0,
        outline = 0xFF4B2A6B,
        userBubble = 0xFF3A1450,
        aiBubble = 0xFF160B23,
        thinking = 0xFF1B1029,
        code = 0xFF05020A,
    ),
    layout = layout(card = 18, bubble = 16, spacing = 13, border = 1f, elevation = 3),
    cardPaint = StyleSurfacePaint.Translucent,
    bubblePaint = StyleSurfacePaint.Translucent,
    buttonPaint = StyleButtonPaint.Glow,
    borderPaint = StyleBorderPaint.Glow,
    buttonPill = true,
)

fun buildNeonLightScheme() = NeonSpec.light.toColorScheme(dark = true)

fun buildNeonDarkScheme() = NeonSpec.dark.toColorScheme(dark = true)

@Composable
fun NeonBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.fillMaxSize()) {
        drawRect(color = colors.background)
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val gap = (size.minDimension / 9f).coerceAtLeast(1f)
        // 用有界 repeat 代替 while 递增，即使画布尺寸异常也最多画 65 条线。
        val columns = (size.width / gap).toInt().coerceIn(1, 64)
        repeat(columns + 1) { index ->
            val x = size.width * index / columns
            drawLine(
                color = colors.primary.copy(alpha = 0.085f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
        }
        val rows = (size.height / gap).toInt().coerceIn(1, 64)
        repeat(rows + 1) { index ->
            val y = size.height * index / rows
            drawLine(
                color = colors.secondary.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
    }
}
