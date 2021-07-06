package awais.instagrabber.repositories.responses

class PostsFetchResponse(
    val feedModels: List<Media>,
    val hasNextPage: Boolean,
    val nextCursor: String?
)