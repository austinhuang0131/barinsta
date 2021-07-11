package awais.instagrabber.db.entities

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.responses.search.SearchItem
import awais.instagrabber.utils.extensions.TAG
import java.time.LocalDateTime

@Entity(tableName = RecentSearch.TABLE_NAME, indices = [Index(value = [RecentSearch.COL_IG_ID, RecentSearch.COL_TYPE], unique = true)])
data class RecentSearch(
    @ColumnInfo(name = COL_ID) @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = COL_IG_ID) val igId: String,
    @ColumnInfo(name = COL_NAME) val name: String,
    @ColumnInfo(name = COL_USERNAME) val username: String?,
    @ColumnInfo(name = COL_PIC_URL) val picUrl: String?,
    @ColumnInfo(name = COL_TYPE) val type: FavoriteType,
    @ColumnInfo(name = COL_LAST_SEARCHED_ON) val lastSearchedOn: LocalDateTime,
) {

    companion object {
        const val TABLE_NAME = "recent_searches"
        private const val COL_ID = "id"
        const val COL_IG_ID = "ig_id"
        private const val COL_NAME = "name"
        private const val COL_USERNAME = "username"
        private const val COL_PIC_URL = "pic_url"
        const val COL_TYPE = "type"
        private const val COL_LAST_SEARCHED_ON = "last_searched_on"

        @JvmStatic
        fun fromSearchItem(searchItem: SearchItem): RecentSearch? {
            val type = searchItem.type ?: return null
            try {
                val igId: String
                val name: String
                val username: String?
                val picUrl: String?
                when (type) {
                    FavoriteType.USER -> {
                        igId = searchItem.user.pk.toString()
                        name = searchItem.user.fullName ?: ""
                        username = searchItem.user.username
                        picUrl = searchItem.user.profilePicUrl
                    }
                    FavoriteType.HASHTAG -> {
                        igId = searchItem.hashtag.id
                        name = searchItem.hashtag.name
                        username = null
                        picUrl = null
                    }
                    FavoriteType.LOCATION -> {
                        igId = searchItem.place.location.pk.toString()
                        name = searchItem.place.title
                        username = null
                        picUrl = null
                    }
                    else -> return null
                }
                return RecentSearch(id = 0, igId, name, username, picUrl, type, LocalDateTime.now())
            } catch (e: Exception) {
                Log.e(TAG, "fromSearchItem: ", e)
            }
            return null
        }
    }
}