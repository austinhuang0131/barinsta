package awais.instagrabber.utils

import awais.instagrabber.repositories.responses.Media
import java.util.*

class KeywordsFilterUtils(private val keywords: ArrayList<String>) {
//    fun filter(caption: String?): Boolean {
//        if (caption == null) return false
//        if (keywords.isEmpty()) return false
//        val temp = caption.toLowerCase()
//        for (s in keywords) {
//            if (temp.contains(s)) return true
//        }
//        return false
//    }

    fun filter(media: Media?): Boolean {
        if (media == null) return false
        val (_, text) = media.caption ?: return false
        if (keywords.isEmpty()) return false
        val temp = text!!.lowercase(Locale.getDefault())
        for (s in keywords) {
            if (temp.contains(s)) return true
        }
        return false
    }

    fun filter(media: List<Media>?): List<Media>? {
        if (keywords.isEmpty()) return media
        if (media == null) return ArrayList()
        val result: MutableList<Media> = ArrayList()
        for (m in media) {
            if (!filter(m)) result.add(m)
        }
        return result
    }
}