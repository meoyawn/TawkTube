package adeln

import com.google.api.services.youtube.YouTube
import com.rometools.rome.io.SyndFeedOutput
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

fun main(args: Array<String>) {
    val client = OkHttpClient()
    val youtube = mkYoutube()

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/video") {
                val videoId = VideoId(call.parameters["v"]!!)
            }

            get("/playlist") { playlist(client, youtube) }

            // legacy
            get("/playlists") { playlist(client, youtube) }
        }
    }.start(wait = true)
}

suspend fun PipelineContext<*>.playlist(client: OkHttpClient, youTube: YouTube): Unit =
    try {
        val id = call.parameters["list"] ?: call.parameters["id"]

        val feed = asFeed(client, youTube, PlaylistId(id!!))

        call.respondWrite {
            SyndFeedOutput().output(feed, this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
