package awais.instagrabber.repositories.responses.directmessages

data class DirectThreadBroadcastResponsePayload(
    val clientContext: String? = null,
    val itemId: String? = null,
    val timestamp: Long = 0,
    val threadId: String? = null,
)