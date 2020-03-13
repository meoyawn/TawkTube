package adeln

import com.rometools.rome.io.SyndFeedOutput
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class Experiments {

    @Test
    fun disk() {
        assert(resolve("https://yadi.sk/d/I5HDo-VY3R4Bvn".toHttpUrl()) == "http://localhost:8080/yandexdisk/public?link=${URLEncoder.encode("https://yadi.sk/d/I5HDo-VY3R4Bvn", "utf-8")}".toHttpUrl())
    }

    @Test
    fun video() {
        assert(resolve("https://www.youtube.com/watch?v=g2tyOLvArw0".toHttpUrl()) == "http://localhost:8080/video?v=g2tyOLvArw0".toHttpUrl())
    }

    @Test
    fun channel() {
        assert(resolve("https://www.youtube.com/channel/UC5CHszV2kJxVHHCP7uJQzBg".toHttpUrl()) == "http://localhost:8080/channel/UC5CHszV2kJxVHHCP7uJQzBg".toHttpUrl())
        assert(resolve("https://www.youtube.com/channel/UC5CHszV2kJxVHHCP7uJQzBg/".toHttpUrl()) == "http://localhost:8080/channel/UC5CHszV2kJxVHHCP7uJQzBg".toHttpUrl())
    }

    @Test
    fun audioUrl() {
        assert(audioUrl(VideoID("ha")) == "http://localhost:8080/audio?v=ha".toHttpUrl())
    }

    @Test
    fun mobileYt() {
        assert(resolve("https://m.youtube.com/playlist?list=PLE7DDD91010BC51F8".toHttpUrl()) == "http://localhost:8080/playlist?list=PLE7DDD91010BC51F8".toHttpUrl())
    }

    @Test
    fun instantRegret() {
        assert(playlistEntries(mkYoutube(), PlaylistID("PLiQrdzH3aBWi6nh1kdbYfy2dd1CSOwBz5"), Player.BROWSER)!!.size == 83)
    }

    @Test
    fun bug34() {
        assert(asFeed(mkYoutube(), PlaylistID("PLrxx1RzoWiyyfhkk84Vkz6XYaLrY6HMdDA"), Player.BROWSER) == null)
        assert(playlistEntries(mkYoutube(), PlaylistID("PLrxx1RzoWiyyfhkk84Vkz6XYaLrY6HMdDA"), Player.BROWSER) == null)
    }

    @Disabled("yandex too many requests")
    @Test
    fun demons() {
        runBlocking {
            SyndFeedOutput().outputString(mkYandexDisk().asFeed("https://yadi.sk/d/4OHFuxV93Xn7H6".toHttpUrl()))
        }
    }

    @Disabled("timeout")
    @Test
    fun audio(): Unit = runBlocking {
        assert(audio(OkHttpClient(), VideoID("Wex12GhUFqE"), Player.BROWSER).type == """audio/webm; codecs="opus"""")
    }
}
