package awais.instagrabber.db

import androidx.room.TypeConverter
import awais.instagrabber.models.enums.FavoriteType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromFavoriteTypeString(value: String?): FavoriteType? =
        if (value == null) null
        else try {
            FavoriteType.valueOf(value)
        } catch (e: Exception) {
            null
        }

    @TypeConverter
    fun favoriteTypeToString(favoriteType: FavoriteType?): String? = favoriteType?.toString()

    @TypeConverter
    fun fromTimestampToLocalDateTime(value: Long?): LocalDateTime? =
        if (value == null) null else LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.systemDefault())

    @TypeConverter
    fun localDateTimeToTimestamp(localDateTime: LocalDateTime?): Long? = localDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}