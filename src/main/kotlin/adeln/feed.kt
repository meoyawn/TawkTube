package adeln

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItemSnippet
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

fun entry(client: OkHttpClient, video: PlaylistItemSnippet): SyndEntry? =
    audio(client, videoId(video))?.let { audio ->
        entry {
            it.modules = mutableListOf(
                itunesEntry {
                    it.image = URL(thumbnail(video).url)
                    it.duration = Duration(audio.lengthMillis())
                    it.order = video.position.toInt()
                },
                mediaEntry {
                    it.mediaContents = arrayOf(
                        mediaContent(audio.url) {
                            it.duration = audio.lengthSeconds
                            it.bitrate = audio.bitrate
                            it.type = audio.type
                            it.fileSize = audio.sizeBytes()
                        }
                    )
                },
                DCModuleImpl()
            )

            it.enclosures = listOf(
                SyndEnclosureImpl().also {
                    it.type = audio.type
                    it.url = audio.url.toString()
                    it.length = audio.sizeBytes()
                }
            )

            it.title = video.title
            it.link = videoLink(videoId(video)).toString()
            it.author = video.channelTitle
            it.description = SyndContentImpl().also {
                it.value = video.description
            }
            it.publishedDate = video.publishedAt.toDate()
        }
    }

suspend fun asFeed(client: OkHttpClient, yt: YouTube, playlistId: PlaylistId): SyndFeed {

    val playlist = yt.playlistInfo(playlistId).await()

    return SyndFeedImpl(Channel(RSS20Generator().type)).also {

        it.modules = mutableListOf(
            FeedInformationImpl().also {
                it.image = URL(thumbnail(playlist).url)
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
