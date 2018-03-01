package adeln

import com.rometools.rome.io.SyndFeedOutput
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.request.ApplicationRequest
import io.ktor.response.respondRedirect
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.title
import kotlinx.html.ul
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

object Secrets {
    const val YT_KEY = "AIzaSyBXaU6RB0KwBFqEz5sdcyjXiNySefvUHLc"
}

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
    val HOST = HttpUrl.parse(System.getenv("HEROKU_URL") ?: "http://localhost:$PORT")!!
}

fun mkClient(): OkHttpClient =
    OkHttpClient.Builder()
        .followRedirects(true)
        .build()

val BLOCKING_IO = Executors.newCachedThreadPool().asCoroutineDispatcher()

fun main(args: Array<String>) {

    val client = mkClient()
    val youtube = mkYoutube()
    val output = SyndFeedOutput()
    val yandexDisk = mkYandexDisk()

    embeddedServer(Netty, port = Config.PORT) {

        install(Compression)

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

                val feed = async(BLOCKING_IO) { asFeed(youtube, channelId, call.request.player()) }.await()

                call.respondWrite {
                    output.output(feed, this)
                }
            }

            get("/user/{username}") {
                val username = ChannelId.ByName(call.parameters["username"]!!)

                val feed = async(BLOCKING_IO) { asFeed(youtube, username, call.request.player()) }.await()

                call.respondWrite {
                    output.output(feed, this)
                }
            }

            get("/playlist") {
                val playlistId = PlaylistID(call.parameters["list"]!!)

                val feed = async(BLOCKING_IO) { asFeed(youtube, playlistId, call.request.player()) }.await()

                call.respondWrite {
                    output.output(feed, this)
                }
            }

            get("/video") {
                val videoId = VideoID(call.parameters["v"]!!)

                call.respondWrite {
                    output.output(asFeed(youtube, videoId, call.request.player()), this)
                }
            }

            get("/audio") {

                val videoId = VideoID(call.parameters["v"]!!)

                val player =
                    if (call.request.isBrowser()) Player.BROWSER
                    else Player.OTHER

                val audio = audio(client, videoId, player)

                println("got audio $audio")

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

fun ApplicationRequest.isBrowser(): Boolean =
    headers[HttpHeaders.UserAgent]?.startsWith("Mozilla") == true

fun ApplicationRequest.player(): Player =
    if (isBrowser()) Player.BROWSER else Player.OTHER

/* ktlint-disable chain-wrapping */
private fun HTML.renderHome(url: String?, resolved: String?) {

    val title = "YouTube to audio podcast converter"

    head {
        title(title)
    }

    body {

        h1 { +title }

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
            input(type = InputType.submit)
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
                p { +"Failed to resolve that url. You can ping me: comrade.adeln@ya.ru" }
        }

        p {
            a(href = "https://github.com/adelnizamutdinov/youtube-rss") { +"Source code" }
        }
    }
}
/* ktlint-enable chain-wrapping */
