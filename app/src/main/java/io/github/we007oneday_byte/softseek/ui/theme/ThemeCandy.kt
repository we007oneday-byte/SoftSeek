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

internal val CandySpec = VisualStyleSpec(
    style = VisualStyle.Candy,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFFFF8FB8,
        onPrimary = 0xFF33101F,
        background = 0xFFFFF8FB,
        onBackground = 0xFF3A2530,
        surface = 0xFFFFFFFF,
        onSurface = 0xFF3A2530,
        secondary = 0xFFFFE08A,
        tertiary = 0xFFA8E6CF,
        surfaceVariant = 0xFFFFE3EE,
        onSurfaceVariant = 0xFF7A5A68,
        outline = 0xFFF3D7E3,
        userBubble = 0xFFFF8FB8,
        aiBubble = 0xFFFFF0F6,
        thinking = 0xFFFDE7F1,
        code = 0xFFFFF5FA,
    ),
    dark = styleColors(
        primary = 0xFFFF9CC2,
        onPrimary = 0xFF2A0A17,
        background = 0xFF241528,
        onBackground = 0xFFFFE8F2,
        surface = 0xFF32203A,
        onSurface = 0xFFFFE8F2,
        secondary = 0xFFFFD98A,
        tertiary = 0xFF9CE8CD,
        surfaceVariant = 0xFF3F2849,
        onSurfaceVariant = 0xFFD8A8BE,
        outline = 0xFF5A3047,
        userBubble = 0xFF7A3350,
        aiBubble = 0xFF2E1824,
        thinking = 0xFF3A1F30,
        code = 0xFF190C14,
    ),
    layout = layout(card = 26, bubble = 22, spacing = 15, border = 1f, elevation = 3),
    cardPaint = StyleSurfacePaint.Translucent,
    bubblePaint = StyleSurfacePaint.Translucent,
    buttonPaint = StyleButtonPaint.CandyGradient,
    borderPaint = StyleBorderPaint.Solid,
    buttonPill = true,
    defaultBackdropBlur = true,
)

fun buildCandyLightScheme() = CandySpec.light.toColorScheme(dark = false)

fun buildCandyDarkScheme() = CandySpec.dark.toColorScheme(dark = true)

@Composable
fun CandyBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val phase = rememberBackdropPhase(animate = !isLowEndDevice, durationMillis = 16000)
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.14f),
                    colors.secondary.copy(alpha = 0.10f),
                    colors.tertiary.copy(alpha = 0.10f),
                    colors.background,
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        if (!isLowEndDevice) {
            val bob = sin(phase * (Math.PI * 2f).toFloat())
            drawCircle(
                color = colors.surface.copy(alpha = 0.36f),
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * 0.84f, size.height * 0.12f + bob * 12f),
            )
            drawCircle(
                color = colors.secondary.copy(alpha = 0.18f),
                radius = size.minDimension * 0.12f,
                center = Offset(size.width * 0.16f, size.height * 0.44f - bob * 8f),
            )
            drawCircle(
                color = colors.tertiary.copy(alpha = 0.16f),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * 0.74f, size.height * 0.84f + bob * 10f),
            )
            drawCircle(
                color = colors.surface.copy(alpha = 0.22f),
                radius = size.minDimension * 0.07f,
                center = Offset(size.width * 0.96f, size.height * 0.62f - bob * 6f),
            )
        }
    }
}
