package adeln

import okhttp3.HttpUrl

fun resolve(url: HttpUrl): HttpUrl? =
    when (url.host) {
        "youtube.com", "www.youtube.com", "m.youtube.com" ->
            resolveYt(url)

        "youtu.be" ->
            resolveShortYt(url)

        "yadi.sk" ->
            buildYaDisk(url)

        else ->
            null
    }

fun resolveShortYt(url: HttpUrl): HttpUrl? =
    url.pathSegments.lastOrNull(String::isNotBlank)?.let { buildVideo(VideoID(it)) }

fun buildYaDisk(url: HttpUrl): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegment("yandexdisk")
        .addPathSegment("public")
        .addQueryParameter("link", url.toString())
        .build()

private fun resolveYt(url: HttpUrl): HttpUrl? =
    url.queryParameter("v")?.takeIf(String::isNotBlank)?.let { buildVideo(VideoID(it)) }
        ?: url.queryParameter("list")?.takeIf(String::isNotBlank)?.let { buildPlaylist(PlaylistID(it)) }
        ?: url.pathSegments.takeIf { "channel" in it }?.lastOrNull(String::isNotBlank)?.let { buildChannel(ChannelId.ById(it)) }
        ?: url.pathSegments.takeIf { "user" in it }?.lastOrNull(String::isNotBlank)?.let { buildUser(ChannelId.ByName(it)) }

fun buildVideo(videoID: VideoID): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegments("video")
        .addQueryParameter("v", videoID.id)
        .build()

fun buildPlaylist(playlistID: PlaylistID): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegment("playlist")
        .addQueryParameter("list", playlistID.id)
        .build()

fun buildChannel(channelID: ChannelId.ById): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegment("channel")
        .addPathSegment(channelID.id)
        .build()

fun buildUser(username: ChannelId.ByName): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegment("user")
        .addPathSegment(username.name)
        .build()
