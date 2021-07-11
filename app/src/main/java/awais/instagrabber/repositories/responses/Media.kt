package awais.instagrabber.repositories.responses

import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.models.enums.MediaItemType.Companion.valueOf
import awais.instagrabber.repositories.responses.feed.EndOfFeedDemarcator
import awais.instagrabber.utils.TextUtils
import java.io.Serializable

data class Media(
    val pk: String? = null,
    val id: String? = null,
    val code: String? = null,
    val takenAt: Long = -1,
    val user: User? = null,
    val canViewerReshare: Boolean = false,
    val imageVersions2: ImageVersions2? = null,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val mediaType: Int = 0,
    val commentLikesEnabled: Boolean = false,
    val commentsDisabled: Boolean = false,
    val nextMaxId: Long = -1,
    val commentCount: Long = 0,
    var likeCount: Long = 0,
    var hasLiked: Boolean = false,
    val isReelMedia: Boolean = false,
    val videoVersions: List<MediaCandidate>? = null,
    val hasAudio: Boolean = false,
    val videoDuration: Double = 0.0,
    val viewCount: Long = 0,
    var caption: Caption? = null,
    val canViewerSave: Boolean = false,
    val audio: Audio? = null,
    val title: String? = null,
    val carouselMedia: List<Media>? = null,
    val location: Location? = null,
    val usertags: Usertags? = null,
    var isSidecarChild: Boolean = false,
    var hasViewerSaved: Boolean = false,
    private val injected: Map<String, Any>? = null,
    val endOfFeedDemarcator: EndOfFeedDemarcator? = null,
    val carouselShareChildMediaId: String? = null // which specific child should dm show first
) : Serializable {
    private var dateString: String? = null

    fun isInjected(): Boolean {
        return injected != null
    }

    // TODO use extension once all usages are converted to kotlin
    // val date: String by lazy {
    //     if (takenAt <= 0) "" else Utils.datetimeParser.format(Date(takenAt * 1000L))
    // }
    val date: String
        get() {
            if (takenAt <= 0) return ""
            if (dateString != null) return dateString ?: ""
            dateString = TextUtils.epochSecondToString(takenAt)
            return dateString ?: ""
        }

    val type: MediaItemType?
        get() = valueOf(mediaType)

    fun setPostCaption(caption: String?) {
        var caption1: Caption? = this.caption
        if (caption1 == null) {
            user ?: return
            caption1 = Caption(userId = user.pk, text = caption ?: "")
            this.caption = caption1
            return
        }
        caption1.text = caption ?: ""
    }
}