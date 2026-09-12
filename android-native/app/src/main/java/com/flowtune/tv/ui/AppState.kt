package com.flowtune.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.flowtune.tv.AppGraph
import com.flowtune.tv.data.LyricLine
import com.flowtune.tv.data.MediaRepository
import com.flowtune.tv.model.Playlist
import com.flowtune.tv.online.OnlineMusic
import com.flowtune.tv.online.OnlinePlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // ---- 在线 ----
    var onlineTab by mutableStateOf<String?>(null)   // null=关闭, search/charts/playlists
    var onlinePlatform by mutableStateOf("wy")
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<OnlineMusic>>(emptyList())
    var isSearching by mutableStateOf(false)
    var boardSongs by mutableStateOf<List<OnlineMusic>?>(null)
    var playlistSongs by mutableStateOf<List<OnlineMusic>?>(null)
    var playlists by mutableStateOf<List<OnlinePlaylist>>(emptyList())

    val currentPlaylist: Playlist?
        get() = config.playlists.value.firstOrNull { it.id == selectedPlaylistId }

    fun loadLyricsFor(song: com.flowtune.tv.model.Song?) {
        if (song == null) { lyrics = emptyList(); return }
        CoroutineScope(Dispatchers.IO).launch {
            val (text, translation) = if (song.online != null) {
                onlineFetchLyric(song)
            } else {
                val path = song.lyricsPath
                if (path != null) runCatching { java.io.File(path).readText() }.getOrDefault("") to ""
                else "" to ""
            }
            val parsed = media.parseLyricsText(text)
            val parsedTrans = media.parseLyricsText(translation)
            val merged = if (parsedTrans.isNotEmpty()) parsed.mapIndexed { i, l ->
                l.copy(translation = parsedTrans.getOrNull(i)?.text ?: l.translation)
            } else parsed
            withContext(Dispatchers.Main) { lyrics = merged }
        }
    }

    private fun onlineFetchLyric(song: com.flowtune.tv.model.Song): Pair<String, String> =
        runCatching { kotlinx.coroutines.runBlocking { AppGraph.online.fetchLyric(song) } }.getOrDefault("" to "")

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

    fun playOnline(songs: List<OnlineMusic>, index: Int) {
        if (songs.isEmpty()) return
        val converted = songs.map { it.toSong() }
        playback.playQueue(converted, index)
        loadLyricsFor(converted.getOrNull(index))
    }

    fun onlineSearch(query: String) {
        searchQuery = query
        isSearching = true
        CoroutineScope(Dispatchers.IO).launch {
            val r = runCatching {
                when (onlinePlatform) {
                    "kg" -> com.flowtune.tv.online.Platforms.kgSearch(query, 1)
                    "kw" -> com.flowtune.tv.online.Platforms.kwSearch(query, 1)
                    "mg" -> com.flowtune.tv.online.Platforms.mgSearch(query, 1)
                    else -> com.flowtune.tv.online.Platforms.wySearch(query, 1)
                }
            }
            withContext(Dispatchers.Main) {
                isSearching = false
                r.onSuccess { searchResults = it.first }
                    .onFailure { searchResults = emptyList() }
            }
        }
    }

    fun loadBoard(boardId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val r = runCatching { com.flowtune.tv.online.Platforms.wyBoardSongs(boardId) }
            withContext(Dispatchers.Main) { boardSongs = r.getOrDefault(emptyList()) }
        }
    }

    fun loadPlaylists() {
        CoroutineScope(Dispatchers.IO).launch {
            val r = runCatching { com.flowtune.tv.online.Platforms.wyHotPlaylists(1) }
            withContext(Dispatchers.Main) { playlists = r.getOrDefault(emptyList()) }
        }
    }

    fun loadPlaylistDetail(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val r = runCatching { com.flowtune.tv.online.Platforms.wyPlaylistDetail(id) }
            withContext(Dispatchers.Main) { playlistSongs = r.getOrDefault(emptyList()) }
        }
    }
}

@Composable
fun rememberAppState(): AppState {
    return remember { AppState(AppGraph.config, AppGraph.media, AppGraph.playback) }
}
