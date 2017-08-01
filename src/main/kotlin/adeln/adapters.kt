package adeln

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.ThumbnailDetails
import com.google.api.services.youtube.model.VideoSnippet

data class Video(
    val id: VideoID,
    val thumbnails: ThumbnailDetails,
    val position: Long?,
    val title: String,
    val channelTitle: String,
    val description: String,
    val publishedAt: DateTime
)

fun VideoSnippet.toVideo(id: VideoID): Video =
    Video(
        id = id,
        thumbnails = thumbnails,
        position = null,
        title = title,
        channelTitle = channelTitle,
        description = description,
        publishedAt = publishedAt
    )

fun PlaylistItemSnippet.toVideo(): Video =
    Video(
        id = VideoID(resourceId.videoId),
        thumbnails = thumbnails,
        position = position,
        title = title,
        channelTitle = channelTitle,
        description = description,
        publishedAt = publishedAt
    )
