package com.flowtune.tv.online

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** 音乐数据模型（在线）。 */
data class OnlineMusic(
    val source: String,
    val songId: String,
    val title: String,
    val singer: String,
    val album: String = "",
    val albumId: String = "",
    val picUrl: String? = null,
    val durationMs: Long = 0,
    val extras: Map<String, String> = emptyMap(),
)

data class OnlinePlaylist(
    val id: String,
    val name: String,
    val picUrl: String? = null,
    val playCount: Long = 0,
    val trackCount: Long = 0,
)

/** 五平台在线 API 直连（端点与请求签名移植自 lx-music-desktop 生态的公开实现）。 */
object Platforms {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // ---------- HTTP ----------
    fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val rb = Request.Builder().url(url).get()
        rb.header("User-Agent", UA)
        headers.forEach { (k, v) -> rb.header(k, v) }
        http.newCall(rb.build()).execute().use { resp ->
            check(resp.isSuccessful) { "HTTP ${resp.code}" }
            return resp.body!!.string()
        }
    }

    fun postForm(url: String, form: String, headers: Map<String, String> = emptyMap()): String {
        val rb = Request.Builder().url(url)
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        rb.header("User-Agent", UA)
        headers.forEach { (k, v) -> rb.header(k, v) }
        http.newCall(rb.build()).execute().use { resp ->
            check(resp.isSuccessful) { "HTTP ${resp.code}" }
            return resp.body!!.string()
        }
    }

    private fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject =
        JSONObject(get(url, headers))

    // ---------- 网易云 eapi 签名 ----------
    private const val EAPI_KEY = "e82ckenh8dichen8"

    private fun md5Hex(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun eapi(apiPath: String, payload: JSONObject): String {
        val text = payload.toString()
        val digest = md5Hex("nobody${apiPath}use${text}md5forencrypt")
        val data = "$apiPath-36cd479b6b5-$text-36cd479b6b5-$digest"
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES"))
        return cipher.doFinal(data.toByteArray()).joinToString("") { "%02X".format(it) }
    }

    private fun eapiPost(apiPath: String, payload: JSONObject): JSONObject {
        val form = "params=${eapi(apiPath, payload)}"
        val body = postForm(
            "http://interface.music.163.com/eapi/batch",
            form,
            mapOf("origin" to "https://music.163.com")
        )
        return JSONObject(body)
    }

    private fun eapiGetJson(apiPath: String, payload: JSONObject): JSONObject {
        val form = "params=${eapi(apiPath, payload)}"
        return JSONObject(
            postForm(
                "http://interface.music.163.com/eapi/batch",
                form,
                mapOf("origin" to "https://music.163.com")
            )
        )
    }

    // ---------- 网易云 ----------
    fun wySearch(query: String, page: Int, limit: Int = 30): Pair<List<OnlineMusic>, Long> {
        val payload = JSONObject()
            .put("keyword", query)
            .put("needCorrect", "1")
            .put("channel", "typing")
            .put("offset", limit * (page - 1))
            .put("scene", "normal")
            .put("total", page == 1)
            .put("limit", limit)
        val data = eapiGetJson("/api/search/song/list/page", payload)
        check(data.optInt("code") == 200) { "网易搜索失败" }
        val resources = data.optJSONObject("data")?.optJSONArray("resources") ?: JSONArray()
        val out = mutableListOf<OnlineMusic>()
        for (i in 0 until resources.length()) {
            val song = resources.optJSONObject(i)?.optJSONObject("baseInfo")?.optJSONObject("simpleSongData")
                ?: continue
            out += wyNormalize(song)
        }
        return out to (data.optJSONObject("data")?.optLong("totalCount", 0) ?: 0L)
    }

    private fun wyNormalize(s: JSONObject): OnlineMusic {
        val al = s.optJSONObject("al")
        return OnlineMusic(
            source = "wy",
            songId = s.optString("id"),
            title = s.optString("name"),
            singer = s.optJSONArray("ar")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") } }?.joinToString("、") ?: "",
            album = al?.optString("name") ?: "",
            albumId = al?.optString("id") ?: "",
            picUrl = al?.optString("picUrl", null)?.takeIf { it.isNotEmpty() },
            durationMs = s.optLong("dt", 0),
        )
    }

    /** 网易云官方榜单（与 lx-music 榜单 id 一致）。 */
    val wyBoards = listOf(
        "3779629" to "新歌榜", "3778678" to "热歌榜", "2884035" to "原创榜",
        "19723756" to "飙升榜", "10520166" to "电音榜", "745956260" to "韩语榜",
        "2809513713" to "欧美热歌榜", "71384707" to "古典榜", "71385702" to "ACG榜",
    )

    fun wyBoardSongs(boardId: String): List<OnlineMusic> {
        val data = eapiGetJson(
            "/api/v3/playlist/detail",
            JSONObject().put("id", boardId).put("n", 100000)
        )
        val tracks = data.optJSONObject("data")?.optJSONObject("playlist")?.optJSONArray("tracks")
            ?: data.optJSONObject("playlist")?.optJSONArray("tracks")
            ?: JSONArray()
        return (0 until tracks.length()).mapNotNull { i ->
            tracks.optJSONObject(i)?.let { wyNormalize(it) }
        }
    }

    fun wyHotPlaylists(page: Int, cat: String = "全部"): List<OnlinePlaylist> {
        val data = eapiGetJson(
            "/api/playlist/list",
            JSONObject()
                .put("cat", cat)
                .put("order", "hot")
                .put("limit", 30)
                .put("offset", 30 * (page - 1))
                .put("total", true)
        )
        val arr = data.optJSONArray("playlists") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val p = arr.optJSONObject(i) ?: return@mapNotNull null
            OnlinePlaylist(
                id = p.optString("id"),
                name = p.optString("name"),
                picUrl = p.optString("coverImgUrl", null).takeIf { it.isNotEmpty() },
                playCount = p.optLong("playCount", 0),
                trackCount = p.optLong("trackCount", 0),
            )
        }
    }

    fun wyPlaylistDetail(id: String): List<OnlineMusic> {
        val data = eapiGetJson(
            "/api/v3/playlist/detail",
            JSONObject().put("id", id).put("n", 100000)
        )
        val pl = data.optJSONObject("data")?.optJSONObject("playlist") ?: data.optJSONObject("playlist")
        val tracks = pl?.optJSONArray("tracks") ?: JSONArray()
        return (0 until tracks.length()).mapNotNull { i ->
            tracks.optJSONObject(i)?.let { wyNormalize(it) }
        }
    }

    /** 网易云歌词（原文+翻译）。 */
    fun wyLyric(songId: String): Pair<String, String> {
        val data = eapiGetJson(
            "/api/song/lyric",
            JSONObject().put("id", songId).put("lv", -1).put("kv", -1).put("tv", -1).put("rv", -1)
        )
        val lrc = data.optJSONObject("lrc")?.optString("lyric") ?: ""
        val tl = data.optJSONObject("tlyric")?.optString("lyric") ?: ""
        return lrc to tl
    }

    /** 免费曲目外链回退（VIP/无版权返回 404 或空音频）。 */
    fun wyOuterUrl(songId: String): String? {
        val url = "https://music.163.com/song/media/outer/url?id=$songId.mp3"
        return try {
            http.newCall(Request.Builder().url(url).head().build()).execute().use { resp ->
                if (resp.code == 200) url else null
            }
        } catch (_: Exception) { null }
    }

    // ---------- 酷狗 ----------
    fun kgSearch(query: String, page: Int, limit: Int = 30): Pair<List<OnlineMusic>, Long> {
        val url = "https://songsearch.kugou.com/song_search_v2?keyword=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&page=$page&pagesize=$limit&userid=0&clientver=&platform=WebFilter&filter=2&iscorrection=1&privilege_filter=0&area_code=1"
        val data = getJson(url, mapOf("Referer" to "https://www.kugou.com/"))
        check(data.optInt("error_code") == 0) { "酷狗搜索失败" }
        val total = data.optJSONObject("data")?.optLong("total", 0) ?: 0
        val lists = data.optJSONObject("data")?.optJSONArray("lists") ?: JSONArray()
        val out = (0 until lists.length()).mapNotNull { i ->
            val s = lists.optJSONObject(i) ?: return@mapNotNull null
            OnlineMusic(
                source = "kg",
                songId = s.optString("Audioid"),
                title = s.optString("SongName").replace("<em>", "").replace("</em>", ""),
                singer = s.optString("SingerName").replace("<em>", "").replace("</em>", ""),
                album = s.optString("AlbumName"),
                albumId = s.optString("AlbumID"),
                durationMs = s.optLong("Duration", 0) * 1000,
                extras = mapOf("FileHash" to s.optString("FileHash")),
            )
        }
        return out to total
    }

    // ---------- 酷我 ----------
    fun kwSearch(query: String, page: Int, limit: Int = 30): Pair<List<OnlineMusic>, Long> {
        val params = mapOf(
            "client" to "kt", "all" to query, "pn" to "${page - 1}", "rn" to "$limit",
            "uid" to "794762570", "ver" to "kwplayer_ar_9.2.2.1", "vipver" to "1",
            "show_copyright_off" to "1", "newver" to "1", "ft" to "music",
            "cluster" to "0", "strategy" to "2012", "encoding" to "utf8",
            "rformat" to "json", "mobi" to "1",
        )
        val qs = params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        val body = get("http://search.kuwo.cn/r.s?$qs", mapOf("Referer" to "https://www.kuwo.cn/"))
            .replace(Regex("^\\(|\\)$"), "")  // 去掉 JSONP 括号
        val data = JSONObject(body)
        val total = data.optLong("TOTAL", data.optLong("total", 0))
        val arr = data.optJSONArray("abslist") ?: JSONArray()
        val out = (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val songId = s.optString("MUSICRID").removePrefix("MUSIC_")
            OnlineMusic(
                source = "kw",
                songId = songId,
                title = s.optString("SONGNAME"),
                singer = s.optString("ARTIST"),
                album = s.optString("ALBUM"),
                picUrl = kwCoverUrl(songId),
                durationMs = s.optLong("DURATION", 0) * 1000,
            )
        }
        return out to total
    }

    private fun kwCoverUrl(songId: String): String? = try {
        val text = get(
            "http://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=500&size=500&rid=$songId",
            mapOf("Referer" to "https://www.kuwo.cn/")
        ).trim()
        if (text.startsWith("http")) text else null
    } catch (_: Exception) { null }

    /** 酷我歌词。 */
    fun kwLyric(songId: String): Pair<String, String> {
        val data = getJson(
            "http://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=$songId",
            mapOf("Referer" to "https://m.kuwo.cn/")
        )
        val arr = data.optJSONArray("lrclist") ?: return "" to ""
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val l = arr.optJSONObject(i) ?: continue
            val t = l.optDouble("time", 0.0)
            val mm = (t.toInt()) / 60; val ss = (t.toInt()) % 60; val ff = ((t % 1) * 100).toInt()
            sb.append("[$mm:${ss.toString().padStart(2, '0')}.${ff.toString().padStart(2, '0')}]").append(l.optString("lineLyric")).append('\n')
        }
        return sb.toString() to ""
    }

    // ---------- 咪咕 ----------
    fun mgSearch(query: String, page: Int, limit: Int = 30): Pair<List<OnlineMusic>, Long> {
        val time = System.currentTimeMillis().toString()
        val deviceId = "963B7AA0D21511ED807EE5846EC87D20"
        val sign = md5Hex("$query" + "6cdc72a439cef99a3418d2a78aa28c73" + "yyapp2d16148780a1dcc7408e06336b98cfd50$deviceId$time")
        val searchSwitch = "%7B%22song%22%3A1%2C%22album%22%3A0%2C%22singer%22%3A0%2C%22tagSong%22%3A1%2C%22mvSong%22%3A0%2C%22bestShow%22%3A1%2C%22songlist%22%3A0%2C%22lyricSong%22%3A0%7D"
        val url = "https://jadeite.migu.cn/music_search/v3/search/searchAll?isCorrect=0&isCopyright=1" +
                "&searchSwitch=$searchSwitch&pageSize=$limit&text=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&pageNo=$page&sort=0&sid=USS"
        val data = getJson(url, mapOf(
            "uiVersion" to "A_music_3.6.1", "deviceId" to deviceId, "timestamp" to time,
            "sign" to sign, "channel" to "0146921",
            "User-Agent" to "Mozilla/5.0 (Linux; U; Android 11.0.0; zh-cn; MI 11 Build/OPR1.170623.032) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        ))
        check(data.optString("code") == "000000") { data.optString("info", "咪咕搜索失败") }
        val resultData = data.optJSONObject("songResultData") ?: JSONObject()
        val total = resultData.optLong("totalCount", 0)
        val arr = resultData.optJSONArray("resultList") ?: JSONArray()
        val out = (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val song = s.optJSONObject("songItem") ?: return@mapNotNull null
            val formats = song.optJSONArray("audioFormats")
            var hasFlac = false; var has320 = false
            if (formats != null) for (j in 0 until formats.length()) {
                when (formats.optJSONObject(j)?.optString("formatType")) {
                    "SQ" -> hasFlac = true
                    "HQ" -> has320 = true
                }
            }
            OnlineMusic(
                source = "mg",
                songId = song.optString("copyrightId"),
                title = song.optString("songName"),
                singer = song.optJSONArray("singers")?.let { arr2 -> (0 until arr2.length()).mapNotNull { arr2.optJSONObject(it)?.optString("name") } }?.joinToString("、") ?: "",
                album = song.optJSONObject("album")?.optString("name") ?: "",
                albumId = song.optJSONObject("album")?.optString("id") ?: "",
                picUrl = song.optJSONArray("albumImgs")?.let { arr ->
                    arr.optJSONObject(0)?.optString("img")?.takeIf { it.isNotEmpty() }
                },
                durationMs = song.optLong("duration", 0),
                extras = mapOf("hasFlac" to hasFlac.toString(), "has320" to has320.toString()),
            )
        }
        return out to total
    }
}
