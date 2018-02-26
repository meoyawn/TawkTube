package adeln

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.ChannelContentDetails
import com.google.api.services.youtube.model.ChannelSnippet
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.Thumbnail
import com.google.api.services.youtube.model.ThumbnailDetails
import com.google.api.services.youtube.model.VideoContentDetails
import com.google.api.services.youtube.model.VideoSnippet
import com.rometools.rome.feed.synd.SyndEntry
import okhttp3.HttpUrl
import java.util.Date

data class VideoID(val id: String)

data class PlaylistID(val id: String)

sealed class ChannelId {
    data class ById(val id: String) : ChannelId()
    data class ByName(val name: String) : ChannelId()
}

fun link(id: VideoID): HttpUrl =
    HttpUrl.parse("https://youtube.com/watch?v=${id.id}")!!

fun link(id: PlaylistID): HttpUrl =
    HttpUrl.parse("https://youtube.com/playlist?list=${id.id}")!!

fun link(channel: ChannelId): HttpUrl =
    when (channel) {
        is ChannelId.ById ->
            "https://youtube.com/channel/${channel.id}"

        is ChannelId.ByName ->
            "https://youtube.com/user/${channel.name}"
    }.let { HttpUrl.parse(it)!! }

fun mkYoutube(): YouTube =
    YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TubeCast")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.YT_KEY))
        .build()

fun ThumbnailDetails.best(): Thumbnail? =
    asSequence()
        .map { (_, v) -> v as Thumbnail }
        .maxBy { it.width ?: Long.MIN_VALUE }

fun DateTime.toDate(): Date =
    Date(value)

data class YtChannel(
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails
)

data class YtVideo(
    val snippet: VideoSnippet,
    val contentDetails: VideoContentDetails
)

fun playlistEntries(yt: YouTube, playlistID: PlaylistID, player: Player): List<SyndEntry> {
    val snippets = yt.playlistItems(playlistID)
    val details = yt.videos(snippets.map { VideoID(it.resourceId.videoId) })
    return snippets.zip(details) { snippet, detail -> entry(snippet.toVideo(), detail, player) }
}

fun YouTube.playlistItems(playlistID: PlaylistID): List<PlaylistItemSnippet> =
    playlistItems()
        .list("snippet")
        .setPlaylistId(playlistID.id)
        .setMaxResults(50)
        .execute()
        .items
        .map { it.snippet }

fun YouTube.playlistInfo(playlistID: PlaylistID): PlaylistSnippet? =
    playlists()
        .list("snippet")
        .setId(playlistID.id)
        .execute()
        .items
        .firstOrNull()
        ?.snippet

fun YouTube.videoInfo(id: VideoID): YtVideo =
    videos()
        .list("snippet,contentDetails")
        .setId(id.id)
        .execute()
        .items
        .first()
        .let { YtVideo(snippet = it.snippet, contentDetails = it.contentDetails) }

fun YouTube.videos(ids: List<VideoID>): List<VideoContentDetails> =
    videos()
        .list("contentDetails")
        .setId(ids.joinToString(separator = ",") { it.id })
        .setMaxResults(50)
        .execute()
        .items
        .map { it.contentDetails }

fun YouTube.channel(channel: ChannelId): YtChannel =
    channels()
        .list("snippet,contentDetails")
        .run {
            when (channel) {
                is ChannelId.ById ->
                    setId(channel.id)

                is ChannelId.ByName ->
                    setForUsername(channel.name)
            }
        }
        .execute()
        .items
        .first()
        .let { YtChannel(snippet = it.snippet, contentDetails = it.contentDetails) }
