package awais.instagrabber.utils

import awais.instagrabber.repositories.responses.Media
import java.util.*
import kotlin.collections.ArrayList

//    fun filter(caption: String?): Boolean {
//        if (caption == null) return false
//        if (keywords.isEmpty()) return false
//        val temp = caption.toLowerCase()
//        for (s in keywords) {
//            if (temp.contains(s)) return true
//        }
//        return false
//    }

private fun containsAnyKeyword(keywords: List<String>, media: Media?): Boolean {
    if (media == null || keywords.isEmpty()) return false
    val (_, text) = media.caption ?: return false
    val temp = text!!.lowercase(Locale.getDefault())
    return keywords.any { temp.contains(it) }
}

fun filter(keywords: List<String>, media: List<Media>?): List<Media>? {
    if (keywords.isEmpty()) return media
    if (media == null) return ArrayList()
    val result: MutableList<Media> = ArrayList()
    media.filterNotTo(result) { containsAnyKeyword(keywords, it) }
    return result
}