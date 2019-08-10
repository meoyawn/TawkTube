package adeln

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLDecoder
import kotlin.coroutines.resumeWithException

private val GET_VIDEO_INFO: HttpUrl = "https://www.youtube.com/get_video_info".toHttpUrl()

fun videoInfoUrl(videoID: VideoID): HttpUrl =
    GET_VIDEO_INFO.newBuilder()
        .setQueryParameter("video_id", videoID.id)
        .build()

fun videoInfoRequest(videoID: VideoID): Request =
    Request.Builder()
        .url(videoInfoUrl(videoID))
        .build()

fun equalsPair(s: String): Pair<String, String> =
    s.split("=").let { (a, b) -> a to URLDecoder.decode(b, "utf-8") }

typealias MimeType = String

data class Audio(
    val type: MimeType,
    val url: HttpUrl,
    val bitrate: Float
)

enum class Player {
    BROWSER,
    OTHER,
}

fun Player.audioType(): MimeType =
    when (this) {
        Player.BROWSER -> "audio/webm"
        Player.OTHER -> "audio/x-m4a"
    }

fun playerType(type: MimeType, player: Player): Boolean =
    when (player) {
        Player.BROWSER ->
            "webm" in type

        Player.OTHER ->
            "webm" !in type
    }

suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }

        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException): Unit =
                cont.resumeWithException(e)

            @Suppress("EXPERIMENTAL_API_USAGE")
            override fun onResponse(call: Call, response: Response): Unit =
                cont.resume(response) { response.close() }
        })
    }

suspend fun audio(client: OkHttpClient, videoID: VideoID, player: Player): Audio =
    client.newCall(videoInfoRequest(videoID))
        .await()
        .body!!
        .string()
        .split("&")
        .asSequence()
        .map(::equalsPair)
        .toMap()
        .let { map ->
            val audios = map["adaptive_fmts"]
                ?.split(",")
                ?.map {
                    val m = it.split("&")
                        .asSequence()
                        .map(::equalsPair)
                        .toMap()

                    Audio(
                        type = m["type"]!!,
                        url = m["url"]!!.toHttpUrl(),
                        bitrate = m["bitrate"]!!.toFloat()
                    )
                }

            goodAudio(audios, player) ?: anyAudio(audios)
        }
        ?: audio(client, FALLBACK, player)

fun goodAudio(audios: List<Audio>?, player: Player): Audio? =
    audios
        ?.filter { "audio" in it.type && playerType(it.type, player) }
        ?.maxBy { it.bitrate }

fun anyAudio(audios: List<Audio>?): Audio? =
    audios
        ?.filter { "audio" in it.type }
        ?.maxBy { it.bitrate }

val FALLBACK = VideoID("OAQ7l33UF3E")
