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
    fun y2mp3() {
        println(
            youtubeToMp3(mkClient(), VideoID("v2soHxEN79c"), mkMoshi())
        )
    }

    @Test
    fun youtube() {
        println(
            audio(mkClient(), VideoID("v2soHxEN79c"), mkMoshi(), Player.BROWSER)
        )
    }

    @Test
    fun badVideo() {
        println(
            audio(mkClient(), VideoID("6G59zsjM2UI"), mkMoshi(), Player.BROWSER)
        )
    }

    @Test
    fun whatthefuck() {
        VideoID("1TN_Ig8sjxc")
    }
}
