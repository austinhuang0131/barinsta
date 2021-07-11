package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.CommentsFetchResponse;
import awais.instagrabber.repositories.responses.ChildCommentsFetchResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface CommentRepository {
    @GET("/api/v1/media/{mediaId}/comments/")
    Call<CommentsFetchResponse> fetchComments(@Path("mediaId") final String mediaId,
                                              @QueryMap final Map<String, String> queryMap);

    @GET("/api/v1/media/{mediaId}/comments/{commentId}/inline_child_comments/")
    Call<ChildCommentsFetchResponse> fetchChildComments(@Path("mediaId") final String mediaId,
                                                        @Path("commentId") final String commentId,
                                                        @QueryMap final Map<String, String> queryMap);

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/comment/")
    Call<String> comment(@Path("mediaId") final String mediaId,
                         @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{mediaId}/comment/bulk_delete/")
    Call<String> commentsBulkDelete(@Path("mediaId") final String mediaId,
                                    @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{commentId}/comment_like/")
    Call<String> commentLike(@Path("commentId") final String commentId,
                             @FieldMap final Map<String, String> signedForm);

    @FormUrlEncoded
    @POST("/api/v1/media/{commentId}/comment_unlike/")
    Call<String> commentUnlike(@Path("commentId") final String commentId,
                               @FieldMap final Map<String, String> signedForm);

    @GET("/api/v1/language/translate/")
    Call<String> translate(@QueryMap final Map<String, String> form);
}
