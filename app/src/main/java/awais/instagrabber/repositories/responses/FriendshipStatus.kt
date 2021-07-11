package awais.instagrabber.repositories.responses

import java.io.Serializable

data class FriendshipStatus(
    val following: Boolean = false,
    val followedBy: Boolean = false,
    val blocking: Boolean = false,
    val muting: Boolean = false,
    val isPrivate: Boolean = false,
    val incomingRequest: Boolean = false,
    val outgoingRequest: Boolean = false,
    val isBestie: Boolean = false,
    val isRestricted: Boolean = false,
    val isMutingReel: Boolean = false,
) : Serializable