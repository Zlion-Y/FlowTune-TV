package com.flowtune.tv.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Song(
    val id: String,
    val path: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationSec: Double = 0.0,
    val coverUri: String? = null,
    val lyricsPath: String? = null,
    /** 在线歌曲的音源信息（本地歌曲为 null）。 */
    val online: OnlineSong? = null,
) {
    val isOnline: Boolean get() = online != null

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("path", path)
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("durationSec", durationSec)
        coverUri?.let { put("coverUri", it) }
        lyricsPath?.let { put("lyricsPath", it) }
        online?.let { put("online", it.toJson()) }
    }

    companion object {
        fun fromJson(o: JSONObject) = Song(
            id = o.optString("id"),
            path = o.optString("path"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            album = o.optString("album"),
            durationSec = o.optDouble("durationSec", 0.0),
            coverUri = o.optStringOrNull("coverUri"),
            lyricsPath = o.optStringOrNull("lyricsPath"),
            online = o.optJSONObject("online")?.let { OnlineSong.fromJson(it) },
        )
    }
}

data class OnlineSong(
    val source: String,             // wy/kw/kg/tx/mg
    val songId: String,
    val title: String = "",
    val singer: String = "",
    val album: String = "",
    val albumId: String = "",
    val picUrl: String? = null,
    val durationMs: Long = 0,
    /** 平台特有字段：kg=FileHash，tx=songmid，mg=copyrightId。 */
    val extras: Map<String, String> = emptyMap(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("source", source)
        put("songId", songId)
        put("title", title)
        put("singer", singer)
        put("album", album)
        put("albumId", albumId)
        picUrl?.let { put("picUrl", it) }
        put("durationMs", durationMs)
        put("extras", JSONObject(extras))
    }

    companion object {
        fun fromJson(o: JSONObject) = OnlineSong(
            source = o.optString("source"),
            songId = o.optString("songId"),
            title = o.optString("title"),
            singer = o.optString("singer"),
            album = o.optString("album"),
            albumId = o.optString("albumId"),
            picUrl = o.optStringOrNull("picUrl"),
            durationMs = o.optLong("durationMs", 0),
            extras = o.optJSONObject("extras")?.let { jo ->
                buildMap { for (k in jo.keys()) put(k, jo.optString(k)) }
            } ?: emptyMap(),
        )
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val folders: List<String>,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("songs", JSONArray().apply { songs.forEach { put(it.toJson()) } })
        put("folders", JSONArray().apply { folders.forEach { put(it) } })
    }

    companion object {
        fun fromJson(o: JSONObject) = Playlist(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name"),
            songs = o.optJSONArray("songs")?.let { arr ->
                List(arr.length()) { i -> Song.fromJson(arr.getJSONObject(i)) }
            } ?: emptyList(),
            folders = o.optJSONArray("folders")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }
            } ?: emptyList(),
        )
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null
