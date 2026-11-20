package io.github.we007oneday_byte.softseek.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun StyleAwareCard(
    modifier: Modifier = Modifier,
    backdropBlur: Boolean? = null,
    shape: Shape? = null,
    contentPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = LocalSoftSeekSpec.current
    val colors = MaterialTheme.colorScheme
    val lowEnd = LocalIsLowEndDevice.current
    val cardShape = shape ?: RoundedCornerShape(spec.layout.cardRadiusDp.dp)
    val blurEnabled = (backdropBlur ?: spec.defaultBackdropBlur) && !lowEnd
    val fill = when (spec.cardPaint) {
        StyleSurfacePaint.Solid -> colors.surface
        StyleSurfacePaint.Translucent -> colors.surface.copy(alpha = 0.84f)
        StyleSurfacePaint.StrokeOnly -> Color.Transparent
    }.let { base ->
        if (blurEnabled) colors.surface.copy(alpha = 0.42f) else base
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = spec.layout.elevationDp.dp,
                shape = cardShape,
                clip = false,
                ambientColor = colors.scrim.copy(alpha = 0.18f),
                spotColor = colors.scrim.copy(alpha = 0.22f),
            )
            .styleGlow(spec)
            .clip(cardShape)
            .styleBorder(spec, cardShape, fill == Color.Transparent),
    ) {
        if (blurEnabled) {
            // 只模糊背景副本，卡片上的文字和内容不经过 Modifier.blur。
            // 必须用 matchParentSize，不能用 fillMaxSize：后者会把卡片撑到父布局最大高度，
            // 通话页底部按键会被挤出屏幕（糖果风格默认开启玻璃模糊时最明显）。
            SoftSeekBackdrop(
                modifier = Modifier
                    .matchParentSize()
                    .blur(spec.layout.blurRadiusDp.dp),
                isLowEndDevice = true,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.surface.copy(alpha = 0.28f)),
            )
        }
        if (spec.decorativeTape && !lowEnd) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-spec.layout.microSpacingDp).dp)
                    .width((spec.layout.spacingDp * 4).dp)
                    .height(spec.layout.microSpacingDp.dp)
                    .rotate(-4f)
                    .background(colors.secondary.copy(alpha = 0.28f)),
            )
        }
        Box(
            modifier = Modifier
                .padding(if (contentPadding) spec.layout.cardPaddingDp.dp else 0.dp),
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colors.onSurface,
            ) {
                content()
            }
        }
    }
}

