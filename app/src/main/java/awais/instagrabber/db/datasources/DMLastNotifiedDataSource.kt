package awais.instagrabber.db.datasources

import android.content.Context
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.dao.DMLastNotifiedDao
import awais.instagrabber.db.entities.DMLastNotified
import java.time.LocalDateTime

class DMLastNotifiedDataSource private constructor(private val dmLastNotifiedDao: DMLastNotifiedDao) {
    suspend fun getDMLastNotified(threadId: String): DMLastNotified? = dmLastNotifiedDao.findDMLastNotifiedByThreadId(threadId)

    suspend fun getAllDMDmLastNotified(): List<DMLastNotified> = dmLastNotifiedDao.getAllDMDmLastNotified()

    suspend fun insertOrUpdateDMLastNotified(
        threadId: String?,
        lastNotifiedMsgTs: LocalDateTime?,
        lastNotifiedAt: LocalDateTime?,
    ) {
        if (threadId == null) return
        val dmLastNotified = getDMLastNotified(threadId)
        val toUpdate = DMLastNotified(
            dmLastNotified?.id ?: 0,
            threadId,
            lastNotifiedMsgTs,
            lastNotifiedAt
        )
        if (dmLastNotified != null) {
            dmLastNotifiedDao.updateDMLastNotified(toUpdate)
            return
        }
        dmLastNotifiedDao.insertDMLastNotified(toUpdate)
    }

    suspend fun deleteDMLastNotified(dmLastNotified: DMLastNotified) = dmLastNotifiedDao.deleteDMLastNotified(dmLastNotified)

    suspend fun deleteAllDMLastNotified() = dmLastNotifiedDao.deleteAllDMLastNotified()

    companion object {
        private lateinit var INSTANCE: DMLastNotifiedDataSource

        @JvmStatic
        fun getInstance(context: Context): DMLastNotifiedDataSource {
            if (!this::INSTANCE.isInitialized) {
                synchronized(DMLastNotifiedDataSource::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        val database = AppDatabase.getDatabase(context)
                        INSTANCE = DMLastNotifiedDataSource(database.dmLastNotifiedDao())
                    }
                }
            }
            return INSTANCE
        }
    }
}