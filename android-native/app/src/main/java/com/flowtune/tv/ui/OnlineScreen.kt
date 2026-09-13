package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
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

private val TAB_TITLES = mapOf(
    "search" to "搜索", "charts" to "排行榜", "playlists" to "歌单", "albums" to "专辑"
)

/**
 * 在线音乐：与桌面版一致的三级结构。
 * 一级=侧栏；二级=内容区左侧列表（榜单/歌单/专辑）；三级=右侧歌曲列表。搜索页直接输入+结果。
 */
@Composable
fun OnlineScreen(
    tab: String,                       // search | charts | playlists | albums
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
    selectedBoardId: String?,
    playlists: List<OnlinePlaylist>,
    onPlaylistClick: (String) -> Unit,
    playlistSongs: List<OnlineMusic>?,
    selectedOnlinePlaylistId: String?,
    albums: List<OnlinePlaylist>,
    onAlbumClick: (String) -> Unit,
    albumSongs: List<OnlineMusic>?,
    selectedAlbumId: String?,
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
        "albums" -> albumSongs ?: emptyList()
        else -> playlistSongs ?: emptyList()
    }
    val hasSubNav = tab != "search"
    val subNavFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    // 进入 tab 时聚焦二级列表；列表加载后聚焦内容首行（保持二级焦点时不动）
    LaunchedEffect(tab) {
        kotlinx.coroutines.delay(120)
        if (hasSubNav) runCatching { subNavFocus.requestFocus() } else runCatching { contentFocus.requestFocus() }
    }

    Column(modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
        // 顶栏：页面标题 + 平台切换（与桌面一致）
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TAB_TITLES[tab] ?: "", color = Color(0xFFF2F2F4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (tab == "search") {
                listOf("wy" to "网易", "kg" to "酷狗", "kw" to "酷我", "mg" to "咪咕").forEach { (id, label) ->
                    Chip(label, active = platform == id) { onPlatformChange(id) }
                    Spacer(Modifier.width(8.dp))
                }
            } else {
                Chip("网易", active = true) { }
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxSize()) {
            // 二级导航列（桌面同款：左侧列表）
            if (hasSubNav) {
                Column(
                    Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .padding(end = 16.dp)
                ) {
                    val subItems: List<Pair<String, String>> = when (tab) {  // id to label
                        "charts" -> boards
                        "playlists" -> playlists.map { it.id to it.name }
                        "albums" -> albums.map { it.id to it.name }
                        else -> emptyList()
                    }
                    val subPics: Map<String, String?> = when (tab) {
                        "playlists" -> playlists.associate { it.id to it.picUrl }
                        "albums" -> albums.associate { it.id to it.picUrl }
                        else -> emptyMap()
                    }
                    if (subItems.isEmpty()) {
                        Text("加载中…", color = Color(0xFF6E6E74), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                    LazyColumn {
                        itemsIndexed(subItems) { idx, (id, name) ->
                            var focused by remember { mutableStateOf(false) }
                            val selected = when (tab) {
                                "charts" -> selectedBoardId == id
                                "playlists" -> selectedOnlinePlaylistId == id
                                "albums" -> selectedAlbumId == id
                                else -> false
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .then(if (idx == 0) Modifier.focusRequester(subNavFocus) else Modifier)
                                    .onFocusChanged { focused = it.isFocused }
                                    .background(
                                        when {
                                            focused -> Color(0xFF2E2E33)
                                            selected -> Color(0xFF26262C)
                                            else -> Color.Transparent
                                        },
                                        TvShape
                                    )
                                    .tvFocusGlow(focused, TvShape)
                                    .clickable {
                                        when (tab) {
                                            "charts" -> onBoardClick(id)
                                            "playlists" -> onPlaylistClick(id)
                                            "albums" -> onAlbumClick(id)
                                        }
                                    }
                                    .onKeyEvent { e ->
                                        if (e.type == KeyEventType.KeyUp && e.key == Key.DirectionRight) {
                                            runCatching { contentFocus.requestFocus() }
                                            true
                                        } else false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                if (tab != "charts") {
                                    val pic = subPics[id]
                                    if (pic != null) {
                                        AsyncImage(
                                            model = pic,
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                }
                                Text(
                                    name,
                                    color = if (focused || selected) Color.White else Color(0xFFC9C9CF),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 三级内容：歌曲列表（搜索页含输入框）
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (tab == "search") {
                    var query by remember(searchQuery) { mutableStateOf(searchQuery) }
                    var fieldFocused by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(ChipBg, TvShape)
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

                when {
                    isSearching -> Text("搜索中…", color = Color(0xFF88888E), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                    list.isEmpty() && tab == "search" && searchQuery.isNotBlank() -> Text("无结果", color = Color(0xFF6E6E74), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                    list.isEmpty() && hasSubNav -> Text("在左侧选择${TAB_TITLES[tab] ?: ""}", color = Color(0xFF6E6E74), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                    list.isEmpty() -> {}
                    else -> LazyColumn {
                        itemsIndexed(list) { idx, music ->
                            OnlineRow(
                                index = idx,
                                music = music,
                                playing = music.toSongId() == currentSongId,
                                onPlay = { onPlay(music) },
                                onQueue = { onQueue(music) },
                                onDownload = { onDownload(music) },
                                onNavLeft = if (hasSubNav) { { runCatching { subNavFocus.requestFocus() } } } else null,
                                focusRequester = if (idx == 0) contentFocus else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun OnlineMusic.toSongId() = "${source}_$songId"

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        label,
        color = if (active || focused) Color.White else Color(0xFFB9B9BF),
        fontSize = 14.sp,
        modifier = Modifier
            .background(
                if (active) ChipBgActive else ChipBg,
                TvShape
            )
            .onFocusChanged { focused = it.isFocused }
            .tvFocusGlow(focused, TvShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** 在线歌曲行：OK=播放，右键=加入队列；左键=二级导航(长按=下载)，搜索页保持左键=下载。 */
@Composable
private fun OnlineRow(
    index: Int,
    music: OnlineMusic,
    playing: Boolean,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onNavLeft: (() -> Unit)?,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var leftHeld by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .background(
                if (playing) Color(0xFF1F3A5F) else Color.Transparent
            )
            .tvFocusGlow(focused, TvShape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable { onPlay() }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft && e.nativeKeyEvent.repeatCount >= 2) {
                    leftHeld = true; false
                } else if (e.type == KeyEventType.KeyUp && focused && e.key == Key.DirectionLeft) {
                    if (leftHeld) { leftHeld = false; onDownload(); true }
                    else if (onNavLeft != null) { onNavLeft(); true }
                    else { onDownload(); true }
                } else if (e.type == KeyEventType.KeyUp && focused && e.key == Key.DirectionRight) {
                    onQueue(); true
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
                music.singer,
                color = Color(0xFF88888E), fontSize = 12.sp, maxLines = 1
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(music.album, color = Color(0xFF88888E), fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(0.7f))
        Text(formatTime(music.durationMs / 1000.0), color = Color(0xFF88888E), fontSize = 12.sp, modifier = Modifier.width(56.dp))
    }
}
