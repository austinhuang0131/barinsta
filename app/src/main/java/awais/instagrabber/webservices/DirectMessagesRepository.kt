package awais.instagrabber.webservices

import awais.instagrabber.repositories.DirectMessagesService
import awais.instagrabber.repositories.requests.directmessages.*
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.giphy.GiphyGif
import awais.instagrabber.utils.TextUtils.extractUrls
import awais.instagrabber.utils.Utils
import org.json.JSONArray
import java.util.*

open class DirectMessagesRepository(private val service: DirectMessagesService) {

    suspend fun fetchInbox(
        cursor: String?,
        seqId: Long,
    ): DirectInboxResponse {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "thread_message_limit" to 10.toString(),
            "persistentBadging" to true.toString(),
            "limit" to 10.toString(),
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
            queryMap["direction"] = "older"
        }
        if (seqId != 0L) {
            queryMap["seq_id"] = seqId.toString()
        }
        return service.fetchInbox(queryMap)
    }

    suspend fun fetchThread(
        threadId: String,
        cursor: String?,
    ): DirectThreadFeedResponse {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "limit" to 20.toString(),
            "direction" to "older",
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
        }
        return service.fetchThread(threadId, queryMap)
    }

    suspend fun fetchUnseenCount(): DirectBadgeCount = service.fetchUnseenCount()

    suspend fun broadcastText(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        text: String,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): DirectThreadBroadcastResponse {
        val urls = extractUrls(text)
        if (urls.isNotEmpty()) {
            return broadcastLink(
                csrfToken,
                userId,
                deviceUuid,
                clientContext,
                threadIdsOrUserIds,
                text,
                urls,
                repliedToItemId,
                repliedToClientContext
            )
        }
        val broadcastOptions = TextBroadcastOptions(clientContext, threadIdsOrUserIds, text)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(csrfToken, userId, deviceUuid, broadcastOptions)
    }

    private suspend fun broadcastLink(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        linkText: String,
        urls: List<String>,
        repliedToItemId: String?,
        repliedToClientContext: String?,
    ): DirectThreadBroadcastResponse {
        val broadcastOptions = LinkBroadcastOptions(clientContext, threadIdsOrUserIds, linkText, urls)
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            broadcastOptions.repliedToItemId = repliedToItemId
            broadcastOptions.repliedToClientContext = repliedToClientContext
        }
        return broadcast(csrfToken, userId, deviceUuid, broadcastOptions)
    }

    suspend fun broadcastPhoto(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        uploadId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, PhotoBroadcastOptions(clientContext, threadIdsOrUserIds, true, uploadId))

    suspend fun broadcastVideo(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        uploadId: String,
        videoResult: String,
        sampled: Boolean,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, VideoBroadcastOptions(clientContext, threadIdsOrUserIds, videoResult, uploadId, sampled))

    suspend fun broadcastVoice(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        uploadId: String,
        waveform: List<Float>,
        samplingFreq: Int,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, VoiceBroadcastOptions(clientContext, threadIdsOrUserIds, uploadId, waveform, samplingFreq))

    suspend fun broadcastStoryReply(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        text: String,
        mediaId: String,
        reelId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, StoryReplyBroadcastOptions(UUID.randomUUID().toString(), threadIdsOrUserIds, text, mediaId, reelId))

    suspend fun broadcastReaction(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        itemId: String,
        emoji: String?,
        delete: Boolean,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, ReactionBroadcastOptions(clientContext, threadIdsOrUserIds, itemId, emoji, delete))

    suspend fun broadcastAnimatedMedia(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        giphyGif: GiphyGif,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, AnimatedMediaBroadcastOptions(clientContext, threadIdsOrUserIds, giphyGif))

    suspend fun broadcastMediaShare(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        mediaId: String,
        childId: String?,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, MediaShareBroadcastOptions(clientContext, threadIdsOrUserIds, mediaId, childId))

    suspend fun broadcastProfile(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        profileId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, ProfileBroadcastOptions(clientContext, threadIdsOrUserIds, profileId))

    suspend fun broadcastStory(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        clientContext: String,
        threadIdsOrUserIds: ThreadIdsOrUserIds,
        mediaId: String,
        reelId: String,
    ): DirectThreadBroadcastResponse =
        broadcast(csrfToken, userId, deviceUuid, StoryBroadcastOptions(clientContext, threadIdsOrUserIds, mediaId, reelId))

    private suspend fun broadcast(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        broadcastOptions: BroadcastOptions,
    ): DirectThreadBroadcastResponse {
        require(broadcastOptions.clientContext.isNotBlank()) { "Broadcast requires a valid client context value" }
        val form = mutableMapOf<String, String>(
            "_csrftoken" to csrfToken,
            "_uid" to userId.toString(10),
            "__uuid" to deviceUuid,
            "client_context" to broadcastOptions.clientContext,
            "mutation_token" to broadcastOptions.clientContext,
        )
        val threadIds = broadcastOptions.threadIds
        val userIds = broadcastOptions.userIds
        require(!userIds.isNullOrEmpty() || !threadIds.isNullOrEmpty()) {
            "Either pass a list of thread ids or a list of lists of user ids"
        }
        if (!threadIds.isNullOrEmpty()) {
            form["thread_ids"] = JSONArray(threadIds).toString()
        }
        if (!userIds.isNullOrEmpty()) {
            form["recipient_users"] = JSONArray(userIds).toString()
        }
        val repliedToItemId = broadcastOptions.repliedToItemId
        val repliedToClientContext = broadcastOptions.repliedToClientContext
        if (!repliedToItemId.isNullOrBlank() && !repliedToClientContext.isNullOrBlank()) {
            form["replied_to_item_id"] = repliedToItemId
            form["replied_to_client_context"] = repliedToClientContext
        }
        form.putAll(broadcastOptions.formMap)
        form["action"] = "send_item"
//        val signedForm = Utils.sign(form)
        return service.broadcast(broadcastOptions.itemType.value, form)
    }

    suspend fun addUsers(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: Collection<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return service.addUsers(threadId, form)
    }

    suspend fun removeUsers(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return service.removeUsers(threadId, form)
    }

    suspend fun updateTitle(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        title: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "title" to title,
        )
        return service.updateTitle(threadId, form)
    }

    suspend fun addAdmins(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return service.addAdmins(threadId, form)
    }

    suspend fun removeAdmins(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: Collection<Long>,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return service.removeAdmins(threadId, form)
    }

    suspend fun deleteItem(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        itemId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.deleteItem(threadId, itemId, form)
    }

    suspend fun rankedRecipients(
        mode: String?,
        showThreads: Boolean?,
        query: String?,
    ): RankedRecipientsResponse {
        // String correctedMode = mode;
        // if (TextUtils.isEmpty(mode) || (!mode.equals("raven") && !mode.equals("reshare"))) {
        //     correctedMode = "raven";
        // }
        val queryMap = mutableMapOf<String, String>()
        if (!mode.isNullOrBlank()) {
            queryMap["mode"] = mode
        }
        if (!query.isNullOrBlank()) {
            queryMap["query"] = query
        }
        if (showThreads != null) {
            queryMap["showThreads"] = showThreads.toString()
        }
        return service.rankedRecipients(queryMap)
    }

    suspend fun forward(
        toThreadId: String,
        itemType: String,
        fromThreadId: String,
        itemId: String,
    ): DirectThreadBroadcastResponse {
        val form = mapOf(
            "action" to "forward_item",
            "thread_id" to toThreadId,
            "item_type" to itemType,
            "forwarded_from_thread_id" to fromThreadId,
            "forwarded_from_thread_item_id" to itemId,
        )
        return service.forward(form)
    }

    suspend fun createThread(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        userIds: List<Long>,
        threadTitle: String?,
    ): DirectThread {
        val userIdStringList = userIds.map { it.toString() }
        val form = mutableMapOf<String, Any>(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "_uid" to userId,
            "recipient_users" to JSONArray(userIdStringList).toString(),
        )
        if (!threadTitle.isNullOrBlank()) {
            form["thread_title"] = threadTitle
        }
        val signedForm = Utils.sign(form)
        return service.createThread(signedForm)
    }

    suspend fun mute(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid
        )
        return service.mute(threadId, form)
    }

    suspend fun unmute(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.unmute(threadId, form)
    }

    suspend fun muteMentions(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.muteMentions(threadId, form)
    }

    suspend fun unmuteMentions(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.unmuteMentions(threadId, form)
    }

    suspend fun participantRequests(
        threadId: String,
        pageSize: Int,
        cursor: String? = null,
    ): DirectThreadParticipantRequestsResponse {
        return service.participantRequests(threadId, pageSize, cursor)
    }

    suspend fun approveParticipantRequests(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: List<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
            // "share_join_chat_story" to String.valueOf(true)
        )
        return service.approveParticipantRequests(threadId, form)
    }

    suspend fun declineParticipantRequests(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        userIds: List<Long>,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "user_ids" to JSONArray(userIds).toString(),
        )
        return service.declineParticipantRequests(threadId, form)
    }

    suspend fun approvalRequired(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.approvalRequired(threadId, form)
    }

    suspend fun approvalNotRequired(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.approvalNotRequired(threadId, form)
    }

    suspend fun leave(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.leave(threadId, form)
    }

    suspend fun end(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): DirectThreadDetailsChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.end(threadId, form)
    }

    suspend fun fetchPendingInbox(cursor: String?, seqId: Long): DirectInboxResponse {
        val queryMap = mutableMapOf(
            "visual_message_return_type" to "unseen",
            "thread_message_limit" to 20.toString(),
            "persistentBadging" to true.toString(),
            "limit" to 10.toString(),
        )
        if (!cursor.isNullOrBlank()) {
            queryMap["cursor"] = cursor
            queryMap["direction"] = "older"
        }
        if (seqId != 0L) {
            queryMap["seq_id"] = seqId.toString()
        }
        return service.fetchPendingInbox(queryMap)
    }

    suspend fun approveRequest(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.approveRequest(threadId, form)
    }

    suspend fun declineRequest(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
    ): String {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
        )
        return service.declineRequest(threadId, form)
    }

    suspend fun markAsSeen(
        csrfToken: String,
        deviceUuid: String,
        threadId: String,
        directItem: DirectItem,
    ): DirectItemSeenResponse? {
        val itemId = directItem.itemId ?: return null
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "use_unified_inbox" to "true",
            "action" to "mark_seen",
            "thread_id" to threadId,
            "item_id" to itemId,
        )
        return service.markItemSeen(threadId, itemId, form)
    }

    companion object {
        @Volatile
        private var INSTANCE: DirectMessagesRepository? = null

        fun getInstance(): DirectMessagesRepository {
            return INSTANCE ?: synchronized(this) {
                val service: DirectMessagesService = RetrofitFactory.retrofit.create(DirectMessagesService::class.java)
                DirectMessagesRepository(service).also { INSTANCE = it }
            }
        }
    }
}