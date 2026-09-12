package com.flowtune.tv.ui

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * TV 焦点高亮统一规范：蓝底白字 + 外围光圈。
 * 用户反馈纯蓝底在电视上仍不够醒目，光圈 = 最外 3dp 白线 + 其外 8dp 半透明蓝晕。
 */
val FocusBg = Color(0xFF4F8CFF)
val FocusGlow = Color(0xFF4F8CFF)

/** 聚焦时叠加白线+光圈；border 画在 bounds 内，0dp 时不影响布局。 */
fun Modifier.tvFocusGlow(focused: Boolean, shape: Shape): Modifier =
    this
        .border(if (focused) 8.dp else 0.dp, FocusGlow.copy(alpha = 0.40f), shape)
        .border(if (focused) 3.dp else 0.dp, Color.White, shape)
