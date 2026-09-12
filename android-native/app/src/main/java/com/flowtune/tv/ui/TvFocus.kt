package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TV 焦点光圈：仅在当前聚焦的控件上出现（2dp 白线 + 4dp 蓝晕）。
 * 必须用条件挂载而非 border(0.dp)——0dp border 会按 1px hairline 渲染，
 * 导致所有控件常亮细边。
 */
val FocusBg = Color(0xFF4F8CFF)
val FocusGlow = Color(0xFF4F8CFF)
val TvShape = RoundedCornerShape(10.dp)

fun Modifier.tvFocusGlow(focused: Boolean, shape: Shape): Modifier =
    if (focused) this
        .border(4.dp, FocusGlow.copy(alpha = 0.55f), shape)
        .border(2.dp, Color.White, shape)
    else this

/** TV 统一圆角文字按钮：聚焦蓝底白字+光圈，与"我的喜欢"侧栏项同圆角风格。 */
@Composable
fun TvButton(
    label: String,
    modifier: Modifier = Modifier,
    container: Color = Color(0xFF2E2E33),
    contentColor: Color = Color.White,
    fontSize: TextUnit = 13.sp,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 9.dp,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) FocusBg else container, TvShape)
            .tvFocusGlow(focused, TvShape)
            .clickable { onClick() }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = contentColor, fontSize = fontSize, maxLines = 1)
    }
}

/** TV 统一圆角图标/内容键：与播放页控制键同款，聚焦蓝底+光圈。 */
@Composable
fun TvKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color(0x883A3A40),
    width: Dp = 52.dp,
    height: Dp = 44.dp,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) FocusBg else container, TvShape)
            .tvFocusGlow(focused, TvShape)
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = iconTint) else content()
    }
}
