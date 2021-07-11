package awais.instagrabber.repositories.responses

class TagFeedResponse(
    val numResults: Int,
    val nextMaxId: String?,
    val moreAvailable: Boolean,
    val status: String,
    val items: List<Media>
)