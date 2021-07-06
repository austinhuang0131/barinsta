package awais.instagrabber.repositories.responses

data class LocationFeedResponse(
    val numResults: Int,
    val nextMaxId: String?,
    val moreAvailable: Boolean?,
    val mediaCount: Long?,
    val status: String,
    val items: List<Media>?,
    val location: Location
)