package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class ThreadContext(
    val type: Int = 0,
    val text: String? = null,
) : Serializable