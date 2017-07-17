package adeln

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

fun videoInfoUrl(videoId: String): HttpUrl =
    HttpUrl.parse("http://www.youtube.com/get_video_info")!!.newBuilder()
        .setQueryParameter("video_id", videoId)
        .build()

fun videoInfoRequest(videoId: String): Request =
    Request.Builder()
        .url(videoInfoUrl(videoId))
        .build()

fun equalsPair(s: String): Pair<String, String> =
    s.split("=").let { (a, b) -> a to URLDecoder.decode(b, "utf-8") }

fun audioUrl(client: OkHttpClient, videoId: String): String =
    client.newCall(videoInfoRequest(videoId))
        .execute()
        .body()!!
        .string()
        .split("&")
        .asSequence()
        .map(::equalsPair)
        .find { (k, _) -> k == "adaptive_fmts" }!!
        .second
        .split(",")
        .asSequence()
        .map {
            it.split("&")
                .asSequence()
                .map(::equalsPair)
                .toMap()
        }
        .filter { "audio" in it["type"]!! }
        .maxBy { it["bitrate"]!!.toInt() }!!["url"]!!
