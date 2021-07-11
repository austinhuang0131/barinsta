package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class MediaShareBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val mediaId: String,
    val childId: String?
) : BroadcastOptions(
    clientContext,
    threadIdsOrUserIds,
    BroadcastItemType.MEDIA_SHARE
) {
    override val formMap: Map<String, String>
        get() = listOfNotNull(
            "media_id" to mediaId,
            if (childId != null) "carousel_share_child_media_id" to childId else null
        ).toMap()
}