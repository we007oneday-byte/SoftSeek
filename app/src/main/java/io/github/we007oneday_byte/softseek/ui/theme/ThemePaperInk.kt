package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import kotlin.math.abs
import kotlin.math.sin

internal val PaperInkSpec = VisualStyleSpec(
    style = VisualStyle.PaperInk,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF26221C,
        onPrimary = 0xFFFFFDF8,
        background = 0xFFF5F0E6,
        onBackground = 0xFF26221C,
        surface = 0xFFFFFDF8,
        onSurface = 0xFF26221C,
        secondary = 0xFF6F6250,
        tertiary = 0xFF8C7A5E,
        surfaceVariant = 0xFFEDE4D5,
        onSurfaceVariant = 0xFF6F6250,
        outline = 0xFFC9BBA5,
        userBubble = 0xFF26221C,
        aiBubble = 0xFFF7F1E6,
        thinking = 0xFFEFE6D6,
        code = 0xFFFBF6EC,
    ),
    dark = styleColors(
        primary = 0xFFE8DDC8,
        onPrimary = 0xFF12100D,
        background = 0xFF12100D,
        onBackground = 0xFFE8DDC8,
        surface = 0xFF1A1712,
        onSurface = 0xFFE8DDC8,
        secondary = 0xFFB8AB95,
        tertiary = 0xFF8C7A5E,
        surfaceVariant = 0xFF262119,
        onSurfaceVariant = 0xFFB8AB95,
        outline = 0xFF3A3327,
        userBubble = 0xFFE8DDC8,
        aiBubble = 0xFF1A1712,
        thinking = 0xFF211D16,
        code = 0xFF0B0A08,
    ),
    layout = layout(card = 20, bubble = 18, spacing = 20, border = 1f, elevation = 1),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.Solid,
    decorativeTape = true,
    fontFamily = FontFamily.Serif,
)

fun buildPaperInkLightScheme() = PaperInkSpec.light.toColorScheme(dark = false)

fun buildPaperInkDarkScheme() = PaperInkSpec.dark.toColorScheme(dark = true)

@Composable
fun PaperInkBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.fillMaxSize()) {
        drawRect(color = colors.background)
        if (!isLowEndDevice) {
            repeat(180) { index ->
                val seed = abs(sin((index + 1) * 12.9898f) * 43758.5453f) % 1f
                val x = size.width * seed
                val y = size.height * (abs(sin((index + 5) * 7.233f) * 43758.5453f) % 1f)
                drawCircle(
                    color = colors.onSurface.copy(alpha = 0.022f),
                    radius = 0.4f + seed * 1.1f,
                    center = Offset(x, y),
                )
            }
        }
    }
}
