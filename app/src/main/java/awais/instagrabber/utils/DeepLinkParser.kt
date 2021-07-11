package awais.instagrabber.utils

import java.util.regex.Pattern

object DeepLinkParser {
    private val TYPE_PATTERN_MAP: Map<DeepLink.Type, DeepLinkPattern> = mapOf(
        DeepLink.Type.USER to DeepLinkPattern("instagram://user?username="),
    )

    @JvmStatic
    fun parse(text: String): DeepLink? {
        for ((key, value) in TYPE_PATTERN_MAP) {
            if (text.startsWith(value.patternText)) {
                return DeepLink(key, value.pattern.matcher(text).replaceAll(""))
            }
        }
        return null
    }

    data class DeepLinkPattern(val patternText: String) {
        val pattern: Pattern = Pattern.compile(patternText, Pattern.LITERAL)
    }

    data class DeepLink(val type: Type, val value: String) {
        enum class Type {
            USER
        }
    }
}