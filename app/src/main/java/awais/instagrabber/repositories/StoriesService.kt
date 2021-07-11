package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.stories.ArchiveResponse
import awais.instagrabber.repositories.responses.stories.ReelsMediaResponse
import awais.instagrabber.repositories.responses.stories.ReelsResponse
import awais.instagrabber.repositories.responses.stories.ReelsTrayResponse
import awais.instagrabber.repositories.responses.stories.StoryMediaResponse
import awais.instagrabber.repositories.responses.stories.StoryStickerResponse
import retrofit2.http.*

interface StoriesService {
    // this one is the same as MediaRepository.fetch BUT you need to make sure it's a story
    @GET("/api/v1/media/{mediaId}/info/")
    suspend fun fetch(@Path("mediaId") mediaId: Long): StoryMediaResponse

    @GET("/api/v1/feed/reels_tray/")
    suspend fun getFeedStories(): ReelsTrayResponse?

    @GET("/api/v1/highlights/{uid}/highlights_tray/")
    suspend fun fetchHighlights(@Path("uid") uid: Long): ReelsTrayResponse?

    @GET("/api/v1/archive/reel/day_shells/")
    suspend fun fetchArchive(@QueryMap queryParams: Map<String, String>): ArchiveResponse?

    @GET("/api/v1/feed/reels_media/")
    suspend fun getReelsMedia(@Query("user_ids") id: String): ReelsMediaResponse

    @GET("/api/v1/{type}/{id}/story/")
    suspend fun getStories(@Path("type") type: String, @Path("id") id: String): ReelsResponse

    @GET("/api/v1/feed/user/{id}/story/")
    suspend fun getUserStories(@Path("id") id: String): ReelsResponse

    @FormUrlEncoded
    @POST("/api/v1/media/{storyId}/{stickerId}/{action}/")
    suspend fun respondToSticker(
        @Path("storyId") storyId: Long,
        @Path("stickerId") stickerId: Long,
        @Path("action") action: String,  // story_poll_vote, story_question_response, story_slider_vote, story_quiz_answer
        @FieldMap form: Map<String, String>,
    ): StoryStickerResponse

    @FormUrlEncoded
    @POST("/api/v2/media/seen/")
    suspend fun seen(
        @QueryMap queryParams: Map<String, String>,
        @FieldMap form: Map<String, String>,
    ): String
}