package awais.instagrabber.repositories.responses

import awais.instagrabber.models.Comment

data class ChildCommentsFetchResponse(
    val childCommentCount: Int,
    val nextMaxChildCursor: String?,
    val childComments: List<Comment>?,
    val hasMoreTailChildComments: Boolean?
)