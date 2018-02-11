package adeln

import com.rometools.rome.io.SyndFeedOutput
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.ul
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.html.respondHtml
import org.jetbrains.ktor.http.HttpHeaders
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
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
    val ADDR = HttpUrl.parse("https://limitless-atoll-85321.herokuapp.com")!!
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
                val url = call.parameters["url"]
                val resolved = url?.let { HttpUrl.parse(it) }?.let { resolve(it) }?.toString()

                call.respondHtml {
                    body {

                        h1 { +"YouTube to audio podcast converter" }

                        p { +"Paste a link to a:" }

                        ul {
                            li { +"youtube video" }
                            li { +"youtube playlist" }
                            li { +"youtube channel" }
                            li { +"yandex disk public folder" }
                        }

                        form(action = "/") {
                            input(name = "url") {
                                size = "100"
                                url?.let { value = it }
                            }
                        }

                        when {
                            url != null && resolved != null -> {

                                p { +"Here's your podcast:" }

                                a(href = resolved) { +resolved }
                            }

                            url != null && resolved == null ->
                                p {
                                    +"Failed to resolve that url. You can ping me on "

                                    a(href = "https://twitter.com/meoyawn") { +"twitter" }
                                }
                        }

                        p {
                            a(href = "https://github.com/adelnizamutdinov/youtube-rss") { +"Source code" }
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

                call.respondRedirect(audio.url.toString())
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
