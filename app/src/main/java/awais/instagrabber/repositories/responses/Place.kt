package awais.instagrabber.repositories.responses

data class Place(
    val location: Location,
    // for search
    val title: String, // those are repeated within location
    val subtitle: String?, // address
    // browser only; for end of address
    val slug: String?,
    // for location info
    val status: String?
)