package com.flowtune.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.model.Song

private val QUALITY_CYCLE = listOf("128k" to "标准 128k", "320k" to "高清 320k", "flac" to "无损 FLAC")

private fun qualityLabel(q: String) = QUALITY_CYCLE.firstOrNull { it.first == q }?.second ?: q

/** 全屏播放页覆盖层：封面（在线URL/本地）+ 静态封面底图 + 逐字歌词；控制键 4s 无操作自动隐藏。 */
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

    // 控制键显隐：打开显示并聚焦播放键；4s 无操作隐藏；隐藏态按导航/OK 键唤醒（消费防误触），
    // 显示态按键只刷新计时、不干预焦点（否则方向键导航会被拉回播放键）
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var wakeCount by remember { mutableStateOf(0) }
    val playKeyFocus = remember { FocusRequester() }
    val overlayFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        runCatching { playKeyFocus.requestFocus() }
    }
    LaunchedEffect(wakeCount) {
        if (wakeCount > 0) {
            kotlinx.coroutines.delay(120) // 等控制键重新组合完成
            runCatching { playKeyFocus.requestFocus() }
        }
    }
    LaunchedEffect(controlsVisible, lastInteractMs) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(4000)
            if (System.currentTimeMillis() - lastInteractMs >= 4000) {
                controlsVisible = false
                runCatching { overlayFocus.requestFocus() } // 隐藏后焦点交给容器，避免隐形焦点误触
            }
        }
    }
    fun touch() { lastInteractMs = System.currentTimeMillis() }
    fun wake() { touch(); controlsVisible = true; wakeCount++ }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF14161C))
            .focusRequester(overlayFocus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && listOf(
                        Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft,
                        Key.DirectionRight, Key.DirectionCenter, Key.Enter
                    ).contains(e.key)
                ) {
                    if (controlsVisible) { touch(); false } else { wake(); true }
                } else false
            }
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
                    .background(Brush.verticalGradient(listOf(Color(0x9B101218), Color(0xE8101018))))
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
                Text(song?.title ?: "", color = Color.White, fontSize = 24.sp, maxLines = 1)
                Text(song?.artist?.ifBlank { "未知艺术家" } ?: "", color = Color(0xFF9A9AA0), fontSize = 14.sp, maxLines = 1)
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

        // 顶部关闭提示
        Text("返回键退出全屏", color = Color(0x99EDEDEF), fontSize = 12.sp, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))

        // 底部控制条：上一首 | 播放 | 下一首 | 队列 | 音质，4s 自动隐藏
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerControlKey(onClick = { touch(); state.playback.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, "上一首", tint = Color.White)
                }
                PlayerControlKey(
                    onClick = { touch(); state.playback.toggle() },
                    big = true,
                    focusRequester = playKeyFocus,
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "播放/暂停", tint = Color.White)
                }
                PlayerControlKey(onClick = { touch(); state.playback.next() }) {
                    Icon(Icons.Filled.SkipNext, "下一首", tint = Color.White)
                }
                PlayerControlKey(onClick = { touch(); state.showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放队列", tint = Color.White)
                }
                if (song?.online != null) {
                    val settings by state.config.settings.collectAsState()
                    PlayerControlKey(
                        onClick = {
                            touch()
                            val cur = settings.playQuality
                            val next = QUALITY_CYCLE[(QUALITY_CYCLE.indexOfFirst { it.first == cur } + 1).coerceAtLeast(0) % QUALITY_CYCLE.size]
                            state.config.updateSettings { it.copy(playQuality = next.first) }
                            // 切音质后重播当前歌立即生效
                            state.playback.playAt(state.playback.index.value)
                        },
                        wide = true,
                    ) {
                        Text(qualityLabel(settings.playQuality), color = Color.White, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** 播放页控制键：自绘焦点光圈（material IconButton 在 TV 上无焦点视觉）。 */
@Composable
private fun PlayerControlKey(
    onClick: () -> Unit,
    big: Boolean = false,
    wide: Boolean = false,
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val w = when {
        wide -> 96.dp
        big -> 64.dp
        else -> 52.dp
    }
    val h = if (big) 64.dp else 52.dp
    Box(
        Modifier
            .size(width = w, height = h)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) FocusBg else Color(0x883A3A40), shape)
            .tvFocusGlow(focused, shape)
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) { content() }
}
