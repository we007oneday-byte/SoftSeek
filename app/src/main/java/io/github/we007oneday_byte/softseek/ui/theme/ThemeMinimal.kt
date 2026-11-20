package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val MinimalSpec = VisualStyleSpec(
    style = VisualStyle.Minimal,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF30363A,
        onPrimary = 0xFFFFFFFF,
        background = 0xFFF7F7F7,
        onBackground = 0xFF1A1D1F,
        surface = 0xFFFFFFFF,
        onSurface = 0xFF1A1D1F,
        secondary = 0xFF5F6B72,
        tertiary = 0xFF727D83,
        surfaceVariant = 0xFFF1F3F4,
        onSurfaceVariant = 0xFF5F6B72,
        outline = 0xFFD9DEE1,
        userBubble = 0xFF30363A,
        aiBubble = 0xFFF1F3F4,
        thinking = 0xFFF4F5F6,
        code = 0xFFF7F8F9,
    ),
    dark = styleColors(
        primary = 0xFFD7DEE3,
        onPrimary = 0xFF121517,
        background = 0xFF111315,
        onBackground = 0xFFE9EDEF,
        surface = 0xFF1A1D1F,
        onSurface = 0xFFE9EDEF,
        secondary = 0xFFA7B2B8,
        tertiary = 0xFF8D999F,
        surfaceVariant = 0xFF252A2D,
        onSurfaceVariant = 0xFFAAB4BA,
        outline = 0xFF3A4145,
        userBubble = 0xFF3D4448,
        aiBubble = 0xFF22272A,
        thinking = 0xFF272C2F,
        code = 0xFF15181A,
    ),
    layout = layout(card = 12, bubble = 10, spacing = 12, border = 0.5f, elevation = 0),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.Solid,
)

fun buildMinimalLightScheme() = MinimalSpec.light.toColorScheme(dark = false)

fun buildMinimalDarkScheme() = MinimalSpec.dark.toColorScheme(dark = true)

@Composable
fun MinimalBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier.fillMaxSize()) {
        drawBackdropBase(background)
    }
}

internal fun DrawScope.drawBackdropBase(color: Color) {
    drawRect(color = color)
}
