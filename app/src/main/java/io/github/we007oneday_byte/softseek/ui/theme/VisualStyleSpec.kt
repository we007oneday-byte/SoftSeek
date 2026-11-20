package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

internal val StyleError = Color(0xFFE53935)
internal val StyleOnError = Color(0xFFFFFFFF)

/** 任务书要求所有主题的 error 固定为 #E53935。 */
data class StyleColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val userBubble: Color,
    val aiBubble: Color,
    val thinking: Color,
    val code: Color,
    val error: Color = StyleError,
    val onError: Color = StyleOnError,
    val scrim: Color = Color.Black,
)

/**
 * 主题布局 token。所有圆角 / 间距 / 边框 / elevation 从这里派生，
 * 通用组件不直接写字面量 dp。
 */
data class StyleLayout(
    val cardRadiusDp: Int = 12,
    val bubbleRadiusDp: Int = 10,
    val spacingDp: Int = 12,
    val borderWidthDp: Float = 0.5f,
    val elevationDp: Int = 0,
) {
    val screenHorizontalPaddingDp: Int
        get() = when {
            spacingDp >= 20 -> 24
            spacingDp >= 18 -> 20
            spacingDp >= 14 -> 16
            else -> 12
        }

    val sectionVerticalPaddingDp: Int get() = maxOf(4, spacingDp / 3)
    val sectionHeaderHorizontalPaddingDp: Int get() = spacingDp + 4
    val sectionHeaderVerticalPaddingDp: Int get() = spacingDp + 2
    val settingItemHorizontalPaddingDp: Int get() = spacingDp + 4
    val settingItemVerticalPaddingDp: Int get() = maxOf(10, spacingDp)
    val cardPaddingDp: Int get() = spacingDp + 2
    val bubblePaddingDp: Int get() = spacingDp
    val smallSpacingDp: Int get() = maxOf(4, spacingDp / 3)
    val microSpacingDp: Int get() = maxOf(2, spacingDp / 6)
    val itemSpacingDp: Int get() = maxOf(6, spacingDp / 2)
    val iconSizeDp: Int get() = 20
    val iconButtonSizeDp: Int get() = 44
    val buttonMinHeightDp: Int get() = if (spacingDp >= 18) 48 else 44
    val buttonHorizontalPaddingDp: Int get() = spacingDp + 2
    val buttonVerticalPaddingDp: Int get() = maxOf(8, spacingDp - 4)
    val pillButtons: Boolean get() = cardRadiusDp >= 20
    val dividerThicknessDp: Float get() = if (borderWidthDp >= 1f) 1f else 0.5f
    val glowRadiusDp: Int get() = if (spacingDp <= 10) 8 else 12
    val blurRadiusDp: Int get() = 16
    val voiceVisualizerSizeDp: Int get() = 160
    val subtitleMaxHeightDp: Int get() = 220
    val inputRadiusDp: Int get() = bubbleRadiusDp
    val imageCornerRadiusDp: Int get() = maxOf(8, cardRadiusDp / 2)
    val bubbleTailRadiusDp: Int get() = maxOf(2, bubbleRadiusDp / 4)
}

enum class StyleSurfacePaint {
    Solid,
    Translucent,
    StrokeOnly,
}

enum class StyleButtonPaint {
    SolidPrimary,
    CandyGradient,
    StrokeOnly,
    Glow,
}

enum class StyleBorderPaint {
    None,
    Solid,
    InkGradient,
    Glow,
}

data class VisualStyleSpec(
    val style: VisualStyle,
    val forceDarkMode: Boolean,
    val light: StyleColors,
    val dark: StyleColors,
    val layout: StyleLayout,
    val cardPaint: StyleSurfacePaint,
    val bubblePaint: StyleSurfacePaint,
    val buttonPaint: StyleButtonPaint,
    val borderPaint: StyleBorderPaint,
    val buttonPill: Boolean = false,
    val bubblePill: Boolean = false,
    val decorativeTape: Boolean = false,
    val defaultBackdropBlur: Boolean = false,
    val fontFamily: FontFamily? = null,
)

internal fun styleColors(
    primary: Long,
    onPrimary: Long,
    background: Long,
    onBackground: Long,
    surface: Long,
    onSurface: Long,
    secondary: Long,
    tertiary: Long,
    surfaceVariant: Long,
    onSurfaceVariant: Long,
    outline: Long,
    userBubble: Long = primary,
    aiBubble: Long = surfaceVariant,
    thinking: Long = surfaceVariant,
    code: Long = surfaceVariant,
    primaryContainer: Long = primary,
    onPrimaryContainer: Long = onPrimary,
    onSecondary: Long = onPrimary,
    secondaryContainer: Long = surfaceVariant,
    onSecondaryContainer: Long = onSurface,
    onTertiary: Long = onPrimary,
    tertiaryContainer: Long = surfaceVariant,
    onTertiaryContainer: Long = onSurface,
): StyleColors = StyleColors(
    primary = Color(primary),
    onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary),
    onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(tertiary),
    onTertiary = Color(onTertiary),
    tertiaryContainer = Color(tertiaryContainer),
    onTertiaryContainer = Color(onTertiaryContainer),
    background = Color(background),
    onBackground = Color(onBackground),
    surface = Color(surface),
    onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant),
    onSurfaceVariant = Color(onSurfaceVariant),
    outline = Color(outline),
    userBubble = Color(userBubble),
    aiBubble = Color(aiBubble),
    thinking = Color(thinking),
    code = Color(code),
)

internal fun layout(
    card: Int,
    bubble: Int,
    spacing: Int,
    border: Float,
    elevation: Int,
): StyleLayout = StyleLayout(
    cardRadiusDp = card,
    bubbleRadiusDp = bubble,
    spacingDp = spacing,
    borderWidthDp = border,
    elevationDp = elevation,
)

/**
 * 唯一按 VisualStyle 分发 spec 的入口。新增枚举成员时编译器会强制补齐。
 */
fun visualStyleSpec(style: VisualStyle): VisualStyleSpec = when (style) {
    VisualStyle.Minimal -> MinimalSpec
    VisualStyle.Cloud -> CloudSpec
    VisualStyle.Pudding -> PuddingSpec
    VisualStyle.Mono -> MonoSpec
    VisualStyle.Taiji -> TaijiSpec
    VisualStyle.Hacker -> HackerSpec
    VisualStyle.LineArt -> LineArtSpec
    VisualStyle.Candy -> CandySpec
    VisualStyle.Neon -> NeonSpec
    VisualStyle.PaperInk -> PaperInkSpec
}
