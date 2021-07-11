package awais.instagrabber.repositories.responses.discover

import awais.instagrabber.repositories.responses.WrappedMedia

data class TopicalExploreFeedResponse(
    val moreAvailable: Boolean,
    val nextMaxId: String?,
    val maxId: String?,
    val status: String,
    val numResults: Int,
    val clusters: List<TopicCluster>?,
    val items: List<WrappedMedia>?
)