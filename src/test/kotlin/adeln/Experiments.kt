package adeln

import org.junit.Test

class Experiments {

    @Test
    fun lol() {
        val youtube = mkYoutube()

        println(
            youtube.channel(ChannelID("UCC3L8QaxqEGUiBC252GHy3w"))
        )
    }
}
