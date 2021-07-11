@file:JvmName("CookieUtils")

package awais.instagrabber.utils

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import awais.instagrabber.db.repositories.AccountRepository
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern

private const val TAG = "CookieUtils"
private val COOKIE_MANAGER = CookieManager.getInstance()

@JvmField
val NET_COOKIE_MANAGER = java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL)

fun setupCookies(cookieRaw: String) {
    val cookieStore = NET_COOKIE_MANAGER.cookieStore
    if (cookieStore == null || TextUtils.isEmpty(cookieRaw)) {
        return
    }
    if (cookieRaw == "LOGOUT") {
        cookieStore.removeAll()
        return
    }
    try {
        val uri1 = URI("https://instagram.com")
        val uri2 = URI("https://instagram.com/")
        val uri3 = URI("https://i.instagram.com/")
        for (cookie in cookieRaw.split("; ")) {
            val strings = cookie.split("=", limit = 2)
            val httpCookie = HttpCookie(strings[0].trim { it <= ' ' }, strings[1].trim { it <= ' ' })
            httpCookie.domain = ".instagram.com"
            httpCookie.path = "/"
            httpCookie.version = 0
            cookieStore.add(uri1, httpCookie)
            cookieStore.add(uri2, httpCookie)
            cookieStore.add(uri3, httpCookie)
        }
    } catch (e: URISyntaxException) {
        Log.e(TAG, "", e)
    }
}

suspend fun removeAllAccounts(context: Context) {
    NET_COOKIE_MANAGER.cookieStore.removeAll()
    AccountRepository.getInstance(context).deleteAllAccounts()
}

fun getUserIdFromCookie(cookies: String?): Long {
    cookies ?: return 0
    val dsUserId = getCookieValue(cookies, "ds_user_id") ?: return 0
    try {
        return dsUserId.toLong()
    } catch (e: NumberFormatException) {
        Log.e(TAG, "getUserIdFromCookie: ", e)
    }
    return 0
}

fun getCsrfTokenFromCookie(cookies: String): String? {
    return getCookieValue(cookies, "csrftoken")
}

private fun getCookieValue(cookies: String, name: String): String? {
    val pattern = Pattern.compile("$name=(.+?);")
    val matcher = pattern.matcher(cookies)
    return if (matcher.find()) {
        matcher.group(1)
    } else null
}

fun getCookie(webViewUrl: String?): String? {
    val domains: List<String> = listOfNotNull(
        if (!TextUtils.isEmpty(webViewUrl)) webViewUrl else null,
        "https://instagram.com",
        "https://instagram.com/",
        "http://instagram.com",
        "http://instagram.com",
        "https://www.instagram.com",
        "https://www.instagram.com/",
        "http://www.instagram.com",
        "http://www.instagram.com/",
    )
    return getLongestCookie(domains)
}

private fun getLongestCookie(domains: List<String>): String? {
    var longestLength = 0
    var longestCookie: String? = null
    for (domain in domains) {
        val cookie = COOKIE_MANAGER.getCookie(domain)
        if (cookie != null) {
            val cookieLength = cookie.length
            if (cookieLength > longestLength) {
                longestCookie = cookie
                longestLength = cookieLength
            }
        }
    }
    return longestCookie
}