package awais.instagrabber.db.repositories

import awais.instagrabber.db.datasources.DMLastNotifiedDataSource
import awais.instagrabber.db.entities.DMLastNotified
import java.time.LocalDateTime

class DMLastNotifiedRepository private constructor(private val dmLastNotifiedDataSource: DMLastNotifiedDataSource) {

    suspend fun getDMLastNotified(threadId: String): DMLastNotified? = dmLastNotifiedDataSource.getDMLastNotified(threadId)

    suspend fun getAllDMDmLastNotified(): List<DMLastNotified> = dmLastNotifiedDataSource.getAllDMDmLastNotified()

    suspend fun insertOrUpdateDMLastNotified(dmLastNotifiedList: List<DMLastNotified>) {
        for (dmLastNotified in dmLastNotifiedList) {
            dmLastNotifiedDataSource.insertOrUpdateDMLastNotified(
                dmLastNotified.threadId,
                dmLastNotified.lastNotifiedMsgTs,
                dmLastNotified.lastNotifiedAt
            )
        }
    }

    suspend fun insertOrUpdateDMLastNotified(
        threadId: String,
        lastNotifiedMsgTs: LocalDateTime,
        lastNotifiedAt: LocalDateTime,
    ): DMLastNotified? {
        dmLastNotifiedDataSource.insertOrUpdateDMLastNotified(threadId, lastNotifiedMsgTs, lastNotifiedAt)
        return dmLastNotifiedDataSource.getDMLastNotified(threadId)
    }

    suspend fun deleteDMLastNotified(dmLastNotified: DMLastNotified) = dmLastNotifiedDataSource.deleteDMLastNotified(dmLastNotified)

    suspend fun deleteAllDMLastNotified() = dmLastNotifiedDataSource.deleteAllDMLastNotified()

    companion object {
        private lateinit var instance: DMLastNotifiedRepository

        @JvmStatic
        fun getInstance(dmLastNotifiedDataSource: DMLastNotifiedDataSource): DMLastNotifiedRepository {
            if (!this::instance.isInitialized) {
                instance = DMLastNotifiedRepository(dmLastNotifiedDataSource)
            }
            return instance
        }
    }
}