package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class PhotoBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val allowFullAspectRatio: Boolean,
    val uploadId: String
) : BroadcastOptions(
    clientContext,
    threadIdsOrUserIds,
    BroadcastItemType.IMAGE
) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "allow_full_aspect_ratio" to allowFullAspectRatio.toString(),
            "upload_id" to uploadId
        )
}