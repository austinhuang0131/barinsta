package awais.instagrabber.repositories.responses.stories

import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.ImageVersions2
import awais.instagrabber.repositories.responses.MediaCandidate
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils
import java.io.Serializable

data class StoryMediaResponse(
    val items: List<StoryMedia?>?, // length 1
    val status: String?
    // ignoring pagination properties
) : Serializable