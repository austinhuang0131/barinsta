package awais.instagrabber.repositories.responses.discover

import awais.instagrabber.repositories.responses.Media
import java.io.Serializable

data class TopicCluster(
    val id: String,
    val title: String,
    val type: String?,
    val canMute: Boolean?,
    val isMuted: Boolean?,
    val rankedPosition: Int,
    var coverMedia: Media?
) : Serializable