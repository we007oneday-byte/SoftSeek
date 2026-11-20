package io.github.we007oneday_byte.softseek.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** 低端设备标记：由 ActivityManager 的内存等级判断一次，不在每帧重复查询。 */
val LocalIsLowEndDevice = staticCompositionLocalOf { false }

fun Context.detectLowEndDevice(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S
    return activityManager.isLowRamDevice || activityManager.memoryClass <= 128
}

@Composable
fun rememberIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.detectLowEndDevice() }
}

@Composable
internal fun rememberBackdropPhase(
    animate: Boolean,
    durationMillis: Int = 9000,
): Float {
    if (!animate) return 0f
    val transition = rememberInfiniteTransition(label = "style-backdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "style-backdrop-phase",
    )
    return phase
}

@Composable
internal fun rememberBackdropDrift(
    animate: Boolean,
    durationMillis: Int = 18000,
): Float {
    val drift = remember { Animatable(0f) }
    LaunchedEffect(animate, durationMillis) {
        if (!animate) {
            drift.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            drift.animateTo(1f, tween(durationMillis, easing = LinearEasing))
            drift.snapTo(0f)
        }
    }
    return drift.value
}
