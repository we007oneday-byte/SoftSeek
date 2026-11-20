package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val LineArtSpec = VisualStyleSpec(
    style = VisualStyle.LineArt,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF1E293B,
        onPrimary = 0xFFFFFFFF,
        background = 0xFFFFFFFF,
        onBackground = 0xFF1E293B,
        surface = 0xFFFFFFFF,
        onSurface = 0xFF1E293B,
        secondary = 0xFF64748B,
        tertiary = 0xFF0EA5E9,
        surfaceVariant = 0xFFF8FAFC,
        onSurfaceVariant = 0xFF64748B,
        outline = 0xFF94A3B8,
        userBubble = 0xFF1E293B,
        aiBubble = 0xFFF8FAFC,
        thinking = 0xFFF1F5F9,
        code = 0xFFF8FAFC,
    ),
    dark = styleColors(
        primary = 0xFFE2E8F0,
        onPrimary = 0xFF0B1119,
        background = 0xFF05080D,
        onBackground = 0xFFE2E8F0,
        surface = 0xFF0B1119,
        onSurface = 0xFFE2E8F0,
        secondary = 0xFF94A3B8,
        tertiary = 0xFF38BDF8,
        surfaceVariant = 0xFF111A24,
        onSurfaceVariant = 0xFF94A3B8,
        outline = 0xFF334155,
        userBubble = 0xFFE2E8F0,
        aiBubble = 0xFF111A24,
        thinking = 0xFF16212D,
        code = 0xFF070C12,
    ),
    layout = layout(card = 16, bubble = 14, spacing = 14, border = 1f, elevation = 0),
    cardPaint = StyleSurfacePaint.StrokeOnly,
    bubblePaint = StyleSurfacePaint.StrokeOnly,
    buttonPaint = StyleButtonPaint.StrokeOnly,
    borderPaint = StyleBorderPaint.Solid,
)

fun buildLineArtLightScheme() = LineArtSpec.light.toColorScheme(dark = false)

fun buildLineArtDarkScheme() = LineArtSpec.dark.toColorScheme(dark = true)

@Composable
fun LineArtBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier.fillMaxSize()) {
        drawBackdropBase(background)
    }
}
