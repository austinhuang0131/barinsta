package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class ReelsMediaResponse(
    val status: String?,
    val reels: Map<String, Story?>?
) : Serializable