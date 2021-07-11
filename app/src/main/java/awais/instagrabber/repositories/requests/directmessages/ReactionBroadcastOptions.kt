package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class ReactionBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val itemId: String,
    val emoji: String?,
    val delete: Boolean
) : BroadcastOptions(clientContext, threadIdsOrUserIds, BroadcastItemType.REACTION) {
    override val formMap: Map<String, String>
        get() = listOfNotNull(
            "item_id" to itemId,
            "reaction_status" to if (delete) "deleted" else "created",
            "reaction_type" to "like",
            if (!emoji.isNullOrBlank()) "emoji" to emoji else null,
        ).toMap()
}