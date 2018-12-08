package adeln

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.VideoContentDetails
import com.rometools.modules.itunes.types.Duration
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import okhttp3.HttpUrl
import java.net.URL

fun entry(video: Video, audio: VideoContentDetails, player: Player): SyndEntry =
    entry {
        val videoID = video.id
        val url = audioUrl(videoID)

        val duration = java.time.Duration.parse(audio.duration)

        it.modules = mutableListOf(
            itunesEntry {
                it.image = video.bestThumbnail()
                it.duration = Duration(duration.toMillis())
                it.order = video.position?.toInt()
                it.author = video.channelTitle
            },
            mediaEntry {
                it.mediaContents = arrayOf(
                    mediaContent(url) {
                        it.duration = duration.seconds
                    }
                )
            },
            dc { }
        )

        it.enclosures = listOf(
            enclosure {
                it.url = url.toString()
                it.type = player.audioType()
            }
        )

        it.title = video.title
        it.link = link(videoID).toString()
        it.author = video.channelTitle
        it.description = content {
            it.value = video.description
        }
        it.publishedDate = video.publishedAt.toDate()
    }

fun audioUrl(videoID: VideoID): HttpUrl =
    Config.HOST.newBuilder()
        .addPathSegment("audio")
        .addQueryParameter("v", videoID.id)
        .build()

fun asFeed(yt: YouTube, videoID: VideoID, player: Player): SyndFeed =
    rss20 {
        val (snippet, audio) = yt.videoInfo(videoID)
        val video = snippet.toVideo(videoID)

        it.modules = mutableListOf(
            itunes {
                it.image = video.bestThumbnail()
                it.author = video.channelTitle
            },
            dc { }
        )

        it.title = video.title
        it.link = link(videoID).toString()
        it.description = video.description
        it.publishedDate = video.publishedAt.toDate()
        it.author = video.channelTitle
        it.entries = listOf(entry(video, audio, player))
    }

fun asFeed(yt: YouTube, playlistID: PlaylistID, player: Player): SyndFeed =
    rss20 {
        val playlist = yt.playlistInfo(playlistID)

        it.modules = mutableListOf(
            itunes {
                it.image = playlist.thumbnails.best()?.url?.let(::URL)
                it.author = playlist.channelTitle
            },
            dc { }
        )

        it.title = playlist.title
        it.link = link(playlistID).toString()
        it.description = playlist.description
        it.publishedDate = playlist.publishedAt.toDate()
        it.author = playlist.channelTitle
        it.entries = playlistEntries(yt, playlistID, player)
    }

fun asFeed(yt: YouTube, channelID: ChannelId, player: Player): SyndFeed? =
    rss20 {
        val (snippet, details) = yt.channel(channelID)

        it.modules = mutableListOf(
            itunes {
                it.image = snippet.thumbnails?.best()?.url?.let(::URL)
                it.author = snippet.title
            },
            dc { }
        )

        it.title = snippet.title
        it.link = link(channelID).toString()
        it.description = snippet.description
        it.publishedDate = snippet.publishedAt.toDate()
        it.author = snippet.title
        it.entries = playlistEntries(yt, PlaylistID(details.relatedPlaylists.uploads), player)
    }
