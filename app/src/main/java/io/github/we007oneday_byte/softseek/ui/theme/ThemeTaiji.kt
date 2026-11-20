package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import kotlin.math.abs
import kotlin.math.sin

internal val TaijiSpec = VisualStyleSpec(
    style = VisualStyle.Taiji,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF141414,
        onPrimary = 0xFFFFFFFF,
        background = 0xFFF4F0E8,
        onBackground = 0xFF1B1813,
        surface = 0xFFFFFAF2,
        onSurface = 0xFF1B1813,
        secondary = 0xFF8C7A62,
        tertiary = 0xFFC4B29A,
        surfaceVariant = 0xFFE9E3D8,
        onSurfaceVariant = 0xFF6F6250,
        outline = 0xFFCBBDA8,
        userBubble = 0xFF1B1813,
        aiBubble = 0xFFEFEAE1,
        thinking = 0xFFE5DED2,
        code = 0xFFF3EFE7,
    ),
    dark = styleColors(
        primary = 0xFFF2F0EB,
        onPrimary = 0xFF0A0908,
        background = 0xFF090909,
        onBackground = 0xFFF2F0EB,
        surface = 0xFF151515,
        onSurface = 0xFFF2F0EB,
        secondary = 0xFFC4B29A,
        tertiary = 0xFF8C7A62,
        surfaceVariant = 0xFF22201B,
        onSurfaceVariant = 0xFFB5A995,
        outline = 0xFF3A352D,
        userBubble = 0xFFF2F0EB,
        aiBubble = 0xFF1F1D19,
        thinking = 0xFF26231E,
        code = 0xFF0F0E0C,
    ),
    layout = layout(card = 36, bubble = 36, spacing = 18, border = 1f, elevation = 1),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.InkGradient,
    bubblePill = true,
)

fun buildTaijiLightScheme() = TaijiSpec.light.toColorScheme(dark = false)

fun buildTaijiDarkScheme() = TaijiSpec.dark.toColorScheme(dark = true)

@Composable
fun TaijiBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.fillMaxSize()) {
        drawRect(color = colors.background)
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val radius = size.minDimension * 0.18f
        val center = Offset(size.width * 0.82f, size.height * 0.16f)
        val yin = colors.primary.copy(alpha = 0.06f)
        val yang = colors.surface.copy(alpha = 0.75f)
        val diameter = radius * 2f

        // 用 drawOval 代替每帧新建 Path，避免频繁主题切换时累积原生 Path 对象。
        drawOval(
            color = yin,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(diameter, diameter),
        )
        drawCircle(
            color = yang,
            radius = radius * 0.5f,
            center = Offset(center.x, center.y - radius * 0.5f),
        )
        drawCircle(
            color = yin,
            radius = radius * 0.5f,
            center = Offset(center.x, center.y + radius * 0.5f),
        )
        drawCircle(
            color = yin,
            radius = radius * 0.11f,
            center = Offset(center.x, center.y + radius * 0.5f),
        )

        if (!isLowEndDevice) {
            repeat(80) { index ->
                val seed = abs(sin((index + 1) * 12.9898f) * 43758.5453f) % 1f
                val x = size.width * seed
                val y = size.height * (abs(sin((index + 3) * 7.233f) * 43758.5453f) % 1f)
                drawCircle(
                    color = colors.onSurface.copy(alpha = 0.025f),
                    radius = 0.6f + seed * 1.2f,
                    center = Offset(x, y),
                )
            }
        }
    }
}
