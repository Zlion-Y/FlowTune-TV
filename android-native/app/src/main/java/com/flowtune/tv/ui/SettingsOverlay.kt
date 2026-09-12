package com.flowtune.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.EffectLevel

/** 设置覆盖层：动效档位是 TV 核心设置。 */
@Composable
fun SettingsOverlay(
    state: AppState,
    onClose: () -> Unit,
) {
    val settings by state.config.settings.collectAsState()

    BackHandler(onBack = onClose)
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { firstFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF14161A))
            .tvBackToClose(onClose)
    ) {
        Column(Modifier.fillMaxSize().padding(48.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("设置", color = Color.White, fontSize = 20.sp)
                Text("返回键关闭", color = Color(0xFF88888E), fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
            LazyColumn {
                item {
                    Text("音源管理（LX 脚本，扫描 /sdcard/Download 与 /sdcard）", color = Color(0xFF9A9AA0), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    SourceManager(state, firstFocus = firstFocus)
                    Spacer(Modifier.height(28.dp))
                    var autoOpenFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onFocusChanged { autoOpenFocused = it.isFocused }
                            .background(Color(0xFF1F1F23), RoundedCornerShape(12.dp))
                            .tvFocusGlow(autoOpenFocused, RoundedCornerShape(12.dp))
                            .clickable {
                                state.config.updateSettings { it.copy(autoOpenPlayer = !it.autoOpenPlayer) }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "播放歌曲自动进入播放页：" + if (settings.autoOpenPlayer) "开" else "关",
                            color = Color.White, fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
