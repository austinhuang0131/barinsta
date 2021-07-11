package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.models.enums.DirectItemType
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class DirectItem(
    var itemId: String? = null,
    val userId: Long = 0,
    private var timestamp: Long = 0,
    val itemType: DirectItemType? = null,
    val text: String? = null,
    val like: String? = null,
    val link: DirectItemLink? = null,
    val clientContext: String? = null,
    val reelShare: DirectItemReelShare? = null,
    val storyShare: DirectItemStoryShare? = null,
    val mediaShare: Media? = null,
    val profile: User? = null,
    val placeholder: DirectItemPlaceholder? = null,
    val media: Media? = null,
    val previewMedias: List<Media>? = null,
    val actionLog: DirectItemActionLog? = null,
    val videoCallEvent: DirectItemVideoCallEvent? = null,
    val clip: DirectItemClip? = null,
    val felixShare: DirectItemFelixShare? = null,
    val visualMedia: DirectItemVisualMedia? = null,
    val animatedMedia: DirectItemAnimatedMedia? = null,
    var reactions: DirectItemReactions? = null,
    val repliedToMessage: DirectItem? = null,
    val voiceMedia: DirectItemVoiceMedia? = null,
    val location: Location? = null,
    val xma: DirectItemXma? = null,
    val hideInThread: Int? = 0,
    val showForwardAttribution: Boolean = false
) : Cloneable, Serializable {
    var isPending = false
    var date: LocalDateTime? = null
        get() {
            if (field == null) {
                field = Instant.ofEpochMilli(timestamp / 1000).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            return field
        }
        private set

    fun getTimestamp(): Long {
        return timestamp
    }

    fun setTimestamp(timestamp: Long) {
        this.timestamp = timestamp
        date = null
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }
}