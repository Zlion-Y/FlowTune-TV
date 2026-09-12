package com.flowtune.tv

import android.content.Context
import com.flowtune.tv.data.ConfigRepository
import com.flowtune.tv.data.MediaRepository
import com.flowtune.tv.player.PlaybackController
import com.flowtune.tv.online.OnlineRepository

/** 轻量依赖图：手动注入，避免引入 Hilt 增大包体与编译时间。 */
object AppGraph {
    lateinit var config: ConfigRepository
        private set
    lateinit var media: MediaRepository
        private set
    lateinit var playback: PlaybackController
        private set
    lateinit var online: OnlineRepository
        private set

    fun init(context: Context) {
        config = ConfigRepository(context)
        media = MediaRepository(context)
        online = OnlineRepository(config)
        playback = PlaybackController(context, config) { song -> online.resolvePlayUrl(song, config.settings.value.playQuality) }
    }
}
