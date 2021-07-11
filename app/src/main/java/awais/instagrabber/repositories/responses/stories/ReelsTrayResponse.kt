package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class ReelsTrayResponse(
    val status: String?,
    val tray: List<Story>?,
    val broadcasts: List<Broadcast>?
) : Serializable