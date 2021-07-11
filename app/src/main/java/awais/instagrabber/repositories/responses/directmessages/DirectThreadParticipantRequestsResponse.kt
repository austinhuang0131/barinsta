package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.User
import java.io.Serializable

data class DirectThreadParticipantRequestsResponse(
    var users: List<User>? = null,
    val requesterUsernames: Map<Long, String>? = null,
    val cursor: String? = null,
    val totalThreadParticipants: Int = 0,
    var totalParticipantRequests: Int = 0,
    val status: String? = null,
) : Serializable, Cloneable {
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any = super.clone()
}