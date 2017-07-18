package adeln

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.rometools.modules.itunes.EntryInformationImpl
import com.rometools.modules.itunes.FeedInformationImpl
import com.rometools.modules.itunes.types.Duration
import com.rometools.modules.mediarss.MediaEntryModuleImpl
import com.rometools.modules.mediarss.types.MediaContent
import com.rometools.modules.mediarss.types.UrlReference
import com.rometools.rome.feed.module.DCModuleImpl
import com.rometools.rome.feed.rss.Channel
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import com.rometools.rome.io.impl.RSS20Generator
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.net.URL
import java.util.concurrent.Executors

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

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

fun entry(client: OkHttpClient, video: PlaylistItemSnippet): SyndEntry =
    SyndEntryImpl().also {
        val audio = audio(client, videoId(video))

        it.modules = mutableListOf(
            EntryInformationImpl().also {
                it.image = URL(thumbnail(video).url)
                it.duration = Duration(audio.lengthMillis())
                it.order = video.position.toInt()
            },
            MediaEntryModuleImpl().also {
                it.mediaContents = arrayOf(
                    MediaContent(UrlReference(audio.url)).also {
                        it.duration = audio.lengthSeconds
                        it.bitrate = audio.bitrate
                        it.type = audio.type
                        it.fileSize = audio.sizeBytes().toLong()
                    }
                )
            },
            DCModuleImpl()
        )

        it.title = video.title
        it.link = videoLink(videoId(video))
        it.author = video.channelTitle

        it.description = SyndContentImpl().also {
            it.value = video.description
        }
        it.publishedDate = video.publishedAt.toDate()

        it.enclosures = listOf(
            SyndEnclosureImpl().also {
                it.type = audio.type
                it.url = audio.url
                it.length = audio.sizeBytes().toLong()
            }
        )
    }

fun playlistLink(playlistId: String): String =
    "https://youtube.com/playlist?list=$playlistId"

fun videoLink(videoId: String): String =
    "https://youtube.com/watch?v=$videoId"

suspend fun asFeed(client: OkHttpClient, yt: YouTube, playlistId: String): SyndFeed {

    val playlist = playlistInfo(yt, playlistId).await().snippet

    return SyndFeedImpl(Channel(RSS20Generator().type)).also {

        it.modules = mutableListOf(
            FeedInformationImpl().also {
                it.image = URL(thumbnail(playlist).url)
            },
            DCModuleImpl()
        )

        it.title = playlist.title
        it.link = playlistLink(playlistId)
        it.description = playlist.description
        it.publishedDate = playlist.publishedAt.toDate()
        it.author = playlist.channelTitle
        it.entries = playlistEntries(client, yt, playlistId).await()
    }
}
