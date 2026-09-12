package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.Song

/** 播放队列覆盖层（右侧滑出风格，简化为全屏半透明面板）。 */
@Composable
fun QueueOverlay(
    queue: List<Song>,
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC111114))
            .focusable()
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && (e.key == Key.Back || e.key == Key.Escape)) {
                    onClose(); true
                } else false
            }
    ) {
        Column(Modifier.fillMaxSize().padding(48.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("播放队列（${queue.size}）", color = Color.White, fontSize = 20.sp)
                Text("返回键关闭", color = Color(0xFF88888E), fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            LazyColumn {
                itemsIndexed(queue) { idx, song ->
                    val active = idx == currentIndex
                    var focused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    active -> Color(0xFF1F3A5F)
                                    focused -> Color(0xFF2A2A2E)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onPlay(idx) }
                            .focusable()
                            .onFocusChanged { focused = it.isFocused }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("${idx + 1}", color = Color(0xFF88888E), fontSize = 13.sp, modifier = Modifier.width(40.dp))
                        Text(song.title, color = if (active) Color(0xFF4F8CFF) else Color(0xFFEDEDEF), fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(song.artist.ifBlank { "未知艺术家" }, color = Color(0xFF88888E), fontSize = 12.sp)
                        Spacer(Modifier.width(16.dp))
                        Text(formatTime(song.durationSec), color = Color(0xFF88888E), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
