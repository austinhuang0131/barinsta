package awais.instagrabber.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import awais.instagrabber.models.enums.FavoriteType
import java.time.LocalDateTime

@Entity(tableName = Favorite.TABLE_NAME)
data class Favorite(
    @ColumnInfo(name = COL_ID) @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = COL_QUERY) val query: String?,
    @ColumnInfo(name = COL_TYPE) val type: FavoriteType?,
    @ColumnInfo(name = COL_DISPLAY_NAME) val displayName: String?,
    @ColumnInfo(name = COL_PIC_URL) val picUrl: String?,
    @ColumnInfo(name = COL_DATE_ADDED) val dateAdded: LocalDateTime?,
) {
    companion object {
        const val TABLE_NAME = "favorites"
        const val COL_ID = "id"
        const val COL_QUERY = "query_text"
        const val COL_TYPE = "type"
        const val COL_DISPLAY_NAME = "display_name"
        const val COL_PIC_URL = "pic_url"
        const val COL_DATE_ADDED = "date_added"
    }
}