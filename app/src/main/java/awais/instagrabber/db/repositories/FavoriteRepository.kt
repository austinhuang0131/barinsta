package awais.instagrabber.db.repositories

import android.content.Context
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.models.enums.FavoriteType

class FavoriteRepository(private val favoriteDataSource: FavoriteDataSource) {

    suspend fun getFavorite(query: String, type: FavoriteType): Favorite? = favoriteDataSource.getFavorite(query, type)

    suspend fun getAllFavorites(): List<Favorite> = favoriteDataSource.getAllFavorites()

    suspend fun insertOrUpdateFavorite(favorite: Favorite) = favoriteDataSource.insertOrUpdateFavorite(favorite)

    suspend fun deleteFavorite(query: String?, type: FavoriteType?) = favoriteDataSource.deleteFavorite(query, type)

    companion object {
        @Volatile
        private var INSTANCE: FavoriteRepository? = null

        fun getInstance(context: Context): FavoriteRepository {
            return INSTANCE ?: synchronized(this) {
                val dataSource: FavoriteDataSource = FavoriteDataSource.getInstance(context)
                FavoriteRepository(dataSource).also { INSTANCE = it }
            }
        }
    }
}