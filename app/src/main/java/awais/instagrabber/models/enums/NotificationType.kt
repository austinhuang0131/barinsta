package awais.instagrabber.models.enums

import java.io.Serializable

enum class NotificationType(val itemType: Int) : Serializable {
    LIKE(60),
    FOLLOW(101),
    COMMENT(12),  // NOT TESTED
    COMMENT_MENTION(66),
    TAGGED(102),  // NOT TESTED
    COMMENT_LIKE(13),
    TAGGED_COMMENT(14),
    RESPONDED_STORY(213),
    REQUEST(75),
    AYML(9999);

    companion object {
        private val map: MutableMap<Int, NotificationType> = mutableMapOf()

        @JvmStatic
        fun valueOfType(itemType: Int): NotificationType? {
            return map[itemType]
        }

        init {
            for (type in values()) {
                map[type.itemType] = type
            }
        }
    }
}