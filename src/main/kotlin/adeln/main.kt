package adeln

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistSnippet
import com.rometools.modules.itunes.EntryInformationImpl
import com.rometools.modules.itunes.FeedInformation
import com.rometools.modules.itunes.FeedInformationImpl
import com.rometools.rome.feed.module.Module
import com.rometools.rome.feed.rss.Channel
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import com.rometools.rome.io.impl.RSS20Generator
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.net.URL
import java.util.Date

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/playlists") {
                val playlistId = call.parameters["id"] ?: "PL22J3VaeABQAT-0aSPq-OKOpQlHyR4k5h"

                val feed = asFeed(OkHttpClient(), mkYoutube(), playlistId)

                call.respondWrite {
                    SyndFeedOutput().output(feed, this)
                }
            }
        }
    }.start(wait = true)
}

fun asFeed(client: OkHttpClient, pi: PlaylistItem): Deferred<SyndEntry> =
    async(CommonPool) {
        SyndEntryImpl().also {
            val audio = audioUrl(client, videoId(pi))

            it.contents = listOf(SyndContentImpl().also {
                it.type = audio.type
                it.value = audio.url
            })
            it.modules = mutableListOf<Module>(EntryInformationImpl().also {
                it.image = URL(thumbnail(pi).url)
            })
        }
    }

fun playlistLink(playlistId: String): String =
    "https://youtube.com/playlist?list=$playlistId"

fun toITunes(playlist: PlaylistSnippet): FeedInformation =
    FeedInformationImpl().also {
        it.image = URL(thumbnail(playlist).url)
    }

suspend fun asFeed(client: OkHttpClient, yt: YouTube, playlistId: String): SyndFeed {

    val playlist = playlistInfo(yt, playlistId).await().snippet

    return SyndFeedImpl(Channel(RSS20Generator().type)).also {
        it.title = playlist.title
        it.link = playlistLink(playlistId)
        it.description = playlist.description
        it.publishedDate = Date(playlist.publishedAt.value)

        it.author = playlist.channelTitle

        it.modules = mutableListOf<Module>(toITunes(playlist))
        it.entries = playlistEntries(client, yt, playlistId).await()
    }
}
