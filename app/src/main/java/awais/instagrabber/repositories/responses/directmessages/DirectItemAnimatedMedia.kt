package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.AnimatedMediaImages
import java.io.Serializable

data class DirectItemAnimatedMedia(
    val id: String? = null,
    val images: AnimatedMediaImages? = null,
    val isRandom: Boolean = false,
    val isSticker: Boolean = false,
) : Serializable