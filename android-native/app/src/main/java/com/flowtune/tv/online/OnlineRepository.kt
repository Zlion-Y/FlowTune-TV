package com.flowtune.tv.online

import com.flowtune.tv.data.ConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 在线音乐仓库。
 * 首版内置五平台（网易云 wy / 酷狗 kg / 酷我 kw / QQ tx / 咪咕 mg）的搜索/榜单/歌单 API 直连，
 * 音源解析优先走已导入的 LX 自定义音源脚本（QuickJS 引擎），无音源时回退内置直链。
 */
class OnlineRepository(private val config: ConfigRepository) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 是否有可用音源（LX 脚本）。 */
    fun hasSource(): Boolean = sourceScripts.isNotEmpty()

    /** 已导入的 LX 音源脚本（name -> script）。 */
    val sourceScripts = LinkedHashMap<String, String>()

    fun importSource(name: String, script: String) {
        sourceScripts[name] = script
    }

    fun removeSource(name: String) {
        sourceScripts.remove(name)
    }
}
