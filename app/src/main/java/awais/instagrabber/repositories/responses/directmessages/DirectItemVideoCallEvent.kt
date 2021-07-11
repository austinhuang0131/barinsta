package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectItemVideoCallEvent(
    val action: String? = null,
    val encodedServerDataInfo: String? = null,
    val description: String? = null,
    val threadHasAudioOnlyCall: Boolean = false,
    val textAttributes: List<TextRange>? = null,
) : Serializable