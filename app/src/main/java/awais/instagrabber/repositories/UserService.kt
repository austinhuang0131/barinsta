package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.FriendshipStatus
import awais.instagrabber.repositories.responses.UserSearchResponse
import awais.instagrabber.repositories.responses.WrappedUser
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("/api/v1/users/{uid}/info/")
    suspend fun getUserInfo(@Path("uid") uid: Long): WrappedUser

    @GET("/api/v1/users/{username}/usernameinfo/")
    suspend fun getUsernameInfo(@Path("username") username: String): WrappedUser

    @GET("/api/v1/friendships/show/{uid}/")
    suspend fun getUserFriendship(@Path("uid") uid: Long): FriendshipStatus

    @GET("/api/v1/users/search/")
    suspend fun search(
        @Query("timezone_offset") timezoneOffset: Float,
        @Query("q") query: String,
    ): UserSearchResponse
}