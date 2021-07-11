package awais.instagrabber.repositories.requests.directmessages

import awais.instagrabber.models.enums.BroadcastItemType

class TextBroadcastOptions(
    clientContext: String,
    threadIdsOrUserIds: ThreadIdsOrUserIds,
    val text: String
) : BroadcastOptions(
    clientContext,
    threadIdsOrUserIds,
    BroadcastItemType.TEXT
) {
    override val formMap: Map<String, String>
        get() = mapOf("text" to text)
}