package awais.instagrabber.models

import java.io.Serializable

class FollowModel(
    val id: String,
    val username: String,
    val fullName: String,
    val profilePicUrl: String
) : Serializable {
    private var hasNextPage = false
        get() = endCursor != null && field

    var isShown = true

    var endCursor: String? = null
        private set

    fun setPageCursor(hasNextPage: Boolean, endCursor: String?) {
        this.endCursor = endCursor
        this.hasNextPage = hasNextPage
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FollowModel

        if (id != other.id) return false
        if (username != other.username) return false
        if (fullName != other.fullName) return false
        if (profilePicUrl != other.profilePicUrl) return false
        if (isShown != other.isShown) return false
        if (endCursor != other.endCursor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + profilePicUrl.hashCode()
        result = 31 * result + isShown.hashCode()
        result = 31 * result + (endCursor?.hashCode() ?: 0)
        return result
    }
}