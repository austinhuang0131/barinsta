package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class TextRange(
    val start: Int = 0,
    val end: Int = 0,
    val color: String? = null,
    val intent: String? = null,
) : Serializable