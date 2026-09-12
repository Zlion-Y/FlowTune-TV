package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.Song
import com.flowtune.tv.player.PlayMode

/** 底部播放栏：横跨全宽，含进度条；歌曲信息区聚焦后 OK=打开全屏播放页，控制键 OK=对应功能。 */
@Composable
fun PlayerBar(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playMode: PlayMode,
    isLoading: Boolean = false,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onCycleMode: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B1F))
    ) {
        // 进度条
        val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF2A2A2E))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Color(0xFF4F8CFF))
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 歌曲信息（聚焦后 OK 打开全屏播放页）
            var infoFocused by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .width(220.dp)
                    .background(if (infoFocused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
                    .tvFocusGlow(infoFocused, RoundedCornerShape(10.dp))
                    .clickable { onOpenDetail() }
                    .focusable()
                    .onFocusChanged { infoFocused = it.isFocused }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    song?.title ?: "未播放",
                    color = Color.White, fontSize = 14.sp, maxLines = 1
                )
                Text(
                    song?.artist?.ifBlank { "未知艺术家" } ?: "未知艺术家",
                    color = if (infoFocused) Color.White.copy(alpha = 0.9f) else Color(0xFF88888E),
                    fontSize = 11.sp, maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            // 控制区
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCycleMode) {
                    Text(playMode.label.take(2), color = Color(0xFFB9B9BF), fontSize = 12.sp)
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color(0xFFEDEDEF))
                }
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF2E2E33), RoundedCornerShape(50))
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color(0xFFEDEDEF))
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = Color(0xFFB9B9BF))
                }
            }
            Spacer(Modifier.weight(1f))
            if (isLoading) {
                Text("解析中…", color = Color(0xFF4F8CFF), fontSize = 12.sp)
            } else {
                Text(
                    formatTime(positionMs / 1000.0) + " / " + formatTime(durationMs / 1000.0),
                    color = Color(0xFF9A9AA0), fontSize = 12.sp
                )
            }
        }
    }
}
