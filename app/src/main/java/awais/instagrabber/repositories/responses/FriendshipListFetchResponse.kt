package awais.instagrabber.repositories.responses

data class FriendshipListFetchResponse(
    var nextMaxId: String?,
    var status: String?,
    var users: List<User>?
) {
    val isMoreAvailable: Boolean
        get() = !nextMaxId.isNullOrBlank()
}