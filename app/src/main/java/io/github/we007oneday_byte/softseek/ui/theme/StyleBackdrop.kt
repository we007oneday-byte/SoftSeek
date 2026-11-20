package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.we007oneday_byte.softseek.core.settings.VisualStyle

/**
 * 十套主题背景分发。每个分支只调用独立 ThemeXxx.kt 中的 Background，
 * 不使用 else，新增主题时由编译器强制提醒。
 */
@Composable
fun CurrentStyleBackground(
    style: VisualStyle,
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean,
) {
    when (style) {
        VisualStyle.Minimal -> MinimalBackground(modifier, isLowEndDevice)
        VisualStyle.Cloud -> CloudBackground(modifier, isLowEndDevice)
        VisualStyle.Pudding -> PuddingBackground(modifier, isLowEndDevice)
        VisualStyle.Mono -> MonoBackground(modifier, isLowEndDevice)
        VisualStyle.Taiji -> TaijiBackground(modifier, isLowEndDevice)
        VisualStyle.Hacker -> HackerBackground(modifier, isLowEndDevice)
        VisualStyle.LineArt -> LineArtBackground(modifier, isLowEndDevice)
        VisualStyle.Candy -> CandyBackground(modifier, isLowEndDevice)
        VisualStyle.Neon -> NeonBackground(modifier, isLowEndDevice)
        VisualStyle.PaperInk -> PaperInkBackground(modifier, isLowEndDevice)
    }
}

@Composable
fun SoftSeekBackdrop(
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean = LocalIsLowEndDevice.current,
) {
    CurrentStyleBackground(
        style = LocalSoftSeekUiSettings.current.visualStyle,
        modifier = modifier.fillMaxSize(),
        isLowEndDevice = isLowEndDevice,
    )
}
