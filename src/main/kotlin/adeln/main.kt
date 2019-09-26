package adeln

import com.rometools.rome.io.SyndFeedOutput
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.engine.apache.Apache
import io.ktor.features.AutoHeadResponse
import io.ktor.features.Compression
import io.ktor.features.MissingRequestParameterException
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.StringValues
import io.ktor.util.filter
import io.ktor.util.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import kotlinx.coroutines.withContext
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h2
import kotlinx.html.h4
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.role
import kotlinx.html.title
import kotlinx.html.ul
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object Config {
    val PORT = System.getenv("PORT")?.toInt() ?: 8080
    val HOST = (System.getenv("HEROKU_URL") ?: "http://localhost:$PORT").toHttpUrl()
    val SECRET = System.getenv("YOUTUBE_SECRET") ?: error("set your YOUTUBE_SECRET env var")
}

fun mkClient(): OkHttpClient =
    OkHttpClient.Builder()
        .followRedirects(true)
        .build()

@KtorExperimentalAPI
fun main() {

    val client = mkClient()
    val youtube = mkYoutube()
    val output = SyndFeedOutput()
    val yandexDisk = mkYandexDisk()
    val ktorClient = HttpClient(Apache)

    embeddedServer(Netty, port = Config.PORT) {

        install(Compression)
        install(AutoHeadResponse)

        routing {

            static {
                resource("robots.txt")
            }

            get("/") {
                val url = call.parameters["url"]
                val resolved = url?.toHttpUrlOrNull()?.let(::resolve)

                call.respondHtml {
                    renderHome(url, resolved)
                }
            }

            get("/channel/{channelId}") {
                val channelId = ChannelId.ById(call.parameters["channelId"]!!)

                val feed = withContext(Dispatchers.IO) { asFeed(youtube, channelId, call.request.player()) }

                call.respondText(text = output.outputString(feed))
            }

            get("/user/{username}") {
                val username = ChannelId.ByName(call.parameters["username"]!!)

                val feed = withContext(Dispatchers.IO) { asFeed(youtube, username, call.request.player()) }

                call.respondText(text = output.outputString(feed))
            }

            get("/playlist") {
                val playlistId = PlaylistID(call.parameters["list"] ?: throw MissingRequestParameterException("list"))

                withContext(Dispatchers.IO) { asFeed(youtube, playlistId, call.request.player()) }
                    ?.let { call.respondText(text = output.outputString(it)) }
                    ?: call.respond(HttpStatusCode.NotFound, "not found")
            }

            get("/video") {
                val videoId = VideoID(call.parameters["v"]!!)

                val feed = asFeed(youtube, videoId, call.request.player())
                call.respondText(text = output.outputString(feed))
            }

            val cache = ConcurrentHashMap<VideoID, Pair<Instant, Audio>>()

            fun Pair<Instant, Audio>.valid(): Boolean =
                Duration.between(first, Instant.now()) < Duration.ofMinutes(4)

            get("/audio") {

                val videoId = VideoID(call.parameters["v"]!!)

                val audio = cache[videoId]?.takeIf { it.valid() }?.second ?: run {

                    println("${call.request.httpMethod.value} ${call.request.uri}")
                    println(call.request.headers.toMap().toList().joinToString(separator = "\n"))

                    val player = when {
                        call.request.isBrowser() -> Player.BROWSER
                        else -> Player.OTHER
                    }

                    audio(client, videoId, player).also { cache[videoId] = Instant.now() to it }
                }

                call.proxy(ktorClient, audio.url)
            }

            route("/yandexdisk") {

                get("/public") {
                    val url = call.parameters["link"]!!.toHttpUrl()
                    val feed = yandexDisk.asFeed(url)
                    call.respondText(text = output.outputString(feed))
                }

                get("/audio") {
                    val publicKey = call.parameters["publicKey"]!!
                    val path = call.parameters["path"]!!
                    withContext(Dispatchers.IO) {
                        call.respondRedirect(url = yandexDisk.getPublicResourceDownloadLink(publicKey, path).href)
                    }
                }
            }
        }

        Unit
    }.start(wait = true)
}

fun ApplicationRequest.isBrowser(): Boolean =
    headers[HttpHeaders.UserAgent]?.startsWith("Mozilla") == true

fun ApplicationRequest.player(): Player =
    if (isBrowser()) Player.BROWSER else Player.OTHER

fun Headers.without(headers: Set<String>): StringValues =
    filter { key, _ -> headers.all { !it.equals(key, ignoreCase = true) } }

suspend fun ApplicationCall.proxy(client: HttpClient, url: HttpUrl) {

    val result =
        client.call(url.toString()) { headers.appendAll(request.headers.without(setOf(HttpHeaders.Host))) }.response

    val resultHeaders = result.headers

    respond(object : OutgoingContent.WriteChannelContent() {

        override val contentLength: Long? = resultHeaders[HttpHeaders.ContentLength]?.toLong()

        override val contentType: ContentType? = resultHeaders[HttpHeaders.ContentType]?.let { ContentType.parse(it) }

        override val headers: Headers = Headers.build {
            appendAll(resultHeaders.without(setOf(HttpHeaders.ContentType, HttpHeaders.ContentLength)))
        }

        override val status: HttpStatusCode? = result.status

        override suspend fun writeTo(channel: ByteWriteChannel) {
            result.content.copyAndClose(channel)
        }
    })
}

private fun HTML.renderHome(url: String?, resolved: HttpUrl?) {

    lang = "en"

    val title = "TawkTube"

    head {
        meta {
            charset = "utf-8"
        }

        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1, shrink-to-fit=no"
        }

        meta {
            name = "description"
            content = "Listen to YouTube and audiobooks as a podcasts"
        }

        link {
            rel = "stylesheet"
            href = "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
        }

        title(title)
    }

    body {
        div(classes = "container") {
            br

            h2 { +title }

            p { +"YouTube to podcast converter" }

            div(classes = "alert alert-warning") {
                role = "alert"

                p {
                    +"Since "
                    a(href = "https://www.producthunt.com/posts/tawktube") { +"the launch on ProductHunt" }
                    +" TawkTube is experiencing difficulties with it's current architecture and the YouTube API daily quota."
                }
                p { +"I'm working on it, but I'm pretty incompetent and it'll take me some time to figure this out." }
                p { +"Thank you for sticking around" }
            }

            h4 { +"Usage" }

            p { +"Paste a link to a:" }

            ul {
                li { +"youtube video" }
                li { +"youtube playlist" }
                li { +"youtube channel" }
                li { +"yandex disk public folder with your audiobook" }
            }

            form(action = "/") {
                div(classes = "input-group") {
                    input(classes = "form-control") {
                        name = "url"
                        url?.let { value = it }
                    }
                    div(classes = "input-group-append") {
                        input(classes = " btn btn-outline-secondary", type = InputType.submit)
                    }
                }
            }

            when {
                url != null && resolved != null -> {

                    p { +"Here's your podcast:" }

                    val theLink = resolved.toString()
                    a(href = theLink) { +theLink }

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

            br

            h4 { +"Money tawk" }

            p { +"You can support the development of this" }

            a(classes = "btn btn-primary mr-1", href = "https://www.paypal.me/adelniz") {
                role = "button"

                +"PayPal"
            }

            a(classes = "btn btn-warning", href = "https://www.patreon.com/TawkTube") {
                role = "button"

                +"Patreon"
            }

            br
            br

            p {
                a(href = "https://github.com/adelnizamutdinov/youtube-rss") { +"Source code" }
            }
        }
    }
}
