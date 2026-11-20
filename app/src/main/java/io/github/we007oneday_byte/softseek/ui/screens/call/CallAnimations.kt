package io.github.we007oneday_byte.softseek.ui.screens.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.we007oneday_byte.softseek.core.settings.VoiceAnimationStyle
import io.github.we007oneday_byte.softseek.core.tts.TtsTextSplitter
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 独立通话收音可视化。volume 是 0..1 的展示电平，由调用方传入，
 * 不绑定 ASR / 麦克风业务对象；复用时可以通过滑块或预览改变 volume。
 */
@Composable
fun VoiceVisualizerCanvas(
    volume: Float,
    style: VoiceAnimationStyle,
    modifier: Modifier = Modifier,
    isLowEndDevice: Boolean = false,
) {
    val spec = LocalSoftSeekSpec.current
    val phase = if (isLowEndDevice) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "voice-visualizer")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "voice-phase",
        )
        value
    }
    val pulse = if (isLowEndDevice) {
        0.35f
    } else {
        val transition = rememberInfiniteTransition(label = "voice-pulse")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "voice-pulse-value",
        )
        value
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val level = volume.coerceIn(0f, 1f)

    if (style == VoiceAnimationStyle.ELASTIC) {
        Row(
            modifier = modifier.size(spec.layout.voiceVisualizerSizeDp.dp),
            horizontalArrangement = Arrangement.spacedBy(spec.layout.smallSpacingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("弹", "性", "字").forEachIndexed { index, glyph ->
                Text(
                    text = glyph,
                    style = MaterialTheme.typography.headlineMedium,
                    color = primary,
                    modifier = Modifier.offset(
                        y = (sin(phase + index * 1.4f) * (4f + level * 8f)).dp,
                    ),
                )
            }
        }
        return
    }

    Canvas(
        modifier = modifier.size(spec.layout.voiceVisualizerSizeDp.dp),
    ) {
        val center = this.center
        val maxR = size.minDimension * 0.42f
        when (style) {
            VoiceAnimationStyle.WAVE -> {
                val step = if (isLowEndDevice) 8f else 4f
                repeat(3) { index ->
                    val path = Path()
                    val amp = maxR * 0.13f * level * (1f - index * 0.22f)
                    var x = center.x - maxR
                    path.moveTo(x, center.y + (index - 1) * 12f)
                    while (x <= center.x + maxR) {
                        val y = center.y + (index - 1) * 12f +
                            sin((x - center.x) / maxR * PI.toFloat() * 3f + phase + index) * amp
                        path.lineTo(x, y)
                        x += step
                    }
                    drawPath(
                        path = path,
                        color = if (index == 0) primary else secondary.copy(alpha = 0.55f),
                        style = Stroke(width = 2f),
                    )
                }
            }

            VoiceAnimationStyle.PULSE -> {
                drawCircle(
                    color = primary.copy(alpha = 0.08f + 0.12f * pulse),
                    radius = maxR * (0.48f + 0.32f * pulse),
                )
                drawCircle(
                    color = primary.copy(alpha = 0.32f),
                    radius = maxR * (0.32f + level * 0.2f),
                    style = Stroke(width = 2f),
                )
                drawCircle(
                    color = primary.copy(alpha = 0.9f),
                    radius = maxR * 0.12f * (1f + level),
                )
            }

            VoiceAnimationStyle.RING -> {
                repeat(3) { index ->
                    drawCircle(
                        color = primary.copy(alpha = (0.7f - index * 0.16f) * (0.4f + level)),
                        radius = maxR * (0.3f + index * 0.18f),
                        style = Stroke(width = 1.5f),
                    )
                }
                drawArc(
                    color = primary,
                    startAngle = (phase * 57.2958f) % 360f,
                    sweepAngle = 86f,
                    useCenter = false,
                    style = Stroke(width = 3f),
                )
            }

            VoiceAnimationStyle.PARTICLES -> {
                val count = if (isLowEndDevice) 8 else 20
                repeat(count) { index ->
                    val seed = abs(sin((index + 1) * 12.9898f) * 43758.5453f) % 1f
                    val angle = (index.toFloat() / count) * PI.toFloat() * 2f + phase * 0.28f
                    val radius = maxR * (0.25f + seed * 0.62f) + pulse * 6f
                    drawCircle(
                        color = if (index % 3 == 0) primary else secondary.copy(alpha = 0.7f),
                        radius = 1.6f + seed * 2.2f + level * 1.4f,
                        center = Offset(
                            x = center.x + cos(angle) * radius,
                            y = center.y + sin(angle) * radius,
                        ),
                    )
                }
            }

            VoiceAnimationStyle.SPECTRUM -> {
                val bars = if (isLowEndDevice) 11 else 23
                val gap = 4f
                val usable = size.width * 0.82f
                val barWidth = usable / bars - gap
                repeat(bars) { index ->
                    val wave = abs(sin(phase * 2f + index * 0.52f))
                    val height = 5f + wave * level * maxR * 0.9f
                    drawRoundRect(
                        color = if (index % 4 == 0) primary else primary.copy(alpha = 0.55f),
                        topLeft = Offset(
                            (size.width - usable) / 2f + index * (barWidth + gap),
                            center.y + maxR * 0.5f - height,
                        ),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }

            VoiceAnimationStyle.TYPEWRITER -> {
                val count = if (isLowEndDevice) 8 else 12
                val reveal = ((pulse * count).toInt() + 1).coerceIn(1, count)
                val width = size.width * 0.7f
                val left = (size.width - width) / 2f
                val block = width / count - 4f
                repeat(count) { index ->
                    drawRoundRect(
                        color = primary.copy(alpha = if (index < reveal) 0.85f else 0.08f),
                        topLeft = Offset(left + index * (block + 4f), center.y - 5f),
                        size = Size(block, 10f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                }
                drawLine(
                    color = primary,
                    start = Offset(left + reveal * (block + 4f), center.y - 18f),
                    end = Offset(left + reveal * (block + 4f), center.y + 18f),
                    strokeWidth = 2f,
                )
            }

            VoiceAnimationStyle.GLOW -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.32f * level + 0.1f),
                            primary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = maxR * 1.1f,
                    ),
                    radius = maxR * 1.1f,
                )
                drawCircle(
                    color = primary.copy(alpha = 0.9f),
                    radius = maxR * (0.13f + level * 0.1f),
                )
            }

            VoiceAnimationStyle.SCAN -> {
                drawRoundRect(
                    color = primary.copy(alpha = 0.4f),
                    topLeft = Offset(center.x - maxR * 0.72f, center.y - maxR * 0.52f),
                    size = Size(maxR * 1.44f, maxR * 1.04f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 1f),
                )
                drawLine(
                    color = primary.copy(alpha = 0.8f),
                    start = Offset(center.x - maxR * 0.68f, center.y + sin(phase) * maxR * 0.42f),
                    end = Offset(center.x + maxR * 0.68f, center.y + sin(phase) * maxR * 0.42f),
                    strokeWidth = 2f,
                )
            }

            VoiceAnimationStyle.ELASTIC -> {
                val count = 5
                val spacing = size.width * 0.9f / (count - 1)
                repeat(count) { index ->
                    val x = size.width * 0.05f + index * spacing
                    val offset = sin(phase * 2f + index * 1.4f) * maxR * 0.18f * level
                    drawCircle(
                        color = primary.copy(alpha = 0.9f - index * 0.08f),
                        radius = 4f + pulse * 2f,
                        center = Offset(x, center.y + offset),
                    )
                }
            }

            VoiceAnimationStyle.RIPPLES -> {
                val count = if (isLowEndDevice) 2 else 4
                repeat(count) { index ->
                    val progress = (pulse + index * 0.25f) % 1f
                    drawCircle(
                        color = primary.copy(alpha = (1f - progress) * 0.55f),
                        radius = maxR * (0.12f + progress * 0.82f),
                        style = Stroke(width = 2f),
                    )
                }
            }
        }
    }
}

