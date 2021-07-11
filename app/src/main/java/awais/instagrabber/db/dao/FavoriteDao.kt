package awais.instagrabber.db.dao

import androidx.room.*
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<Favorite>

    @Query("SELECT * FROM favorites WHERE query_text = :query and type = :type")
    suspend fun findFavoriteByQueryAndType(query: String, type: FavoriteType): Favorite?

    @Insert
    suspend fun insertFavorites(vararg favorites: Favorite)

    @Update
    suspend fun updateFavorites(vararg favorites: Favorite)

    @Delete
    suspend fun deleteFavorites(vararg favorites: Favorite)

    @Query("DELETE from favorites")
    suspend fun deleteAllFavorites()
}