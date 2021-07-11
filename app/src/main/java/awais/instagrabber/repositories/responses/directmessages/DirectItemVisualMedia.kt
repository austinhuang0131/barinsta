package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.models.enums.RavenMediaViewMode
import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

data class DirectItemVisualMedia(
    val urlExpireAtSecs: Long = 0,
    val playbackDurationSecs: Int = 0,
    val seenUserIds: List<Long>? = null,
    val viewMode: RavenMediaViewMode? = null,
    val seenCount: Int = 0,
    val replayExpiringAtUs: Long = 0,
    val expiringMediaActionSummary: RavenExpiringMediaActionSummary? = null,
    val media: Media? = null,
) : Serializable