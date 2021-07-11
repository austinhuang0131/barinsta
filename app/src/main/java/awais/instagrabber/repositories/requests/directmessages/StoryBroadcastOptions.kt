package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class StoryBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val mediaId: String,
    val reelId: String
) : BroadcastOptions(clientContext, threadIdsOrUserIds, BroadcastItemType.STORY) {
    override val formMap: Map<String, String>
        get() = mapOf(
            "story_media_id" to mediaId,
            "reel_id" to reelId,
        )
}