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
    fun badVideo() {
        println(
            audio(mkClient(), VideoID("6G59zsjM2UI"), Player.BROWSER)
        )
    }

    @Test
    fun whatthefuck() {
        VideoID("1TN_Ig8sjxc")
    }
}
