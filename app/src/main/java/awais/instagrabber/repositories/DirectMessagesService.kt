package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.directmessages.*
import retrofit2.http.*

interface DirectMessagesService {
    @GET("/api/v1/direct_v2/inbox/")
    suspend fun fetchInbox(@QueryMap queryMap: Map<String, String>): DirectInboxResponse

    @GET("/api/v1/direct_v2/pending_inbox/")
    suspend fun fetchPendingInbox(@QueryMap queryMap: Map<String, String>): DirectInboxResponse

    @GET("/api/v1/direct_v2/threads/{threadId}/")
    suspend fun fetchThread(
        @Path("threadId") threadId: String,
        @QueryMap queryMap: Map<String, String>,
    ): DirectThreadFeedResponse

    @GET("/api/v1/direct_v2/get_badge_count/?no_raven=1")
    suspend fun fetchUnseenCount(): DirectBadgeCount

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/broadcast/{item}/")
    suspend fun broadcast(
        @Path("item") item: String,
        @FieldMap signedForm: Map<String, String>,
    ): DirectThreadBroadcastResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/add_user/")
    suspend fun addUsers(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/remove_users/")
    suspend fun removeUsers(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/update_title/")
    suspend fun updateTitle(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/add_admins/")
    suspend fun addAdmins(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/remove_admins/")
    suspend fun removeAdmins(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/items/{itemId}/delete/")
    suspend fun deleteItem(
        @Path("threadId") threadId: String,
        @Path("itemId") itemId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @GET("/api/v1/direct_v2/ranked_recipients/")
    suspend fun rankedRecipients(@QueryMap queryMap: Map<String, String>): RankedRecipientsResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/broadcast/forward/")
    suspend fun forward(@FieldMap form: Map<String, String>): DirectThreadBroadcastResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/create_group_thread/")
    suspend fun createThread(@FieldMap signedForm: Map<String, String>): DirectThread

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/mute/")
    suspend fun mute(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/unmute/")
    suspend fun unmute(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/mute_mentions/")
    suspend fun muteMentions(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String?>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/unmute_mentions/")
    suspend fun unmuteMentions(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @GET("/api/v1/direct_v2/threads/{threadId}/participant_requests/")
    suspend fun participantRequests(
        @Path("threadId") threadId: String,
        @Query("page_size") pageSize: Int,
        @Query("cursor") cursor: String?,
    ): DirectThreadParticipantRequestsResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/approve_participant_requests/")
    suspend fun approveParticipantRequests(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/deny_participant_requests/")
    suspend fun declineParticipantRequests(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/approval_required_for_new_members/")
    suspend fun approvalRequired(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/approval_not_required_for_new_members/")
    suspend fun approvalNotRequired(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/leave/")
    suspend fun leave(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/remove_all_users/")
    suspend fun end(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): DirectThreadDetailsChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/approve/")
    suspend fun approveRequest(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/decline/")
    suspend fun declineRequest(
        @Path("threadId") threadId: String,
        @FieldMap form: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/direct_v2/threads/{threadId}/items/{itemId}/seen/")
    suspend fun markItemSeen(
        @Path("threadId") threadId: String,
        @Path("itemId") itemId: String,
        @FieldMap form: Map<String, String>,
    ): DirectItemSeenResponse
}