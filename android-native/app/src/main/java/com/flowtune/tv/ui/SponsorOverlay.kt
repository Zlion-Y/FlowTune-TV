package com.flowtune.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 赞助作者覆盖层：展示微信/支付宝收款码，OK/返回关闭。 */
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
            .background(Color(0xF2111114))
            .focusRequester(closeFocus)
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFF1F1F23), RoundedCornerShape(16.dp))
                .padding(horizontal = 44.dp, vertical = 30.dp)
        ) {
            Text("赞助作者", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "FlowTune 免费开源，如果它给你带来了好的体验，欢迎请作者喝杯咖啡～",
                color = Color(0xFF9A9AA0), fontSize = 14.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(com.flowtune.tv.R.drawable.sponsor_wechat),
                        contentDescription = "微信收款码",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("微信", color = Color(0xFF4CC98A), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(com.flowtune.tv.R.drawable.sponsor_alipay),
                        contentDescription = "支付宝收款码",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("支付宝", color = Color(0xFF5B9BFF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("按 OK 或返回键关闭", color = Color(0xFF88888E), fontSize = 12.sp)
        }
    }
}
