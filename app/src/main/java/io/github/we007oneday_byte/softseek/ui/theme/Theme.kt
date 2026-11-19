package io.github.we007oneday_byte.softseek.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.github.we007oneday_byte.softseek.core.settings.AppSettings
import io.github.we007oneday_byte.softseek.core.settings.AppThemeMode
import io.github.we007oneday_byte.softseek.core.settings.CodeTheme

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

data class SoftSeekUiSettings(
    val bubbleRadiusDp: Int = 16,
    val codeTheme: CodeTheme = CodeTheme.FOLLOW_THEME,
)

val LocalSoftSeekUiSettings = staticCompositionLocalOf { SoftSeekUiSettings() }

val MaterialTheme.softSeekColors: SoftSeekExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSoftSeekColors.current

@Composable
fun SoftSeekTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val dynamicScheme = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val colorScheme = dynamicScheme ?: if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkSoftSeekExtendedColors else LightSoftSeekExtendedColors

    CompositionLocalProvider(
        LocalSoftSeekColors provides extendedColors,
        LocalSoftSeekUiSettings provides SoftSeekUiSettings(
            bubbleRadiusDp = settings.bubbleRadiusDp,
            codeTheme = settings.codeTheme,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(SoftSeekTypography, settings.fontScale),
            content = content,
        )
    }
}
