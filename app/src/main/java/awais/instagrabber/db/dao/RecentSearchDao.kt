package awais.instagrabber.db.dao

import androidx.room.*
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.models.enums.FavoriteType

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY last_searched_on DESC")
    suspend fun getAllRecentSearches(): List<RecentSearch>

    @Query("SELECT * FROM recent_searches WHERE `ig_id` = :igId AND `type` = :type")
    suspend fun getRecentSearchByIgIdAndType(igId: String, type: FavoriteType): RecentSearch?

    @Query("SELECT * FROM recent_searches WHERE instr(`name`, :query) > 0")
    suspend fun findRecentSearchesWithNameContaining(query: String): List<RecentSearch>

    @Insert
    suspend fun insertRecentSearch(recentSearch: RecentSearch)

    @Update
    suspend fun updateRecentSearch(recentSearch: RecentSearch)

    @Delete
    suspend fun deleteRecentSearch(recentSearch: RecentSearch)

    // @Query("DELETE from recent_searches")
    // void deleteAllRecentSearches();
}