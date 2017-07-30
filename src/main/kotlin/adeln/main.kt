package adeln

import com.rometools.rome.io.SyndFeedOutput
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.io.PrintWriter

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

            get("/playlist") {
                try {
                    val id = call.parameters["list"]

                    val feed = asFeed(client, youtube, PlaylistId(id!!))

                    call.respondWrite {
                        SyndFeedOutput().output(feed, this)
                    }
                } catch (e: Exception) {
                    call.response.status(HttpStatusCode.NotImplemented)
                    call.respondWrite {
                        e.printStackTrace(PrintWriter(this))
                    }
                }
            }

            get("/audio") {
                val videoId = VideoId(call.parameters["v"]!!)
                audio(client, videoId)
                    ?.let { call.respondRedirect(it.url.toString()) }
                    ?: error("404")
            }
        }
    }.start(wait = true)
}
