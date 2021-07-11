package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectThreadLastSeenAt(
    val timestamp: String? = null,
    val itemId: String? = null,
) : Serializable