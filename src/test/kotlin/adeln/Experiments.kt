package adeln

import com.rometools.rome.io.SyndFeedOutput
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import java.net.URLEncoder

class Experiments {

    @Test
    fun disk() {
        resolve("https://yadi.sk/d/I5HDo-VY3R4Bvn".toHttpUrl()) shouldEqual
            "http://localhost:8080/yandexdisk/public?link=${URLEncoder.encode("https://yadi.sk/d/I5HDo-VY3R4Bvn", "utf-8")}".toHttpUrl()
    }

    @Test
    fun video() {
        resolve("https://www.youtube.com/watch?v=g2tyOLvArw0".toHttpUrl()) shouldEqual
            "http://localhost:8080/video?v=g2tyOLvArw0".toHttpUrl()
    }

    @Test
    fun audioUrl() {
        audioUrl(VideoID("ha")) shouldEqual "http://localhost:8080/audio?v=ha".toHttpUrl()
    }

    @Test
    fun mobileYt() {
        resolve("https://m.youtube.com/playlist?list=PLE7DDD91010BC51F8".toHttpUrl()) shouldEqual
            "http://localhost:8080/playlist?list=PLE7DDD91010BC51F8".toHttpUrl()
    }

    @Test
    fun instantRegret() {
        playlistEntries(mkYoutube(), PlaylistID("PLiQrdzH3aBWi6nh1kdbYfy2dd1CSOwBz5"), Player.BROWSER)!! should haveSize(69)
    }

    @Test
    fun bug34() {
        asFeed(mkYoutube(), PlaylistID("PLrxx1RzoWiyyfhkk84Vkz6XYaLrY6HMdDA"), Player.BROWSER) shouldBe null
        playlistEntries(mkYoutube(), PlaylistID("PLrxx1RzoWiyyfhkk84Vkz6XYaLrY6HMdDA"), Player.BROWSER) shouldBe null
    }

    @Ignore("yandex too many requests")
    @Test
    fun demons() {
        runBlocking {
            SyndFeedOutput().outputString(mkYandexDisk().asFeed("https://yadi.sk/d/4OHFuxV93Xn7H6".toHttpUrl()))
        }
    }

    @Test
    fun audio() {
        runBlocking {
            audio(OkHttpClient(), VideoID("Wex12GhUFqE"), Player.BROWSER).type shouldEqual """audio/webm; codecs="opus""""
        }
    }
}
