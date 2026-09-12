package com.flowtune.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.model.Song
import java.io.File

/** 全屏播放页覆盖层：封面 + 逐字歌词 + 背景按档位降级。 */
@Composable
fun PlayerDetailOverlay(
    state: AppState,
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    effectLevel: EffectLevel,
    onClose: () -> Unit,
) {
    val coverPath = song?.coverUri
    var coverBitmap by remember(song?.id) { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 封面加载（本地文件）
    LaunchedEffect(coverPath) {
        if (coverPath != null) {
            coverBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { android.graphics.BitmapFactory.decodeFile(coverPath!!) }.getOrNull()
            }
        } else {
            coverBitmap = null
        }
    }

    // 背景层：OFF/LOW = 深色纯色渐变；MEDIUM+ = 封面主色渐变
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF20242E), Color(0xFF14161C))
                )
            )
    ) {
        // 封面左侧 + 歌词右侧
        Row(Modifier.fillMaxSize().padding(horizontal = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(320.dp).clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Box(
                        Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2A2A30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", color = Color(0xFF6E6E74), fontSize = 64.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(song?.title ?: "", color = Color.White, fontSize = 24.sp)
                Text(song?.artist?.ifBlank { "未知艺术家" } ?: "", color = Color(0xFF9A9AA0), fontSize = 14.sp)
            }
            Spacer(Modifier.width(64.dp))
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SyncLyricsPosition(positionMs)
                LyricsCanvas(
                    lyrics = state.lyrics,
                    positionMs = positionMs,
                    effectLevel = effectLevel,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.lyrics.isEmpty()) {
                    Text(
                        "暂无歌词",
                        color = Color(0xFF6E6E74), fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // 顶部关闭提示 & 底部控制
        Row(
            Modifier.align(Alignment.TopStart).padding(20.dp)
        ) { Text("返回键退出全屏", color = Color(0xFF6E6E74), fontSize = 12.sp) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            IconButton(onClick = { state.playback.previous() }) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
            }
            IconButton(
                onClick = { state.playback.toggle() },
                modifier = Modifier.size(64.dp).background(Color(0xFF3A3A40), RoundedCornerShape(50))
            ) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
            }
            IconButton(onClick = { state.playback.next() }) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White)
            }
        }
    }
}
