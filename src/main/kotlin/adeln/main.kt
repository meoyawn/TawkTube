package adeln

import com.rometools.rome.io.SyndFeedOutput
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpHeaders
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import org.jetbrains.ktor.util.toMap
import java.io.PrintWriter

object Secrets {
    val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

object Config {
    val ADDR = "http://165.227.137.147"
}

fun mkClient(): OkHttpClient =
    OkHttpClient.Builder()
        .followRedirects(true)
        .build()

fun mkMoshi(): Moshi =
    Moshi.Builder()
        .build()

fun main(args: Array<String>) {

    val client = mkClient()
    val youtube = mkYoutube()
    val output = SyndFeedOutput()
    val moshi = mkMoshi()

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/channel") {

            }

            get("/video") {
                notImplemented {
                    val videoId = VideoID(call.parameters["v"]!!)
                    asFeed(client, youtube, videoId, moshi = moshi)
                        ?.let { feed ->
                            call.respondWrite {
                                output.output(feed, this)
                            }
                        }
                        ?: call.response.status(HttpStatusCode.NotImplemented)
                }
            }

            get("/playlist") {
                notImplemented {
                    val playlistId = PlaylistID(call.parameters["list"]!!)

                    val feed = asFeed(client, youtube, playlistId, moshi)

                    call.respondWrite {
                        output.output(feed, this)
                    }
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
                audio(client, videoId, moshi, player)
                    ?.let { call.respondRedirect(it.url.toString()) }
                    ?: error("404")
            }
        }
    }.start(wait = true)
}

suspend fun <T : Any> PipelineContext<T>.notImplemented(f: suspend PipelineContext<T>.() -> Unit): Unit =
    try {
        f()
    } catch (e: Exception) {
        call.response.status(HttpStatusCode.NotImplemented)
        call.respondWrite {
            e.printStackTrace(PrintWriter(this))
        }
    }
