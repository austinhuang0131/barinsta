package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class ProfileBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val profileId: String
) : BroadcastOptions(
    clientContext,
    threadIdsOrUserIds,
    BroadcastItemType.PROFILE
) {
    override val formMap: Map<String, String>
        get() = mapOf("profile_user_id" to profileId)
}