package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.model.Playlist
import com.flowtune.tv.model.Song

private val Accent = Color(0xFF4F8CFF)

@Composable
fun PlaylistScreen(
    playlist: Playlist?,
    currentSongId: String?,
    onPlay: (Int) -> Unit,
    onImportFolder: (String) -> Unit,
    onAddToQueue: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlist == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("请选择一个歌单", color = Color(0xFF88888E))
        }
        return
    }
    var folderDialog by remember { mutableStateOf(false) }

    Column(modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(playlist.name, color = Color(0xFFF2F2F4), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${playlist.songs.size} 首歌曲", color = Color(0xFF9A9AA0), fontSize = 13.sp)
            }
            TvButton(
                "▶ 播放全部",
                container = Accent,
                fontSize = 14.sp,
                verticalPadding = 13.dp,
                onClick = { if (playlist.songs.isNotEmpty()) onPlay(0) },
            )
            Spacer(Modifier.width(12.dp))
            TvButton(
                "＋ 文件夹",
                fontSize = 14.sp,
                verticalPadding = 13.dp,
                onClick = { folderDialog = true },
            )
        }

        Spacer(Modifier.height(18.dp))

        // 表头
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF232327))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("#", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.width(36.dp))
            Text("标题", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.weight(2.2f))
            Text("艺术家", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.weight(1.4f))
            Text("专辑", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.weight(1.4f))
            Text("时长", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.width(64.dp))
        }

        if (playlist.songs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("暂无歌曲", color = Color(0xFF6E6E74), fontSize = 15.sp)
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(playlist.songs) { idx, song ->
                    SongRow(
                        index = idx,
                        song = song,
                        playing = song.id == currentSongId,
                        onEnter = { onPlay(idx) },
                        onQueue = { onAddToQueue(song) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }
    }

    if (folderDialog) {
        FolderPickerDialog(
            onPick = { folder ->
                folderDialog = false
                onImportFolder(folder)
            },
            onDismiss = { folderDialog = false }
        )
    }
}

@Composable
private fun SongRow(
    index: Int,
    song: Song,
    playing: Boolean,
    onEnter: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                if (playing) Color(0xFF1F3A5F) else Color.Transparent
            )
            .tvFocusGlow(focused, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable { onEnter() }
            .onKeyEvent { e ->
                // 长按右键 = 加入队列（遥控器语义）
                if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionRight && focused) {
                    onQueue(); true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("${index + 1}", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.width(36.dp))
        Text(song.title, color = if (focused || playing) Color.White else Color(0xFFEDEDEF), fontSize = 15.sp, modifier = Modifier.weight(2.2f), maxLines = 1)
        Text(song.artist.ifBlank { "未知艺术家" }, color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.weight(1.4f), maxLines = 1)
        Text(song.album.ifBlank { "-" }, color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.weight(1.4f), maxLines = 1)
        Text(formatTime(song.durationSec), color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.width(64.dp))
    }
}

internal fun formatTime(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
