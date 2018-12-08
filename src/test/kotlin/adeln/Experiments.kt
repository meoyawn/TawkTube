package adeln

import com.rometools.rome.io.SyndFeedOutput
import io.kotlintest.matchers.haveSize
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldEqual
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.Test
import java.net.URLEncoder

class Experiments {

    @Test
    fun disk() {
        val diskLink = "https://yadi.sk/d/I5HDo-VY3R4Bvn"
        resolve(HttpUrl.parse(diskLink)!!) shouldEqual
            HttpUrl.parse("http://localhost:8080/yandexdisk/public?link=${URLEncoder.encode(diskLink, "utf-8")}")
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
    fun mobileYt() {
        resolve(HttpUrl.parse("https://m.youtube.com/playlist?list=PLE7DDD91010BC51F8")!!) shouldEqual
            HttpUrl.parse("http://localhost:8080/playlist?list=PLE7DDD91010BC51F8")
    }

    @Test
    fun instantRegret() {
        playlistEntries(mkYoutube(), PlaylistID("PLiQrdzH3aBWi6nh1kdbYfy2dd1CSOwBz5"), Player.BROWSER) should
            haveSize(70)
    }

    @Test
    fun demons() {
        runBlocking {
            SyndFeedOutput().outputString(mkYandexDisk().asFeed(HttpUrl.parse("https://yadi.sk/d/4OHFuxV93Xn7H6")!!))
        }
    }
}
