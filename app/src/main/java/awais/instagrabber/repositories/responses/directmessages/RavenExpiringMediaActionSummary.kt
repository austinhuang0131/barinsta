package awais.instagrabber.repositories.responses.directmessages

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RavenExpiringMediaActionSummary(
    val timestamp: Long = 0,
    val count: Int = 0,
    val type: ActionType? = null,
) : Serializable

// thanks to http://github.com/warifp/InstagramAutoPostImageUrl/blob/master/vendor/mgp25/instagram-php/src/Response/Model/ActionBadge.php
enum class ActionType {
    @SerializedName("raven_delivered")
    DELIVERED,

    @SerializedName("raven_sent")
    SENT,

    @SerializedName("raven_opened")
    OPENED,

    @SerializedName("raven_screenshot")
    SCREENSHOT,

    @SerializedName("raven_replayed")
    REPLAYED,

    @SerializedName("raven_cannot_deliver")
    CANNOT_DELIVER,

    @SerializedName("raven_sending")
    SENDING,

    @SerializedName("raven_blocked")
    BLOCKED,

    @SerializedName("raven_unknown")
    UNKNOWN,

    @SerializedName("raven_suggested")
    SUGGESTED,
}