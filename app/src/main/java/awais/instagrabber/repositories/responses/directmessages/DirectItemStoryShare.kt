package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

data class DirectItemStoryShare(
    val reelId: String? = null,
    val reelType: String? = null,
    val text: String? = null,
    val isReelPersisted: Boolean = false,
    val media: Media? = null,
    val title: String? = null,
    val message: String? = null,
) : Serializable