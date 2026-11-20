package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.core.settings.AppThemeMode
import io.github.we007oneday_byte.softseek.core.settings.CodeTheme
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

data class SoftSeekUiSettings(
    val visualStyle: VisualStyle = VisualStyle.Minimal,
    val layout: StyleLayout = StyleLayout(),
    val codeTheme: CodeTheme = CodeTheme.FOLLOW_THEME,
    val isDark: Boolean = false,
) {
    val bubbleRadiusDp: Int get() = layout.bubbleRadiusDp
    val cardRadiusDp: Int get() = layout.cardRadiusDp
    val cardPaddingDp: Int get() = layout.cardPaddingDp
    val spacingDp: Int get() = layout.spacingDp
    val borderWidthDp: Float get() = layout.borderWidthDp
    val elevationDp: Int get() = layout.elevationDp
    val screenHorizontalPaddingDp: Int get() = layout.screenHorizontalPaddingDp
    val sectionVerticalPaddingDp: Int get() = layout.sectionVerticalPaddingDp
    val sectionHeaderHorizontalPaddingDp: Int get() = layout.sectionHeaderHorizontalPaddingDp
    val sectionHeaderVerticalPaddingDp: Int get() = layout.sectionHeaderVerticalPaddingDp
    val settingItemHorizontalPaddingDp: Int get() = layout.settingItemHorizontalPaddingDp
    val settingItemVerticalPaddingDp: Int get() = layout.settingItemVerticalPaddingDp
    val smallSpacingDp: Int get() = layout.smallSpacingDp
    val microSpacingDp: Int get() = layout.microSpacingDp
    val iconSizeDp: Int get() = layout.iconSizeDp
    val iconButtonSizeDp: Int get() = layout.iconButtonSizeDp
    val buttonMinHeightDp: Int get() = layout.buttonMinHeightDp
    val buttonHorizontalPaddingDp: Int get() = layout.buttonHorizontalPaddingDp
    val buttonVerticalPaddingDp: Int get() = layout.buttonVerticalPaddingDp
    val dividerThicknessDp: Float get() = layout.dividerThicknessDp
    val inputRadiusDp: Int get() = layout.inputRadiusDp
}

val LocalSoftSeekUiSettings = staticCompositionLocalOf { SoftSeekUiSettings() }

val LocalSoftSeekSpec = staticCompositionLocalOf { MinimalSpec }

val LocalSoftSeekDark = staticCompositionLocalOf { false }

val MaterialTheme.softSeekColors: SoftSeekExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSoftSeekColors.current

@Composable
fun SoftSeekTheme(
    settings: AppSettings = AppSettings(),
    isLowEndDeviceOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val spec = visualStyleSpec(settings.visualStyle)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> spec.forceDarkMode || systemDark
        AppThemeMode.LIGHT -> spec.forceDarkMode
        AppThemeMode.DARK -> true
    }

    val targetScheme = if (darkTheme) {
        darkSchemeFor(settings.visualStyle)
    } else {
        lightSchemeFor(settings.visualStyle)
    }

    // 只对任务书指定的五个关键字段做平滑过渡，其余字段立即切换。
    val animatedBackground by animateColorAsState(
        targetValue = targetScheme.background,
        animationSpec = tween(360),
        label = "style-background",
    )
    val animatedSurface by animateColorAsState(
        targetValue = targetScheme.surface,
        animationSpec = tween(360),
        label = "style-surface",
    )
    val animatedPrimary by animateColorAsState(
        targetValue = targetScheme.primary,
        animationSpec = tween(360),
        label = "style-primary",
    )
    val animatedOnBackground by animateColorAsState(
        targetValue = targetScheme.onBackground,
        animationSpec = tween(360),
        label = "style-on-background",
    )
    val animatedOnSurface by animateColorAsState(
        targetValue = targetScheme.onSurface,
        animationSpec = tween(360),
        label = "style-on-surface",
    )

    val colorScheme = targetScheme.copy(
        background = animatedBackground,
        surface = animatedSurface,
        primary = animatedPrimary,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
    )

    val extendedColors = SoftSeekExtendedColors(
        userBubble = spec.light.userBubble.takeUnless { darkTheme } ?: spec.dark.userBubble,
        aiBubble = spec.light.aiBubble.takeUnless { darkTheme } ?: spec.dark.aiBubble,
        thinkingBackground = spec.light.thinking.takeUnless { darkTheme } ?: spec.dark.thinking,
        textPrimary = animatedOnSurface,
        textSecondary = animatedOnSurface.copy(alpha = 0.62f),
        divider = colorScheme.outline,
        codeBackground = spec.light.code.takeUnless { darkTheme } ?: spec.dark.code,
    )

    // 用户手动调过气泡圆角时以用户值为准；默认 16 视为未调整，跟随主题 spec。
    val bubbleRadius = settings.bubbleRadiusDp.takeIf { it != 16 } ?: spec.layout.bubbleRadiusDp
    val layout = spec.layout.copy(bubbleRadiusDp = bubbleRadius)
    val detectedLowEnd = rememberIsLowEndDevice()
    val lowEnd = isLowEndDeviceOverride ?: detectedLowEnd

    CompositionLocalProvider(
        LocalSoftSeekColors provides extendedColors,
        LocalSoftSeekSpec provides spec,
        LocalSoftSeekDark provides darkTheme,
        LocalIsLowEndDevice provides lowEnd,
        LocalSoftSeekUiSettings provides SoftSeekUiSettings(
            visualStyle = settings.visualStyle,
            layout = layout,
            codeTheme = settings.codeTheme,
            isDark = darkTheme,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(SoftSeekTypography, settings.fontScale)
                .withFontFamily(spec.fontFamily),
            content = content,
        )
    }
}

private fun lightSchemeFor(style: VisualStyle): ColorScheme = when (style) {
    VisualStyle.Minimal -> buildMinimalLightScheme()
    VisualStyle.Cloud -> buildCloudLightScheme()
    VisualStyle.Pudding -> buildPuddingLightScheme()
    VisualStyle.Mono -> buildMonoLightScheme()
    VisualStyle.Taiji -> buildTaijiLightScheme()
    VisualStyle.Hacker -> buildHackerLightScheme()
    VisualStyle.LineArt -> buildLineArtLightScheme()
    VisualStyle.Candy -> buildCandyLightScheme()
    VisualStyle.Neon -> buildNeonLightScheme()
    VisualStyle.PaperInk -> buildPaperInkLightScheme()
}

private fun darkSchemeFor(style: VisualStyle): ColorScheme = when (style) {
    VisualStyle.Minimal -> buildMinimalDarkScheme()
    VisualStyle.Cloud -> buildCloudDarkScheme()
    VisualStyle.Pudding -> buildPuddingDarkScheme()
    VisualStyle.Mono -> buildMonoDarkScheme()
    VisualStyle.Taiji -> buildTaijiDarkScheme()
    VisualStyle.Hacker -> buildHackerDarkScheme()
    VisualStyle.LineArt -> buildLineArtDarkScheme()
    VisualStyle.Candy -> buildCandyDarkScheme()
    VisualStyle.Neon -> buildNeonDarkScheme()
    VisualStyle.PaperInk -> buildPaperInkDarkScheme()
}
