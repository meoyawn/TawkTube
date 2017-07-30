package adeln

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.Thumbnail
import com.google.api.services.youtube.model.ThumbnailDetails
import com.google.api.services.youtube.model.VideoSnippet
import com.rometools.rome.feed.synd.SyndEntry
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.Date
import java.util.concurrent.Executors

data class VideoId(val id: String)
data class PlaylistId(val id: String)

fun videoLink(videoId: VideoId): HttpUrl =
    HttpUrl.parse("https://youtube.com/watch?v=${videoId.id}")!!

fun playlistLink(playlistId: PlaylistId): HttpUrl =
    HttpUrl.parse("https://youtube.com/playlist?list=${playlistId.id}")!!

fun mkYoutube(): YouTube =
    YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TubeCast")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.YT_KEY))
        .build()

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

fun ThumbnailDetails.best(): Thumbnail =
    maxBy { (_, v) ->
        val t = v as Thumbnail
        t.width * t.height
    }!!.value as Thumbnail

fun playlistEntries(client: OkHttpClient, yt: YouTube, playlistId: PlaylistId): Deferred<List<SyndEntry>> =
    async(BLOCKING_IO) {
        yt.playlistItems()
            .list("snippet")
            .setPlaylistId(playlistId.id)
            .setMaxResults(50)
            .execute()
            .items
            .map {
                async(BLOCKING_IO) {
                    entry(client, it.snippet.toRepr())
                }
            }
            .mapNotNull {
                it.await()
            }
    }

fun YouTube.playlistInfo(playlistId: PlaylistId): Deferred<PlaylistSnippet> =
    async(BLOCKING_IO) {
        playlists()
            .list("snippet")
            .setId(playlistId.id)
            .execute()
            .items
            .first()
            .snippet
    }

fun DateTime.toDate(): Date =
    Date(value)

fun YouTube.videoInfo(id: VideoId): VideoSnippet =
    videos()
        .list("snippet")
        .setId(id.id)
        .execute()
        .items
        .first()
        .snippet

data class Video(
    val id: VideoId,
    val thumbnails: ThumbnailDetails,
    val position: Long?,
    val title: String,
    val channelTitle: String,
    val description: String,
    val publishedAt: DateTime
)

fun VideoSnippet.toRepr(id: VideoId): Video =
    Video(
        id = id,
        thumbnails = thumbnails,
        position = null,
        title = title,
        channelTitle = channelTitle,
        description = description,
        publishedAt = publishedAt
    )

fun PlaylistItemSnippet.toRepr(): Video =
    Video(
        id = VideoId(resourceId.videoId),
        thumbnails = thumbnails,
        position = position,
        title = title,
        channelTitle = channelTitle,
        description = description,
        publishedAt = publishedAt
    )
