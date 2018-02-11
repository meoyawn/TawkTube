package adeln

import okhttp3.HttpUrl

fun resolve(url: HttpUrl): HttpUrl? =
    when (url.host()) {
        "youtube.com", "www.youtube.com" ->
            resolveYt(url)

        "youtu.be" ->
            resolveShortYt(url)

        "yadi.sk" ->
            buildYaDisk(url)

        else ->
            null
    }

fun resolveShortYt(url: HttpUrl): HttpUrl? =
    url.pathSegments().lastOrNull()?.takeIf(String::isNotBlank)?.let { buildVideo(VideoID(it)) }

fun buildYaDisk(url: HttpUrl): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegment("yandexdisk")
        .addPathSegment("public")
        .addQueryParameter("link", url.toString())
        .build()

fun resolveYt(url: HttpUrl): HttpUrl? =
    url.queryParameter("v")?.takeIf(String::isNotBlank)?.let { buildVideo(VideoID(it)) }
        ?: url.queryParameter("list")?.takeIf(String::isNotBlank)?.let { buildPlaylist(PlaylistID(it)) }
        ?: url.pathSegments()?.takeIf { "channel" in it }?.last()?.takeIf(String::isNotBlank)?.let { buildChannel(ChannelId.ById(it)) }
        ?: url.pathSegments()?.takeIf { "user" in it }?.last()?.takeIf(String::isNotBlank)?.let { buildUser(ChannelId.ByName(it)) }

fun buildVideo(videoID: VideoID): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegments("video")
        .addQueryParameter("v", videoID.id)
        .build()

fun buildPlaylist(playlistID: PlaylistID): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegment("playlist")
        .addQueryParameter("list", playlistID.id)
        .build()

fun buildChannel(channelID: ChannelId.ById): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegment("channel")
        .addPathSegment(channelID.id)
        .build()

fun buildUser(username: ChannelId.ByName): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegment("user")
        .addPathSegment(username.name)
        .build()