@Composable
fun PartialTranscript(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = text.isNotBlank(),
        enter = fadeIn(tween(220)) +
            slideInVertically(tween(260)) { height -> height / 3 } +
            scaleIn(tween(260), initialScale = 0.96f),
        exit = androidx.compose.animation.fadeOut(tween(120)),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 3,
        )
    }
}

/**
 * 独立字幕区：只负责逐句入场和当前句高亮，不读取 ASR、TTS 或 ViewModel。
 */
@Composable
fun CallSubtitleArea(
    textList: List<String>,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
) {
    val spec = LocalSoftSeekSpec.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = spec.layout.subtitleMaxHeightDp.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        textList.forEachIndexed { index, line ->
            val state = remember(index, line) {
                MutableTransitionState(false).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn(tween(260)) +
                    slideInVertically(tween(280)) { height -> height / 4 } +
                    scaleIn(tween(280), initialScale = 0.97f),
            ) {
                Column {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (index == highlightIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        fontWeight = if (index == highlightIndex) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    )
                    Spacer(Modifier.height(spec.layout.microSpacingDp.dp))
                }
            }
        }
    }
}

@Composable
fun StreamingAssistantText(
    text: String,
    spokenSentenceIndex: Int,
    messageKey: String,
    modifier: Modifier = Modifier,
) {
    val spec = LocalSoftSeekSpec.current
    val sentences = remember(text, messageKey) { TtsTextSplitter.split(text) }
    val highlight = if (spokenSentenceIndex in sentences.indices) spokenSentenceIndex else -1

    Column(modifier = modifier.fillMaxWidth()) {
        if (sentences.isEmpty() && text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            CallSubtitleArea(
                textList = sentences,
                highlightIndex = highlight,
            )
        }
        if (text.isBlank()) {
            Text(
                text = "说完停顿一下，我会自动提交。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spec.layout.microSpacingDp.dp),
            )
        }
        Spacer(Modifier.height(spec.layout.spacingDp.dp))
    }
}
