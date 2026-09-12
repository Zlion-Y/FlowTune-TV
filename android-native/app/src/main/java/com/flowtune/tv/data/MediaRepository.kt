package com.flowtune.tv.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.flowtune.tv.model.Playlist
import com.flowtune.tv.model.Song
import org.json.JSONObject
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** 本地媒体库：扫描、标签、封面、歌词（行为对齐 Web 版 Rust/Android 桥）。 */
class MediaRepository(private val context: Context) {

    val audioExtensions = setOf("mp3", "flac", "wav", "ogg", "oga", "m4a", "m4b", "aac", "opus", "wma", "ape", "wv")

    fun scanFolder(folder: String): List<String> {
        val root = File(folder)
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .map { it.absolutePath }
            .toList()
            .sortedBy { it.lowercase() }
    }

    fun songFromPath(path: String): Song {
        val file = File(path)
        val meta = readTags(file)
        return Song(
            id = UUID.randomUUID().toString(),
            path = path,
            title = meta.title.ifBlank { file.nameWithoutExtension },
            artist = meta.artist,
            album = meta.album,
            durationSec = meta.durationSec,
            coverUri = meta.coverPath,
            lyricsPath = meta.lyricsPath,
        )
    }

    data class Tags(
        val title: String, val artist: String, val album: String,
        val durationSec: Double, val coverPath: String?, val lyricsPath: String?,
    )

    fun readTags(file: File): Tags {
        var title = ""; var artist = ""; var album = ""
        var durationSec = 0.0; var coverPath: String? = null; var lyricsPath: String? = null

        // jaudiotagger 读标签 + 内嵌封面
        runCatching {
            val af = AudioFileIO.read(file)
            val tag = af.tag
            fun f(k: FieldKey) = runCatching { tag.getFirst(k) ?: "" }.getOrDefault("")
            title = f(FieldKey.TITLE)
            artist = f(FieldKey.ARTIST)
            album = f(FieldKey.ALBUM)
            val art = runCatching { tag.firstArtwork }.getOrNull()
            if (art?.binaryData != null) {
                val cache = File(context.cacheDir, "covers")
                cache.mkdirs()
                val out = File(cache, file.hashCode().toString() + ".img")
                if (!out.isFile) out.writeBytes(art.binaryData)
                coverPath = out.absolutePath
            }
            val lyrics = runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrDefault("")
            if (lyrics.isNotBlank()) {
                val cache = File(context.cacheDir, "lyrics")
                cache.mkdirs()
                val out = File(cache, file.hashCode().toString() + ".lrc")
                if (!out.isFile) out.writeText(lyrics)
                lyricsPath = out.absolutePath
            }
        }
        durationSec = runCatching { AudioFileIO.read(file).audioHeader.trackLength.toDouble() }.getOrDefault(0.0)

        // 兜底：MediaMetadataRetriever
        if (durationSec <= 0.0 || coverPath == null) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                if (durationSec <= 0.0) {
                    durationSec = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000.0
                }
                if (coverPath == null) {
                    val art = mmr.embeddedPicture
                    if (art != null) {
                        val cache = File(context.cacheDir, "covers"); cache.mkdirs()
                        val out = File(cache, file.hashCode().toString() + ".img")
                        if (!out.isFile) out.writeBytes(art)
                        coverPath = out.absolutePath
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { if (android.os.Build.VERSION.SDK_INT >= 29) mmr.release() }
            }
        }

        // 同目录 sidecar 歌词（内嵌没有时）
        if (lyricsPath == null) {
            val base = file.absolutePath.substringBeforeLast('.', file.absolutePath)
            for (ext in listOf("lrc", "LRC", "txt")) {
                val cand = File("$base.$ext")
                if (cand.isFile) {
                    val text = decodeText(cand.readBytes())
                    if (text.isNotBlank()) { lyricsPath = cand.absolutePath; break }
                }
            }
        }
        // 同目录封面（cover/folder/front）
        if (coverPath == null) {
            val dir = file.parentFile ?: return Tags(title, artist, album, durationSec, null, lyricsPath)
            val stem = file.nameWithoutExtension.lowercase()
            for (name in listOf(stem, "cover", "folder", "front", "albumart")) {
                for (ext in listOf("jpg", "jpeg", "png", "webp")) {
                    val cand = File(dir, "$name.$ext")
                    if (cand.isFile) { coverPath = cand.absolutePath; break }
                }
                if (coverPath != null) break
            }
        }
        return Tags(title, artist, album, durationSec, coverPath, lyricsPath)
    }

