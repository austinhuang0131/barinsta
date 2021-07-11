package awais.instagrabber.repositories.responses

import java.io.Serializable

data class ImageUrl(val url: String, private val width: Int, private val height: Int) : Serializable