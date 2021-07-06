package awais.instagrabber.repositories.responses

data class UserSearchResponse(
    val numResults: Int,
    val users: List<User>?,
    val status: String
)