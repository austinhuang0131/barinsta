package awais.instagrabber.repositories.responses

data class LikersResponse(val users: List<User>, val userCount: Long, val status: String)