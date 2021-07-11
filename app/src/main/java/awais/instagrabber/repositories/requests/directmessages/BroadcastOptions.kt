package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

sealed class BroadcastOptions(
    val clientContext: String,
    private val threadIdsOrUserIds: ThreadIdsOrUserIds,
    val itemType: BroadcastItemType
) {
    var repliedToItemId: String? = null
    var repliedToClientContext: String? = null
    val threadIds: List<String>?
        get() = threadIdsOrUserIds.threadIds
    val userIds: List<List<String>>?
        get() = threadIdsOrUserIds.userIds

    abstract val formMap: Map<String, String>
}