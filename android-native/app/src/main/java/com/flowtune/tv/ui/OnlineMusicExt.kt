package com.flowtune.tv.ui

import com.flowtune.tv.online.OnlineMusic
import com.flowtune.tv.model.Song
import java.util.UUID

fun OnlineMusic.toSong(): Song = Song(
    id = "${source}_$songId",
    path = "",
    title = title,
    artist = singer,
    album = album,
    durationSec = durationMs / 1000.0,
    coverUri = picUrl,
    online = com.flowtune.tv.model.OnlineSong(
        source = source,
        songId = songId,
        title = title,
        singer = singer,
        album = album,
        albumId = albumId,
        picUrl = picUrl,
        durationMs = durationMs,
        extras = extras,
    ),
)
