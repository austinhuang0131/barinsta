package awais.instagrabber.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.managers.DirectMessagesManager
import awais.instagrabber.models.Resource
import awais.instagrabber.models.enums.BroadcastItemType
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.UserProfileContextLink
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient
import awais.instagrabber.repositories.responses.stories.Story
import awais.instagrabber.utils.ControlledRunner
import awais.instagrabber.utils.Event
import awais.instagrabber.utils.SingleRunner
import awais.instagrabber.utils.extensions.TAG
import awais.instagrabber.utils.extensions.isReallyPrivate
import awais.instagrabber.viewmodels.ProfileFragmentViewModel.ProfileAction.*
import awais.instagrabber.viewmodels.ProfileFragmentViewModel.ProfileEvent.*
import awais.instagrabber.webservices.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ProfileFragmentViewModel(
    private val state: SavedStateHandle,
    private val csrfToken: String?,
    private val deviceUuid: String?,
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val storiesRepository: StoriesRepository,
    private val mediaRepository: MediaRepository,
    private val graphQLRepository: GraphQLRepository,
    private val favoriteRepository: FavoriteRepository,
    private val directMessagesRepository: DirectMessagesRepository,
    private val messageManager: DirectMessagesManager?,
    ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _currentUser = MutableLiveData<Resource<User?>>(Resource.loading(null))
    private val _isFavorite = MutableLiveData(false)
    private val profileAction = MutableLiveData(INIT)
    private val _eventLiveData = MutableLiveData<Event<ProfileEvent>?>()

    private var previousUsername: String? = null

    enum class ProfileAction {
        INIT,
        REFRESH,
        REFRESH_FRIENDSHIP,
    }

    sealed class ProfileEvent {
        object ShowConfirmUnfollowDialog : ProfileEvent()
        class DMButtonState(val disabled: Boolean) : ProfileEvent()
        class NavigateToThread(val threadId: String, val username: String) : ProfileEvent()
        class ShowTranslation(val result: String) : ProfileEvent()
    }

    val currentUser: LiveData<Resource<User?>> = _currentUser
    val isLoggedIn: LiveData<Boolean> = currentUser.map { it.data != null }
    val isFavorite: LiveData<Boolean> = _isFavorite
    val eventLiveData: LiveData<Event<ProfileEvent>?> = _eventLiveData

    private val currentUserStateUsernameActionLiveData: LiveData<Triple<Resource<User?>, Resource<String?>, ProfileAction>> =
        object : MediatorLiveData<Triple<Resource<User?>, Resource<String?>, ProfileAction>>() {
            var user: Resource<User?> = Resource.loading(null)
            var stateUsername: Resource<String?> = Resource.loading(null)
            var action: ProfileAction = INIT

            init {
                addSource(currentUser) { currentUser ->
                    this.user = currentUser
                    value = Triple(currentUser, stateUsername, action)
                }
                addSource(state.getLiveData<String?>("username")) { username ->
                    this.stateUsername = Resource.success(username.substringAfter('@'))
                    value = Triple(user, this.stateUsername, action)
                }
                addSource(profileAction) { action ->
                    this.action = action
                    value = Triple(user, stateUsername, action)
                }
                // trigger currentUserStateUsernameActionLiveData switch map with a state username success resource
                if (!state.contains("username")) {
                    this.stateUsername = Resource.success(null)
                    value = Triple(user, this.stateUsername, action)
                }
            }
        }

    private val profileFetchControlledRunner = ControlledRunner<User?>()
    val profile: LiveData<Resource<User?>> = currentUserStateUsernameActionLiveData.switchMap {
        val (currentUserResource, stateUsernameResource, action) = it
        liveData<Resource<User?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            if (action == INIT && previousUsername != null && stateUsernameResource.data == previousUsername) return@liveData
            if (currentUserResource.status == Resource.Status.LOADING || stateUsernameResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(profileCopy.value?.data))
                return@liveData
            }
            val currentUser = currentUserResource.data
            val stateUsername = stateUsernameResource.data
            if (stateUsername.isNullOrBlank()) {
                emit(Resource.success(currentUser))
                return@liveData
            }
            try {
                when (action) {
                    INIT, REFRESH -> {
                        previousUsername = stateUsername
                        val fetchedUser = profileFetchControlledRunner.cancelPreviousThenRun { fetchUser(currentUser, stateUsername) }
                        emit(Resource.success(fetchedUser))
                        if (fetchedUser != null) {
                            checkAndUpdateFavorite(fetchedUser)
                        }
                    }
                    REFRESH_FRIENDSHIP -> {
                        var profile = profileCopy.value?.data ?: return@liveData
                        profile = profile.copy(friendshipStatus = userRepository.getUserFriendship(profile.pk))
                        emit(Resource.success(profile))
                    }
                }
            } catch (e: Exception) {
                emit(Resource.error(e.message, profileCopy.value?.data))
                Log.e(TAG, "fetching user: ", e)
            }
        }
    }
    val profileCopy = profile

    val currentUserProfileActionLiveData: LiveData<Triple<Resource<User?>, Resource<User?>, ProfileAction>> =
        object : MediatorLiveData<Triple<Resource<User?>, Resource<User?>, ProfileAction>>() {
            var currentUser: Resource<User?> = Resource.loading(null)
            var profile: Resource<User?> = Resource.loading(null)
            var action: ProfileAction = INIT

            init {
                addSource(this@ProfileFragmentViewModel.currentUser) { currentUser ->
                    this.currentUser = currentUser
                    value = Triple(currentUser, profile, action)
                }
                addSource(this@ProfileFragmentViewModel.profile) { profile ->
                    this.profile = profile
                    value = Triple(currentUser, this.profile, action)
                }
                addSource(profileAction) { action ->
                    this.action = action
                    value = Triple(currentUser, this.profile, action)
                }
            }
        }

    private val storyFetchControlledRunner = ControlledRunner<Story?>()
    val userStories: LiveData<Resource<Story?>> = currentUserProfileActionLiveData.switchMap { currentUserAndProfilePair ->
        liveData<Resource<Story?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            val (currentUserResource, profileResource, action) = currentUserAndProfilePair
            if (action != INIT && action != REFRESH) {
                return@liveData
            }
            // don't fetch if not logged in
            if (currentUserResource.data == null) {
                emit(Resource.success(null))
                return@liveData
            }
            if (currentUserResource.status == Resource.Status.LOADING || profileResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val user = profileResource.data
            if (user == null) {
                emit(Resource.success(null))
                return@liveData
            }
            try {
                val fetchedStories = storyFetchControlledRunner.cancelPreviousThenRun { fetchUserStory(user) }
                emit(Resource.success(fetchedStories))
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
                Log.e(TAG, "fetching story: ", e)
            }
        }
    }

    private val highlightsFetchControlledRunner = ControlledRunner<List<Story>?>()
    val userHighlights: LiveData<Resource<List<Story>?>> = currentUserProfileActionLiveData.switchMap { currentUserAndProfilePair ->
        liveData<Resource<List<Story>?>>(context = viewModelScope.coroutineContext + ioDispatcher) {
            val (currentUserResource, profileResource, action) = currentUserAndProfilePair
            if (action != INIT && action != REFRESH) {
                return@liveData
            }
            // don't fetch if not logged in
            if (currentUserResource.data == null) {
                emit(Resource.success(null))
                return@liveData
            }
            if (currentUserResource.status == Resource.Status.LOADING || profileResource.status == Resource.Status.LOADING) {
                emit(Resource.loading(null))
                return@liveData
            }
            val user = profileResource.data
            if (user == null) {
                emit(Resource.success(null))
                return@liveData
            }
            try {
                val fetchedHighlights = highlightsFetchControlledRunner.cancelPreviousThenRun { fetchUserHighlights(user) }
                emit(Resource.success(fetchedHighlights))
            } catch (e: Exception) {
                emit(Resource.error(e.message, null))
                Log.e(TAG, "fetching highlights: ", e)
            }
        }
    }

    private suspend fun fetchUser(
        currentUser: User?,
        stateUsername: String,
    ): User? {
        if (currentUser != null) {
            // logged in
            val tempUser = userRepository.getUsernameInfo(stateUsername)
            if (!tempUser.isReallyPrivate(currentUser)) {
                tempUser.friendshipStatus = userRepository.getUserFriendship(tempUser.pk)
            }
            return tempUser
        }
        // anonymous
        return graphQLRepository.fetchUser(stateUsername)
    }

    private suspend fun fetchUserStory(fetchedUser: User): Story? = storiesRepository.getStories(
        StoryViewerOptions.forUser(fetchedUser.pk, fetchedUser.fullName)
    )

    private suspend fun fetchUserHighlights(fetchedUser: User): List<Story> = storiesRepository.fetchHighlights(fetchedUser.pk)

    private suspend fun checkAndUpdateFavorite(fetchedUser: User) {
        try {
            val favorite = favoriteRepository.getFavorite(fetchedUser.username, FavoriteType.USER)
            if (favorite == null) {
                _isFavorite.postValue(false)
                return
            }
            _isFavorite.postValue(true)
            favoriteRepository.insertOrUpdateFavorite(
                Favorite(
                    favorite.id,
                    fetchedUser.username,
                    FavoriteType.USER,
                    fetchedUser.fullName,
                    fetchedUser.profilePicUrl,
                    favorite.dateAdded
                )
            )
        } catch (e: Exception) {
            _isFavorite.postValue(false)
            Log.e(TAG, "checkAndUpdateFavorite: ", e)
        }
    }

    fun setCurrentUser(currentUser: Resource<User?>) {
        _currentUser.postValue(currentUser)
    }

    fun shareDm(result: RankedRecipient) {
        val mediaId = profile.value?.data?.pk ?: return
        messageManager?.sendMedia(result, mediaId.toString(10), null, BroadcastItemType.PROFILE, viewModelScope)
    }

    fun shareDm(recipients: Set<RankedRecipient>) {
        val mediaId = profile.value?.data?.pk ?: return
        messageManager?.sendMedia(recipients, mediaId.toString(10), null, BroadcastItemType.PROFILE, viewModelScope)
    }

    fun refresh() {
        profileAction.postValue(REFRESH)
    }

    private val toggleFavoriteControlledRunner = SingleRunner()
    fun toggleFavorite() {
        val username = profile.value?.data?.username ?: return
        val fullName = profile.value?.data?.fullName ?: return
        val profilePicUrl = profile.value?.data?.profilePicUrl ?: return
        viewModelScope.launch(Dispatchers.IO) {
            toggleFavoriteControlledRunner.afterPrevious {
                try {
                    val favorite = favoriteRepository.getFavorite(username, FavoriteType.USER)
                    if (favorite == null) {
                        // insert
                        favoriteRepository.insertOrUpdateFavorite(
                            Favorite(
                                0,
                                username,
                                FavoriteType.USER,
                                fullName,
                                profilePicUrl,
                                LocalDateTime.now()
                            )
                        )
                        _isFavorite.postValue(true)
                        return@afterPrevious
                    }
                    // delete
                    favoriteRepository.deleteFavorite(username, FavoriteType.USER)
                    _isFavorite.postValue(false)
                } catch (e: Exception) {
                    Log.e(TAG, "checkAndUpdateFavorite: ", e)
                }
            }
        }
    }

    private val toggleFollowSingleRunner = SingleRunner()
    fun toggleFollow(confirmed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleFollowSingleRunner.afterPrevious {
                try {
                    val following = profile.value?.data?.friendshipStatus?.following ?: false
                    val currentUserId = currentUser.value?.data?.pk ?: return@afterPrevious
                    val targetUserId = profile.value?.data?.pk ?: return@afterPrevious
                    val csrfToken = csrfToken ?: return@afterPrevious
                    val deviceUuid = deviceUuid ?: return@afterPrevious
                    if (following) {
                        if (!confirmed) {
                            _eventLiveData.postValue(Event(ShowConfirmUnfollowDialog))
                            return@afterPrevious
                        }
                        // unfollow
                        friendshipRepository.unfollow(
                            csrfToken,
                            currentUserId,
                            deviceUuid,
                            targetUserId
                        )
                        profileAction.postValue(REFRESH_FRIENDSHIP)
                        return@afterPrevious
                    }
                    friendshipRepository.follow(
                        csrfToken,
                        currentUserId,
                        deviceUuid,
                        targetUserId
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "toggleFollow: ", e)
                }
            }
        }
    }

    private val sendDmSingleRunner = SingleRunner()
    fun sendDm() {
        viewModelScope.launch(Dispatchers.IO) {
            sendDmSingleRunner.afterPrevious {
                _eventLiveData.postValue(Event(DMButtonState(true)))
                try {
                    val currentUserId = currentUser.value?.data?.pk ?: return@afterPrevious
                    val targetUserId = profile.value?.data?.pk ?: return@afterPrevious
                    val csrfToken = csrfToken ?: return@afterPrevious
                    val deviceUuid = deviceUuid ?: return@afterPrevious
                    val username = profile.value?.data?.username ?: return@afterPrevious
                    val thread = directMessagesRepository.createThread(
                        csrfToken,
                        currentUserId,
                        deviceUuid,
                        listOf(targetUserId),
                        null,
                    )
                    val inboxManager = DirectMessagesManager.inboxManager
                    if (!inboxManager.containsThread(thread.threadId)) {
                        thread.isTemp = true
                        inboxManager.addThread(thread, 0)
                    }
                    val threadId = thread.threadId ?: return@afterPrevious
                    _eventLiveData.postValue(Event(NavigateToThread(threadId, username)))
                    delay(200) // Add delay so that the postValue in finally does not overwrite the NavigateToThread event
                } catch (e: Exception) {
                    Log.e(TAG, "sendDm: ", e)
                } finally {
                    _eventLiveData.postValue(Event(DMButtonState(false)))
                }
            }
        }
    }

    private val restrictUserSingleRunner = SingleRunner()
    fun restrictUser() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            restrictUserSingleRunner.afterPrevious {
                try {
                    val profile = profile.value?.data ?: return@afterPrevious
                    friendshipRepository.toggleRestrict(
                        csrfToken ?: return@afterPrevious,
                        deviceUuid ?: return@afterPrevious,
                        profile.pk,
                        !(profile.friendshipStatus?.isRestricted ?: false),
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "restrictUser: ", e)
                }
            }
        }
    }

    private val blockUserSingleRunner = SingleRunner()
    fun blockUser() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            blockUserSingleRunner.afterPrevious {
                try {
                    val profile = profile.value?.data ?: return@afterPrevious
                    friendshipRepository.changeBlock(
                        csrfToken ?: return@afterPrevious,
                        currentUser.value?.data?.pk ?: return@afterPrevious,
                        deviceUuid ?: return@afterPrevious,
                        profile.friendshipStatus?.blocking ?: return@afterPrevious,
                        profile.pk
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "blockUser: ", e)
                }
            }
        }
    }

    private val muteStoriesSingleRunner = SingleRunner()
    fun muteStories() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            muteStoriesSingleRunner.afterPrevious {
                try {
                    val profile = profile.value?.data ?: return@afterPrevious
                    friendshipRepository.changeMute(
                        csrfToken ?: return@afterPrevious,
                        currentUser.value?.data?.pk ?: return@afterPrevious,
                        deviceUuid ?: return@afterPrevious,
                        profile.friendshipStatus?.isMutingReel ?: return@afterPrevious,
                        profile.pk,
                        true
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "muteStories: ", e)
                }
            }
        }
    }

    private val mutePostsSingleRunner = SingleRunner()
    fun mutePosts() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            mutePostsSingleRunner.afterPrevious {
                try {
                    val profile = profile.value?.data ?: return@afterPrevious
                    friendshipRepository.changeMute(
                        csrfToken ?: return@afterPrevious,
                        currentUser.value?.data?.pk ?: return@afterPrevious,
                        deviceUuid ?: return@afterPrevious,
                        profile.friendshipStatus?.muting ?: return@afterPrevious,
                        profile.pk,
                        false
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "mutePosts: ", e)
                }
            }
        }
    }

    private val removeFollowerSingleRunner = SingleRunner()
    fun removeFollower() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            removeFollowerSingleRunner.afterPrevious {
                try {
                    friendshipRepository.removeFollower(
                        csrfToken ?: return@afterPrevious,
                        currentUser.value?.data?.pk ?: return@afterPrevious,
                        deviceUuid ?: return@afterPrevious,
                        profile.value?.data?.pk ?: return@afterPrevious
                    )
                    profileAction.postValue(REFRESH_FRIENDSHIP)
                } catch (e: Exception) {
                    Log.e(TAG, "removeFollower: ", e)
                }
            }
        }
    }

    private val translateBioSingleRunner = SingleRunner()
    fun translateBio() {
        if (isLoggedIn.value == false) return
        viewModelScope.launch(Dispatchers.IO) {
            translateBioSingleRunner.afterPrevious {
                try {
                    val result = mediaRepository.translate(
                        profile.value?.data?.pk?.toString() ?: return@afterPrevious,
                        "3"
                    )
                    if (result.isNullOrBlank()) return@afterPrevious
                    _eventLiveData.postValue(Event(ShowTranslation(result)))
                } catch (e: Exception) {
                    Log.e(TAG, "translateBio: ", e)
                }
            }
        }
    }

    /**
     * Username of profile without '`@`'
     */
    val username: LiveData<String> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> ""
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.username ?: ""
        }
    }
    val profilePicUrl: LiveData<String?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.profilePicUrl
        }
    }
    val fullName: LiveData<String?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> ""
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.fullName
        }
    }
    val biography: LiveData<String?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> ""
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.biography
        }
    }
    val url: LiveData<String?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> ""
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.externalUrl
        }
    }
    val followersCount: LiveData<Long?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.followerCount
        }
    }
    val followingCount: LiveData<Long?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.followingCount
        }
    }
    val postCount: LiveData<Long?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.mediaCount
        }
    }
    val isPrivate: LiveData<Boolean?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.isPrivate
        }
    }
    val isVerified: LiveData<Boolean?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.isVerified
        }
    }
    val friendshipStatus: LiveData<FriendshipStatus?> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.friendshipStatus
        }
    }
    val profileContext: LiveData<Pair<String?, List<UserProfileContextLink>?>> = Transformations.map(profile) {
        return@map when (it.status) {
            Resource.Status.ERROR -> null to null
            Resource.Status.LOADING, Resource.Status.SUCCESS -> it.data?.profileContext to it.data?.profileContextLinksWithUserIds
        }
    }
}

@Suppress("UNCHECKED_CAST")
class ProfileFragmentViewModelFactory(
    private val csrfToken: String?,
    private val deviceUuid: String?,
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val storiesRepository: StoriesRepository,
    private val mediaRepository: MediaRepository,
    private val graphQLRepository: GraphQLRepository,
    private val favoriteRepository: FavoriteRepository,
    private val directMessagesRepository: DirectMessagesRepository,
    private val messageManager: DirectMessagesManager?,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        return ProfileFragmentViewModel(
            handle,
            csrfToken,
            deviceUuid,
            userRepository,
            friendshipRepository,
            storiesRepository,
            mediaRepository,
            graphQLRepository,
            favoriteRepository,
            directMessagesRepository,
            messageManager,
            Dispatchers.IO,
        ) as T
    }
}
