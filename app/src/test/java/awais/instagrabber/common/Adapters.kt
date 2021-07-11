package awais.instagrabber.common

import awais.instagrabber.db.dao.AccountDao
import awais.instagrabber.db.dao.FavoriteDao
import awais.instagrabber.db.entities.Account
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.*
import awais.instagrabber.repositories.responses.*
import awais.instagrabber.repositories.responses.directmessages.*
import awais.instagrabber.repositories.responses.stories.ArchiveResponse
import awais.instagrabber.repositories.responses.stories.ReelsTrayResponse
import awais.instagrabber.repositories.responses.stories.StoryStickerResponse

open class UserServiceAdapter : UserService {
    override suspend fun getUserInfo(uid: Long): WrappedUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUsernameInfo(username: String): WrappedUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUserFriendship(uid: Long): FriendshipStatus = FriendshipStatus()

    override suspend fun search(timezoneOffset: Float, query: String): UserSearchResponse {
        TODO("Not yet implemented")
    }
}

open class FriendshipServiceAdapter : FriendshipService {
    override suspend fun change(action: String, id: Long, form: Map<String, String>): FriendshipChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun toggleRestrict(action: String, form: Map<String, String>): FriendshipRestrictResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getList(userId: Long, type: String, queryParams: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun changeMute(action: String, form: Map<String, String>): FriendshipChangeResponse {
        TODO("Not yet implemented")
    }
}

open class StoriesServiceAdapter : StoriesService {
    override suspend fun fetch(mediaId: Long): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFeedStories(): ReelsTrayResponse? {
        TODO("Not yet implemented")
    }

    override suspend fun fetchHighlights(uid: Long): ReelsTrayResponse? {
        TODO("Not yet implemented")
    }

    override suspend fun fetchArchive(queryParams: Map<String, String>): ArchiveResponse? {
        TODO("Not yet implemented")
    }

    override suspend fun getUserStory(url: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun respondToSticker(storyId: String, stickerId: String, action: String, form: Map<String, String>): StoryStickerResponse {
        TODO("Not yet implemented")
    }

    override suspend fun seen(queryParams: Map<String, String>, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }
}

open class MediaServiceAdapter : MediaService {
    override suspend fun fetch(mediaId: Long): MediaInfoResponse {
        TODO("Not yet implemented")
    }

    override suspend fun fetchLikes(mediaId: String, action: String): LikersResponse {
        TODO("Not yet implemented")
    }

    override suspend fun action(action: String, mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun editCaption(mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun translate(form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun uploadFinish(retryContext: String, queryParams: Map<String, String>, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun delete(mediaId: String, mediaType: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun archive(mediaId: String, signedForm: Map<String, String>): String {
        TODO("Not yet implemented")
    }
}

open class GraphQLServiceAdapter : GraphQLService {
    override suspend fun fetch(queryParams: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun getUser(username: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getPost(shortcode: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getTag(tag: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getLocation(locationId: Long): String {
        TODO("Not yet implemented")
    }
}

open class AccountDaoAdapter : AccountDao {
    override suspend fun getAllAccounts(): List<Account> {
        TODO("Not yet implemented")
    }

    override suspend fun findAccountByUid(uid: String): Account? {
        TODO("Not yet implemented")
    }

    override suspend fun insertAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun updateAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccounts(vararg accounts: Account) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllAccounts() {
        TODO("Not yet implemented")
    }
}

open class FavoriteDaoAdapter : FavoriteDao {
    override suspend fun getAllFavorites(): List<Favorite> = emptyList()

    override suspend fun findFavoriteByQueryAndType(query: String, type: FavoriteType): Favorite? = null

    override suspend fun insertFavorites(vararg favorites: Favorite) {}

    override suspend fun updateFavorites(vararg favorites: Favorite) {}

    override suspend fun deleteFavorites(vararg favorites: Favorite) {}

    override suspend fun deleteAllFavorites() {}
}

open class DirectMessagesServiceAdapter: DirectMessagesService {
    override suspend fun fetchInbox(queryMap: Map<String, String>): DirectInboxResponse {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPendingInbox(queryMap: Map<String, String>): DirectInboxResponse {
        TODO("Not yet implemented")
    }

    override suspend fun fetchThread(threadId: String, queryMap: Map<String, String>): DirectThreadFeedResponse {
        TODO("Not yet implemented")
    }

    override suspend fun fetchUnseenCount(): DirectBadgeCount {
        TODO("Not yet implemented")
    }

    override suspend fun broadcast(item: String, signedForm: Map<String, String>): DirectThreadBroadcastResponse {
        TODO("Not yet implemented")
    }

    override suspend fun addUsers(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun removeUsers(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun updateTitle(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun addAdmins(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun removeAdmins(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun deleteItem(threadId: String, itemId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun rankedRecipients(queryMap: Map<String, String>): RankedRecipientsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun forward(form: Map<String, String>): DirectThreadBroadcastResponse {
        TODO("Not yet implemented")
    }

    override suspend fun createThread(signedForm: Map<String, String>): DirectThread {
        TODO("Not yet implemented")
    }

    override suspend fun mute(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun unmute(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun muteMentions(threadId: String, form: Map<String, String?>): String {
        TODO("Not yet implemented")
    }

    override suspend fun unmuteMentions(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun participantRequests(threadId: String, pageSize: Int, cursor: String?): DirectThreadParticipantRequestsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun approveParticipantRequests(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun declineParticipantRequests(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun approvalRequired(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun approvalNotRequired(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun leave(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun end(threadId: String, form: Map<String, String>): DirectThreadDetailsChangeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun approveRequest(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun declineRequest(threadId: String, form: Map<String, String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun markItemSeen(threadId: String, itemId: String, form: Map<String, String>): DirectItemSeenResponse {
        TODO("Not yet implemented")
    }

}