package com.flowtune.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flowtune.tv.player.PlayMode

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

    val currentSong = queue.getOrNull(current)
    val activePlaylist = playlists.firstOrNull { it.id == state.selectedPlaylistId }

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
                    onSelect = { state.selectedPlaylistId = it },
                    onOpenSettings = { state.showSettings = true },
                    modifier = Modifier.width(220.dp).fillMaxHeight(),
                )
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
            // 播放栏
            PlayerBar(
                song = currentSong,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                playMode = playMode,
                onToggle = { state.playback.toggle() },
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

        // 设置
        if (state.showSettings) {
            SettingsOverlay(
                state = state,
                onClose = { state.showSettings = false },
            )
        }
    }
}
