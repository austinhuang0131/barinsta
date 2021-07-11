package awais.instagrabber.utils

import java.time.LocalDateTime
import java.util.*

object DateUtils {
    val timezoneOffset: Int
        get() {
            val calendar = Calendar.getInstance(Locale.getDefault())
            return -(calendar[Calendar.ZONE_OFFSET] + calendar[Calendar.DST_OFFSET]) / (60 * 1000)
        }

    @JvmStatic
    fun isBeforeOrEqual(localDateTime: LocalDateTime, comparedTo: LocalDateTime): Boolean {
        return localDateTime.isBefore(comparedTo) || localDateTime.isEqual(comparedTo)
    }
}