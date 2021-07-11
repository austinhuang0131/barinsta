package awais.instagrabber.webservices

import awais.instagrabber.repositories.FriendshipService
import awais.instagrabber.repositories.responses.FriendshipChangeResponse
import awais.instagrabber.repositories.responses.FriendshipListFetchResponse
import awais.instagrabber.repositories.responses.FriendshipRestrictResponse
import awais.instagrabber.utils.Utils
import awais.instagrabber.webservices.RetrofitFactory.retrofit

class FriendshipRepository(private val service: FriendshipService) {

    suspend fun follow(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, "create", targetUserId)

    suspend fun unfollow(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, "destroy", targetUserId)

    suspend fun changeBlock(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        unblock: Boolean,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, if (unblock) "unblock" else "block", targetUserId)

    suspend fun toggleRestrict(
        csrfToken: String,
        deviceUuid: String,
        targetUserId: Long,
        restrict: Boolean,
    ): FriendshipRestrictResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uuid" to deviceUuid,
            "target_user_id" to targetUserId.toString(),
        )
        val action = if (restrict) "restrict" else "unrestrict"
        return service.toggleRestrict(action, form)
    }

    suspend fun approve(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, "approve", targetUserId)

    suspend fun ignore(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, "ignore", targetUserId)

    suspend fun removeFollower(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        targetUserId: Long,
    ): FriendshipChangeResponse = change(csrfToken, userId, deviceUuid, "remove_follower", targetUserId)

    private suspend fun change(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        action: String,
        targetUserId: Long,
    ): FriendshipChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId,
            "_uuid" to deviceUuid,
            "radio_type" to "wifi-none",
            "user_id" to targetUserId,
        )
        val signedForm = Utils.sign(form)
        return service.change(action, targetUserId, signedForm)
    }

    suspend fun changeMute(
        csrfToken: String,
        userId: Long,
        deviceUuid: String,
        unmute: Boolean,
        targetUserId: Long,
        story: Boolean,  // true for story, false for posts
    ): FriendshipChangeResponse {
        val form = mapOf(
            "_csrftoken" to csrfToken,
            "_uid" to userId.toString(),
            "_uuid" to deviceUuid,
            (if (story) "target_reel_author_id" else "target_posts_author_id") to targetUserId.toString(),
        )
        return service.changeMute(
            if (unmute) "unmute_posts_or_story_from_follow" else "mute_posts_or_story_from_follow",
            form
        )
    }

    suspend fun getList(
        follower: Boolean,
        targetUserId: Long,
        maxId: String?,
        query: String?
    ): FriendshipListFetchResponse {
        val queryMap: MutableMap<String, String> = mutableMapOf()
        if (!maxId.isNullOrEmpty()) queryMap.set("max_id", maxId)
        if (!query.isNullOrEmpty()) queryMap.set("query", query)
        return service.getList(targetUserId, if (follower) "followers" else "following", queryMap.toMap())
    }

    companion object {
        @Volatile
        private var INSTANCE: FriendshipRepository? = null

        fun getInstance(): FriendshipRepository {
            return INSTANCE ?: synchronized(this) {
                val service: FriendshipService = retrofit.create(FriendshipService::class.java)
                FriendshipRepository(service).also { INSTANCE = it }
            }
        }
    }
}