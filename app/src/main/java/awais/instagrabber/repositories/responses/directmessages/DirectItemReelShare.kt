package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

data class DirectItemReelShare(
    val text: String? = null,
    val type: String? = null,
    val reelOwnerId: Long = 0,
    val mentionedUserId: Long = 0,
    val isReelPersisted: Boolean = false,
    val reelType: String? = null,
    val media: Media? = null,
    val reactionInfo: DirectItemReelShareReactionInfo? = null,
) : Serializable