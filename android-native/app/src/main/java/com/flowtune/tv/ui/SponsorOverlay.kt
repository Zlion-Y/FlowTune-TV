package com.flowtune.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val REPO_URL = "https://github.com/Zlion-Y/FlowTune-TV"

/** 赞助作者覆盖层：说明 + 仓库地址，OK/返回关闭。 */
@Composable
fun SponsorOverlay(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { closeFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC111114))
            .focusRequester(closeFocus)
            .focusable()
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .background(Color(0xFF1F1F23), RoundedCornerShape(16.dp))
                .padding(horizontal = 40.dp, vertical = 32.dp)
        ) {
            Text("赞助作者", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text(
                "FlowTune 是一款免费开源的 Android TV 音乐播放器，\n由作者在业余时间开发与维护。",
                color = Color(0xFFCFCFD4), fontSize = 15.sp,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "如果它给你带来了好的体验，欢迎通过\n微信 / 支付宝 赞助支持（金额随意）。",
                color = Color(0xFFCFCFD4), fontSize = 15.sp,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "收款码与最新版本请见项目主页：",
                color = Color(0xFF88888E), fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(REPO_URL, color = FocusBg, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Text("按 OK 或返回键关闭", color = Color(0xFF88888E), fontSize = 12.sp)
        }
    }
}
