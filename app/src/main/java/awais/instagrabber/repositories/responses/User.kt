package awais.instagrabber.repositories.responses

import java.io.Serializable


data class User @JvmOverloads constructor(
    val pk: Long = 0,
    val username: String = "",
    val fullName: String? = "",
    val isPrivate: Boolean = false,
    val profilePicUrl: String? = null,
    val isVerified: Boolean = false,
    val profilePicId: String? = null,
    var friendshipStatus: FriendshipStatus? = null,
    val hasAnonymousProfilePicture: Boolean = false,
    val isUnpublished: Boolean = false,
    val isFavorite: Boolean = false,
    val isDirectappInstalled: Boolean = false,
    val hasChaining: Boolean = false,
    val reelAutoArchive: String? = null,
    val allowedCommenterType: String? = null,
    val mediaCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val followingTagCount: Long = 0,
    val biography: String? = null,
    val externalUrl: String? = null,
    val usertagsCount: Long = 0,
    val publicEmail: String? = null,
    val hdProfilePicUrlInfo: ImageUrl? = null,
    val profileContext: String? = null, // "also followed by" your friends
    val profileContextLinksWithUserIds: List<UserProfileContextLink>? = null, // ^
    val socialContext: String? = null, // AYML
    val interopMessagingUserFbid: String? = null, // in DMs only: Facebook user ID
) : Serializable {
    val hDProfilePicUrl: String
        get() = hdProfilePicUrlInfo?.url ?: profilePicUrl ?: ""
}