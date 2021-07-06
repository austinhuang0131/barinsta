package awais.instagrabber.models.enums

import java.io.Serializable
import java.util.*

enum class FollowingType(val id: Int) : Serializable {
    FOLLOWING(1),
    NOT_FOLLOWING(0);

    companion object {
        private val map: MutableMap<Int, FollowingType> = mutableMapOf()

        @JvmStatic
        fun valueOf(id: Int): FollowingType? {
            return map[id]
        }

        init {
            for (type in values()) {
                map[type.id] = type
            }
        }
    }
}