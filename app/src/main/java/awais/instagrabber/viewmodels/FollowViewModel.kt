package awais.instagrabber.viewmodels

import androidx.lifecycle.*
import awais.instagrabber.models.Resource
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.webservices.FriendshipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FollowViewModel : ViewModel() {
    // data
    val userId = MutableLiveData<Long>()
    private val followers = MutableLiveData<List<User>>()
    private val followings = MutableLiveData<List<User>>()
    private val searchResults = MutableLiveData<List<User>>()

    // cursors
    private val followersMaxId = MutableLiveData<String?>("")
    private val followingMaxId = MutableLiveData<String?>("")
    private val searchingMaxId = MutableLiveData<String?>("")
    private val searchQuery = MutableLiveData<String?>()

    // comparison
    val status: LiveData<Pair<Boolean, Boolean>> = object : MediatorLiveData<Pair<Boolean, Boolean>>() {
            init {
                postValue(Pair(false, false))
                addSource(followersMaxId) {
                    if (it == null) {
                        postValue(Pair(true, value!!.second))
                    }
                    else fetch(true, it)
                }
                addSource(followingMaxId) {
                    if (it == null) {
                        postValue(Pair(value!!.first, true))
                    }
                    else fetch(false, it)
                }
            }
        }
    val comparison: LiveData<Triple<List<User>, List<User>, List<User>>> =
        object : MediatorLiveData<Triple<List<User>, List<User>, List<User>>>() {
            init {
                addSource(status) {
                    if (it.first && it.second) {
                        val followersList = followers.value!!
                        val followingList = followings.value!!
                        val allUsers: MutableList<User> = mutableListOf()
                        allUsers.addAll(followersList)
                        allUsers.addAll(followingList)
                        val followersMap = followersList.groupBy { it.pk }
                        val followingMap = followingList.groupBy { it.pk }
                        val mutual: MutableList<User> = mutableListOf()
                        val onlyFollowing: MutableList<User> = mutableListOf()
                        val onlyFollowers: MutableList<User> = mutableListOf()
                        allUsers.forEach {
                            val isFollowing = followingMap.get(it.pk) != null
                            val isFollower = followersMap.get(it.pk) != null
                            if (isFollowing && isFollower) mutual.add(it)
                            else if (isFollowing) onlyFollowing.add(it)
                            else if (isFollower) onlyFollowers.add(it)
                        }
                        postValue(Triple(mutual, onlyFollowing, onlyFollowers))
                    }
                }
            }
        }

    private val friendshipRepository: FriendshipRepository by lazy { FriendshipRepository.getInstance() }

    // fetch: supply max ID for continuous fetch
    fun fetch(follower: Boolean, nextMaxId: String?): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(Resource.loading(null))
        val maxId = if (follower) followersMaxId else followingMaxId
        if (maxId.value == null && nextMaxId == null) data.postValue(Resource.success(null))
        else if (userId.value == null) data.postValue(Resource.error("No user ID supplied!", null))
        else viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempList = friendshipRepository.getList(
                    follower,
                    userId.value!!,
                    nextMaxId ?: maxId.value,
                    null
                )
                if (!tempList.status.equals("ok")) {
                    data.postValue(Resource.error("Status not ok!", null))
                }
                else {
                    if (tempList.users != null) {
                        val liveData = if (follower) followers else followings
                        val currentList = if (liveData.value != null) liveData.value!!.toMutableList()
                                          else mutableListOf()
                        currentList.addAll(tempList.users!!)
                        liveData.postValue(currentList.toList())
                    }
                    maxId.postValue(tempList.nextMaxId)
                    data.postValue(Resource.success(null))
                }
            } catch (e: Exception) {
                data.postValue(Resource.error(e.message, null))
            }
        }
        return data
    }

    fun getList(follower: Boolean): LiveData<List<User>> {
        return if (follower) followers else followings
    }

    fun search(follower: Boolean): LiveData<Resource<Any?>> {
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(Resource.loading(null))
        val query = searchQuery.value
        if (searchingMaxId.value == null) data.postValue(Resource.success(null))
        else if (userId.value == null) data.postValue(Resource.error("No user ID supplied!", null))
        else if (query.isNullOrEmpty()) data.postValue(Resource.error("No query supplied!", null))
        else viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempList = friendshipRepository.getList(
                    follower,
                    userId.value!!,
                    searchingMaxId.value,
                    query
                )
                if (!tempList.status.equals("ok")) {
                    data.postValue(Resource.error("Status not ok!", null))
                }
                else {
                    if (tempList.users != null) {
                        val currentList = if (searchResults.value != null) searchResults.value!!.toMutableList()
                                          else mutableListOf()
                        currentList.addAll(tempList.users!!)
                        searchResults.postValue(currentList.toList())
                    }
                    searchingMaxId.postValue(tempList.nextMaxId)
                    data.postValue(Resource.success(null))
                }
            } catch (e: Exception) {
                data.postValue(Resource.error(e.message, null))
            }
        }
        return data
    }

    fun getSearch(): LiveData<List<User>> {
        return searchResults
    }

    fun setQuery(query: String?, follower: Boolean) {
        searchQuery.value = query
        if (!query.isNullOrEmpty()) search(follower)
    }

    fun clearProgress() {
        followersMaxId.value = ""
        followingMaxId.value = ""
        searchingMaxId.value = ""
        followings.value = listOf<User>()
        followers.value = listOf<User>()
        searchResults.value = listOf<User>()
    }
}