package adeln

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.Thumbnail
import com.google.api.services.youtube.model.ThumbnailDetails
import com.rometools.rome.feed.synd.SyndEntry
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient

fun mkYoutube(): YouTube =
    YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TubeCast")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.YT_KEY))
        .build()

fun thumbnail(playlist: PlaylistSnippet): Thumbnail =
    playlist.thumbnails.best()

fun thumbnail(item: PlaylistItemSnippet): Thumbnail =
    item.thumbnails.best()

fun ThumbnailDetails.best(): Thumbnail =
    maxBy { (_, v) -> (v as Thumbnail).width }!!.value as Thumbnail

fun videoId(pi: PlaylistItemSnippet): String =
    pi.resourceId.videoId

fun playlistEntries(client: OkHttpClient, yt: YouTube, playlistId: String): Deferred<List<SyndEntry>> =
    async(BLOCKING_IO) {
        yt.playlistItems()
            .list("snippet")
            .setPlaylistId(playlistId)
            .setMaxResults(50)
            .execute()
            .items
            .map {
                async(BLOCKING_IO) {
                    entry(client, it.snippet)
                }
            }
            .map {
                it.await()
            }
    }

fun playlistInfo(yt: YouTube, playlistId: String): Deferred<Playlist> =
    async(BLOCKING_IO) {
        yt.playlists()
            .list("snippet")
            .setId(playlistId)
            .execute()
            .items
            .first()
    }
