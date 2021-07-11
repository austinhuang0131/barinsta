package awais.instagrabber.utils

import android.net.Uri
import androidx.core.net.toUri

private const val domain = "barinsta"

fun getDirectThreadDeepLink(threadId: String, threadTitle: String, isPending: Boolean = false): Uri =
    "$domain://dm_thread/$threadId/$threadTitle?pending=${isPending}".toUri()

fun getProfileDeepLink(username: String): Uri = "$domain://profile/$username".toUri()

fun getPostDeepLink(shortCode: String): Uri = "$domain://post/$shortCode".toUri()

fun getLocationDeepLink(locationId: Long): Uri = "$domain://location/$locationId".toUri()

fun getLocationDeepLink(locationId: String): Uri = "$domain://location/$locationId".toUri()

fun getHashtagDeepLink(hashtag: String): Uri = "$domain://hashtag/$hashtag".toUri()

fun getNotificationsDeepLink(type: String, targetId: Long = 0): Uri = "$domain://notifications/$type?targetId=$targetId".toUri()

fun getSearchDeepLink(): Uri = "$domain://search".toUri()