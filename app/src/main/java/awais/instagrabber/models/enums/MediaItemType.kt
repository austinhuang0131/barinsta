package awais.instagrabber.models.enums

import java.io.Serializable

enum class MediaItemType(val id: Int) : Serializable {
    MEDIA_TYPE_IMAGE(1),
    MEDIA_TYPE_VIDEO(2),
    MEDIA_TYPE_SLIDER(8),
    MEDIA_TYPE_VOICE(11),
    MEDIA_TYPE_LIVE(5); // arbitrary

    companion object {
        private val map: MutableMap<Int, MediaItemType> = mutableMapOf()

        @JvmStatic
        fun valueOf(id: Int): MediaItemType? {
            return map[id]
        }

        init {
            for (type in values()) {
                map[type.id] = type
            }
        }
    }
}