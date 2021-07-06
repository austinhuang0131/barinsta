package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class ReelsResponse(
    val status: String?,
    val reel: Story?, // users
    val story: Story?, // hashtag and locations (unused)
    val broadcast: Broadcast?
) : Serializable