package com.flowtune.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.flowtune.tv.AppGraph
import com.flowtune.tv.data.MediaRepository
import com.flowtune.tv.data.LyricLine
import com.flowtune.tv.model.Playlist

/** 应用级 UI 状态。 */
class AppState(
    val config: com.flowtune.tv.data.ConfigRepository,
    val media: MediaRepository,
    val playback: com.flowtune.tv.player.PlaybackController,
) {
    var selectedPlaylistId by mutableStateOf("favorites")
    var showPlayerDetail by mutableStateOf(false)
    var showQueue by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var exitRequestedAt by mutableStateOf(0L)

    val currentPlaylist: Playlist?
        get() = config.playlists.value.firstOrNull { it.id == selectedPlaylistId }

    fun loadLyricsFor(song: com.flowtune.tv.model.Song?) {
        val path = song?.lyricsPath ?: run { lyrics = emptyList(); return }
        CoroutineScope(Dispatchers.IO).launch {
            val text = runCatching { java.io.File(path).readText() }.getOrDefault("")
            val parsed = media.parseLyricsText(text)
            withContext(Dispatchers.Main) {
                lyrics = parsed
            }
        }
    }

    fun importFolderToCurrent(folder: String) {
        val pl = currentPlaylist ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val updated = media.addFolder(pl, folder)
            withContext(Dispatchers.Main) {
                config.updatePlaylist(updated)
            }
        }
    }

    fun playPlaylist(pl: Playlist, index: Int = 0) {
        if (pl.songs.isEmpty()) return
        playback.playQueue(pl.songs, index)
        loadLyricsFor(pl.songs.getOrNull(index))
    }
}

@Composable
fun rememberAppState(): AppState {
    return androidx.compose.runtime.remember { AppState(AppGraph.config, AppGraph.media, AppGraph.playback) }
}
