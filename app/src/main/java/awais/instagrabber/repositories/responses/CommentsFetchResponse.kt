package awais.instagrabber.repositories.responses

import awais.instagrabber.models.Comment

data class CommentsFetchResponse(
    val commentCount: Int,
    val nextMinId: String?,
    val comments: List<Comment>?,
    val hasMoreComments: Boolean
)