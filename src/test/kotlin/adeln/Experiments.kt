package adeln

import io.kotlintest.matchers.shouldEqual
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.HttpUrl
import org.junit.Test

class Experiments {

    @Test
    fun channel() {
        println(
            mkYoutube().channel(ChannelId.ById("UCC3L8QaxqEGUiBC252GHy3w"))
        )

        println(
            mkYoutube().channel(ChannelId.ById("UCFQ6Gptuq-sLflbJ4YY3Umw"))
        )
    }

    @Test
    fun youtube() {
        println(
            audio(mkClient(), VideoID("v2soHxEN79c"), Player.BROWSER)
        )
    }

    @Test
    fun badVideos() {
        listOf("6G59zsjM2UI", "1gYIGFr7zA8", "9qOQGBVedAQ", "1TN_Ig8sjxc")
            .map(::VideoID)
            .forEach {
                println(
                    audio(mkClient(), it, Player.BROWSER)
                )
            }
    }

    @Test
    fun badPlaylist() {
        runBlocking {
            println(asFeed(mkClient(), mkYoutube(), PlaylistID("PLdJo8g6QW5jboqKz4d6H3UawnFzJRH4hO")))
        }
    }

    @Test
    fun badAudio() {
        println(
            audio(mkClient(), VideoID("iAst9D6js1g"), Player.BROWSER)
        )
    }

    @Test
    fun disk() {
        resolve(HttpUrl.parse("https://yadi.sk/d/I5HDo-VY3R4Bvn")!!) shouldEqual
            HttpUrl.parse("http://localhost:8080/yandexdisk/public?link=https://yadi.sk/d/I5HDo-VY3R4Bvn")
    }

    @Test
    fun video() {
        resolve(HttpUrl.parse("https://www.youtube.com/watch?v=g2tyOLvArw0")!!) shouldEqual
            HttpUrl.parse("http://localhost:8080/video?v=g2tyOLvArw0")
    }

    @Test
    fun audioUrl() {
        audioUrl(VideoID("ha")) shouldEqual HttpUrl.parse("http://localhost:8080/audio?v=ha")
    }

    @Test
    fun recursiveFolder() {
        println(mkYandexDisk().recursiveResource("https://yadi.sk/d/EtEGI2fZ3SQUGS").files.map { it.pathToTitle() })
    }

    @Test
    fun mobileYt() {
        resolve(HttpUrl.parse("https://m.youtube.com/playlist?list=PLE7DDD91010BC51F8")!!) shouldEqual
            HttpUrl.parse("http://localhost:8080/playlist?list=PLE7DDD91010BC51F8")
    }
}
