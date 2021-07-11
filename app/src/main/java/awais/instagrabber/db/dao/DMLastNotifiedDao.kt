package awais.instagrabber.db.dao

import androidx.room.*
import awais.instagrabber.db.entities.DMLastNotified

@Dao
interface DMLastNotifiedDao {
    @Query("SELECT * FROM dm_last_notified")
    suspend fun getAllDMDmLastNotified(): List<DMLastNotified>

    @Query("SELECT * FROM dm_last_notified WHERE thread_id = :threadId")
    suspend fun findDMLastNotifiedByThreadId(threadId: String): DMLastNotified?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDMLastNotified(vararg dmLastNotified: DMLastNotified)

    @Update
    suspend fun updateDMLastNotified(vararg dmLastNotified: DMLastNotified)

    @Delete
    suspend fun deleteDMLastNotified(vararg dmLastNotified: DMLastNotified)

    @Query("DELETE from dm_last_notified")
    suspend fun deleteAllDMLastNotified()
}