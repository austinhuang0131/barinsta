package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemActionLog(
    val description: String? = null,
    val bold: List<TextRange>? = null,
    val textAttributes: List<TextRange>? = null
) : Serializable