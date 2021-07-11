package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemLink(
    val text: String? = null,
    val linkContext: DirectItemLinkContext? = null,
    val clientContext: String? = null,
    val mutationToken: String? = null,
) : Serializable