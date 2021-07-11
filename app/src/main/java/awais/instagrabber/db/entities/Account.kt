package awais.instagrabber.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = Account.TABLE_NAME)
data class Account(
    @ColumnInfo(name = COL_ID) @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = COL_UID) val uid: String?,
    @ColumnInfo(name = COL_USERNAME) val username: String?,
    @ColumnInfo(name = COL_COOKIE) val cookie: String?,
    @ColumnInfo(name = COL_FULL_NAME) val fullName: String?,
    @ColumnInfo(name = COL_PROFILE_PIC) val profilePic: String?,
) {
    @Ignore
    var isSelected = false

    val isValid: Boolean
        get() = !uid.isNullOrBlank() && !username.isNullOrBlank() && !cookie.isNullOrBlank()

    companion object {
        const val TABLE_NAME = "accounts"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_COOKIE = "cookie"
        const val COL_UID = "uid"
        const val COL_FULL_NAME = "full_name"
        const val COL_PROFILE_PIC = "profile_pic"
    }
}