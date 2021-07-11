package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

data class DirectItemVoiceMedia(
    val media: Media? = null,
    val seenCount: Int = 0,
    val viewMode: String? = null,
) : Serializable