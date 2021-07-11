package awais.instagrabber.repositories.responses

import java.io.Serializable

data class AnimatedMediaFixedHeight(val height: Int, val width: Int, val mp4: String?, val url: String?, val webp: String?) : Serializable