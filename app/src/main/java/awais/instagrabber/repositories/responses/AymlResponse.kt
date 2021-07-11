package awais.instagrabber.repositories.responses

import java.io.Serializable

data class AymlResponse(val newSuggestedUsers: AymlUserList?, val suggestedUsers: AymlUserList?) : Serializable

data class AymlUser(
    val user: User?,
    val algorithm: String?,
    val socialContext: String?,
    val uuid: String?
) : Serializable

data class AymlUserList(val suggestions: List<AymlUser>?) : Serializable