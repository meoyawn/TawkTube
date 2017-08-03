package adeln

import com.squareup.moshi.Moshi
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

fun bitrate(length: Long, size: Long): Float =
    (size * 8F) / length

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

fun audio(client: OkHttpClient, videoID: VideoID, moshi: Moshi, player: Player = Player.OTHER): Audio? =
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
                        url = HttpUrl.parse(m["url"])!!,
                        bitrate = m["bitrate"]!!.toFloat(),
                        lengthSeconds = map["length_seconds"]!!.toLong()
                    )
                }
                ?.toList()

            goodAudio(audios, player)
                ?: anyAudio(audios)
                ?: youtubeToMp3(client, videoID, moshi)
        }

fun goodAudio(audios: List<Audio>?, player: Player): Audio? =
    audios
        ?.filter { "audio" in it.type && playerType(it.type, player) }
        ?.maxBy { it.bitrate }

fun anyAudio(audios: List<Audio>?): Audio? =
    audios
        ?.filter { "audio" in it.type }
        ?.maxBy { it.bitrate }

data class YoutubeToMp3(
    val title: String,
    val length: Long,
    val link: String
)

fun youtubeToMp3(client: OkHttpClient, videoID: VideoID, moshi: Moshi): Audio {
    val req = Request.Builder()
        .url("http://www.youtubeinmp3.com/fetch/?format=JSON&video=https://www.youtube.com/watch?v=${videoID.id}")
        .build()

    val resp = client.newCall(req).execute()

    val yiM3 = moshi.adapter(YoutubeToMp3::class.java)
        .fromJson(resp.body()!!.source())!!

    val bitrate = Request.Builder()
        .head()
        .url(yiM3.link)
        .build()

    val hs = client.newCall(bitrate).execute().headers()

    val size = hs["Content-Length"]!!.toLong()

    return Audio(
        type = hs["Content-Type"]!!,
        url = HttpUrl.parse(yiM3.link)!!,
        bitrate = adeln.bitrate(yiM3.length, size),
        lengthSeconds = yiM3.length
    )
}
