package adeln

import com.rometools.rome.io.SyndFeedOutput
import okhttp3.OkHttpClient
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpHeaders
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.response.respondWrite
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import org.jetbrains.ktor.util.toMap

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

object Config {
    val ADDR = "https://limitless-atoll-85321.herokuapp.com"
}

fun mkClient(): OkHttpClient =
    OkHttpClient.Builder()
        .followRedirects(true)
        .build()

fun main(args: Array<String>) {

    val client = mkClient()
    val youtube = mkYoutube()
    val output = SyndFeedOutput()

    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port) {
        routing {
            get("/channel") {

            }

            get("/video") {
                val videoId = VideoID(call.parameters["v"]!!)

                call.respondWrite {
                    output.output(asFeed(client, youtube, videoId), this)
                }
            }

            get("/playlist") {
                val playlistId = PlaylistID(call.parameters["list"]!!)

                val feed = asFeed(client, youtube, playlistId)

                call.respondWrite {
                    output.output(feed, this)
                }
            }

            get("/audio") {
                val headers = call.request.headers
                println(headers.toMap())

                val player = headers[HttpHeaders.UserAgent]
                    ?.startsWith("Mozilla/")
                    .takeIf { it == true }
                    ?.let { Player.BROWSER }
                    ?: Player.OTHER

                val videoId = VideoID(call.parameters["v"]!!)
                val audio = audio(client, videoId, player)
                call.respondRedirect(audio.url.toString())
            }
        }
    }.start(wait = true)
}
