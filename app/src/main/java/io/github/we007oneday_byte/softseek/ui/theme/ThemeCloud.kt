package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle
import kotlin.math.PI
import kotlin.math.sin

internal val CloudSpec = VisualStyleSpec(
    style = VisualStyle.Cloud,
    forceDarkMode = false,
    light = styleColors(
        primary = 0xFF7FE0FF,
        onPrimary = 0xFF0B2833,
        background = 0xFFF8FCFF,
        onBackground = 0xFF183246,
        surface = 0xFFCFE2F3,
        onSurface = 0xFF183246,
        secondary = 0xFFA9D4F5,
        tertiary = 0xFFB7E3D0,
        surfaceVariant = 0xFFDCEBF7,
        onSurfaceVariant = 0xFF4F6B82,
        outline = 0xFFA9C6DF,
        userBubble = 0xFF7FE0FF,
        aiBubble = 0xFFDCEBF7,
        thinking = 0xFFE8F3FB,
        code = 0xFFEBF5FB,
    ),
    dark = styleColors(
        primary = 0xFFA9E9FF,
        onPrimary = 0xFF04121F,
        background = 0xFF0A1522,
        onBackground = 0xFFE4F3FF,
        surface = 0xFF1A2E40,
        onSurface = 0xFFE4F3FF,
        secondary = 0xFF7EA8C8,
        tertiary = 0xFFA8D8C0,
        surfaceVariant = 0xFF172838,
        onSurfaceVariant = 0xFF9DB8D0,
        outline = 0xFF2B455C,
        userBubble = 0xFF2F6E8C,
        aiBubble = 0xFF172838,
        thinking = 0xFF1D3448,
        code = 0xFF06101A,
    ),
    layout = layout(card = 28, bubble = 24, spacing = 16, border = 1f, elevation = 2),
    cardPaint = StyleSurfacePaint.Solid,
    bubblePaint = StyleSurfacePaint.Solid,
    buttonPaint = StyleButtonPaint.SolidPrimary,
    borderPaint = StyleBorderPaint.Solid,
    buttonPill = true,
)

fun buildCloudLightScheme() = CloudSpec.light.toColorScheme(dark = false)

fun buildCloudDarkScheme() = CloudSpec.dark.toColorScheme(dark = true)

@Composable
fun CloudBackground(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val drift = rememberBackdropDrift(animate = !isLowEndDevice)
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.background,
                    colors.surface.copy(alpha = 0.42f),
                    colors.background,
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        if (!isLowEndDevice) {
            val wave = sin(drift * (PI * 2f).toFloat())
            cloud(
                center = Offset(size.width * 0.2f + wave * size.width * 0.025f, size.height * 0.2f),
                radius = size.minDimension * 0.16f,
                color = colors.surface.copy(alpha = 0.48f),
            )
            cloud(
                center = Offset(size.width * 0.78f - wave * size.width * 0.018f, size.height * 0.56f),
                radius = size.minDimension * 0.21f,
                color = colors.surface.copy(alpha = 0.3f),
            )
            cloud(
                center = Offset(size.width * 0.38f + wave * size.width * 0.012f, size.height * 0.86f),
                radius = size.minDimension * 0.12f,
                color = colors.surface.copy(alpha = 0.2f),
            )
        }
    }
}

private fun DrawScope.cloud(center: Offset, radius: Float, color: androidx.compose.ui.graphics.Color) {
    val path = Path().apply {
        addOval(
            Rect(
                center.x - radius * 1.55f,
                center.y - radius * 0.55f,
                center.x + radius * 0.15f,
                center.y + radius * 0.65f,
            ),
        )
        addOval(
            Rect(
                center.x - radius * 0.7f,
                center.y - radius,
                center.x + radius * 0.8f,
                center.y + radius * 0.65f,
            ),
        )
        addOval(
            Rect(
                center.x - radius * 0.1f,
                center.y - radius * 0.55f,
                center.x + radius * 1.6f,
                center.y + radius * 0.65f,
            ),
        )
    }
    drawPath(path = path, color = color)
}
