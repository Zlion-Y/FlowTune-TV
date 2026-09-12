package com.flowtune.tv.model

import org.json.JSONObject

/** 动效档位：TV 默认 OFF，可按机器性能向上调。 */
enum class EffectLevel(val label: String) {
    OFF("关闭"),
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    val dynamicBackground: Boolean get() = this >= MEDIUM
    val coverReflection: Boolean get() = this >= LOW
    val wordSpring: Boolean get() = this >= MEDIUM      // 逐字弹簧动画
    val wordBlur: Boolean get() = this >= HIGH          // 未唱行模糊
    val lyricFps: Int get() = when (this) {
        OFF -> 15
        LOW -> 20
        MEDIUM -> 30
        HIGH -> 45
    }
}

data class AppSettings(
    val effectLevel: EffectLevel = EffectLevel.OFF,
    val effectLevelTouched: Boolean = false,
    val themeDark: Boolean = true,
    val accentColor: String = "#0078d4",
    val autoplay: Boolean = false,
    val savePlaybackState: Boolean = true,
    val playQuality: String = "320k",
    val autoOpenPlayer: Boolean = false,
    val downloadQuality: String = "flac",
    val downloadFolder: String = "",
    val checkUpdateOnLaunch: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("effectLevel", effectLevel.name)
        put("effectLevelTouched", effectLevelTouched)
        put("themeDark", themeDark)
        put("accentColor", accentColor)
        put("autoplay", autoplay)
        put("savePlaybackState", savePlaybackState)
        put("playQuality", playQuality)
        put("downloadQuality", downloadQuality)
        put("downloadFolder", downloadFolder)
        put("checkUpdateOnLaunch", checkUpdateOnLaunch)
    }

    companion object {
        fun fromJson(o: JSONObject) = AppSettings(
            effectLevel = o.optString("effectLevel", "OFF").let { name ->
                EffectLevel.entries.firstOrNull { it.name == name } ?: EffectLevel.OFF
            },
            effectLevelTouched = o.optBoolean("effectLevelTouched", false),
            themeDark = o.optBoolean("themeDark", true),
            accentColor = o.optString("accentColor", "#0078d4"),
            autoplay = o.optBoolean("autoplay", false),
            savePlaybackState = o.optBoolean("savePlaybackState", true),
            playQuality = o.optString("playQuality", "320k"),
            autoOpenPlayer = o.optBoolean("autoOpenPlayer", false),
            downloadQuality = o.optString("downloadQuality", "flac"),
            downloadFolder = o.optString("downloadFolder", ""),
            checkUpdateOnLaunch = o.optBoolean("checkUpdateOnLaunch", false),
        )
    }
}
