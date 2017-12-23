package adeln

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

fun videoInfoUrl(videoID: VideoID): HttpUrl =
    HttpUrl.parse("http://www.youtube.com/get_video_info")!!.newBuilder()
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
    val bitrate: Float,
    val lengthSeconds: Long
)

fun Audio.sizeBytes(): Long =
    (lengthSeconds * bitrate / 8F).toLong()

fun Audio.lengthMillis(): Long =
    lengthSeconds * 1000L

enum class Player {
    BROWSER,
    OTHER,
}

fun playerType(type: MimeType, player: Player): Boolean =
    when (player) {
        Player.BROWSER ->
            "webm" in type

        Player.OTHER ->
            "webm" !in type
    }

fun audio(client: OkHttpClient, videoID: VideoID, player: Player = Player.OTHER): Audio =
    client.newCall(videoInfoRequest(videoID))
        .execute()
        .body()!!
        .string()
        .split("&")
        .asSequence()
        .map(::equalsPair)
        .toMap()
        .let { map ->
            val audios = map["adaptive_fmts"]
                ?.split(",")
                ?.asSequence()
                ?.map {
                    val m = it.split("&")
                        .asSequence()
                        .map(::equalsPair)
                        .toMap()

                    Audio(
                        type = m["type"]!!,
                        url = HttpUrl.parse(m["url"]!!)!!,
                        bitrate = m["bitrate"]!!.toFloat(),
                        lengthSeconds = map["length_seconds"]!!.toLong()
                    )
                }
                ?.toList()

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
