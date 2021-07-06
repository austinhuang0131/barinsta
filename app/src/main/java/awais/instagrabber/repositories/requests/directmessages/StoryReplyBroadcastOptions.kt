package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class StoryReplyBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val text: String,
    val mediaId: String,
    val reelId: String
) : BroadcastOptions(clientContext, threadIdsOrUserIds, BroadcastItemType.REELSHARE) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "text" to text,
            "media_id" to mediaId,
            "reel_id" to reelId,
            "entry" to "reel",
        )
}