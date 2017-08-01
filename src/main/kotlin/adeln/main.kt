package adeln

import com.rometools.rome.io.SyndFeedOutput
import okhttp3.OkHttpClient
import org.jetbrains.ktor.content.respondWrite
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
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
    val output = SyndFeedOutput()

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/channel") {

            }

            get("/video") {
                notImplemented {
                    val videoId = VideoID(call.parameters["v"]!!)
                    asFeed(client, youtube, videoId)
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

                    val feed = asFeed(client, youtube, playlistId)

                    call.respondWrite {
                        output.output(feed, this)
                    }
                }
            }

            get("/audio") {
                val videoId = VideoID(call.parameters["v"]!!)
                audio(client, videoId)
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
