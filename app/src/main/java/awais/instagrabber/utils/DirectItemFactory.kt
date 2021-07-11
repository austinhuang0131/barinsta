@file:JvmName("DirectItemFactory")

package awais.instagrabber.utils

import android.net.Uri
import awais.instagrabber.models.enums.DirectItemType
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.*
import awais.instagrabber.repositories.responses.directmessages.DirectItem
import awais.instagrabber.repositories.responses.directmessages.DirectItemAnimatedMedia
import awais.instagrabber.repositories.responses.directmessages.DirectItemVoiceMedia
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import java.util.*

fun createText(
    userId: Long,
    clientContext: String?,
    text: String?,
    repliedToMessage: DirectItem?
): DirectItem {
    return DirectItem(
        itemId = UUID.randomUUID().toString(),
        userId = userId,
        timestamp = System.currentTimeMillis() * 1000,
        itemType = DirectItemType.TEXT,
        text = text,
        clientContext = clientContext,
        repliedToMessage = repliedToMessage,
    )
}

fun createImageOrVideo(
    userId: Long,
    clientContext: String?,
    uri: Uri,
    width: Int,
    height: Int,
    isVideo: Boolean
): DirectItem {
    val imageVersions2 = ImageVersions2(listOf(MediaCandidate(width, height, uri.toString())))
    var videoVersions: List<MediaCandidate>? = null
    if (isVideo) {
        val videoVersion = MediaCandidate(
            width,
            height,
            uri.toString()
        )
        videoVersions = listOf(videoVersion)
    }
    val media = Media(
        id = UUID.randomUUID().toString(),
        imageVersions2 = imageVersions2,
        originalWidth = width,
        originalHeight = height,
        mediaType = if (isVideo) MediaItemType.MEDIA_TYPE_VIDEO.id else MediaItemType.MEDIA_TYPE_IMAGE.id,
        videoVersions = videoVersions,
    )
    return DirectItem(
        itemId = UUID.randomUUID().toString(),
        userId = userId,
        timestamp = System.currentTimeMillis() * 1000,
        itemType = DirectItemType.MEDIA,
        clientContext = clientContext,
        media = media,
    )
}

fun createVoice(
    userId: Long,
    clientContext: String?,
    uri: Uri,
    duration: Long,
    waveform: List<Float>?,
    samplingFreq: Int
): DirectItem {
    val audio = Audio(
        uri.toString(),
        duration,
        waveform,
        samplingFreq,
        0
    )
    val media = Media(
        id = UUID.randomUUID().toString(),
        mediaType = MediaItemType.MEDIA_TYPE_VOICE.id,
        audio = audio,
    )
    val voiceMedia = DirectItemVoiceMedia(
        media,
        0,
        "permanent"
    )
    return DirectItem(
        itemId = UUID.randomUUID().toString(),
        userId = userId,
        timestamp = System.currentTimeMillis() * 1000,
        itemType = DirectItemType.VOICE_MEDIA,
        clientContext = clientContext,
        media = media,
        voiceMedia = voiceMedia,
    )
}

fun createAnimatedMedia(
    userId: Long,
    clientContext: String?,
    giphyGif: GiphyGif
): DirectItem {
    val animatedImages = AnimatedMediaImages(giphyGif.images.fixedHeight)
    val animateMedia = DirectItemAnimatedMedia(
        giphyGif.id,
        animatedImages,
        false,
        giphyGif.isSticker
    )
    return DirectItem(
        itemId = UUID.randomUUID().toString(),
        userId = userId,
        timestamp = System.currentTimeMillis() * 1000,
        itemType = DirectItemType.ANIMATED_MEDIA,
        clientContext = clientContext,
        animatedMedia = animateMedia,
    )
}