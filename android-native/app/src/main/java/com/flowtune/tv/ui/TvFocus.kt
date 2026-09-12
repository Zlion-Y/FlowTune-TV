package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TV 焦点光圈（参考系统音乐库样式）：亮蓝描边 + 向外渐隐的柔光晕，
 * 聚焦时不改控件底色，只有光圈。多层递减 alpha 模拟柔光。
 * 必须条件挂载而非 border(0.dp)——0dp border 会按 1px hairline 渲染。
 */
val FocusGlow = Color(0xFF5B9BFF)
val TvShape = RoundedCornerShape(10.dp)

fun Modifier.tvFocusGlow(focused: Boolean, shape: Shape): Modifier =
    if (focused) this
        .border(10.dp, FocusGlow.copy(alpha = 0.10f), shape)
        .border(8.dp, FocusGlow.copy(alpha = 0.22f), shape)
        .border(5.dp, FocusGlow.copy(alpha = 0.45f), shape)
        .border(3.dp, FocusGlow, shape)
    else this

/** TV 统一圆角文字按钮：聚焦亮光圈（底色不变），与全局圆角风格一致。 */
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
            .background(container, TvShape)
            .tvFocusGlow(focused, TvShape)
            .clickable { onClick() }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = contentColor, fontSize = fontSize, maxLines = 1)
    }
}

/** TV 统一圆角图标/内容键：聚焦亮光圈（底色不变）。 */
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
            .background(container, TvShape)
            .tvFocusGlow(focused, TvShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = iconTint) else content()
    }
}
