package adeln

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.VideoContentDetails
import com.rometools.modules.itunes.EntryInformationImpl
import com.rometools.modules.itunes.FeedInformationImpl
import com.rometools.modules.itunes.types.Duration
import com.rometools.modules.mediarss.MediaEntryModuleImpl
import com.rometools.modules.mediarss.types.MediaContent
import com.rometools.modules.mediarss.types.UrlReference
import com.rometools.rome.feed.module.DCModuleImpl
import com.rometools.rome.feed.rss.Channel
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.impl.RSS20Generator
import okhttp3.HttpUrl
import java.net.URL

inline fun itunesEntry(f: (EntryInformationImpl) -> Unit): EntryInformationImpl =
    EntryInformationImpl().also(f)

inline fun mediaEntry(f: (MediaEntryModuleImpl) -> Unit): MediaEntryModuleImpl =
    MediaEntryModuleImpl().also(f)

inline fun entry(f: (SyndEntryImpl) -> Unit): SyndEntryImpl =
    SyndEntryImpl().also(f)

inline fun mediaContent(url: HttpUrl, f: (MediaContent) -> Unit): MediaContent =
    MediaContent(UrlReference(url.uri())).also(f)

inline fun rss20(f: (SyndFeedImpl) -> Unit): SyndFeedImpl =
    SyndFeedImpl(Channel(RSS20Generator().type)).also(f)

inline fun itunes(f: (FeedInformationImpl) -> Unit): FeedInformationImpl =
    FeedInformationImpl().also(f)


fun Video.bestThumbnail(): URL? =
    thumbnails?.best()?.url.let { URL(it) }

fun entry(video: Video, audio: VideoContentDetails): SyndEntryImpl =
    entry {
        val videoID = video.id
        val url = audioUrl(videoID)

        val duration = java.time.Duration.parse(audio.duration)

        it.modules = mutableListOf(
            itunesEntry { itunes ->
                itunes.image = video.bestThumbnail()
                itunes.duration = Duration(duration.toMillis())
                video.position?.let {
                    itunes.order = it.toInt()
                }
                itunes.author = video.channelTitle
            },
            mediaEntry {
                it.mediaContents = arrayOf(
                    mediaContent(url) {
                        it.duration = duration.seconds
                    }
                )
            },
            DCModuleImpl()
        )

        it.enclosures = listOf(
            SyndEnclosureImpl().also {
                it.url = url.toString()
            }
        )

        it.title = video.title
        it.link = link(videoID).toString()
        it.author = video.channelTitle
        it.description = SyndContentImpl().also {
            it.value = video.description
        }
        it.publishedDate = video.publishedAt.toDate()
    }

fun audioUrl(videoID: VideoID): HttpUrl =
    Config.ADDR.newBuilder()
        .addPathSegment("audio")
        .addQueryParameter("v", videoID.id)
        .build()

fun asFeed(yt: YouTube, videoID: VideoID): SyndFeed {

    val (snippet, audio) = yt.videoInfo(videoID)
    val video = snippet.toVideo(videoID)

    return rss20 {
        it.modules = mutableListOf(
            itunes {
                it.image = video.bestThumbnail()
                it.author = video.channelTitle
            },
            DCModuleImpl()
        )

        it.title = video.title
        it.link = link(videoID).toString()
        it.description = video.description
        it.publishedDate = video.publishedAt.toDate()
        it.author = video.channelTitle
        it.entries = listOf(entry(video, audio))
    }
}

fun asFeed(yt: YouTube, playlistID: PlaylistID): SyndFeed? {

    val playlist = yt.playlistInfo(playlistID) ?: return null

    return rss20 {
        it.modules = mutableListOf(
            itunes {
                it.image = playlist.thumbnails.best()?.url?.let { URL(it) }
                it.author = playlist.channelTitle
            },
            DCModuleImpl()
        )

        it.title = playlist.title
        it.link = link(playlistID).toString()
        it.description = playlist.description
        it.publishedDate = playlist.publishedAt.toDate()
        it.author = playlist.channelTitle
        it.entries = playlistEntries(yt, playlistID)
    }
}

fun asFeed(yt: YouTube, channelID: ChannelId): SyndFeed? {

    val (snippet, details) = yt.channel(channelID)

    return rss20 {
        it.modules = mutableListOf(
            itunes {
                it.image = snippet.thumbnails?.best()?.url?.let { URL(it) }
                it.author = snippet.title
            },
            DCModuleImpl()
        )

        it.title = snippet.title
        it.link = link(channelID).toString()
        it.description = snippet.description
        it.publishedDate = snippet.publishedAt.toDate()
        it.author = snippet.title
        it.entries = playlistEntries(yt, PlaylistID(details.relatedPlaylists.uploads))
    }
}
