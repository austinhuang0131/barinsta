package awais.instagrabber.repositories.responses.stories

import android.net.Uri
import java.io.Serializable

// https://github.com/austinhuang0131/barinsta/issues/1151
data class StoryAppAttribution(
    val name: String?,
    val appActionText: String?,
    val contentUrl: String?
) : Serializable {
    val url: String?
        get() {
            val uri = Uri.parse(contentUrl)
            return if (uri.getHost().equals("open.spotify.com")) contentUrl?.split("?")?.get(0)
                   else contentUrl
        }
}