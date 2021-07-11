package awais.instagrabber.repositories.responses.directmessages

data class DirectThreadBroadcastResponse(
    val action: String? = null,
    val statusCode: String? = null,
    val payload: DirectThreadBroadcastResponsePayload? = null,
    val messageMetadata: List<DirectThreadBroadcastResponseMessageMetadata>? = null,
    val status: String? = null
)