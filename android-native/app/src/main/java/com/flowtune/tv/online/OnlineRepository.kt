package com.flowtune.tv.online

import android.content.Context
import com.flowtune.tv.data.ConfigRepository
import com.flowtune.tv.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File

/** 音源信息（设置页展示用）。 */
data class SourceInfo(val name: String, val version: String, val author: String, val file: String)

/**
 * 在线音乐仓库：音源管理（LX 脚本）、播放地址解析、下载。
 * 搜索/榜单/歌单数据在 [Platforms]。
 */
class OnlineRepository(private val config: ConfigRepository) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------- 音源（LX 脚本） ----------
    data class LoadedSource(val info: SourceInfo, val engine: LxSourceEngine)

    private val _sources = MutableStateFlow<List<LoadedSource>>(emptyList())
    val sources: StateFlow<List<LoadedSource>> = _sources.asStateFlow()

    private val _activeSource = MutableStateFlow<LoadedSource?>(null)
    val activeSource: StateFlow<LoadedSource?> = _activeSource.asStateFlow()

    private val _downloadState = MutableStateFlow<String?>(null)
    fun clearDownload() { _downloadState.value = null }
    val downloadState: StateFlow<String?> = _downloadState.asStateFlow()

    /** 从常见目录扫描可导入的 .js 音源脚本。 */
    fun scanSourceFiles(context: Context): List<File> {
        val dirs = listOf(
            File("/sdcard/Download"),
            File("/sdcard"),
            context.getExternalFilesDir(null),
        )
        val seen = mutableSetOf<String>()
        return dirs.flatMap { dir ->
            dir?.listFiles()?.filter { it.isFile && it.extension.equals("js", true) } ?: emptyList()
        }.filter { seen.add(it.absolutePath) }
    }

    suspend fun importSource(file: File): Result<SourceInfo> = runCatching {
        val script = file.readText()
        val meta = LxSourceEngine.parseScriptMeta(script)
        val engine = LxSourceEngine.load(script, meta)
        val info = SourceInfo(meta.name, meta.version, meta.author, file.absolutePath)
        val loaded = LoadedSource(info, engine)
        _sources.value = _sources.value + loaded
        if (_activeSource.value == null) _activeSource.value = loaded
        info
    }

    fun removeSource(info: SourceInfo) {
        _sources.value = _sources.value.filter { it.info != info }
        if (_activeSource.value?.info == info) _activeSource.value = _sources.value.firstOrNull()
    }

    fun setActive(info: SourceInfo) {
        _activeSource.value = _sources.value.firstOrNull { it.info == info }
    }

    // ---------- URL 解析 ----------
    fun hasSource(): Boolean = _activeSource.value != null

    private fun musicInfoJson(m: com.flowtune.tv.model.OnlineSong): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("songId", m.songId)
            put("source", m.source)
            put("title", m.title)
            put("name", m.title)
            put("singer", m.singer)
            put("album", m.album)
            put("albumId", m.albumId)
            put("interval", "${m.durationMs / 1000}秒")
            put("hash", m.extras["FileHash"] ?: "")
            put("songmid", m.songId)
            put("copyrightId", m.songId)
            put("rid", m.songId)
            put("meta", org.json.JSONObject().put("qualitys", emptyList<Any>()))
        }
    }

    /** 解析在线歌曲播放地址：音源优先，网易云回退外链。 */
    suspend fun resolvePlayUrl(song: Song, quality: String): Result<String> = withContext(Dispatchers.IO) {
        val m = song.online ?: return@withContext Result.failure(RuntimeException("非在线歌曲"))
        val active = _activeSource.value
        android.util.Log.d("FlowTune/Online", "resolvePlayUrl source=${m.source} quality=$quality hasSource=${active != null}")
        if (active != null) {
            runCatching {
                active.engine.callHandler(
                    "musicUrl", m.source,
                    org.json.JSONObject()
                        .put("musicInfo", musicInfoJson(m))
                        .put("type", quality)
                        .toString()
                )
            }.recoverCatching { err ->
                android.util.Log.e("FlowTune/Online", "source resolve failed: ${err.message}")
                if (m.source == "wy") {
                    val url = Platforms.wyOuterUrl(m.songId)
                        ?: throw RuntimeException("音源解析失败：${err.message}")
                    _downloadState.value = "音源解析失败，已回退试听链接（受版权限制可能只有 30 秒）"
                    url
                } else throw RuntimeException("音源解析失败：${err.message}")
            }
        } else {
            if (m.source == "wy") {
                Platforms.wyOuterUrl(m.songId)?.let { Result.success(it) }
                    ?: Result.failure(RuntimeException("无可用音源（非免费曲目需在设置导入音源）"))
            } else {
                Result.failure(RuntimeException("未导入音源，请在设置中导入 LX 音源脚本"))
            }
        }
    }

    /** 在线歌词：wy/kw 直连。返回 (原文, 翻译)。 */
    suspend fun fetchLyric(song: Song): Pair<String, String> = withContext(Dispatchers.IO) {
        val m = song.online ?: return@withContext "" to ""
        runCatching {
            when (m.source) {
                "wy" -> Platforms.wyLyric(m.songId)
                "kw" -> Platforms.kwLyric(m.songId)
                else -> "" to ""
            }
        }.getOrDefault("" to "")
    }

    // ---------- 下载 ----------
    fun download(song: Song) {
        scope.launch {
            _downloadState.value = "开始下载：${song.title}"
            try {
                val quality = config.settings.value.downloadQuality
                val url = resolvePlayUrl(song, quality).getOrThrow()
                val dir = File("/sdcard/Music/FlowTune").apply { mkdirs() }
                val ext = when {
                    url.contains("flac", true) -> "flac"
                    url.contains(".m4a", true) -> "m4a"
                    else -> "mp3"
                }
                val safe = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
                val fileName = "${song.artist.ifBlank { "未知" }} - $safe.$ext"
                val target = File(dir, fileName)

                _downloadState.value = "下载中：$fileName"
                val request = Request.Builder().url(url).build()
                Platforms.http.newCall(request).execute().use { resp ->
                    check(resp.isSuccessful) { "HTTP ${resp.code}" }
                    resp.body!!.byteStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                _downloadState.value = "写入标签：$fileName"
                runCatching {
                    val af = AudioFileIO.read(target)
                    val tag = af.tagAndConvertOrCreateAndSetDefault
                    tag.setField(FieldKey.TITLE, song.title)
                    tag.setField(FieldKey.ARTIST, song.artist.ifBlank { "未知艺术家" })
                    if (song.album.isNotBlank()) runCatching { tag.setField(FieldKey.ALBUM, song.album) }
                    song.online?.picUrl?.let { pic ->
                        runCatching {
                            val bytes = Platforms.http.newCall(Request.Builder().url(pic).build())
                                .execute().body!!.bytes()
                            val art = StandardArtwork().apply {
                                binaryData = bytes
                                mimeType = "image/jpeg"
                            }
                            tag.setField(art)
                        }
                    }
                    af.commit()
                }

                _downloadState.value = "下载完成：$fileName"
            } catch (e: Exception) {
                _downloadState.value = "下载失败：${e.message}"
            }
            delay(4000)
            _downloadState.value = null
        }
    }
}
