package awais.instagrabber.utils

import android.util.Patterns
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.math.absoluteValue

object TextUtils {
    var dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(Constants.defaultDateTimeFormat)

    @JvmStatic
    fun isEmpty(charSequence: CharSequence?): Boolean {
        if (charSequence.isNullOrBlank()) return true
        if (charSequence is String) {
            var str = charSequence
            if ("" == str || "null" == str || str.isEmpty()) return true
            str = str.trim { it <= ' ' }
            return "" == str || "null" == str || str.isEmpty()
        }
        return "null".contentEquals(charSequence) || "".contentEquals(charSequence)
    }

    @JvmStatic
    @JvmOverloads
    fun millisToTimeString(millis: Long, includeHoursAlways: Boolean = false): String {
        val sec = (millis / 1000).toInt() % 60
        var min = (millis / (1000 * 60)).toInt()
        if (min >= 60) {
            min = (millis / (1000 * 60) % 60).toInt()
            val hr = (millis / (1000 * 60 * 60) % 24).toInt()
            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hr, min, sec)
        }
        return if (includeHoursAlways) {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", 0, min, sec)
        } else String.format(Locale.ENGLISH, "%02d:%02d", min, sec)
    }

    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    @JvmStatic
    fun getRelativeDateTimeString(from: Long): String {
        val now = LocalDateTime.now()
        val then = LocalDateTime.ofInstant(Instant.ofEpochMilli(from), ZoneId.systemDefault())
        val days = Duration.between(now, then).toDays().absoluteValue
        return then.format(if (days == 0L) timeFormatter else dateFormatter)
    }

    @JvmStatic
    fun extractUrls(text: String): List<String> {
        if (isEmpty(text)) return emptyList()
        val matcher = Patterns.WEB_URL.matcher(text)
        val urls: MutableList<String> = ArrayList()
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls
    }

    // https://github.com/notslang/instagram-id-to-url-segment
    @JvmStatic
    fun shortcodeToId(shortcode: String): Long {
        var result = 0L
        var i = 0
        while (i < shortcode.length && i < 11) {
            val c = shortcode[i]
            val k = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".indexOf(c)
            result = result * 64 + k
            i++
        }
        return result
    }

    @JvmStatic
    fun setFormatter(datetimeParser: DateTimeFormatter) {
        if (!DateUtils.checkFormatterValid(datetimeParser)) return
        this.dateTimeFormatter = datetimeParser
    }

    @JvmStatic
    fun epochSecondToString(epochSecond: Long): String {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(epochSecond),
            ZoneId.systemDefault()
        ).format(dateTimeFormatter)
    }

    @JvmStatic
    fun nowToString(): String {
        return LocalDateTime.now().format(dateTimeFormatter)
    }
}