@Composable
fun StyleAwareBubble(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val spec = LocalSoftSeekSpec.current
    val colors = MaterialTheme.colorScheme
    val styleColors = if (LocalSoftSeekDark.current) spec.dark else spec.light
    val radius = spec.layout.bubbleRadiusDp.dp
    val tail = spec.layout.bubbleTailRadiusDp.dp
    val shape = if (spec.bubblePill) {
        RoundedCornerShape(percent = 50)
    } else if (isMine) {
        RoundedCornerShape(radius, radius, tail, radius)
    } else {
        RoundedCornerShape(radius, radius, radius, tail)
    }
    val fill = when (spec.bubblePaint) {
        StyleSurfacePaint.Solid -> if (isMine) colors.primary else styleColors.aiBubble
        StyleSurfacePaint.Translucent -> {
            if (isMine) colors.primary.copy(alpha = 0.88f) else styleColors.aiBubble.copy(alpha = 0.82f)
        }
        StyleSurfacePaint.StrokeOnly -> Color.Transparent
    }
    val contentColor = if (isMine && spec.bubblePaint != StyleSurfacePaint.StrokeOnly) {
        colors.onPrimary
    } else if (spec.bubblePaint == StyleSurfacePaint.StrokeOnly) {
        colors.primary
    } else {
        colors.onSurface
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .styleBorder(spec, shape, fill == Color.Transparent),
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            Box(Modifier.padding(spec.layout.bubblePaddingDp.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StyleAwareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    contentPadding: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val spec = LocalSoftSeekSpec.current
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "style-button-press",
    )

    val shape = when (spec.buttonPaint) {
        StyleButtonPaint.StrokeOnly -> RoundedCornerShape(spec.layout.borderWidthDp.dp)
        else -> if (spec.buttonPill) {
            RoundedCornerShape(percent = 50)
        } else {
            RoundedCornerShape(spec.layout.cardRadiusDp.dp)
        }
    }
    val fill = when (spec.buttonPaint) {
        StyleButtonPaint.SolidPrimary -> if (primary) colors.primary else colors.surfaceVariant
        StyleButtonPaint.CandyGradient -> Brush.horizontalGradient(
            listOf(colors.primary, colors.secondary, colors.tertiary),
        )
        StyleButtonPaint.StrokeOnly -> Color.Transparent
        StyleButtonPaint.Glow -> if (primary) {
            colors.primary.copy(alpha = 0.16f)
        } else {
            colors.surface.copy(alpha = 0.16f)
        }
    }
    val contentColor = when (spec.buttonPaint) {
        StyleButtonPaint.StrokeOnly -> colors.primary
        StyleButtonPaint.CandyGradient -> colors.onPrimary
        StyleButtonPaint.Glow -> colors.primary
        StyleButtonPaint.SolidPrimary -> if (primary) colors.onPrimary else colors.onSurface
    }

    Box(
        modifier = modifier
            .scale(scale)
            .styleGlow(spec)
            .clip(shape)
            .styleButtonFill(fill, shape)
            .styleBorder(spec, shape, fill == Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = spec.layout.buttonMinHeightDp.dp)
            .padding(
                horizontal = if (contentPadding) spec.layout.buttonHorizontalPaddingDp.dp else 0.dp,
                vertical = if (contentPadding) spec.layout.buttonVerticalPaddingDp.dp else 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun StyleAwareIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    content: @Composable () -> Unit,
) {
    val spec = LocalSoftSeekSpec.current
    StyleAwareButton(
        onClick = onClick,
        modifier = modifier.size(spec.layout.iconButtonSizeDp.dp),
        enabled = enabled,
        primary = primary,
        contentPadding = false,
    ) {
        content()
    }
}

@Composable
fun StyleAwareSettingItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val spec = LocalSoftSeekSpec.current
    val colors = MaterialTheme.colorScheme
    val clickableModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(
                horizontal = spec.layout.settingItemHorizontalPaddingDp.dp,
                vertical = spec.layout.settingItemVerticalPaddingDp.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(spec.layout.iconSizeDp.dp),
                tint = colors.onSurfaceVariant,
            )
            Spacer(Modifier.width(spec.layout.itemSpacingDp.dp))
        }
        ColumnScopeContent(
            title = title,
            subtitle = subtitle,
            colors = colors,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun ColumnScopeContent(
    title: String,
    subtitle: String?,
    colors: androidx.compose.material3.ColorScheme,
    modifier: Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(LocalSoftSeekSpec.current.layout.microSpacingDp.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
fun StyleAwareDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    val spec = LocalSoftSeekSpec.current
    androidx.compose.foundation.Canvas(modifier = modifier.height(spec.layout.dividerThicknessDp.dp)) {
        if (spec.borderPaint == StyleBorderPaint.Solid && spec.cardPaint == StyleSurfacePaint.StrokeOnly) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = spec.layout.dividerThicknessDp.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
            )
        } else {
            drawRect(color = color)
        }
    }
}

@Composable
fun SoftSeekButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    StyleAwareButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        primary = primary,
        content = content,
    )
}

@Composable
fun SoftSeekIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    content: @Composable () -> Unit,
) {
    StyleAwareIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        primary = primary,
        content = content,
    )
}

private fun Modifier.styleButtonFill(fill: Any, shape: Shape): Modifier = when (fill) {
    is Color -> background(fill, shape)
    is Brush -> background(fill, shape)
    else -> this
}

@Composable
private fun Modifier.styleBorder(
    spec: VisualStyleSpec,
    shape: Shape,
    transparentFill: Boolean,
): Modifier {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val outline = colors.outline
    return when (spec.borderPaint) {
        StyleBorderPaint.None -> this
        StyleBorderPaint.Solid -> border(
            width = spec.layout.borderWidthDp.dp,
            color = if (transparentFill) primary else outline,
            shape = shape,
        )
        StyleBorderPaint.InkGradient -> drawWithContent {
            drawContent()
            if (size.width <= 0f || size.height <= 0f) return@drawWithContent
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        primary.copy(alpha = 0.55f),
                        colors.secondary.copy(alpha = 0.35f),
                        primary.copy(alpha = 0.55f),
                    ),
                ),
                style = Stroke(width = spec.layout.borderWidthDp.dp.toPx()),
                cornerRadius = CornerRadius(spec.layout.cardRadiusDp.dp.toPx()),
            )
        }
        StyleBorderPaint.Glow -> border(
            width = spec.layout.borderWidthDp.dp,
            color = primary.copy(alpha = 0.75f),
            shape = shape,
        )
    }
}

@Composable
private fun Modifier.styleGlow(spec: VisualStyleSpec): Modifier {
    val lowEnd = LocalIsLowEndDevice.current
    if (spec.borderPaint != StyleBorderPaint.Glow || lowEnd) return this
    val primary = MaterialTheme.colorScheme.primary
    return drawWithContent {
        if (size.width <= 0f || size.height <= 0f) {
            drawContent()
            return@drawWithContent
        }
        val glow = spec.layout.glowRadiusDp.dp.toPx()
        val baseRadius = spec.layout.cardRadiusDp.dp.toPx()
        repeat(3) { index ->
            val spread = glow * (index + 1) / 3f
            drawRoundRect(
                color = primary.copy(alpha = 0.10f / (index + 1)),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2f, size.height + spread * 2f),
                cornerRadius = CornerRadius(baseRadius + spread),
                style = Stroke(width = spread),
            )
        }
        drawContent()
    }
}
