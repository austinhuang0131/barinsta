package awais.instagrabber.repositories.responses.directmessages

import awais.instagrabber.repositories.responses.User
import java.io.Serializable

data class RankedRecipient(
    val user: User? = null,
    val thread: DirectThread? = null,
) : Serializable {
    companion object {
        @JvmStatic
        fun of(user: User): RankedRecipient {
            return RankedRecipient(user = user)
        }

        @JvmStatic
        fun of(thread: DirectThread): RankedRecipient {
            return RankedRecipient(thread = thread)
        }
    }
}