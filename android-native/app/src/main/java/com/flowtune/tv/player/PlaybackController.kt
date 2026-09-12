package com.flowtune.tv.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.flowtune.tv.model.Song
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 播放引擎：ExoPlayer + MediaSession + 队列。全部状态以 StateFlow 暴露给 UI。 */
class PlaybackController(
    context: Context,
    private val config: com.flowtune.tv.data.ConfigRepository,
    private val urlResolver: (suspend (Song) -> Result<String>)? = null,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    val currentSong: Song? get() = _queue.value.getOrNull(_index.value)

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    // 网易云外链等会 302 http→https，必须允许跨协议重定向
                    .setAllowCrossProtocolRedirects(true)
                    .setUserAgent("Mozilla/5.0 (Linux; Android 11; TV) FlowTune/2.1.1")
                    .setConnectTimeoutMs(10_000)
                    .setReadTimeoutMs(15_000)
            )
        )
        .build().apply {
        addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _error.value = "播放出错：${error.errorCodeName}"
                _loading.value = false
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                _speed.value = params.speed
            }
            override fun onPlaybackStateChanged(state: Int) {
                // 播完（含 30s 试听片段结束）自动切下一首，避免停住像卡死
                if (state == androidx.media3.common.Player.STATE_ENDED) next()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _durationMs.value = duration
            }
        })
    }

    private val session = MediaSessionCompat(context, "FlowTune").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { next() }
            override fun onSkipToPrevious() { previous() }
            override fun onSeekTo(pos: Long) { seekTo(pos) }
            override fun onFastForward() { seekTo(positionMs.value + 10_000) }
            override fun onRewind() { seekTo((positionMs.value - 10_000).coerceAtLeast(0)) }
        })
        isActive = true
    }

    init {
        // 进度条 250ms 节流上报（比 Web 版 1s 细，歌词滚动更跟手）
        scope.launch {
            while (true) {
                if (player.isPlaying) {
                    _positionMs.value = player.currentPosition
                    val d = player.duration
                    if (d > 0 && d != _durationMs.value) {
                        _durationMs.value = d
                        _queue.value.getOrNull(_index.value)?.let { updateMetadataDuration(d, it) }
                    }
                    updateSessionState()
                }
                delay(250)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        _queue.value = songs
        playAt(startIndex.coerceIn(0, songs.size - 1))
    }

    fun playAt(index: Int) {
        val song = _queue.value.getOrNull(index) ?: return
        _index.value = index
        _error.value = null
        if (song.online == null) {
            playLocal(song)
        } else {
            scope.launch { playOnline(song) }
        }
    }

    private fun playLocal(song: Song) {
        val item = MediaItem.fromUri(song.path)
        player.setMediaItem(item)
        player.prepare()
        player.play()
        updateMetadata(song)
    }

    private suspend fun playOnline(song: Song) {
        _loading.value = true
        try {
            val url = urlResolver?.invoke(song)?.getOrThrow()
                ?: throw RuntimeException("未配置音源")
            _loading.value = false
            val item = MediaItem.fromUri(url)
            player.setMediaItem(item)
            player.prepare()
            player.play()
            updateMetadata(song)
        } catch (e: Exception) {
            android.util.Log.e("FlowTune/Online", "playOnline failed", e)
            _loading.value = false
            _isPlaying.value = false
            _error.value = "播放失败：${e.message}"
        }
    }

    fun play() {
        if (_queue.value.isEmpty()) return
        if (_index.value < 0) { playAt(0); return }
        player.play()
    }

    fun pause() = player.pause()

    fun toggle() {
        if (player.isPlaying) pause() else play()
    }

    fun next() {
        val count = _queue.value.size
        if (count == 0) return
        val cur = _index.value
        val target = when (_playMode.value) {
            PlayMode.SINGLE -> cur
            PlayMode.SHUFFLE -> (0 until count).filter { it != cur }.randomOrNull() ?: cur
            PlayMode.SEQUENTIAL -> (cur + 1) % count
            PlayMode.REVERSE -> if (cur - 1 < 0) count - 1 else cur - 1
        }
        playAt(target)
    }

    fun previous() {
        val count = _queue.value.size
        if (count == 0) return
        val cur = _index.value
        val target = when (_playMode.value) {
            PlayMode.SHUFFLE -> (0 until count).filter { it != cur }.randomOrNull() ?: cur
            PlayMode.SINGLE -> cur
            PlayMode.SEQUENTIAL -> if (cur - 1 < 0) count - 1 else cur - 1
            PlayMode.REVERSE -> (cur + 1) % count
        }
        playAt(target)
    }

    fun cyclePlayMode() {
        _playMode.value = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.SINGLE
            PlayMode.SINGLE -> PlayMode.REVERSE
            PlayMode.REVERSE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SEQUENTIAL
        }
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        _positionMs.value = ms
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun release() {
        session.release()
        player.release()
    }

    private fun updateMetadataDuration(durationMs: Long, song: Song) {
        session.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            .build())
    }

    private fun updateMetadata(song: Song) {
        session.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (song.durationSec * 1000).toLong())
            .build())
        updateSessionState()
    }

    private fun updateSessionState() {
        val state = if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        session.setPlaybackState(PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO
                    or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND)
            .setState(state, _positionMs.value, if (_isPlaying.value) _speed.value else 0f)
            .build())
    }
}

enum class PlayMode(val label: String) {
    SEQUENTIAL("顺序播放"), SINGLE("单曲循环"), REVERSE("列表循环"), SHUFFLE("随机播放")
}
