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
import com.squareup.moshi.Moshi
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

fun videoLink(videoID: VideoID): HttpUrl =
    HttpUrl.parse("https://youtube.com/watch?v=${videoID.id}")!!

fun playlistLink(playlistID: PlaylistID): HttpUrl =
    HttpUrl.parse("https://youtube.com/playlist?list=${playlistID.id}")!!

fun mkYoutube(): YouTube =
    YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TubeCast")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.YT_KEY))
        .build()

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

fun ThumbnailDetails.best(): Thumbnail =
    asSequence()
        .map { (_, v) -> v as Thumbnail }
        .maxBy { it.width }!!

fun DateTime.toDate(): Date =
    Date(value)

data class Channel(
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails
)

fun playlistEntries(client: OkHttpClient, yt: YouTube, playlistID: PlaylistID, moshi: Moshi): Deferred<List<SyndEntry>> =
    async(BLOCKING_IO) {
        yt.playlistItems()
            .list("snippet")
            .setPlaylistId(playlistID.id)
            .setMaxResults(50)
            .execute()
            .items
            .map {
                async(BLOCKING_IO) {
                    entry(client, it.snippet.toVideo(), moshi)
                }
            }
            .mapNotNull {
                it.await()
            }
    }

fun YouTube.playlistInfo(playlistID: PlaylistID): PlaylistSnippet =
    playlists()
        .list("snippet")
        .setId(playlistID.id)
        .execute()
        .items
        .first()
        .snippet

fun YouTube.videoInfo(id: VideoID): VideoSnippet =
    videos()
        .list("snippet")
        .setId(id.id)
        .execute()
        .items
        .first()
        .snippet

fun YouTube.channel(id: ChannelID): Channel =
    channels()
        .list("snippet,contentDetails")
        .setId(id.id)
        .execute()
        .items
        .first()
        .let {
            Channel(it.snippet, it.contentDetails)
        }
