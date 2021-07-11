package awais.instagrabber.repositories.responses.directmessages

data class RankedRecipientsResponse(
    val rankedRecipients: List<RankedRecipient>? = null,
    val expires: Long = 0,
    val filtered: Boolean = false,
    val requestId: String? = null,
    val rankToken: String? = null,
    val status: String? = null,
)