package awais.instagrabber.utils

import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.repositories.responses.directmessages.RankedRecipientsResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object RankedRecipientsCache {
    private var lastUpdatedOn: LocalDateTime? = null
    var isUpdateInitiated = false
    var isFailed = false
    val rankedRecipients: List<RankedRecipient>
        get() = response?.rankedRecipients ?: emptyList()

    var response: RankedRecipientsResponse? = null
        set(value) {
            field = value
            lastUpdatedOn = LocalDateTime.now()
        }

    val isExpired: Boolean
        get() {
            if (lastUpdatedOn == null || response == null) return true
            val expiresInSecs = response!!.expires
            return LocalDateTime.now().isAfter(lastUpdatedOn!!.plus(expiresInSecs, ChronoUnit.SECONDS))
        }
}