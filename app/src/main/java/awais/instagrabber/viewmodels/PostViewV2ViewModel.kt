package awais.instagrabber.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import awais.instagrabber.R
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.models.enums.MediaItemType
import awais.instagrabber.repositories.responses.Caption
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.utils.getCsrfTokenFromCookie
import awais.instagrabber.utils.getUserIdFromCookie
import awais.instagrabber.webservices.MediaRepository
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class PostViewV2ViewModel : ViewModel() {
    private val user = MutableLiveData<User?>()
    private val caption = MutableLiveData<Caption?>()
    private val location = MutableLiveData<Location?>()
    private val date = MutableLiveData<String>()
    private val likeCount = MutableLiveData(0L)
    private val commentCount = MutableLiveData(0L)
    private val viewCount = MutableLiveData(0L)
    private val type = MutableLiveData<MediaItemType?>()
    private val liked = MutableLiveData(false)
    private val saved = MutableLiveData(false)
    private val options = MutableLiveData<List<Int>>(ArrayList())
    private var messageManager: DirectMessagesManager? = null
    private val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
    private val deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID)
    private val csrfToken = getCsrfTokenFromCookie(cookie)
    private val viewerId = getUserIdFromCookie(cookie)
    private val mediaRepository: MediaRepository by lazy { MediaRepository.getInstance() }

    lateinit var media: Media
        private set
    val isLoggedIn = cookie.isNotBlank() && !csrfToken.isNullOrBlank() && viewerId != 0L

    fun setMedia(media: Media) {
        this.media = media
        user.postValue(media.user)
        caption.postValue(media.caption)
        location.postValue(media.location)
        date.postValue(media.date)
        likeCount.postValue(media.likeCount)
        commentCount.postValue(media.commentCount)
        viewCount.postValue(if (media.type == MediaItemType.MEDIA_TYPE_VIDEO) media.viewCount else null)
        type.postValue(media.type)
        liked.postValue(media.hasLiked)
        saved.postValue(media.hasViewerSaved)
        initOptions()
    }

    private fun initOptions() {
        val builder = ImmutableList.builder<Int>()
        val user1 = media.user
        if (isLoggedIn && user1 != null && user1.pk == viewerId) {
            builder.add(R.id.edit_caption)
            builder.add(R.id.delete)
        }
        options.postValue(builder.build())
    }

    fun getUser(): LiveData<User?> {
        return user
    }

    fun getCaption(): LiveData<Caption?> {
        return caption
    }

    fun getLocation(): LiveData<Location?> {
        return location
    }

    fun getDate(): LiveData<String> {
        return date
    }

    fun getLikeCount(): LiveData<Long> {
        return likeCount
    }

    fun getCommentCount(): LiveData<Long> {
        return commentCount
    }

    fun getViewCount(): LiveData<Long?> {
        return viewCount
    }

    fun getType(): LiveData<MediaItemType?> {
        return type
    }

    fun getLiked(): LiveData<Boolean> {
        return liked
    }

    fun getSaved(): LiveData<Boolean> {
        return saved
    }

    fun getOptions(): LiveData<List<Int>> {
        return options
    }

    fun toggleLike(): LiveData<Resource<Any?>> {
        return if (media.hasLiked) {
            unlike()
        } else like()
    }

    fun like(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaId = media.pk ?: return@launch
                val liked = mediaRepository.like(csrfToken!!, viewerId, deviceUuid, mediaId)
                updateMediaLikeUnlike(data, liked)
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unlike(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaId = media.pk ?: return@launch
                val unliked = mediaRepository.unlike(csrfToken!!, viewerId, deviceUuid, mediaId)
                updateMediaLikeUnlike(data, unliked)
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    private fun updateMediaLikeUnlike(data: MutableLiveData<Resource<Any?>>, result: Boolean) {
        if (!result) {
            data.postValue(error("", null))
            return
        }
        data.postValue(success(true))
        val currentLikesCount = media.likeCount
        val updatedCount: Long
        if (!media.hasLiked) {
            updatedCount = currentLikesCount + 1
            media.hasLiked = true
        } else {
            updatedCount = currentLikesCount - 1
            media.hasLiked = false
        }
        media.likeCount = updatedCount
        likeCount.postValue(updatedCount)
        liked.postValue(media.hasLiked)
    }

    fun toggleSave(): LiveData<Resource<Any?>> {
        return if (!media.hasViewerSaved) {
            save(null, false)
        } else unsave()
    }

    fun toggleSave(collection: String?, ignoreSaveState: Boolean): LiveData<Resource<Any?>> {
        return save(collection, ignoreSaveState)
    }

    fun save(collection: String?, ignoreSaveState: Boolean): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaId = media.pk ?: return@launch
                val saved = mediaRepository.save(csrfToken!!, viewerId, deviceUuid, mediaId, collection)
                getSaveUnsaveCallback(data, saved, ignoreSaveState)
            } catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun unsave(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            val mediaId = media.pk ?: return@launch
            val unsaved = mediaRepository.unsave(csrfToken!!, viewerId, deviceUuid, mediaId)
            getSaveUnsaveCallback(data, unsaved, false)
        }
        return data
    }

    private fun getSaveUnsaveCallback(
        data: MutableLiveData<Resource<Any?>>,
        result: Boolean,
        ignoreSaveState: Boolean,
    ) {
        if (!result) {
            data.postValue(error("", null))
            return
        }
        data.postValue(success(true))
        if (!ignoreSaveState) media.hasViewerSaved = !media.hasViewerSaved
        saved.postValue(media.hasViewerSaved)
    }

    fun updateCaption(caption: String): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val postId = media.pk ?: return@launch
                val result = mediaRepository.editCaption(csrfToken!!, viewerId, deviceUuid, postId, caption)
                if (result) {
                    data.postValue(success(""))
                    media.setPostCaption(caption)
                    this@PostViewV2ViewModel.caption.postValue(media.caption)
                    return@launch
                }
                data.postValue(error("", null))
            } catch (e: Exception) {
                Log.e(TAG, "Error editing caption", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun translateCaption(): LiveData<Resource<String?>> {
        val data = MutableLiveData<Resource<String?>>()
        data.postValue(loading(null))
        val value = caption.value
        val pk = value?.pk
        if (pk == null) {
            data.postValue(error("caption is null", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = mediaRepository.translate(pk, "1") ?: return@launch
                if (result.isBlank()) {
                    // data.postValue(error("", null))
                    return@launch
                }
                data.postValue(success(result))
            } catch (e: Exception) {
                Log.e(TAG, "Error translating comment", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun hasPk(): Boolean {
        return media.pk != null
    }

    fun setViewCount(viewCount: Long?) {
        this.viewCount.postValue(viewCount)
    }

    fun delete(): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        if (!isLoggedIn) {
            data.postValue(error("Not logged in!", null))
            return data
        }
        val mediaId = media.id
        val mediaType = media.type
        if (mediaId == null || mediaType == null) {
            data.postValue(error("media id or type is null", null))
            return data
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = mediaRepository.delete(csrfToken!!, viewerId, deviceUuid, mediaId, mediaType)
                if (response == null) {
                    data.postValue(success(Any()))
                    return@launch
                }
                data.postValue(success(Any()))
            } catch (e: Exception) {
                Log.e(TAG, "delete: ", e)
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    fun shareDm(result: RankedRecipient, child: Int) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = media.id ?: return
        val childId = if (child == -1) null else media.carouselMedia?.get(child)?.id
        messageManager?.sendMedia(result, mediaId, childId, BroadcastItemType.MEDIA_SHARE, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>, child: Int) {
        if (messageManager == null) {
            messageManager = DirectMessagesManager
        }
        val mediaId = media.id ?: return
        val childId = if (child == -1) null else media.carouselMedia?.get(child)?.id
        messageManager?.sendMedia(recipients, mediaId, childId, BroadcastItemType.MEDIA_SHARE, viewModelScope)
    }
}