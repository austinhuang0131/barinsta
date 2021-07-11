package awais.instagrabber.repositories.responses

import java.io.Serializable

data class Caption(
    val userId: Long = 0,
    var text: String? = null,
) : Serializable {
    var pk: String? = null
}