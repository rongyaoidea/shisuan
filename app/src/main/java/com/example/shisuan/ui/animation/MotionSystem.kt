package com.example.shisuan.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 物理动效系统 — 弹簧/惯性/按压反馈
 *
 * 所有动效基于物理参数（阻尼、刚度、惯性），而非线性时间插值，
 * 让交互有「重量感」：
 *  - 按压：按下弹压缩小，松手弹簧回弹（带轻微过冲）
 *  - 入场：卡片带弹性进场（从位移+缩放弹入）
 */

// ── 弹簧参数（物理常量） ──
object Springs {
    /** 按压回弹：刚性强、阻尼适中，短促有力 */
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    /** 入场弹性：低刚度、低阻尼，明显的过冲弹跳 */
    val entrance = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    /** 离场阻尼：高阻尼平滑滑出 */
    val exit = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * 按压缩放反馈 — 按下缩小到 0.96，松手弹簧回弹，并触发 onClick
 *
 * @param onLongClick 长按回调（可选），用于卡片级辅助操作（如删除入口）；
 *   与点击共用同一 interactionSource，长按期间同样生效缩放反馈
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressScale(
    onClick: (() -> Unit)? = null,
    pressedScale: Float = 0.96f,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = Springs.press,
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = true,
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
}

/**
 * 入场动画 — 从 24dp 位移 + 透明度 0 弹性弹入
 */
@Composable
fun Modifier.entranceAnimation(
    index: Int = 0,
    delayPerItem: Int = 60
): Modifier {
    val offsetY = remember { Animatable(24f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(index) {
        delay((index * delayPerItem).toLong())
        launch { alpha.animateTo(1f, tween(200)) }
        offsetY.animateTo(0f, Springs.entrance)
    }

    return this.graphicsLayer {
        translationY = offsetY.value
        this.alpha = alpha.value
    }
}

/**
 * 数字滚动 — 数值变化时平滑滑向新值（无过冲，金钱数值回弹会显轻浮）。
 *
 * 初次组合直接显示终值（不从 0 起播，避免列表滚动时满屏乱滚）；
 * 只响应后续 value 变化，如原料改价后吨价的滑升/滑降。
 */
@Composable
fun AnimatedNumber(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified
) {
    val displayed = remember { mutableDoubleStateOf(value) }
    LaunchedEffect(value) {
        val from = displayed.doubleValue
        if (from != value) {
            Animatable(0f).animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                displayed.doubleValue = from + (value - from) * this.value
            }
        }
    }
    Text(
        format(displayed.doubleValue),
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}
