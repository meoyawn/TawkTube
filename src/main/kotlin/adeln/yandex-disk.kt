package adeln

import com.rometools.rome.feed.module.DCModuleImpl
import com.rometools.rome.feed.module.Module
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.yandex.disk.rest.OkHttpClientFactory
import com.yandex.disk.rest.json.Resource
import com.yandex.disk.rest.retrofit.CloudApi
import com.yandex.disk.rest.retrofit.ErrorHandlerImpl
import okhttp3.HttpUrl
import retrofit.RestAdapter
import retrofit.client.OkClient
import java.net.URL

typealias YandexDisk = CloudApi

fun mkYandexDisk(): YandexDisk =
    RestAdapter.Builder()
        .setClient(OkClient(OkHttpClientFactory.makeClient()))
        .setEndpoint("https://cloud-api.yandex.net")
        .setErrorHandler(ErrorHandlerImpl())
        .setLogLevel(RestAdapter.LogLevel.BASIC)
        .build()
        .create(YandexDisk::class.java)

enum class PreviewSize { XL }

fun YandexDisk.listPublicResources(publicKey: String,
                                   previewSize: PreviewSize,
                                   previewCrop: Boolean,
                                   limit: Int): Resource =
    listPublicResources(
        publicKey,
        null,
        null,
        limit,
        null,
        null,
        previewSize.name,
        previewCrop
    )

fun asEntry(res: Resource): SyndEntryImpl =
    entry {

        val url = Config.ADDR.newBuilder()
            .addPathSegments("yandexdisk/audio")
            .addQueryParameter("publicKey", res.publicKey)
            .addQueryParameter("path", res.path.path)
            .build()

        it.modules = mutableListOf(DCModuleImpl() as Module)

        it.enclosures = listOf(
            SyndEnclosureImpl().also {
                it.type = res.mimeType
                it.url = url.toString()
//                it.length = audio.sizeBytes()
            }
        )

        it.title = res.name
        it.link = res.publicUrl
        it.publishedDate = res.created
    }

fun YandexDisk.asFeed(url: HttpUrl): SyndFeedImpl =
    rss20 {
        val dir = listPublicResources(
            publicKey = url.toString(),
            previewSize = PreviewSize.XL,
            previewCrop = true,
            limit = Int.MAX_VALUE
        )
        val files = dir.resourceList.items

        it.modules = mutableListOf(
            itunes {
                it.image = files.find { it.mediaType == "image" }?.preview?.let { URL(it) }
            },
            DCModuleImpl()
        )

        it.title = dir.name
        it.description = dir.publicUrl
        it.link = dir.publicUrl
        it.publishedDate = dir.created
        it.entries = files.filter { it.mediaType == "audio" }.map(::asEntry)
    }
