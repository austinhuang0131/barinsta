package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemLinkContext(
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkSummary: String? = null,
    val linkImageUrl: String? = null
) : Serializable