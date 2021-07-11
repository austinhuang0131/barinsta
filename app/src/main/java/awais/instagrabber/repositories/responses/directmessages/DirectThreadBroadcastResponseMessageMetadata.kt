package awais.instagrabber.repositories.responses.directmessages

data class DirectThreadBroadcastResponseMessageMetadata(
    val clientContext: String? = null,
    val itemId: String? = null,
    val timestamp: Long = 0,
    val threadId: String? = null,
    val participantIds: List<String>? = null,
)