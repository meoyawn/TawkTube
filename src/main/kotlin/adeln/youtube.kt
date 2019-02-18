package adeln

import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.ChannelContentDetails
import com.google.api.services.youtube.model.ChannelSnippet
import com.google.api.services.youtube.model.PlaylistItemListResponse
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

data class YtChannel(
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails
)

data class YtVideo(
    val snippet: VideoSnippet,
    val contentDetails: VideoContentDetails
)

data class Details(
    val id: VideoID,
    val details: VideoContentDetails
)

fun link(id: VideoID): HttpUrl =
    HttpUrl.get("https://youtube.com/watch?v=${id.id}")

fun link(id: PlaylistID): HttpUrl =
    HttpUrl.get("https://youtube.com/playlist?list=${id.id}")

fun link(channel: ChannelId): HttpUrl =
    HttpUrl.get(when (channel) {
        is ChannelId.ById ->
            "https://youtube.com/channel/${channel.id}"

        is ChannelId.ByName ->
            "https://youtube.com/user/${channel.name}"
    })

fun mkYoutube(): YouTube =
    YouTube.Builder(ApacheHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TawkTube")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.SECRET))
        .build()

fun ThumbnailDetails.best(): Thumbnail? =
    asSequence()
        .map { (_, v) -> v as Thumbnail }
        .maxBy { it.width ?: Long.MIN_VALUE }

fun DateTime.toDate(): Date =
    Date(value)

fun playlistEntries(yt: YouTube, playlistID: PlaylistID, player: Player): List<SyndEntry> {
    val full = paging<PlaylistVideos, String>(
        load = { yt.playlistVideos(playlistID, pageToken = it) },
        nextPage = { it.items.nextPageToken },
        limit = 18
    )

    val items = full.flatMap { it.items.items }
    val details = full.flatMap { it.videos }

    return details.map { detail ->
        val snippet = items.first { it.snippet.resourceId.videoId == detail.id.id }
        entry(video = snippet.snippet.toVideo(), audio = detail.details, player = player)
    }
}

private fun YouTube.playlistItems(playlistID: PlaylistID, pageToken: String?): PlaylistItemListResponse =
    playlistItems()
        .list("snippet")
        .setPlaylistId(playlistID.id)
        .setMaxResults(50)
        .setPageToken(pageToken)
        .execute()

data class PlaylistVideos(
    val items: PlaylistItemListResponse,
    val videos: List<Details>
)

fun YouTube.playlistVideos(playlistID: PlaylistID, pageToken: String?): PlaylistVideos {
    val items = playlistItems(playlistID, pageToken)
    val videos = videos(items.items.map { VideoID(it.snippet.resourceId.videoId) })
    return PlaylistVideos(items, videos)
}

fun YouTube.playlistInfo(playlistID: PlaylistID): PlaylistSnippet =
    playlists()
        .list("snippet")
        .setId(playlistID.id)
        .execute()
        .items
        .first()
        .snippet

fun YouTube.videoInfo(id: VideoID): YtVideo =
    videos()
        .list("snippet,contentDetails")
        .setId(id.id)
        .execute()
        .items
        .first()
        .let { YtVideo(snippet = it.snippet, contentDetails = it.contentDetails) }

fun YouTube.videos(ids: List<VideoID>): List<Details> =
    videos()
        .list("contentDetails")
        .setId(ids.joinToString(separator = ",") { it.id })
        .setMaxResults(50)
        .execute()
        .items
        .map { Details(VideoID(it.id), it.contentDetails) }

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
