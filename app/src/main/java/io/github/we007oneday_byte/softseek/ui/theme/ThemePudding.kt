package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import kotlin.math.sin

internal val PuddingSpec = VisualStyleSpec(
    style = VisualStyle.Pudding,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFFF2A65A,
        onPrimary = 0xFF3A1B08,
        background = 0xFFFFF4D9,
        onBackground = 0xFF4A2A1A,
        surface = 0xFFFFFDF7,
        onSurface = 0xFF3D2414,
        secondary = 0xFFFFDDE4,
        tertiary = 0xFFC7E8D8,
        surfaceVariant = 0xFFFBE8CE,
        onSurfaceVariant = 0xFF8A6040,
        outline = 0xFFE8C9A3,
        userBubble = 0xFFF6B26B,
        aiBubble = 0xFFFFF2E2,
        thinking = 0xFFFBE8D2,
        code = 0xFFFFF8EF,
    ),
    dark = styleColors(
        primary = 0xFFFFC27F,
        onPrimary = 0xFF2A1508,
        background = 0xFF1B1008,
        onBackground = 0xFFFFEDDC,
        surface = 0xFF2A1B10,
        onSurface = 0xFFFFEDDC,
        secondary = 0xFFF2B8C6,
        tertiary = 0xFFA8D5BA,
        surfaceVariant = 0xFF352316,
        onSurfaceVariant = 0xFFD0A98A,
        outline = 0xFF4D3520,
        userBubble = 0xFF5A341C,
        aiBubble = 0xFF2B1A0F,
        thinking = 0xFF342014,
        code = 0xFF140B06,
    ),
    layout = layout(card = 32, bubble = 28, spacing = 14, border = 1f, elevation = 4),
    cardPaint = StyleSurfacePaint.Translucent,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.Solid,
    buttonPill = true,
    bubblePill = true,
)

fun buildPuddingLightScheme() = PuddingSpec.light.toColorScheme(dark = false)

fun buildPuddingDarkScheme() = PuddingSpec.dark.toColorScheme(dark = true)

@Composable
fun PuddingBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val phase = rememberBackdropPhase(animate = !isLowEndDevice, durationMillis = 12000)
    Canvas(modifier.fillMaxSize()) {
        drawRect(color = colors.background)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.16f),
                    colors.secondary.copy(alpha = 0.07f),
                    colors.background.copy(alpha = 0f),
                ),
                center = Offset(size.width * 0.18f, size.height * 0.12f),
                radius = size.minDimension * 0.95f,
            ),
        )
        if (!isLowEndDevice) {
            val pulse = 0.5f + 0.5f * sin(phase * (Math.PI * 2f).toFloat())
            drawCircle(
                color = colors.surface.copy(alpha = 0.22f + pulse * 0.08f),
                radius = size.minDimension * 0.13f,
                center = Offset(size.width * 0.82f, size.height * 0.2f),
            )
            drawCircle(
                color = colors.surface.copy(alpha = 0.16f),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.9f, size.height * 0.78f),
            )
            drawCircle(
                color = colors.primary.copy(alpha = 0.05f),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.12f, size.height * 0.88f),
            )
        }
    }
}
