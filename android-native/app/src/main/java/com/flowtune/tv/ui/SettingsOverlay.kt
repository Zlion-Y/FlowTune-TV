package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.AppGraph

/** 设置覆盖层：动效档位是 TV 核心设置。 */
@Composable
fun SettingsOverlay(
    state: AppState,
    onClose: () -> Unit,
) {
    val settings by state.config.settings.collectAsState()

    BackHandler(onBack = onClose)
    val settingsFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { settingsFocus.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC111114))
            .focusRequester(settingsFocus)
            .focusable()

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
                    SourceManager(state)
                    Spacer(Modifier.height(28.dp))
                    Text("动效档位（按盒子性能调整）", color = Color(0xFF9A9AA0), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EffectLevel.entries.forEach { level ->
                            val selected = settings.effectLevel == level
                            var focused by remember { mutableStateOf(false) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(
                                        when {
                                            selected -> Color(0xFF4F8CFF)
                                            focused -> FocusBg
                                            else -> Color(0xFF1F1F23)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        state.config.updateSettings {
                                            it.copy(effectLevel = level, effectLevelTouched = true)
                                        }
                                    }
                                    .focusable()
                                    .onFocusChanged { focused = it.isFocused }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(level.label, color = Color.White, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    when (level) {
                                        EffectLevel.OFF -> "无动效·最省电"
                                        EffectLevel.LOW -> "封面倒影"
                                        EffectLevel.MEDIUM -> "逐字歌词·动态背景"
                                        EffectLevel.HIGH -> "全特效"
                                    },
                                    color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "档位说明：\n" +
                                "· 关闭：15fps 逐行歌词，无任何动画（老盒子推荐）\n" +
                                "· 低：20fps + 封面倒影\n" +
                                "· 中：30fps + 逐字弹簧歌词 + 动态背景\n" +
                                "· 高：45fps + 未唱行模糊 + 全部效果",
                        color = Color(0xFF88888E), fontSize = 12.sp
                    )
                }
            }
        }
    }
}
