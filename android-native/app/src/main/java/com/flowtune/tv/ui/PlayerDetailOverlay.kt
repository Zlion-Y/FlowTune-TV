package com.flowtune.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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

/** 全屏播放页覆盖层：封面（在线URL/本地）+ 静态封面底图 + 逐字歌词，动效按档位降级。 */
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
    val isLocalCover = song?.online == null
    var coverBitmap by remember(song?.id) { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 本地封面解码；在线封面走 coil（AsyncImage）
    LaunchedEffect(coverPath, isLocalCover) {
        if (coverPath != null && isLocalCover) {
            coverBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { android.graphics.BitmapFactory.decodeFile(coverPath) }.getOrNull()
            }
        } else {
            coverBitmap = null
        }
    }
    val coverModel: Any? = when {
        coverBitmap != null -> coverBitmap!!.asImageBitmap()
        coverPath != null && !isLocalCover -> coverPath
        else -> null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF14161C))
    ) {
        // 静态封面底图：所有档位显示（动效档位只影响歌词动画）
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0x9B101218), Color(0xE8101018)))
                    )
            )
        }

        // 封面左侧 + 歌词右侧
        Row(Modifier.fillMaxSize().padding(horizontal = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2A2A30)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("♪", color = Color(0xFF6E6E74), fontSize = 64.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(song?.title ?: "", color = Color.White, fontSize = 24.sp)
                Text(song?.artist?.ifBlank { "未知艺术家" } ?: "", color = Color(0xFF9A9AA0), fontSize = 14.sp)
            }
            Spacer(Modifier.width(64.dp))
            Box(Modifier.weight(1f).fillMaxHeight()) {
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
        ) { Text("返回键退出全屏", color = Color(0x99EDEDEF), fontSize = 12.sp) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            PlayerControlKey(onClick = { state.playback.previous() }) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
            }
            PlayerControlKey(onClick = { state.playback.toggle() }, big = true) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
            }
            PlayerControlKey(onClick = { state.playback.next() }) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White)
            }
        }
    }
}

/** 播放页控制键：自绘焦点光圈（material IconButton 在 TV 上无焦点视觉）。 */
@Composable
private fun PlayerControlKey(
    onClick: () -> Unit,
    big: Boolean = false,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val size = if (big) 64.dp else 52.dp
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .size(size)
            .background(
                if (focused) FocusBg else Color(0x883A3A40),
                shape
            )
            .tvFocusGlow(focused, shape)
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { focused = it.isFocused },
        contentAlignment = Alignment.Center
    ) { content() }
}
