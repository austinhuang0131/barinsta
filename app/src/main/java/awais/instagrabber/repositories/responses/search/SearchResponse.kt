package awais.instagrabber.repositories.responses.search

data class SearchResponse(
    // app
    val list: List<SearchItem>?,
    // browser
    val users: List<SearchItem>?,
    val places: List<SearchItem>?,
    val hashtags: List<SearchItem>?,
    // universal
    val status: String?,
)