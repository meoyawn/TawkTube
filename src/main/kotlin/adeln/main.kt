package adeln

import com.rometools.rome.io.SyndFeedOutput
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.HTML
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


object Secrets {
    const val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
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
                    renderHome(url, resolved)
                }
            }

            get("/channel/{channelId}") {
                val channelId = ChannelId.ById(call.parameters["channelId"]!!)

                val feed = asFeed(client, youtube, channelId)

                call.respondWrite {
                    output.output(feed, this)
                }
            }

            get("/user/{username}") {
                val username = ChannelId.ByName(call.parameters["username"]!!)

                val feed = asFeed(client, youtube, username)

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

private fun HTML.renderHome(url: String?, resolved: String?): Unit =
    body {

        h1 { +"YouTube to audio podcast converter" }

        p { +"Paste a link to a:" }

        ul {
            li { +"youtube video" }
            li { +"youtube playlist" }
            li { +"youtube channel" }
            li { +"yandex disk public folder with your audiobook" }
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

                p {
                    +"You can listen to it using "

                    a(href = "https://pocketcasts.com/submit") { +"Pocket Casts" }

                    +" or "

                    a(href = "https://player.fm/importer/feed") { +"Player FM" }
                }
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