    fun decodeText(data: ByteArray): String {
        try {
            val dec = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            return dec.decode(java.nio.ByteBuffer.wrap(data)).toString().removePrefix("\uFEFF")
        } catch (_: Exception) { }
        return runCatching {
            val dec = charset("GBK").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            dec.decode(java.nio.ByteBuffer.wrap(data)).toString()
        }.getOrDefault(String(data, Charsets.UTF_8))
    }

    // ---------- 歌单操作 ----------
    fun addFolder(playlist: Playlist, folder: String): Playlist {
        val existing = playlist.songs.map { it.path }.toSet()
        val added = scanFolder(folder).filter { it !in existing }.map { songFromPath(it) }
        return playlist.copy(songs = playlist.songs + added, folders = playlist.folders + folder)
    }

    fun addPaths(playlist: Playlist, paths: List<String>): Playlist {
        val existing = playlist.songs.map { it.path }.toSet()
        val files = mutableListOf<String>()
        for (p in paths) {
            val f = File(p)
            when {
                f.isFile && f.extension.lowercase() in audioExtensions -> files.add(p)
                f.isDirectory -> files.addAll(scanFolder(p))
            }
        }
        val added = files.filter { it !in existing }.map { songFromPath(it) }
        return playlist.copy(songs = playlist.songs + added)
    }

    fun parseLyricsText(text: String): List<LyricLine> = LyricParser.parse(text)
}

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    /** 逐字时间轴（LRC A2 / YRC 有，普通 LRC 为空 = 逐行模式）。 */
    val words: List<LyricWord> = emptyList(),
    val translation: String? = null,
)

data class LyricWord(val startMs: Long, val endMs: Long, val text: String)

object LyricParser {
    private val lineRe = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordRe = Regex("""<(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?>""")

    fun parse(text: String): List<LyricLine> {
        val raw = text.replace("\r\n", "\n").replace("\r", "\n")
        val translations = mutableMapOf<Int, String>()
        val lines = mutableListOf<Triple<Long, String, List<LyricWord>?>>()

        for (src in raw.lines()) {
            val matches = lineRe.findAll(src).toList()
            if (matches.isEmpty()) continue
            val content = src.substring(matches.last().range.last + 1).trim()
            if (content.isEmpty()) continue
            val t = matches[0].let { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLong()
                min * 60_000 + sec * 1000 + frac
            }
            // 逐字时间轴（A2/YRC 简化：`<mm:ss.xx>字` 序列）
            val wordMatches = wordRe.findAll(content).toList()
            val words: List<LyricWord>? = if (wordMatches.isNotEmpty()) {
                val plain = content.replace(wordRe, "")
                if (plain.isEmpty()) null else {
                    val tokens = content.split(wordRe).filter { it.isNotEmpty() }
                    if (tokens.size == wordMatches.size) {
                        wordMatches.zip(tokens).zipWithNext().mapNotNull { (pair, next) ->
                            val (m, token) = pair
                            val start = m.groupValues.let { g -> g[1].toLong() * 60_000 + g[2].toLong() * 1000 + g[3].padEnd(3, '0').take(3).toLong() }
                            val n = next.first
                            val end = n.groupValues.let { g -> g[1].toLong() * 60_000 + g[2].toLong() * 1000 + g[3].padEnd(3, '0').take(3).toLong() }
                            LyricWord(start, end, token)
                        }
                    } else null
                }
            } else null
            // 双语：`原文\n译文` 或以 `/` 分隔——简化：保留原文，译文从带 "译" 标签行取
            val isTranslation = src.contains("]" + "\\n") || content.contains("（译）") || content.contains("(译)")
            if (isTranslation && lines.isNotEmpty()) {
                translations[lines.size - 1] = content
            } else {
                lines.add(Triple(t, content, words))
            }
        }

        return lines.sortedBy { it.first }.mapIndexed { i, (t, text, words) ->
            val next = lines.getOrNull(i + 1)?.first ?: (t + 8_000)
            LyricLine(
                startMs = t,
                endMs = next,
                text = text,
                words = words ?: emptyList(),
                translation = translations[i],
            )
        }
    }
}
