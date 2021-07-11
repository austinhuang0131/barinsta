package awais.instagrabber.repositories.responses

import awais.instagrabber.models.FollowModel

data class FriendshipListFetchResponse(
    var nextMaxId: String?,
    var status: String?,
    var items: List<FollowModel>?
) {
    val isMoreAvailable: Boolean
        get() = !nextMaxId.isNullOrBlank()

    fun setNextMaxId(nextMaxId: String): FriendshipListFetchResponse {
        this.nextMaxId = nextMaxId
        return this
    }

    fun setStatus(status: String): FriendshipListFetchResponse {
        this.status = status
        return this
    }

    fun setItems(items: List<FollowModel>): FriendshipListFetchResponse {
        this.items = items
        return this
    }
}