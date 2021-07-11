package awais.instagrabber.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = DMLastNotified.TABLE_NAME, indices = [Index(value = [DMLastNotified.COL_THREAD_ID], unique = true)])
data class DMLastNotified(
    @ColumnInfo(name = COL_ID) @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = COL_THREAD_ID) val threadId: String?,
    @ColumnInfo(name = COL_LAST_NOTIFIED_MSG_TS) val lastNotifiedMsgTs: LocalDateTime?,
    @ColumnInfo(name = COL_LAST_NOTIFIED_AT) val lastNotifiedAt: LocalDateTime?,
) {
    companion object {
        const val TABLE_NAME = "dm_last_notified"
        const val COL_ID = "id"
        const val COL_THREAD_ID = "thread_id"
        const val COL_LAST_NOTIFIED_MSG_TS = "last_notified_msg_ts"
        const val COL_LAST_NOTIFIED_AT = "last_notified_at"
    }
}