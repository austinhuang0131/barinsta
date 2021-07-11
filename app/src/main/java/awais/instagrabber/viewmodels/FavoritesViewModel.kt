package awais.instagrabber.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.utils.extensions.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _list = MutableLiveData<List<Favorite>>()
    val list: LiveData<List<Favorite>> = _list

    private val favoriteRepository: FavoriteRepository = FavoriteRepository.getInstance(application)

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _list.postValue(favoriteRepository.getAllFavorites())
            } catch (e: Exception) {
                Log.e(TAG, "fetch: ", e)
            }
        }
    }

    fun delete(favorite: Favorite, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                favoriteRepository.deleteFavorite(favorite.query, favorite.type)
                withContext(Dispatchers.Main) { onSuccess() }
                _list.postValue(favoriteRepository.getAllFavorites())
            } catch (e: Exception) {
                Log.e(TAG, "delete: ", e)
            }
        }
    }
}