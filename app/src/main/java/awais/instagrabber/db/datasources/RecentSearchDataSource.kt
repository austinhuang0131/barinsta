package awais.instagrabber.db.datasources

import android.content.Context
import awais.instagrabber.db.AppDatabase
import awais.instagrabber.db.dao.RecentSearchDao
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.models.enums.FavoriteType

class RecentSearchDataSource private constructor(private val recentSearchDao: RecentSearchDao) {

    suspend fun getRecentSearchByIgIdAndType(igId: String, type: FavoriteType): RecentSearch? =
        recentSearchDao.getRecentSearchByIgIdAndType(igId, type)

    suspend fun getAllRecentSearches(): List<RecentSearch> = recentSearchDao.getAllRecentSearches()

    suspend fun insertOrUpdateRecentSearch(recentSearch: RecentSearch) {
        if (recentSearch.id != 0) {
            recentSearchDao.updateRecentSearch(recentSearch)
            return
        }
        recentSearchDao.insertRecentSearch(recentSearch)
    }

    suspend fun deleteRecentSearch(recentSearch: RecentSearch) = recentSearchDao.deleteRecentSearch(recentSearch)

    companion object {
        private lateinit var INSTANCE: RecentSearchDataSource

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): RecentSearchDataSource {
            if (!this::INSTANCE.isInitialized) {
                synchronized(RecentSearchDataSource::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        val database = AppDatabase.getDatabase(context)
                        INSTANCE = RecentSearchDataSource(database.recentSearchDao())
                    }
                }
            }
            return INSTANCE
        }
    }
}