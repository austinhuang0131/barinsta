package awais.instagrabber.repositories.responses

class UserFeedResponse(
    val numResults: Int,
    val nextMaxId: String?,
    val moreAvailable: Boolean,
    val status: String,
    val items: List<Media>
)