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
import com.yandex.disk.rest.util.ResourcePath
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
        .setLogLevel(RestAdapter.LogLevel.NONE)
        .build()
        .create(YandexDisk::class.java)

enum class PreviewSize { XL }

fun YandexDisk.listPublicResources(
    publicKey: String,
    path: ResourcePath? = null,
    previewSize: PreviewSize = PreviewSize.XL,
    previewCrop: Boolean = true,
    limit: Int = Int.MAX_VALUE
): Resource =
    listPublicResources(
        publicKey,
        path?.path,
        null,
        limit,
        null,
        null,
        previewSize.name,
        previewCrop
    )

operator fun <T> ((T) -> Boolean).not(): (T) -> Boolean =
    { !invoke(it) }

data class RecursiveFolder(
    val dir: Resource,
    val files: List<Resource>
)

fun YandexDisk.recursiveResource(publicKey: String, parent: Resource? = null): RecursiveFolder {

    val dir = listPublicResources(publicKey = publicKey, path = parent?.path)

    val items = dir.resourceList.items
    val deepFiles = items.filter(Resource::isDir).flatMap {
        Thread.sleep(300)
        recursiveResource(it.publicKey, it).files
    }
    val flatFiles = items.filter(!Resource::isDir)

    return RecursiveFolder(dir = dir, files = deepFiles + flatFiles)
}

fun Resource.pathToTitle(): String =
    path.path
        .replace(oldValue = "/", newValue = " ")
        .replaceAfterLast(delimiter = ".", replacement = "")
        .let { if (it.endsWith(suffix = ".")) it.dropLast(n = 1) else it }
        .trim()

fun asEntry(res: Resource): SyndEntryImpl =
    entry {

        val url = Config.HOST.newBuilder()
            .addPathSegments("yandexdisk/audio")
            .addQueryParameter("publicKey", res.publicKey)
            .addQueryParameter("path", res.path.path)
            .build()

        it.modules = mutableListOf<Module>(DCModuleImpl())

        it.enclosures = listOf(
            SyndEnclosureImpl().also {
                it.type = res.mimeType
                it.url = url.toString()
            }
        )

        it.title = res.pathToTitle()
        it.link = res.publicUrl
        it.publishedDate = res.created
    }

fun YandexDisk.asFeed(url: HttpUrl): SyndFeedImpl =
    rss20 {
        val (dir, files) = recursiveResource(publicKey = url.toString())

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
