package com.flowtune.tv.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val QUALITY_CYCLE = listOf("128k" to "标准 128k", "320k" to "高清 320k", "flac" to "无损 FLAC")
private val SPEED_CYCLE = listOf(1.0f, 1.25f, 1.5f, 0.75f)

private fun qualityBadge(q: String) = when (q) {
    "flac" -> "FLAC"
    "320k" -> "320K"
    else -> "128K"
}

/** 全屏播放页：一比一还原 FluentPlayer 布局（仅去桌面歌词/音量），背景为静态封面模糊。 */
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

    // 静态模糊封面：解码原图 → 缩成 ~24px 小图 → 放大显示即为模糊（全 API 兼容）
    var blurredBg by remember(song?.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(coverPath, isLocalCover) {
        blurredBg = if (coverPath == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val src = if (isLocalCover) {
                    BitmapFactory.decodeFile(coverPath)
                } else {
                    val bytes = java.net.URL(coverPath).openStream().use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                src?.let {
                    val small = android.graphics.Bitmap.createScaledBitmap(it, 24, 24, true)
                    if (small !== it) it.recycle()
                    small
                }
            }.getOrNull()
        }
    }
    val sharpCoverModel: Any? = when {
        coverPath != null && isLocalCover -> coverPath
        coverPath != null -> coverPath
        else -> null
    }

    // 控制条显隐：打开显示并聚焦播放键；4s 无操作隐藏；隐藏态按键唤醒（消费防误触）
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
            kotlinx.coroutines.delay(120)
            runCatching { playKeyFocus.requestFocus() }
        }
    }
    LaunchedEffect(controlsVisible, lastInteractMs) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(4000)
            if (System.currentTimeMillis() - lastInteractMs >= 4000) {
                controlsVisible = false
                runCatching { overlayFocus.requestFocus() }
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
        // 静态模糊封面背景 + 深色遮罩
        if (blurredBg != null) {
            Image(
                bitmap = blurredBg!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x73101218), Color(0xC2101218))))
        )

        // 主区：左封面 + 右歌词
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 56.dp, end = 48.dp, bottom = 96.dp, top = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2A2A30)),
                contentAlignment = Alignment.Center
            ) {
                if (coverPath != null) {
                    AsyncImage(
                        model = sharpCoverModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("♪", color = Color(0xFF6E6E74), fontSize = 56.sp)
                }
            }
            Spacer(Modifier.width(56.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // 歌词视图指示圆点（原版样式）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (i) {
                                        0 -> Color.White.copy(alpha = 0.95f)
                                        1 -> Color.White.copy(alpha = 0.55f)
                                        else -> Color.White.copy(alpha = 0.22f)
                                    }
                                )
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LyricsCanvas(
                        lyrics = state.lyrics,
                        positionMs = positionMs,
                        effectLevel = effectLevel,
                        alignLeft = true,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (state.lyrics.isEmpty()) {
                        Text("暂无歌词", color = Color(0xFF6E6E74), fontSize = 15.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        // 顶部提示
        Text(
            "返回键退出全屏",
            color = Color(0x80EDEDEF), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
        )

        // 底栏：进度条横贯 + 歌名 | 控制 | 时间/音质/倍速（一比一还原，4s 自动隐藏）
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                ProgressBar(positionMs, durationMs)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0x3D000000))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左：歌名/歌手
                    Column(Modifier.width(260.dp)) {
                        Text(song?.title ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(song?.artist?.ifBlank { "未知艺术家" } ?: "", color = Color(0xFF9A9AA0), fontSize = 11.sp, maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    // 中：模式 | 上一首 | 播放(正圆蓝) | 下一首 | 队列
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val playMode by state.playback.playMode.collectAsState()
                        TvButton(
                            playMode.label.take(2),
                            container = Color.Transparent,
                            fontSize = 12.sp,
                            horizontalPadding = 8.dp,
                            verticalPadding = 8.dp,
                            onClick = { touch(); state.playback.cyclePlayMode() },
                        )
                        TvKeyButton(onClick = { touch(); state.playback.previous() }) {
                            Icon(Icons.Filled.SkipPrevious, "上一首", tint = Color.White)
                        }
                        PlayRoundKey(
                            isPlaying = isPlaying,
                            focusRequester = playKeyFocus,
                            onClick = { touch(); state.playback.toggle() },
                        )
                        TvKeyButton(onClick = { touch(); state.playback.next() }) {
                            Icon(Icons.Filled.SkipNext, "下一首", tint = Color.White)
                        }
                        TvKeyButton(onClick = { touch(); state.showQueue = true }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放队列", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // 右：时间 | 音质 | 倍速
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val speed by state.playback.speed.collectAsState()
                        Text(
                            formatTime(positionMs / 1000.0) + " / " + formatTime(durationMs / 1000.0),
                            color = Color(0xFFD8D8DC), fontSize = 12.sp
                        )
                        if (song?.online != null) {
                            val settings by state.config.settings.collectAsState()
                            TvButton(
                                qualityBadge(settings.playQuality),
                                container = Color(0x2EFFFFFF),
                                fontSize = 11.sp,
                                horizontalPadding = 10.dp,
                                verticalPadding = 6.dp,
                                onClick = {
                                    touch()
                                    val next = QUALITY_CYCLE[(QUALITY_CYCLE.indexOfFirst { it.first == settings.playQuality } + 1).coerceAtLeast(0) % QUALITY_CYCLE.size]
                                    state.config.updateSettings { it.copy(playQuality = next.first) }
                                    state.playback.playAt(state.playback.index.value)
                                },
                            )
                        }
                        TvButton(
                            String.format("%.2fx", speed),
                            container = Color.Transparent,
                            fontSize = 12.sp,
                            horizontalPadding = 6.dp,
                            verticalPadding = 6.dp,
                            onClick = {
                                touch()
                                val next = SPEED_CYCLE[(SPEED_CYCLE.indexOf(speed) + 1) % SPEED_CYCLE.size]
                                state.playback.setSpeed(next)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 播放大圆键：蓝底正圆白标，与原版一致，聚焦加光圈。 */
@Composable
private fun PlayRoundKey(
    isPlaying: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .size(56.dp)
            .background(FocusGlow, CircleShape)
            .tvFocusGlow(focused, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "播放/暂停", tint = Color.White)
    }
}

/** 细进度条（原版样式：横贯底栏顶部，蓝色已播）。 */
@Composable
private fun ProgressBar(positionMs: Long, durationMs: Long) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x33FFFFFF))) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(FocusGlow))
    }
}
