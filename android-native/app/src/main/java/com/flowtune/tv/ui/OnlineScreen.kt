package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flowtune.tv.online.OnlineMusic
import com.flowtune.tv.online.OnlinePlaylist

private val Accent = Color(0xFF4F8CFF)
private val ChipBg = Color(0xFF232327)
private val ChipBgActive = Accent

/** 在线音乐：搜索 / 排行榜 / 歌单 三视图 + 平台切换。 */
@Composable
fun OnlineScreen(
    tab: String,                       // search | charts | playlists
    onTabChange: (String) -> Unit,
    platform: String,                  // wy | kg | kw | mg
    onPlatformChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<OnlineMusic>,
    boards: List<Pair<String, String>>,
    onBoardClick: (String) -> Unit,
    boardSongs: List<OnlineMusic>?,
    playlists: List<OnlinePlaylist>,
    onPlaylistClick: (String) -> Unit,
    playlistSongs: List<OnlineMusic>?,
    currentSongId: String?,
    isSearching: Boolean,
    onPlay: (OnlineMusic) -> Unit,
    onQueue: (OnlineMusic) -> Unit,
    onDownload: (OnlineMusic) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = when (tab) {
        "search" -> searchResults
        "charts" -> boardSongs ?: emptyList()
        else -> playlistSongs ?: emptyList()
    }
    val listFocusRequester = remember { FocusRequester() }
    LaunchedEffect(list) {
        if (list.isNotEmpty()) {
            kotlinx.coroutines.delay(120)
            listFocusRequester.requestFocus()
        }
    }

    Column(modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
        // 顶部：标签 + 平台
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("search" to "搜索", "charts" to "排行榜", "playlists" to "歌单").forEach { (id, label) ->
                Chip(label, active = tab == id) { onTabChange(id) }
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.weight(1f))
            if (tab == "search" || tab == "charts") {
                listOf("wy" to "网易", "kg" to "酷狗", "kw" to "酷我", "mg" to "咪咕").forEach { (id, label) ->
                    val show = tab == "search" || id == "wy"
                    if (show) {
                        Chip(label, active = platform == id) { onPlatformChange(id) }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        when (tab) {
            "search" -> {
                var query by remember(searchQuery) { mutableStateOf(searchQuery) }
                var fieldFocused by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(ChipBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .tvFocusGlow(fieldFocused, TvShape)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFFEDEDEF), fontSize = 15.sp),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { fieldFocused = it.isFocused }
                        )
                        if (query.isEmpty()) {
                            Text("输入歌名 / 歌手", color = Color(0xFF6E6E74), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    TvButton(
                        "搜索",
                        container = Accent,
                        fontSize = 14.sp,
                        verticalPadding = 13.dp,
                        onClick = { if (query.isNotBlank()) onSearch(query) },
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            "charts" -> {
                if (boardSongs == null) {
                    LazyColumn {
                        itemsIndexed(boards) { _, (id, name) ->
                            Text(
                                name,
                                color = Color(0xFFEDEDEF), fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ChipBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                    .clickable { onBoardClick(id) }
                                    .focusable()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    return@Column
                }
            }
            "playlists" -> {
                if (playlistSongs == null) {
                    LazyColumn {
                        itemsIndexed(playlists) { _, pl ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ChipBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                    .clickable { onPlaylistClick(pl.id) }
                                    .focusable()
                                    .padding(10.dp)
                            ) {
                                if (pl.picUrl != null) {
                                    AsyncImage(
                                        model = pl.picUrl,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column {
                                    Text(pl.name, color = Color(0xFFEDEDEF), fontSize = 15.sp, maxLines = 1)
                                    Text("播放 ${pl.playCount / 10000}万", color = Color(0xFF88888E), fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    return@Column
                }
            }
        }

        // 列表视图（搜索结果 / 榜单曲目 / 歌单曲目）
        if (tab == "search" || ((tab == "charts" || tab == "playlists") && list.isNotEmpty())) {
            if (isSearching) {
                Text("搜索中…", color = Color(0xFF88888E), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
            } else if (list.isEmpty()) {
                Text("无结果", color = Color(0xFF6E6E74), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn {
                    itemsIndexed(list) { idx, m ->
                        OnlineRow(
                            index = idx,
                            music = m,
                            playing = m.songId == currentSongId,
                            onPlay = { onPlay(m) },
                            onQueue = { onQueue(m) },
                            onDownload = { onDownload(m) },
                            focusRequester = if (idx == 0) listFocusRequester else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        label,
        color = if (active || focused) Color.White else Color(0xFFB9B9BF),
        fontSize = 14.sp,
        modifier = Modifier
            .background(
                when {
                    focused -> FocusBg
                    active -> ChipBgActive
                    else -> ChipBg
                },
                androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .tvFocusGlow(focused, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** 在线歌曲行：OK=播放，右键=加入队列，左键=下载。 */
@Composable
private fun OnlineRow(
    index: Int,
    music: OnlineMusic,
    playing: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    focused -> FocusBg
                    playing -> Color(0xFF1F3A5F)
                    else -> Color.Transparent
                }
            )
            .tvFocusGlow(focused, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { onPlay() }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable()
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && focused) {
                    when (e.key) {
                        Key.DirectionRight -> { onQueue(); true }
                        Key.DirectionLeft -> { onDownload(); true }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("${index + 1}", color = Color(0xFF9A9AA0), fontSize = 13.sp, modifier = Modifier.width(34.dp))
        if (music.picUrl != null) {
            AsyncImage(
                model = music.picUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                music.title,
                color = if (focused || playing) Color.White else Color(0xFFEDEDEF),
                fontSize = 15.sp, maxLines = 1
            )
            Text(
                "${music.singer} · ${music.album}".trim(' ', '·'),
                color = Color(0xFF9A9AA0), fontSize = 12.sp, maxLines = 1
            )
        }
        Text("${music.durationMs / 60000}:${((music.durationMs % 60000) / 1000).toString().padStart(2, '0')}",
            color = Color(0xFF9A9AA0), fontSize = 12.sp)
    }
}
