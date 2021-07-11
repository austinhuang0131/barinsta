package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.LikersResponse
import awais.instagrabber.repositories.responses.MediaInfoResponse
import retrofit2.http.*

interface MediaService {
    @GET("/api/v1/media/{mediaId}/info/")
    suspend fun fetch(@Path("mediaId") mediaId: Long): MediaInfoResponse

    @GET("/api/v1/media/{mediaId}/{action}/")
    suspend fun fetchLikes(
        @Path("mediaId") mediaId: String, // one of "likers" or "comment_likers"
        @Path("action") action: String,
    ): LikersResponse

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/{action}/")
    suspend fun action(
        @Path("action") action: String,
        @Path("mediaId") mediaId: String,
        @FieldMap signedForm: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/edit_media/")
    suspend fun editCaption(
        @Path("mediaId") mediaId: String,
        @FieldMap signedForm: Map<String, String>,
    ): String

    @GET("/api/v1/language/translate/")
    suspend fun translate(@QueryMap form: Map<String, String>): String

    @FormUrlEncoded
    @POST("/api/v1/media/upload_finish/")
    suspend fun uploadFinish(
        @Header("retry_context") retryContext: String,
        @QueryMap queryParams: Map<String, String>,
        @FieldMap signedForm: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/delete/")
    suspend fun delete(
        @Path("mediaId") mediaId: String,
        @Query("media_type") mediaType: String,
        @FieldMap signedForm: Map<String, String>,
    ): String

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/archive/")
    suspend fun archive(
        @Path("mediaId") mediaId: String,
        @FieldMap signedForm: Map<String, String>,
    ): String
}