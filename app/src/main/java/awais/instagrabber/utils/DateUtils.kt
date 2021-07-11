package awais.instagrabber.utils

import android.util.Log
import awais.instagrabber.utils.extensions.TAG
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    @JvmStatic
    fun checkFormatterValid(datetimeParser: DateTimeFormatter): Boolean = try {
        LocalDateTime.now().format(datetimeParser)
        true
    } catch (e: Exception) {
        Log.e(TAG, "checkFormatterValid: ", e)
        false
    }
}