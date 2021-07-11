package awais.instagrabber.models.enums

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class DirectItemType(val id: Int) : Serializable {
    UNKNOWN(0),

    @SerializedName("text")
    TEXT(1),

    @SerializedName("like")
    LIKE(2),

    @SerializedName("link")
    LINK(3),

    @SerializedName("media")
    MEDIA(4),

    @SerializedName("raven_media")
    RAVEN_MEDIA(5),

    @SerializedName("profile")
    PROFILE(6),

    @SerializedName("video_call_event")
    VIDEO_CALL_EVENT(7),

    @SerializedName("animated_media")
    ANIMATED_MEDIA(8),

    @SerializedName("voice_media")
    VOICE_MEDIA(9),

    @SerializedName("media_share")
    MEDIA_SHARE(10),

    @SerializedName("reel_share")
    REEL_SHARE(11),

    @SerializedName("action_log")
    ACTION_LOG(12),

    @SerializedName("placeholder")
    PLACEHOLDER(13),

    @SerializedName("story_share")
    STORY_SHARE(14),

    @SerializedName("clip")
    CLIP(15),        // media_share but reel

    @SerializedName("felix_share")
    FELIX_SHARE(16), // media_share but igtv

    @SerializedName("location")
    LOCATION(17),

    @SerializedName("xma")
    XMA(18); // self avatar stickers

    companion object {
        private val map: MutableMap<Int, DirectItemType> = mutableMapOf()

        @JvmStatic
        fun getTypeFromId(id: Int): DirectItemType {
            return map[id] ?: UNKNOWN
        }

        fun getName(directItemType: DirectItemType): String? {
            when (directItemType) {
                TEXT -> return "text"
                LIKE -> return "like"
                LINK -> return "link"
                MEDIA -> return "media"
                RAVEN_MEDIA -> return "raven_media"
                PROFILE -> return "profile"
                VIDEO_CALL_EVENT -> return "video_call_event"
                ANIMATED_MEDIA -> return "animated_media"
                VOICE_MEDIA -> return "voice_media"
                MEDIA_SHARE -> return "media_share"
                REEL_SHARE -> return "reel_share"
                ACTION_LOG -> return "action_log"
                PLACEHOLDER -> return "placeholder"
                STORY_SHARE -> return "story_share"
                CLIP -> return "clip"
                FELIX_SHARE -> return "felix_share"
                LOCATION -> return "location"
                else -> return null
            }
        }

        init {
            for (type in values()) {
                map[type.id] = type
            }
        }
    }
}