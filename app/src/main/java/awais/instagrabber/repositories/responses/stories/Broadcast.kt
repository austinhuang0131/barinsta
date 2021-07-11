package awais.instagrabber.repositories.responses.stories

import awais.instagrabber.repositories.responses.User
import java.io.Serializable

data class Broadcast(
    val id: String?,
    val dashPlaybackUrl: String?,
    val dashAbrPlaybackUrl: String?, // adaptive quality
    val viewerCount: Double?, // always .0
    val muted: Boolean?,
    val coverFrameUrl: String?,
    val broadcastOwner: User?,
    val publishedTime: Long?
) : Serializable