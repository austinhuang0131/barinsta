package awais.instagrabber.repositories.requests.directmessages

data class ThreadIdsOrUserIds(val threadIds: List<String>? = null, val userIds: List<List<String>>? = null) {
    companion object {
        @JvmStatic
        fun of(threadId: String): ThreadIdsOrUserIds {
            return ThreadIdsOrUserIds(listOf(threadId), null)
        }

        fun ofOneUser(userId: String): ThreadIdsOrUserIds {
            return ThreadIdsOrUserIds(null, listOf(listOf(userId)))
        }
    }
}