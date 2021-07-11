package awais.instagrabber.db.repositories

import awais.instagrabber.db.datasources.RecentSearchDataSource
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.models.enums.FavoriteType
import java.time.LocalDateTime

class RecentSearchRepository private constructor(private val recentSearchDataSource: RecentSearchDataSource) {
    suspend fun getRecentSearch(igId: String, type: FavoriteType): RecentSearch? = recentSearchDataSource.getRecentSearchByIgIdAndType(igId, type)

    suspend fun getAllRecentSearches(): List<RecentSearch> = recentSearchDataSource.getAllRecentSearches()

    suspend fun insertOrUpdateRecentSearch(recentSearch: RecentSearch) =
        insertOrUpdateRecentSearch(recentSearch.igId, recentSearch.name, recentSearch.username, recentSearch.picUrl, recentSearch.type)

    private suspend fun insertOrUpdateRecentSearch(
        igId: String,
        name: String,
        username: String?,
        picUrl: String?,
        type: FavoriteType,
    ) {
        var recentSearch = recentSearchDataSource.getRecentSearchByIgIdAndType(igId, type)
        recentSearch = RecentSearch(recentSearch?.id ?: 0, igId, name, username, picUrl, type, LocalDateTime.now())
        recentSearchDataSource.insertOrUpdateRecentSearch(recentSearch)
    }

    suspend fun deleteRecentSearchByIgIdAndType(igId: String, type: FavoriteType) {
        val recentSearch = recentSearchDataSource.getRecentSearchByIgIdAndType(igId, type)
        if (recentSearch != null) {
            recentSearchDataSource.deleteRecentSearch(recentSearch)
        }
    }

    suspend fun deleteRecentSearch(recentSearch: RecentSearch) = recentSearchDataSource.deleteRecentSearch(recentSearch)

    companion object {
        private lateinit var instance: RecentSearchRepository

        @JvmStatic
        fun getInstance(recentSearchDataSource: RecentSearchDataSource): RecentSearchRepository {
            if (!this::instance.isInitialized) {
                instance = RecentSearchRepository(recentSearchDataSource)
            }
            return instance
        }
    }
}