package adeln

import com.google.api.services.youtube.YouTube
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
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.impl.RSS20Generator
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
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

fun media(audio: Audio, url: HttpUrl): MediaEntryModuleImpl =
    mediaEntry {
        it.mediaContents = arrayOf(
            mediaContent(url) {
                it.duration = audio.lengthSeconds
                it.bitrate = audio.bitrate
                it.type = audio.type
                it.fileSize = audio.sizeBytes()
            }
        )
    }

fun enclosures(audio: Audio, url: HttpUrl): List<SyndEnclosureImpl> =
    listOf(
        SyndEnclosureImpl().also {
            it.type = audio.type
            it.url = url.toString()
            it.length = audio.sizeBytes()
        }
    )

fun entry(client: OkHttpClient, video: Video): SyndEntry? =
    audio(client, video.id)?.let {
        entry(video, it)
    }

fun entry(video: Video, audio: Audio): SyndEntryImpl =
    entry {
        val url = HttpUrl.parse("${Config.ADDR}/audio?v=${video.id.id}")!!

        it.modules = mutableListOf(
            itunesEntry { itunes ->
                itunes.image = URL(video.thumbnails.best().url)
                itunes.duration = Duration(audio.lengthMillis())
                video.position?.let {
                    itunes.order = it.toInt()
                }
            },
            media(audio, url),
            DCModuleImpl()
        )

        it.enclosures = enclosures(audio, url)

        it.title = video.title
        it.link = videoLink(video.id).toString()
        it.author = video.channelTitle
        it.description = SyndContentImpl().also {
            it.value = video.description
        }
        it.publishedDate = video.publishedAt.toDate()
}

fun asFeed(client: OkHttpClient, yt: YouTube, videoId: VideoId): SyndFeed? =
    audio(client, videoId)?.let { audio ->
        val video = yt.videoInfo(videoId).toVideo(videoId)

        rss20 {
            it.modules = mutableListOf(
                itunes {
                    it.image = URL(video.thumbnails.best().url)
                },
                DCModuleImpl()
            )

            it.title = video.title
            it.link = videoLink(videoId).toString()
            it.description = video.description
            it.publishedDate = video.publishedAt.toDate()
            it.author = video.channelTitle
            it.entries = listOf(entry(video, audio))
        }
    }

suspend fun asFeed(client: OkHttpClient, yt: YouTube, playlistId: PlaylistId): SyndFeed {

    val playlist = yt.playlistInfo(playlistId).await()

    return rss20 {
        it.modules = mutableListOf(
            itunes {
                it.image = URL(playlist.thumbnails.best().url)
            },
            DCModuleImpl()
        )

        it.title = playlist.title
        it.link = playlistLink(playlistId).toString()
        it.description = playlist.description
        it.publishedDate = playlist.publishedAt.toDate()
        it.author = playlist.channelTitle
        it.entries = playlistEntries(client, yt, playlistId).await()
    }
}
