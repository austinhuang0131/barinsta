package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemPlaceholder(
    val isLinked: Boolean = false,
    val title: String? = null,
    val message: String? = null,
) : Serializable