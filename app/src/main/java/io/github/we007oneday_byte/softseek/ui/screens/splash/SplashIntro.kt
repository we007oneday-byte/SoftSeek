package io.github.we007oneday_byte.softseek.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.we007oneday_byte.softseek.R
import kotlinx.coroutines.delay

/**
 * ZL-01-22：系统 Splash 之后的品牌开屏。
 * 只做 0.8～1.2 秒的 SK 标记缩放/淡出，可点击跳过；不阻塞进入。
 */
@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.88f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        delay(160)
        alpha.animateTo(0f, tween(durationMillis = 220))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onFinished() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(188.dp)
                    .alpha(alpha.value)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "SOFTSEEK",
                color = Color.White.copy(alpha = textAlpha.value * 0.72f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 5.sp,
            )
        }
    }
}
