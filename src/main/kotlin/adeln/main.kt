package adeln

import com.google.api.services.youtube.YouTube
import com.rometools.rome.io.SyndFeedOutput
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

object Config {
    val ADDR = "http://165.227.137.147"
}

fun main(args: Array<String>) {
    val client = OkHttpClient()
    val youtube = mkYoutube()

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/channel") {

            }

            get("/video") {
                val videoId = VideoId(call.parameters["v"]!!)
            }

            get("/playlist") { playlist(client, youtube) }

            get("/audio") {
                val videoId = VideoId(call.parameters["v"]!!)
                audio(client, videoId)
                    ?.let { call.respondRedirect(it.url.toString()) }
                    ?: error("404")
            }
        }
    }.start(wait = true)
}

suspend fun PipelineContext<*>.playlist(client: OkHttpClient, youTube: YouTube): Unit =
    try {
        val id = call.parameters["list"]

        val feed = asFeed(client, youTube, PlaylistId(id!!))

        call.respondWrite {
            SyndFeedOutput().output(feed, this)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
