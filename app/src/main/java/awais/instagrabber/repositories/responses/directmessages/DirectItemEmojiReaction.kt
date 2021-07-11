package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemEmojiReaction(
    val senderId: Long = 0,
    val timestamp: Long = 0,
    val emoji: String? = null,
    val superReactType: String? = null
) : Serializable