package awais.instagrabber.repositories.responses.stories

import awais.instagrabber.repositories.responses.ImageUrl
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils
import java.io.Serializable

data class Story(
    // universal
    val id: String? = null,
    val latestReelMedia: Long? = null, // = timestamp
    val mediaCount: Int? = null,
    // for stories and highlights
    var seen: Long? = null,
    val user: User? = null,
    // for stories
    val muted: Boolean? = null,
    val hasBestiesMedia: Boolean? = null,
    val items: List<StoryMedia>? = null, // may be null
    // for highlights
    val coverMedia: CoverMedia? = null,
    val title: String? = null,
    // for archives
    val coverImageVersion: ImageUrl? = null,
    // invented fields
    val broadcast: Broadcast? = null, // does not naturally occur
) : Serializable {
    val dateTime: String
        get() = if (latestReelMedia != null) TextUtils.epochSecondToString(latestReelMedia) else ""
    // note that archives have property "timestamp" but is ignored
}