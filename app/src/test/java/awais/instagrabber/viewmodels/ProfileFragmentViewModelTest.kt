package awais.instagrabber.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import awais.instagrabber.repositories.FriendshipService
import awais.instagrabber.repositories.UserService
import awais.instagrabber.repositories.responses.*
import awais.instagrabber.webservices.FriendshipRepository
import awais.instagrabber.webservices.UserRepository
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ProfileFragmentViewModelTest {
    private val userService = object : UserService {
        override suspend fun getUserInfo(uid: Long): WrappedUser {
            TODO("Not yet implemented")
        }

        override suspend fun getUsernameInfo(username: String): WrappedUser {
            TODO("Not yet implemented")
        }

        override suspend fun getUserFriendship(uid: Long): FriendshipStatus {
            TODO("Not yet implemented")
        }

        override suspend fun search(timezoneOffset: Float, query: String): UserSearchResponse {
            TODO("Not yet implemented")
        }
    }

    private val friendshipService = object : FriendshipService {
        override suspend fun change(action: String, id: Long, form: Map<String, String>): FriendshipChangeResponse {
            TODO("Not yet implemented")
        }

        override suspend fun toggleRestrict(action: String, form: Map<String, String>): FriendshipRestrictResponse {
            TODO("Not yet implemented")
        }

        override suspend fun getList(userId: Long, type: String, queryParams: Map<String, String>): String {
            TODO("Not yet implemented")
        }

        override suspend fun changeMute(action: String, form: Map<String, String>): FriendshipChangeResponse {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun testNoUsernameNoCurrentUser() {
        val state = SavedStateHandle(
            mutableMapOf<String, Any>(
                "username" to ""
            )
        )
        val userRepository = UserRepository(userService)
        val friendshipRepository = FriendshipRepository(friendshipService)
        val viewModel = ProfileFragmentViewModel(state, userRepository, friendshipRepository)
    }
}