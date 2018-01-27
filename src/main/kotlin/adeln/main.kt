package adeln

import com.rometools.rome.io.SyndFeedOutput
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.input
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import org.jetbrains.ktor.cio.WriteChannel
import org.jetbrains.ktor.cio.toOutputStream
import org.jetbrains.ktor.content.FinalContent
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.html.respondHtml
import org.jetbrains.ktor.http.HttpHeaders
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.response.respondWrite
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing

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
    val yandexDisk = mkYandexDisk()

    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port) {
        routing {

            get("/") {
                call.respondHtml {
                    head {

                    }
                    body {
                        input {

                        }
                    }
                }
            }

            get("/channel/{channelId}") {
                val channelId = ChannelID(call.parameters["channelId"]!!)

                val feed = asFeed(client, youtube, channelId)

                call.respondWrite {
                    output.output(feed, this)
                }
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

                feed?.let {
                    call.respondWrite {
                        output.output(feed, this)
                    }
                } ?: call.respondText(status = HttpStatusCode.NotFound, text = "$playlistId does not exist")
            }

            get("/audio") {

                val videoId = VideoID(call.parameters["v"]!!)
                val browser = call.request.headers[HttpHeaders.UserAgent]?.startsWith("Mozilla/") == true

                val player =
                    if (browser) Player.BROWSER
                    else Player.OTHER

                val audio = audio(client, videoId, player)

                val req = Request.Builder()
                    .url(audio.url)
                    .build()

                val resp = client.newCall(req).await()
                call.response.status(HttpStatusCode.fromValue(resp.code()))
                call.response.okHeaders = resp.headers()
                call.respond(object : FinalContent.WriteChannelContent() {
                    suspend override fun writeTo(channel: WriteChannel) {
                        Okio.buffer(Okio.sink(channel.toOutputStream())).use { sink ->
                            resp.body()!!.source().use { src ->
                                src.readAll(sink)
                            }
                        }
                    }
                })
            }

            route("/yandexdisk") {

                get("/public") {
                    val url = HttpUrl.parse(call.parameters["link"]!!)!!
                    val feed = yandexDisk.asFeed(url)

                    call.respondWrite {
                        output.output(feed, this)
                    }
                }

                get("/audio") {
                    val publicKey = call.parameters["publicKey"]!!
                    val path = call.parameters["path"]!!
                    call.respondRedirect(yandexDisk.getPublicResourceDownloadLink(publicKey, path).href)
                }
            }

        }
    }.start(wait = true)
}
