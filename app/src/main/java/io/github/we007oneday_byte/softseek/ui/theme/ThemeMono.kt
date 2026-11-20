package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val MonoSpec = VisualStyleSpec(
    style = VisualStyle.Mono,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF101010,
        onPrimary = 0xFFFFFFFF,
        background = 0xFFFFFFFF,
        onBackground = 0xFF101010,
        surface = 0xFFFFFFFF,
        onSurface = 0xFF101010,
        secondary = 0xFF555555,
        tertiary = 0xFF777777,
        surfaceVariant = 0xFFF2F2F2,
        onSurfaceVariant = 0xFF666666,
        outline = 0xFFD0D0D0,
        userBubble = 0xFF101010,
        aiBubble = 0xFFF2F2F2,
        thinking = 0xFFF5F5F5,
        code = 0xFFF7F7F7,
    ),
    dark = styleColors(
        primary = 0xFFF2F2F2,
        onPrimary = 0xFF000000,
        background = 0xFF000000,
        onBackground = 0xFFF2F2F2,
        surface = 0xFF0B0B0B,
        onSurface = 0xFFF2F2F2,
        secondary = 0xFFB8B8B8,
        tertiary = 0xFF8E8E8E,
        surfaceVariant = 0xFF1A1A1A,
        onSurfaceVariant = 0xFFA8A8A8,
        outline = 0xFF333333,
        userBubble = 0xFFF2F2F2,
        aiBubble = 0xFF1A1A1A,
        thinking = 0xFF202020,
        code = 0xFF101010,
    ),
    layout = layout(card = 8, bubble = 8, spacing = 12, border = 1f, elevation = 0),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.Solid,
)

fun buildMonoLightScheme() = MonoSpec.light.toColorScheme(dark = false)

fun buildMonoDarkScheme() = MonoSpec.dark.toColorScheme(dark = true)

@Composable
fun MonoBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier.fillMaxSize()) {
        drawBackdropBase(background)
    }
}
