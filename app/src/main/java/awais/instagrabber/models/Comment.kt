package awais.instagrabber.models

import awais.instagrabber.repositories.responses.User
import awais.instagrabber.utils.TextUtils
import java.io.Serializable
import java.util.*

class Comment(
    val pk: String,
    val text: String,
    val createdAt: Long,
    var commentLikeCount: Long,
    private var hasLikedComment: Boolean,
    val user: User,
    val childCommentCount: Int
) : Serializable, Cloneable {
    val dateTime: String
        get() = TextUtils.epochSecondToString(createdAt)

    fun getLiked(): Boolean {
        return hasLikedComment
    }

    fun setLiked(hasLikedComment: Boolean) {
        commentLikeCount = if (hasLikedComment) commentLikeCount + 1 else commentLikeCount - 1
        this.hasLikedComment = hasLikedComment
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Comment

        if (pk != other.pk) return false
        if (text != other.text) return false
        if (createdAt != other.createdAt) return false
        if (commentLikeCount != other.commentLikeCount) return false
        if (hasLikedComment != other.hasLikedComment) return false
        if (user != other.user) return false
        if (childCommentCount != other.childCommentCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pk.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + commentLikeCount.hashCode()
        result = 31 * result + hasLikedComment.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + childCommentCount
        return result
    }

    override fun toString(): String {
        return "Comment(pk='$pk', text='$text', createdAt=$createdAt, commentLikeCount=$commentLikeCount, hasLikedComment=$hasLikedComment, user=$user, childCommentCount=$childCommentCount)"
    }
}