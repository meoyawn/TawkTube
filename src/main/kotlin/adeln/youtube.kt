package adeln

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.Thumbnail
import com.rometools.rome.feed.synd.SyndEntry
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient

fun mkYoutube(): YouTube =
    YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}
        .setApplicationName("TubeCast")
        .setYouTubeRequestInitializer(YouTubeRequestInitializer(Secrets.YT_KEY))
        .build()

fun thumbnail(playlist: PlaylistSnippet): Thumbnail =
    playlist.thumbnails.high

fun thumbnail(item: PlaylistItem): Thumbnail =
    item.snippet.thumbnails.high

fun videoId(pi: PlaylistItem): String =
    pi.snippet.resourceId.videoId

fun playlistEntries(client: OkHttpClient, yt: YouTube, playlistId: String): Deferred<List<SyndEntry>> =
    async(CommonPool) {
        yt.playlistItems()
            .list("snippet")
            .setPlaylistId(playlistId)
            .setMaxResults(50)
            .execute()
            .items
            .map {
                asFeed(client, it)
            }
            .map {
                it.await()
            }
    }

fun playlistInfo(yt: YouTube, playlistId: String): Deferred<Playlist> =
    async(CommonPool) {
        yt.playlists()
            .list("snippet")
            .setId(playlistId)
            .execute()
            .items
            .first()
    }
