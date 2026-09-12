package com.flowtune.tv.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 退出确认：继续后台播放（moveTaskToBack，音乐不断）或退出软件（暂停+结束）。 */
@Composable
fun ExitConfirmDialog(
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? Activity
    BackHandler(onBack = onDismiss)
    val bgFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        runCatching { bgFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99101014)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(480.dp)
                .background(Color(0xFF1F1F23), RoundedCornerShape(16.dp))
                .padding(horizontal = 32.dp, vertical = 30.dp)
        ) {
            Text("退出 FlowTune？", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "继续播放将回到电视桌面，音乐不会中断；\n退出软件会停止播放并关闭应用。",
                color = Color(0xFF9A9AA0), fontSize = 13.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ExitButton("继续播放", focusRequester = bgFocus) {
                    activity?.moveTaskToBack(true)
                    onDismiss()
                }
                ExitButton("退出软件", danger = true) {
                    activity?.finishAffinity()
                }
            }
        }
    }
}

@Composable
private fun ExitButton(
    label: String,
    danger: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .background(
                when {
                    focused -> if (danger) Color(0xFFD84A4A) else FocusBg
                    danger -> Color(0xFF3A2325)
                    else -> Color(0xFF2E2E33)
                },
                shape
            )
            .tvFocusGlow(focused, shape)
            .clickable { onClick() }
            .padding(horizontal = 26.dp, vertical = 12.dp)
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}
