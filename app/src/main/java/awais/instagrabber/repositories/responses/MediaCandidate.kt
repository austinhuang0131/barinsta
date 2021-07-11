package awais.instagrabber.repositories.responses

import java.io.Serializable

data class MediaCandidate(val width: Int, val height: Int, val url: String) : Serializable