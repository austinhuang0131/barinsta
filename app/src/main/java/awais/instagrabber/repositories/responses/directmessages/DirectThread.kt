package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.User
import java.io.Serializable

data class DirectThread(
    val threadId: String? = null,
    val threadV2Id: String? = null,
    var users: List<User>? = null,
    var leftUsers: List<User>? = null,
    var adminUserIds: List<Long>? = null,
    var items: List<DirectItem>? = null,
    val lastActivityAt: Long = 0,
    var muted: Boolean = false,
    val isPin: Boolean = false,
    val named: Boolean = false,
    val canonical: Boolean = false,
    var pending: Boolean = false,
    val archived: Boolean = false,
    val valuedRequest: Boolean = false,
    val threadType: String? = null,
    val viewerId: Long = 0,
    val threadTitle: String? = null,
    val pendingScore: String? = null,
    val folder: Long = 0,
    val vcMuted: Boolean = false,
    val isGroup: Boolean = false,
    var mentionsMuted: Boolean = false,
    val inviter: User? = null,
    val hasOlder: Boolean = false,
    val hasNewer: Boolean = false,
    var lastSeenAt: Map<Long, DirectThreadLastSeenAt>? = null,
    val newestCursor: String? = null,
    val oldestCursor: String? = null,
    val isSpam: Boolean = false,
    val lastPermanentItem: DirectItem? = null,
    val directStory: DirectThreadDirectStory? = null,
    var approvalRequiredForNewMembers: Boolean = false,
    var inputMode: Int = 0,
    val threadContextItems: List<ThreadContext>? = null
) : Serializable, Cloneable {
    var isTemp = false

    val firstDirectItem: DirectItem?
        get() = items?.firstNotNullOfOrNull { it }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any = super.clone()
}