package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.User

data class DirectInboxResponse(
    val viewer: User? = null,
    val inbox: DirectInbox? = null,
    val seqId: Long = 0,
    val snapshotAtMs: Long = 0,
    val pendingRequestsTotal: Int = 0,
    val hasPendingTopRequests: Boolean = false,
    val mostRecentInviter: User? = null,
    val status: String? = null,
)