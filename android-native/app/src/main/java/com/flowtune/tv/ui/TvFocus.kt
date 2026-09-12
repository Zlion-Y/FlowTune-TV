package com.flowtune.tv.ui

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * TV 焦点光圈：仅在当前聚焦的控件上出现（2dp 白线 + 4dp 蓝晕）。
 * 必须用条件挂载而非 border(0.dp)——0dp border 会按 1px hairline 渲染，
 * 导致所有控件常亮细边。
 */
val FocusBg = Color(0xFF4F8CFF)
val FocusGlow = Color(0xFF4F8CFF)

fun Modifier.tvFocusGlow(focused: Boolean, shape: Shape): Modifier =
    if (focused) this
        .border(4.dp, FocusGlow.copy(alpha = 0.55f), shape)
        .border(2.dp, Color.White, shape)
    else this
