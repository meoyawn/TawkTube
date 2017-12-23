package adeln

import org.junit.Test

class Experiments {

    @Test
    fun channel() {
        println(
            mkYoutube().channel(ChannelID("UCC3L8QaxqEGUiBC252GHy3w"))
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
}
