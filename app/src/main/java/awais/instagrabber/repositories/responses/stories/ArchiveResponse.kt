package awais.instagrabber.repositories.responses.stories

data class ArchiveResponse(
    val numResults: Int,
    val maxId: String?,
    val moreAvailable: Boolean,
    val status: String,
    val items: List<Story>
)