package awais.instagrabber.utils

import android.net.Uri
import android.text.TextUtils
import awais.instagrabber.models.IntentModel
import awais.instagrabber.models.enums.IntentModelType

object IntentUtils {
    @JvmStatic
    fun parseUrl(url: String): IntentModel? {
        val parsedUrl = Uri.parse(url).normalizeScheme()

        // final String domain = parsedUrl.getHost().replaceFirst("^www\\.", "");
        // final boolean isHttpsUri = "https".equals(parsedUrl.getScheme());
        val paths = parsedUrl.pathSegments
        if (paths.isEmpty()) {
            return null
        }
        var path = paths[0]
        var text: String? = null
        var type = IntentModelType.UNKNOWN
        if (1 == paths.size) {
            text = path
            type = IntentModelType.USERNAME
        } else if ("_u" == path) {
            text = paths[1]
            type = IntentModelType.USERNAME
        } else if ("p" == path || "reel" == path || "tv" == path) {
            text = paths[1]
            type = IntentModelType.POST
        } else if (2 < paths.size && "explore" == path) {
            path = paths[1]
            if ("locations" == path) {
                text = paths[2]
                type = IntentModelType.LOCATION
            }
            if ("tags" == path) {
                text = paths[2]
                type = IntentModelType.HASHTAG
            }
        }
        return if (TextUtils.isEmpty(text)) {
            null
        } else IntentModel(type, text!!)
    }
}