package com.flowtune.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtune.tv.AppGraph
import com.flowtune.tv.player.PlayMode
import androidx.compose.material3.Text

/** FlowTune 主布局：侧栏 + 内容 + 播放栏；全屏播放页/队列/设置以覆盖层呈现。 */
@Composable
fun FlowTuneApp(state: AppState) {
    val playlists by state.config.playlists.collectAsState()
    val isPlaying by state.playback.isPlaying.collectAsState()
    val current by state.playback.index.collectAsState()
    val queue by state.playback.queue.collectAsState()
    val position by state.playback.positionMs.collectAsState()
    val duration by state.playback.durationMs.collectAsState()
    val playMode by state.playback.playMode.collectAsState()
    val settings by state.config.settings.collectAsState()
    val downloadState by AppGraph.online.downloadState.collectAsState()
    val playError by AppGraph.playback.error.collectAsState()
    val loading by AppGraph.playback.loading.collectAsState()

    val currentSong = queue.getOrNull(current)
    val activePlaylist = playlists.firstOrNull { it.id == state.selectedPlaylistId }

    BackHandler(enabled = state.showPlayerDetail) { state.showPlayerDetail = false }
    BackHandler(enabled = state.showQueue) { state.showQueue = false }
    BackHandler(enabled = state.showSettings) { state.showSettings = false }
    BackHandler(enabled = state.onlineTab != null) {
        when (state.onlineTab) {
            "charts" -> if (state.boardSongs != null) state.boardSongs = null else state.onlineTab = null
            "playlists" -> if (state.playlistSongs != null) state.playlistSongs = null else state.onlineTab = null
            else -> state.onlineTab = null
        }
    }
    LaunchedEffect(Unit) { state.loadPlaylists() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF16161A))
    ) {
        Column(Modifier.fillMaxSize()) {
            // 主内容区
            Row(Modifier.weight(1f)) {
                Sidebar(
                    playlists = playlists,
                    selectedId = state.selectedPlaylistId,
                    onlineTab = state.onlineTab,
                    onSelect = { state.selectedPlaylistId = it; state.onlineTab = null },
                    onOpenOnline = { state.onlineTab = it },
                    onOpenSettings = { state.showSettings = true },
                    modifier = Modifier.width(220.dp).fillMaxHeight(),
                )
                if (state.onlineTab != null) {
                    OnlineScreen(
                        tab = state.onlineTab!!,
                        onTabChange = { state.onlineTab = it },
                        platform = state.onlinePlatform,
                        onPlatformChange = { state.onlinePlatform = it },
                        searchQuery = state.searchQuery,
                        onSearchQueryChange = { state.searchQuery = it },
                        onSearch = { state.onlineSearch(it) },
                        searchResults = state.searchResults,
                        boards = com.flowtune.tv.online.Platforms.wyBoards,
                        onBoardClick = { state.loadBoard(it) },
                        boardSongs = state.boardSongs,
                        playlists = state.playlists,
                        onPlaylistClick = { state.loadPlaylistDetail(it) },
                        playlistSongs = state.playlistSongs,
                        currentSongId = currentSong?.id,
                        isSearching = state.isSearching,
                        onPlay = { m -> state.playOnline(listOf(m), 0) },
                        onQueue = { m ->
                            val q = queue + m.toSong()
                            state.playback.playQueue(q, if (queue.isEmpty()) 0 else queue.size)
                        },
                        onDownload = { m -> AppGraph.online.download(m.toSong()) },
                        onBack = { state.onlineTab = null },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                } else {
                PlaylistScreen(
                    playlist = activePlaylist,
                    currentSongId = currentSong?.id,
                    onPlay = { idx -> activePlaylist?.let { state.playPlaylist(it, idx) } },
                    onImportFolder = { state.importFolderToCurrent(it) },
                    onAddToQueue = { song ->
                        val q = queue + song
                        state.playback.playQueue(q, if (queue.isEmpty()) 0 else queue.size)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                }
            }
            // 播放栏
            PlayerBar(
                song = currentSong,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                playMode = playMode,
                onToggle = { state.playback.toggle() },
                isLoading = loading,
                onNext = { state.playback.next() },
                onPrev = { state.playback.previous() },
                onCycleMode = { state.playback.cyclePlayMode() },
                onOpenDetail = { state.showPlayerDetail = true },
                onOpenQueue = { state.showQueue = true },
            )
        }

        // 全屏播放页
        if (state.showPlayerDetail) {
            PlayerDetailOverlay(
                state = state,
                song = currentSong,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                effectLevel = settings.effectLevel,
                onClose = { state.showPlayerDetail = false },
            )
        }

        // 播放队列
        if (state.showQueue) {
            QueueOverlay(
                queue = queue,
                currentIndex = current,
                onPlay = { state.playback.playAt(it) },
                onClose = { state.showQueue = false },
            )
        }

        // 下载/错误提示
        val toast = downloadState ?: playError
        if (toast != null) {
            LaunchedEffect(toast) {
                if (toast == playError) AppGraph.playback.clearError()
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(Color(0xEE2E2E33), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(toast, color = Color.White, fontSize = 15.sp)
            }
        }

        // 设置
        if (state.showSettings) {
            SettingsOverlay(
                state = state,
                onClose = { state.showSettings = false },
            )
        }
    }
}
