package awais.instagrabber.models

import awais.instagrabber.models.enums.MediaItemType

data class UploadVideoOptions(
    val uploadId: String,
    val name: String,
    val byteLength: Long = 0,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isSideCar: Boolean = false,
    // Stories
    val forAlbum: Boolean = false,
    val isDirect: Boolean = false,
    val isDirectVoice: Boolean = false,
    val isForDirectStory: Boolean = false,
    val isIgtvVideo: Boolean = false,
    val waterfallId: String? = null,
    val offset: Long = 0,
    val mediaType: MediaItemType? = null,
)