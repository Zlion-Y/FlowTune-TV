package com.flowtune.tv.data

import android.content.Context
import android.net.Uri
import com.flowtune.tv.model.AppSettings
import com.flowtune.tv.model.EffectLevel
import com.flowtune.tv.model.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 配置持久化（JSON 格式兼容 Web 版 config.json 的 playlists/settings 结构，
 * 老版本 WebView 的配置文件可直接迁移）。
 */
class ConfigRepository(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val configFile = File(context.filesDir, "config.json")

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _playlists = MutableStateFlow(defaultPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        scope.launch { load() }
    }

    fun defaultPlaylists(): List<Playlist> = listOf(
        Playlist(id = "favorites", name = "我的喜欢", songs = emptyList(), folders = emptyList())
    )

    private suspend fun load() {
        if (!configFile.isFile) return
        runCatching {
            val obj = JSONObject(configFile.readText())
            val s = obj.optJSONObject("settings")
            if (s != null) _settings.value = AppSettings.fromJson(s)
            val pls = obj.optJSONArray("playlists")
            if (pls != null) {
                _playlists.value = List(pls.length()) { i -> Playlist.fromJson(pls.getJSONObject(i)) }
            }
        }
    }

    fun save() {
        scope.launch {
            runCatching {
                val obj = JSONObject()
                obj.put("playlists", JSONArray().apply { _playlists.value.forEach { put(it.toJson()) } })
                obj.put("settings", _settings.value.toJson())
                val tmp = File(configFile.parentFile, "config.json.tmp")
                tmp.writeText(obj.toString(2))
                tmp.renameTo(configFile)
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
        save()
    }

    fun updatePlaylists(list: List<Playlist>) {
        _playlists.value = list
        save()
    }

    fun updatePlaylist(playlist: Playlist) {
        updatePlaylists(_playlists.value.map { if (it.id == playlist.id) playlist else it })
    }

    /** 首次启用时的默认效果档位：TV 上默认关闭全部动效。 */
    fun ensureEffectDefaults() {
        if (!_settings.value.effectLevelTouched) {
            updateSettings { it.copy(effectLevel = EffectLevel.OFF, effectLevelTouched = true) }
        }
    }
}
