package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.FriendshipChangeResponse
import awais.instagrabber.repositories.responses.FriendshipRestrictResponse
import retrofit2.http.*

interface FriendshipService {
    @FormUrlEncoded
    @POST("/api/v1/friendships/{action}/{id}/")
    suspend fun change(
        @Path("action") action: String,
        @Path("id") id: Long,
        @FieldMap form: Map<String, String>,
    ): FriendshipChangeResponse

    @FormUrlEncoded
    @POST("/api/v1/restrict_action/{action}/")
    suspend fun toggleRestrict(
        @Path("action") action: String,
        @FieldMap form: Map<String, String>,
    ): FriendshipRestrictResponse

    @GET("/api/v1/friendships/{userId}/{type}/")
    suspend fun getList(
        @Path("userId") userId: Long,
        @Path("type") type: String,  // following or followers
        @QueryMap(encoded = true) queryParams: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/friendships/{action}/")
    suspend fun changeMute(
        @Path("action") action: String,
        @FieldMap form: Map<String, String>,
    ): FriendshipChangeResponse
}