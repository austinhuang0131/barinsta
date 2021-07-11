package awais.instagrabber.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.MainCoroutineScopeRule
import awais.instagrabber.common.*
import awais.instagrabber.db.datasources.FavoriteDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.getOrAwaitValue
import awais.instagrabber.models.Resource
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.requests.StoryViewerOptions
import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.User
import awais.instagrabber.repositories.responses.stories.Story
import awais.instagrabber.webservices.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    private lateinit var testPublicUser: User
    private lateinit var testPublicUser1: User

    private val csrfToken = "csrfToken"
    private val deviceUuid = "deviceUuid"

    @Before
    fun setup() {
        testPublicUser = User(
            pk = 100,
            username = "test",
            fullName = "Test user"
        )
        testPublicUser1 = User(
            pk = 101,
            username = "test1",
            fullName = "Test1 user1"
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `no state username and null current user`() {
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            null,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        viewModel.setCurrentUser(Resource.success(null))
        assertNull(viewModel.profile.getOrAwaitValue().data)
        assertEquals("", viewModel.username.getOrAwaitValue())
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `no state username with current user provided`() {
        val viewModel = ProfileFragmentViewModel(
            SavedStateHandle(),
            csrfToken,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        assertNull(viewModel.profile.getOrAwaitValue().data)
        val user = User()
        viewModel.setCurrentUser(Resource.success(user))
        assertEquals(true, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(user, profile.data)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `state username without '@' and no current user`() {
        // username without `@`
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        testPublicUsernameNoCurrentUserCommon(state)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `state username with '@' and no current user`() {
        // username with `@`
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to "@${testPublicUser.username}"
            )
        )
        testPublicUsernameNoCurrentUserCommon(state)
    }

    @ExperimentalCoroutinesApi
    private fun testPublicUsernameNoCurrentUserCommon(state: SavedStateHandle) {
        val graphQLRepository = object : GraphQLRepository(GraphQLServiceAdapter()) {
            override suspend fun fetchUser(username: String): User = testPublicUser
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            null,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            graphQLRepository,
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `state username without '@' and current user provided`() {
        // username without `@`
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        testPublicUsernameCurrentUserCommon(state)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `state username with '@' and current user provided`() {
        // username with `@`
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to "@${testPublicUser.username}"
            )
        )
        testPublicUsernameCurrentUserCommon(state)
    }

    @ExperimentalCoroutinesApi
    private fun testPublicUsernameCurrentUserCommon(state: SavedStateHandle) {
        val friendshipStatus = FriendshipStatus(following = true)
        val userRepository = object : UserRepository(UserServiceAdapter()) {
            override suspend fun getUsernameInfo(username: String): User = testPublicUser
            override suspend fun getUserFriendship(uid: Long): FriendshipStatus = friendshipStatus
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            csrfToken,
            deviceUuid,
            userRepository,
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(User()))
        assertEquals(true, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
        assertEquals(friendshipStatus, profile.data?.friendshipStatus)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `state username changes`() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        val graphQLRepository = object : GraphQLRepository(GraphQLServiceAdapter()) {
            override suspend fun fetchUser(username: String): User = when (username) {
                testPublicUser.username -> testPublicUser
                testPublicUser1.username -> testPublicUser1
                else -> throw JSONException("")
            }
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            null,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            graphQLRepository,
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
        state.set("username", testPublicUser1.username)
        profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser1, profile.data)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `should update favorite in db if fetched user is a favorite`() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        val favorite = Favorite(
            1,
            testPublicUser.username,
            FavoriteType.USER,
            testPublicUser.username,
            "test url",
            LocalDateTime.now()
        )
        val graphQLRepository = object : GraphQLRepository(GraphQLServiceAdapter()) {
            override suspend fun fetchUser(username: String): User = testPublicUser
        }
        var updateFavoriteCalled = false
        val favoriteRepository = FavoriteRepository(FavoriteDataSource(object : FavoriteDaoAdapter() {
            override suspend fun findFavoriteByQueryAndType(query: String, type: FavoriteType): Favorite = favorite
            override suspend fun updateFavorites(vararg favorites: Favorite) {
                updateFavoriteCalled = true
            }
        }))
        val viewModel = ProfileFragmentViewModel(
            state,
            null,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            graphQLRepository,
            favoriteRepository,
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(true, viewModel.isFavorite.getOrAwaitValue())
        assertTrue(updateFavoriteCalled)
    }


    @ExperimentalCoroutinesApi
    @Test
    fun `should fetch user stories and highlights when logged in`() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        val testUserStories = Story()
        val testUserHighlights = listOf(Story())
        val userRepository = object : UserRepository(UserServiceAdapter()) {
            override suspend fun getUsernameInfo(username: String): User = testPublicUser
        }
        val storiesRepository = object : StoriesRepository(StoriesServiceAdapter()) {
            override suspend fun getStories(options: StoryViewerOptions): Story = testUserStories
            override suspend fun fetchHighlights(profileId: Long): List<Story> = testUserHighlights
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            csrfToken,
            deviceUuid,
            userRepository,
            FriendshipRepository(FriendshipServiceAdapter()),
            storiesRepository,
            MediaRepository(MediaServiceAdapter()),
            GraphQLRepository(GraphQLServiceAdapter()),
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(User()))
        assertEquals(true, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        var userStories = viewModel.userStories.getOrAwaitValue()
        while (userStories.status == Resource.Status.LOADING) {
            userStories = viewModel.userStories.getOrAwaitValue()
        }
        assertEquals(testUserStories, userStories.data)
        var userHighlights = viewModel.userHighlights.getOrAwaitValue()
        while (userHighlights.status == Resource.Status.LOADING) {
            userHighlights = viewModel.userHighlights.getOrAwaitValue()
        }
        assertEquals(testUserHighlights, userHighlights.data)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `should refresh correctly`() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any?>(
                "username" to testPublicUser.username
            )
        )
        val graphQLRepository = object : GraphQLRepository(GraphQLServiceAdapter()) {
            override suspend fun fetchUser(username: String): User = testPublicUser
        }
        val viewModel = ProfileFragmentViewModel(
            state,
            null,
            deviceUuid,
            UserRepository(UserServiceAdapter()),
            FriendshipRepository(FriendshipServiceAdapter()),
            StoriesRepository(StoriesServiceAdapter()),
            MediaRepository(MediaServiceAdapter()),
            graphQLRepository,
            FavoriteRepository(FavoriteDataSource(FavoriteDaoAdapter())),
            DirectMessagesRepository(DirectMessagesServiceAdapter()),
            null,
            coroutineScope.dispatcher,
        )
        viewModel.setCurrentUser(Resource.success(null))
        assertEquals(false, viewModel.isLoggedIn.getOrAwaitValue())
        var profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
        testPublicUser = testPublicUser.copy(biography = "new bio")
        viewModel.refresh()
        profile = viewModel.profile.getOrAwaitValue()
        while (profile.status == Resource.Status.LOADING) {
            profile = viewModel.profile.getOrAwaitValue()
        }
        assertEquals(testPublicUser, profile.data)
    }
}