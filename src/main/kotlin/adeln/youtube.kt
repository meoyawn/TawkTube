package adeln

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.ChannelContentDetails
import com.google.api.services.youtube.model.ChannelSnippet
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

data class VideoID(val id: String)
data class PlaylistID(val id: String)
data class ChannelID(val id: String)

fun link(id: VideoID): HttpUrl =
    HttpUrl.parse("https://youtube.com/watch?v=${id.id}")!!

fun link(id: PlaylistID): HttpUrl =
    HttpUrl.parse("https://youtube.com/playlist?list=${id.id}")!!

fun link(id: ChannelID): HttpUrl =
    HttpUrl.parse("https://youtube.com/channel/${id.id}")!!

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

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

fun playlistEntries(client: OkHttpClient, yt: YouTube, playlistID: PlaylistID): Deferred<List<SyndEntry>> =
    async(BLOCKING_IO) {
        yt.playlistItems()
            .list("snippet")
            .setPlaylistId(playlistID.id)
            .setMaxResults(50)
            .execute()
            .items
            .map {
                async(BLOCKING_IO) {
                    entry(client, it.snippet.toVideo())
                }
            }
            .map {
                it.await()
            }
    }

fun YouTube.playlistInfo(playlistID: PlaylistID): PlaylistSnippet? =
    playlists()
        .list("snippet")
        .setId(playlistID.id)
        .execute()
        .items
        .firstOrNull()
        ?.snippet

fun YouTube.videoInfo(id: VideoID): VideoSnippet =
    videos()
        .list("snippet")
        .setId(id.id)
        .execute()
        .items
        .first()
        .snippet

fun YouTube.channel(id: ChannelID): YtChannel =
    channels()
        .list("snippet,contentDetails")
        .setId(id.id)
        .execute()
        .items
        .first()
        .let {
            YtChannel(
                snippet = it.snippet,
                contentDetails = it.contentDetails
            )
        }
