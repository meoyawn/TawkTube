package adeln

import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class YoutubeToMp3(
    val title: String,
    val length: Long,
    val link: String
)

fun youtubeToMp3(client: OkHttpClient, videoID: VideoID, moshi: Moshi): Audio? {
    val videoLink = videoLink(videoID)

    val req = Request.Builder()
        .url("http://www.youtubeinmp3.com/fetch/?format=JSON&video=$videoLink")
        .build()

    val resp = client.newCall(req).execute()

    return try {
        val yiM3 = moshi.adapter(YoutubeToMp3::class.java)
            .fromJson(resp.body()!!.source())!!

        val bitrate = Request.Builder()
            .head()
            .url(yiM3.link)
            .build()

        val hs = client.newCall(bitrate).execute().headers()

        hs["Content-Length"]?.toLong()?.let { size ->
            Audio(
                type = hs["Content-Type"]!!,
                url = HttpUrl.parse(yiM3.link)!!,
                bitrate = adeln.bitrate(yiM3.length, size),
                lengthSeconds = yiM3.length
            )
        }
    } catch (e: JsonEncodingException) {
        println("failed $videoLink")
        null
    }
}
