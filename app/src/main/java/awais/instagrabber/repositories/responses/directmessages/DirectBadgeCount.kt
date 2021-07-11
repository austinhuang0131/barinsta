package awais.instagrabber.repositories.responses.directmessages

data class DirectBadgeCount(
    val userId: Long = 0,
    val badgeCount: Int = 0,
    val badgeCountAtMs: Long = 0,
    val status: String? = null
)