package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary = SoftSeekPrimaryLight,
    onPrimary = SoftSeekSurfaceLight,
    primaryContainer = SoftSeekThinkingLight,
    onPrimaryContainer = SoftSeekTextPrimaryLight,
    secondary = SoftSeekPrimaryVariantLight,
    onSecondary = SoftSeekSurfaceLight,
    background = SoftSeekBackgroundLight,
    onBackground = SoftSeekTextPrimaryLight,
    surface = SoftSeekSurfaceLight,
    onSurface = SoftSeekTextPrimaryLight,
    surfaceVariant = SoftSeekAiBubbleLight,
    onSurfaceVariant = SoftSeekTextSecondaryLight,
    outline = SoftSeekDividerLight,
    error = SoftSeekErrorLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftSeekPrimaryDark,
    onPrimary = SoftSeekBackgroundDark,
    primaryContainer = SoftSeekThinkingDark,
    onPrimaryContainer = SoftSeekTextPrimaryDark,
    secondary = SoftSeekPrimaryVariantDark,
    onSecondary = SoftSeekBackgroundDark,
    background = SoftSeekBackgroundDark,
    onBackground = SoftSeekTextPrimaryDark,
    surface = SoftSeekSurfaceDark,
    onSurface = SoftSeekTextPrimaryDark,
    surfaceVariant = SoftSeekAiBubbleDark,
    onSurfaceVariant = SoftSeekTextSecondaryDark,
    outline = SoftSeekDividerDark,
    error = SoftSeekErrorDark,
)

val MaterialTheme.softSeekColors: SoftSeekExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSoftSeekColors.current

@Composable
fun SoftSeekTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = when (ThemeManager.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkSoftSeekExtendedColors else LightSoftSeekExtendedColors

    CompositionLocalProvider(LocalSoftSeekColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoftSeekTypography,
            content = content,
        )
    }
}
