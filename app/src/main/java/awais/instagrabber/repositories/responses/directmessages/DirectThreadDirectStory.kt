package awais.instagrabber.repositories.responses.directmessages

import java.io.Serializable

data class DirectThreadDirectStory(
    val items: List<DirectItem>? = null,
    val unseenCount: Int = 0,
) : Serializable