package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 将完整主题色板映射为 Material3 ColorScheme。
 * 只在这里补齐 Material3 的结构字段，不在页面里散落颜色。
 */
internal fun StyleColors.toColorScheme(dark: Boolean): ColorScheme {
    val containerLowest = lerp(surface, onSurface, if (dark) 0.025f else 0.015f)
    val containerLow = lerp(surface, onSurface, if (dark) 0.05f else 0.035f)
    val container = lerp(surface, onSurface, if (dark) 0.075f else 0.055f)
    val containerHigh = lerp(surface, onSurface, if (dark) 0.11f else 0.075f)
    val containerHighest = lerp(surface, onSurface, if (dark) 0.15f else 0.10f)
    val surfaceDim = lerp(surface, background, if (dark) 0.12f else 0.04f)
    val surfaceBright = if (dark) lerp(surface, onSurface, 0.12f) else surface
    val errorContainer = lerp(surface, error, if (dark) 0.18f else 0.12f)
    val onErrorContainer = lerp(onSurface, onError, if (dark) 0.35f else 0.20f)

    val common = ColorSchemeFields(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.55f),
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        surfaceContainerLow = containerLow,
        surfaceContainerLowest = containerLowest,
    )

    return if (dark) {
        darkColorScheme(
            primary = common.primary,
            onPrimary = common.onPrimary,
            primaryContainer = common.primaryContainer,
            onPrimaryContainer = common.onPrimaryContainer,
            inversePrimary = common.inversePrimary,
            secondary = common.secondary,
            onSecondary = common.onSecondary,
            secondaryContainer = common.secondaryContainer,
            onSecondaryContainer = common.onSecondaryContainer,
            tertiary = common.tertiary,
            onTertiary = common.onTertiary,
            tertiaryContainer = common.tertiaryContainer,
            onTertiaryContainer = common.onTertiaryContainer,
            background = common.background,
            onBackground = common.onBackground,
            surface = common.surface,
            onSurface = common.onSurface,
            surfaceVariant = common.surfaceVariant,
            onSurfaceVariant = common.onSurfaceVariant,
            surfaceTint = common.surfaceTint,
            inverseSurface = common.inverseSurface,
            inverseOnSurface = common.inverseOnSurface,
            error = common.error,
            onError = common.onError,
            errorContainer = common.errorContainer,
            onErrorContainer = common.onErrorContainer,
            outline = common.outline,
            outlineVariant = common.outlineVariant,
            scrim = common.scrim,
            surfaceBright = common.surfaceBright,
            surfaceDim = common.surfaceDim,
            surfaceContainer = common.surfaceContainer,
            surfaceContainerHigh = common.surfaceContainerHigh,
            surfaceContainerHighest = common.surfaceContainerHighest,
            surfaceContainerLow = common.surfaceContainerLow,
            surfaceContainerLowest = common.surfaceContainerLowest,
        )
    } else {
        lightColorScheme(
            primary = common.primary,
            onPrimary = common.onPrimary,
            primaryContainer = common.primaryContainer,
            onPrimaryContainer = common.onPrimaryContainer,
            inversePrimary = common.inversePrimary,
            secondary = common.secondary,
            onSecondary = common.onSecondary,
            secondaryContainer = common.secondaryContainer,
            onSecondaryContainer = common.onSecondaryContainer,
            tertiary = common.tertiary,
            onTertiary = common.onTertiary,
            tertiaryContainer = common.tertiaryContainer,
            onTertiaryContainer = common.onTertiaryContainer,
            background = common.background,
            onBackground = common.onBackground,
            surface = common.surface,
            onSurface = common.onSurface,
            surfaceVariant = common.surfaceVariant,
            onSurfaceVariant = common.onSurfaceVariant,
            surfaceTint = common.surfaceTint,
            inverseSurface = common.inverseSurface,
            inverseOnSurface = common.inverseOnSurface,
            error = common.error,
            onError = common.onError,
            errorContainer = common.errorContainer,
            onErrorContainer = common.onErrorContainer,
            outline = common.outline,
            outlineVariant = common.outlineVariant,
            scrim = common.scrim,
            surfaceBright = common.surfaceBright,
            surfaceDim = common.surfaceDim,
            surfaceContainer = common.surfaceContainer,
            surfaceContainerHigh = common.surfaceContainerHigh,
            surfaceContainerHighest = common.surfaceContainerHighest,
            surfaceContainerLow = common.surfaceContainerLow,
            surfaceContainerLowest = common.surfaceContainerLowest,
        )
    }
}

private data class ColorSchemeFields(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
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
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
)
